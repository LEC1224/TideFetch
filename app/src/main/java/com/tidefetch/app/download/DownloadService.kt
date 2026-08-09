package com.tidefetch.app.download

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tidefetch.app.model.DownloadConfig
import com.tidefetch.app.model.DownloadPhase
import com.tidefetch.app.model.OutputFormat
import com.tidefetch.app.model.ResolutionPreset
import com.tidefetch.app.util.UrlTools
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class DownloadService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeSession = AtomicReference<DownloadSession?>(null)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> cancelActiveDownload(startId)
            ACTION_START -> startDownload(intent, startId)
            else -> stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        activeSession.get()?.let { session ->
            session.lifecycle.set(SESSION_CANCEL_REQUESTED)
            destroyProcessSafely(session.processId)
            session.job?.cancel()
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        val session = activeSession.get()
        if (session != null) {
            session.timedOut = true
            session.lifecycle.set(SESSION_CANCEL_REQUESTED)
            DownloadEvents.markError(
                "Android reached its long-running transfer limit. Try a shorter item or a lower resolution.",
            )
            destroyProcessSafely(session.processId)
            session.job?.cancel()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
    }

    private fun startDownload(intent: Intent, startId: Int) {
        // onStartCommand is serialized on the main thread. Dropped duplicate startIds are
        // accounted for by the accepted session's final unconditional stopSelf().
        if (activeSession.get() != null) {
            DownloadEvents.addLog("A download is already active; ignored a duplicate start request")
            return
        }
        val config = intent.toConfig()
        if (config == null) {
            DownloadEvents.markError("That link or output option is not valid.")
            stopSelf(startId)
            return
        }

        val session = DownloadSession(processId = UUID.randomUUID().toString())
        if (!activeSession.compareAndSet(null, session)) return
        DownloadEvents.begin()
        try {
            startForeground(
                DownloadNotifications.foregroundId,
                DownloadNotifications.active(this, "Preparing the download engine…"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } catch (error: Throwable) {
            activeSession.compareAndSet(session, null)
            DownloadEvents.markError("Android could not start the background download service.")
            DownloadEvents.addLog(error.message.orEmpty())
            stopSelf(startId)
            return
        }

        val job = serviceScope.launch { performDownload(config, session) }
        session.job = job
        if (session.lifecycle.get() == SESSION_CANCEL_REQUESTED) job.cancel()
    }

    private suspend fun performDownload(config: DownloadConfig, session: DownloadSession) {
        val stagingRoot = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir
        val jobsDirectory = File(stagingRoot, "jobs")
        val stagingDirectory = File(jobsDirectory, session.processId)
        val resultManifest = File(stagingDirectory, "completed-file.txt")
        try {
            cleanupOrphanedJobs(jobsDirectory, session.processId)
            check(stagingDirectory.mkdirs() || stagingDirectory.isDirectory) {
                "Could not create temporary download storage"
            }
            ensureSessionActive(session)
            DownloaderRuntime.ensureInitialized(this)
            ensureSessionActive(session)

            val request = FormatSelector.createRequest(config, stagingDirectory, resultManifest)
            DownloadEvents.addLog("Starting ${config.format.displayName}")
            notifyProgress("Connecting to the source…", null)

            val response = withTimeout(MAX_DOWNLOAD_DURATION_MILLIS) {
                runInterruptible(Dispatchers.IO) {
                    YoutubeDL.getInstance().execute(
                        request = request,
                        processId = session.processId,
                        // Keep stderr separate: the wrapper places it in the thrown
                        // YoutubeDLException, which preserves useful failure details.
                        redirectErrorStream = false,
                    ) { progress, etaSeconds, line ->
                        if (session.lifecycle.get() == SESSION_RUNNING &&
                            activeSession.get() === session
                        ) {
                            DownloadEvents.updateProgress(progress, etaSeconds, line)
                            val snapshot = DownloadEvents.state.value
                            notifyProgress(snapshot.status, snapshot.progress)
                        }
                    }
                }
            }
            response.err.takeIf(String::isNotBlank)?.let(DownloadEvents::addLog)
            check(response.exitCode == 0) { "yt-dlp exited with code ${response.exitCode}" }
            ensureSessionActive(session)

            val completedFile = locateCompletedFile(stagingDirectory, resultManifest)
            DownloadEvents.markSaving(completedFile.name)
            notifyProgress("Saving to your media library…", null)
            val published = MediaStoreWriter.publish(
                context = this,
                source = completedFile,
                requestedFormat = config.format,
                beforeCommit = {
                    // A cancel racing before MediaStore publication wins. Once this
                    // transition succeeds, TideFetch finishes the atomic publish.
                    if (!session.lifecycle.compareAndSet(
                            SESSION_RUNNING,
                            SESSION_COMMITTING,
                        )
                    ) {
                        throw CancellationException()
                    }
                },
            )
            DownloadEvents.markSuccess(
                published.uri,
                published.mimeType,
                published.displayName,
            )
            showCompletion(published)
        } catch (_: YoutubeDL.CanceledException) {
            if (!session.timedOut) DownloadEvents.markCanceled()
        } catch (_: TimeoutCancellationException) {
            DownloadEvents.markError(
                "This transfer ran for too long. Try a shorter item or a lower resolution.",
            )
        } catch (_: CancellationException) {
            if (!session.timedOut) DownloadEvents.markCanceled()
        } catch (error: Throwable) {
            val rawMessage = error.message.orEmpty().ifBlank { error::class.java.simpleName }
            DownloadEvents.addLog(rawMessage)
            DownloadEvents.markError(friendlyError(rawMessage))
        } finally {
            val removed = runCatching { stagingDirectory.deleteRecursively() }.getOrDefault(false)
            if (stagingDirectory.exists() && !removed) {
                DownloadEvents.addLog("Could not fully remove temporary files")
            }
            // Main-immediate serialization prevents a new onStartCommand from racing
            // between clearing the session and stopping the old started service.
            withContext(NonCancellable + Dispatchers.Main.immediate) {
                if (activeSession.get() === session) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    activeSession.set(null)
                    stopSelf()
                }
            }
        }
    }

    private fun cancelActiveDownload(startId: Int) {
        val session = activeSession.get()
        if (session == null) {
            stopSelf(startId)
            return
        }
        if (session.lifecycle.compareAndSet(SESSION_RUNNING, SESSION_CANCEL_REQUESTED)) {
            DownloadEvents.markCanceling()
            destroyProcessSafely(session.processId)
            session.job?.cancel()
        } else if (session.lifecycle.get() == SESSION_COMMITTING) {
            DownloadEvents.addLog("The file is already being published; finishing the save safely")
        }
    }

    private suspend fun ensureSessionActive(session: DownloadSession) {
        currentCoroutineContext().ensureActive()
        if (session.lifecycle.get() != SESSION_RUNNING) throw CancellationException()
    }

    private fun destroyProcessSafely(processId: String) {
        // 0.18.1 has a compound-map race in destroyProcessById; never let a
        // dependency-side NPE prevent our own coroutine cancellation/cleanup.
        runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }
            .onFailure { DownloadEvents.addLog("Could not signal the yt-dlp process: ${it.message}") }
    }

    private fun cleanupOrphanedJobs(jobsDirectory: File, currentId: String) {
        jobsDirectory.listFiles()
            ?.filter { it.name != currentId }
            ?.forEach { orphan ->
                if (!orphan.deleteRecursively() && orphan.exists()) {
                    DownloadEvents.addLog("Could not remove an old temporary download")
                }
            }
    }

    private fun locateCompletedFile(stagingDirectory: File, manifest: File): File {
        val stagingPath = stagingDirectory.canonicalFile.toPath()
        val reportedFile = manifest.takeIf(File::isFile)
            ?.readLines()
            ?.asReversed()
            ?.asSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.map { File(it) }
            ?.firstOrNull { candidate ->
                runCatching {
                    candidate.isFile && candidate.canonicalFile.toPath().startsWith(stagingPath)
                }.getOrDefault(false)
            }
        if (reportedFile != null) return reportedFile

        return stagingDirectory.walkTopDown()
            .filter(File::isFile)
            .filterNot { file ->
                file == manifest || file.name.endsWith(".part") ||
                    file.name.endsWith(".ytdl") || file.name.contains(".temp.")
            }
            .maxByOrNull(File::lastModified)
            ?: error("yt-dlp finished, but no completed media file was found")
    }

    @SuppressLint("MissingPermission") // Guarded by canPostNotifications immediately below.
    private fun notifyProgress(status: String, progress: Float?) {
        if (!canPostNotifications()) return
        runCatching {
            NotificationManagerCompat.from(this).notify(
                DownloadNotifications.foregroundId,
                DownloadNotifications.active(this, status, progress),
            )
        }
    }

    @SuppressLint("MissingPermission") // Guarded by canPostNotifications immediately below.
    private fun showCompletion(media: PublishedMedia) {
        if (!canPostNotifications()) return
        runCatching {
            getSystemService(NotificationManager::class.java).notify(
                DownloadNotifications.completionId,
                DownloadNotifications.completed(
                    this,
                    media.displayName,
                    media.uri,
                    media.mimeType,
                ),
            )
        }
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun friendlyError(raw: String): String {
        val message = raw.lowercase()
        return when {
            "unsupported url" in message ->
                "This site or link type is not supported by the bundled yt-dlp version."
            "requested format is not available" in message ->
                "That resolution or format is not available for this link. Try Original or another format."
            "private" in message || "login" in message || "cookies" in message ||
                "sign in" in message ->
                "This media needs an authenticated account or is not publicly available."
            "network" in message || "timed out" in message || "connection" in message ||
                "unable to download" in message ->
                "The source could not be reached. Check your connection and try again."
            "no space" in message || "enospc" in message ->
                "There is not enough free storage to finish this download."
            else -> "yt-dlp could not complete this download. Open Technical details for the exact log."
        }
    }

    private fun Intent.toConfig(): DownloadConfig? {
        val url = getStringExtra(EXTRA_URL)?.trim()?.takeIf(UrlTools::isValidWebUrl) ?: return null
        val resolution = getStringExtra(EXTRA_RESOLUTION)
            ?.let { stored -> ResolutionPreset.entries.firstOrNull { it.name == stored } }
            ?: return null
        val format = getStringExtra(EXTRA_FORMAT)
            ?.let { stored -> OutputFormat.entries.firstOrNull { it.name == stored } }
            ?: return null
        return DownloadConfig(url, resolution, format)
    }

    private class DownloadSession(val processId: String) {
        val lifecycle = AtomicInteger(SESSION_RUNNING)
        @Volatile var job: Job? = null
        @Volatile var timedOut: Boolean = false
    }

    companion object {
        const val ACTION_START = "com.tidefetch.app.action.START_DOWNLOAD"
        const val ACTION_CANCEL = "com.tidefetch.app.action.CANCEL_DOWNLOAD"
        const val EXTRA_URL = "url"
        const val EXTRA_RESOLUTION = "resolution"
        const val EXTRA_FORMAT = "format"

        private const val SESSION_RUNNING = 0
        private const val SESSION_CANCEL_REQUESTED = 1
        private const val SESSION_COMMITTING = 2
        private const val MAX_DOWNLOAD_DURATION_MILLIS = 5L * 60L * 60L * 1_000L
    }
}
