package com.tidefetch.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.tidefetch.app.ui.TideFetchRoute
import com.tidefetch.app.ui.TideFetchViewModel

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<TideFetchViewModel>()
    private var clipboardChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent { TideFetchRoute(viewModel) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!clipboardChecked) {
            clipboardChecked = true
            val clipboard = getSystemService(ClipboardManager::class.java)
            val text = clipboard.primaryClip
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.coerceToText(this)
            viewModel.considerClipboard(text)
        }
    }

    override fun onStop() {
        // A later foreground visit is a new opportunity to pick up a newly
        // copied link, while the ViewModel still prevents overwriting text.
        clipboardChecked = false
        super.onStop()
    }

    private fun handleIntent(intent: Intent?) {
        when {
            intent?.action == Intent.ACTION_SEND && intent.type == "text/plain" -> {
                viewModel.acceptSharedText(intent.getStringExtra(Intent.EXTRA_TEXT))
            }
            intent?.action == ACTION_OPEN_MEDIA -> openDownloadedMedia(intent)
        }
    }

    private fun openDownloadedMedia(source: Intent) {
        val uri = source.data ?: return
        val viewer = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, source.type ?: "*/*")
            clipData = ClipData.newUri(contentResolver, "TideFetch media", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(viewer) }
            .onFailure { viewModel.reportMediaOpenFailure() }
    }

    companion object {
        const val ACTION_OPEN_MEDIA = "com.tidefetch.app.action.OPEN_MEDIA"
    }
}
