package com.tidefetch.app.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tidefetch.app.R
import com.tidefetch.app.data.ThemeMode
import com.tidefetch.app.model.DownloadPhase
import com.tidefetch.app.model.DownloadSnapshot
import com.tidefetch.app.model.OutputFormat
import com.tidefetch.app.model.ResolutionPreset
import com.tidefetch.app.ui.theme.TideFetchTheme
import com.tidefetch.app.util.UrlTools
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun TideFetchRoute(viewModel: TideFetchViewModel) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    TideFetchTheme(themeMode) {
        val url by viewModel.url.collectAsStateWithLifecycle()
        val resolution by viewModel.resolution.collectAsStateWithLifecycle()
        val format by viewModel.format.collectAsStateWithLifecycle()
        val download by viewModel.download.collectAsStateWithLifecycle()
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        var showThemeDialog by remember { mutableStateOf(false) }

        LaunchedEffect(viewModel) {
            viewModel.notices.collectLatest { notice ->
                val result = snackbar.showSnackbar(
                    message = notice.message,
                    actionLabel = if (notice.canUndoClipboard) "Undo" else null,
                )
                if (notice.canUndoClipboard &&
                    result == androidx.compose.material3.SnackbarResult.ActionPerformed
                ) {
                    viewModel.undoClipboardFill()
                }
            }
        }

        val notificationPermission = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (!granted) {
                scope.launch {
                    snackbar.showSnackbar(
                        "Notifications are off. The download will still continue in the background.",
                    )
                }
            }
            viewModel.startDownload()
        }
        val requestDownload = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.startDownload()
            }
        }

        TideFetchScreen(
            url = url,
            resolution = resolution,
            format = format,
            download = download,
            snackbarHostState = snackbar,
            onUrlChange = viewModel::setUrl,
            onResolutionChange = viewModel::setResolution,
            onFormatChange = viewModel::setFormat,
            onDownload = requestDownload,
            onCancel = viewModel::cancelDownload,
            onClearLogs = viewModel::clearLogs,
            onReset = { viewModel.resetOutcome(clearUrl = true) },
            onOpen = {
                download.outputUri?.let {
                    if (!openMedia(context, it, download.outputMimeType)) {
                        scope.launch { snackbar.showSnackbar("No app could open this media format.") }
                    }
                }
            },
            onShare = { download.outputUri?.let { shareMedia(context, it, download.outputMimeType) } },
            onChooseTheme = { showThemeDialog = true },
        )

        if (showThemeDialog) {
            ThemeDialog(
                selected = themeMode,
                onSelect = {
                    viewModel.setThemeMode(it)
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false },
            )
        }
    }
}

@Composable
private fun TideFetchScreen(
    url: String,
    resolution: ResolutionPreset,
    format: OutputFormat,
    download: DownloadSnapshot,
    snackbarHostState: SnackbarHostState,
    onUrlChange: (String) -> Unit,
    onResolutionChange: (ResolutionPreset) -> Unit,
    onFormatChange: (OutputFormat) -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onClearLogs: () -> Unit,
    onReset: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onChooseTheme: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val pageGradient = Brush.verticalGradient(
        listOf(colors.primaryContainer.copy(alpha = 0.34f), colors.background, colors.background),
    )
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // The first gradient stop is intentionally translucent. Give it a
        // resolved-theme base so a forced app theme never exposes the
        // system-selected window background beneath the status bar.
        containerColor = colors.background,
    ) { scaffoldPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(pageGradient)
                .padding(scaffoldPadding),
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { Header(onChooseTheme) }
                item {
                    InputCard(
                        url = url,
                        enabled = !download.isActive,
                        onUrlChange = onUrlChange,
                    )
                }
                item {
                    OptionsCard(
                        resolution = resolution,
                        format = format,
                        enabled = !download.isActive,
                        onResolutionChange = onResolutionChange,
                        onFormatChange = onFormatChange,
                    )
                }
                item {
                    if (download.isActive) {
                        ActiveDownloadCard(download, onCancel)
                    } else {
                        Button(
                            onClick = onDownload,
                            enabled = UrlTools.isValidWebUrl(url),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                            shape = RoundedCornerShape(17.dp),
                        ) {
                            Icon(Icons.Outlined.Download, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Download to media library", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (!download.isActive && download.phase != DownloadPhase.IDLE) {
                    item {
                        OutcomeCard(
                            download,
                            onOpen,
                            onShare,
                            onReset,
                            onDownload,
                            canRetry = UrlTools.isValidWebUrl(url),
                        )
                    }
                }
                item {
                    TechnicalDetailsCard(
                        logs = download.logs,
                        expandForError = download.phase == DownloadPhase.ERROR,
                        onClearLogs = onClearLogs,
                    )
                }
                item {
                    Text(
                        text = "Download only media you own or have permission to save. " +
                            "Some sites may require an authenticated session that TideFetch does not access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(onChooseTheme: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.tidefetch_mark),
                contentDescription = null,
                modifier = Modifier.size(54.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "TideFetch",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Bring media ashore.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onChooseTheme) {
            Icon(Icons.Outlined.Settings, contentDescription = "Appearance settings")
        }
    }
}

@Composable
private fun InputCard(
    url: String,
    enabled: Boolean,
    onUrlChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val platform = UrlTools.platformLabel(url)
    val invalidUrl = url.isNotBlank() && !UrlTools.isValidWebUrl(url)
    AppCard {
        Text("Media link", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Paste a public post or video link from YouTube, Facebook, X, and other supported sites.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            label = { Text("URL") },
            placeholder = { Text("https://…") },
            leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
            trailingIcon = {
                Row {
                    if (url.isNotBlank()) {
                        IconButton(onClick = { onUrlChange("") }, enabled = enabled) {
                            Icon(Icons.Outlined.Clear, contentDescription = "Clear link")
                        }
                    } else {
                        IconButton(
                            enabled = enabled,
                            onClick = {
                                val clipboard = context.getSystemService(ClipboardManager::class.java)
                                val text = clipboard.primaryClip
                                    ?.takeIf { it.itemCount > 0 }
                                    ?.getItemAt(0)
                                    ?.coerceToText(context)
                                UrlTools.firstWebUrl(text)?.let(onUrlChange)
                            },
                        ) {
                            Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste link")
                        }
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            maxLines = 3,
            isError = invalidUrl,
            supportingText = if (invalidUrl) {
                { Text("Enter a complete http or https link") }
            } else {
                null
            },
            shape = RoundedCornerShape(16.dp),
        )
        if (platform != null && UrlTools.isValidWebUrl(url)) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(platform, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun OptionsCard(
    resolution: ResolutionPreset,
    format: OutputFormat,
    enabled: Boolean,
    onResolutionChange: (ResolutionPreset) -> Unit,
    onFormatChange: (OutputFormat) -> Unit,
) {
    var resolutionExpanded by remember { mutableStateOf(false) }
    AppCard {
        Text("Save as", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            OutputFormat.entries.forEach { choice ->
                FilterChip(
                    selected = choice == format,
                    onClick = { onFormatChange(choice) },
                    enabled = enabled,
                    label = { Text(choice.shortName) },
                    leadingIcon = {
                        Icon(
                            if (choice.isAudioOnly) Icons.Outlined.AudioFile else Icons.Outlined.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }

        AnimatedVisibility(!format.isAudioOnly) {
            Column {
                Spacer(Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = resolutionExpanded,
                    onExpandedChange = { if (enabled) resolutionExpanded = !resolutionExpanded },
                ) {
                    OutlinedTextField(
                        value = resolution.displayName,
                        onValueChange = {},
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled)
                            .fillMaxWidth(),
                        readOnly = true,
                        enabled = enabled,
                        label = { Text("Resolution") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(resolutionExpanded)
                        },
                        shape = RoundedCornerShape(16.dp),
                    )
                    ExposedDropdownMenu(
                        expanded = resolutionExpanded,
                        onDismissRequest = { resolutionExpanded = false },
                    ) {
                        ResolutionPreset.entries.forEach { choice ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(choice.displayName) },
                                onClick = {
                                    onResolutionChange(choice)
                                    resolutionExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = when (format) {
                OutputFormat.MP4_H264 ->
                    "Default: best available resolution with H.264 video and AAC audio preferred."
                OutputFormat.MP4_SOURCE ->
                    "Keeps source codecs where MP4 supports them; incompatible sources may fail to remux."
                OutputFormat.WEBM ->
                    "Keeps high-quality WebM streams, commonly VP9/AV1 with Opus audio."
                OutputFormat.MP3 -> "Extracts audio locally as a high-quality variable-bitrate MP3."
                OutputFormat.WAV -> "Extracts uncompressed PCM audio. WAV files can be large."
                OutputFormat.M4A -> "Keeps or converts the best audio stream into an M4A container."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActiveDownloadCard(download: DownloadSnapshot, onCancel: () -> Unit) {
    AppCard(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    download.status,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = download.status
                    },
                )
                download.etaSeconds?.takeIf { it > 0 }?.let {
                    Text(
                        "About ${formatEta(it)} remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (download.progress != null) {
            LinearProgressIndicator(
                progress = { download.progress ?: 0f },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "${(download.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(14.dp))
        OutlinedButton(
            onClick = onCancel,
            enabled = download.phase != DownloadPhase.SAVING,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Cancel, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (download.phase == DownloadPhase.SAVING) "Finishing save…" else "Cancel")
        }
    }
}

@Composable
private fun OutcomeCard(
    download: DownloadSnapshot,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onReset: () -> Unit,
    onRetry: () -> Unit,
    canRetry: Boolean,
) {
    val success = download.phase == DownloadPhase.SUCCESS
    val error = download.phase == DownloadPhase.ERROR
    AppCard(
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when {
                    success -> Icons.Outlined.CheckCircle
                    error -> Icons.Outlined.ErrorOutline
                    else -> Icons.Outlined.Cancel
                },
                contentDescription = null,
                tint = when {
                    success -> MaterialTheme.colorScheme.tertiary
                    error -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(download.status, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val detail = when {
                    success -> download.fileName
                    error -> download.errorMessage
                    else -> "No file was saved."
                }
                detail?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (error) 5 else 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (error) {
                    download.errorSuggestion?.let { suggestion ->
                        Spacer(Modifier.height(5.dp))
                        Text(
                            suggestion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (success) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Open")
                }
                OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Share, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Share")
                }
            }
            TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Text("Download another")
            }
        } else if (error) {
            Button(
                onClick = onRetry,
                enabled = canRetry,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Try again")
            }
        } else {
            TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun TechnicalDetailsCard(
    logs: List<String>,
    expandForError: Boolean,
    onClearLogs: () -> Unit,
) {
    var expanded by remember(expandForError) { mutableStateOf(expandForError) }
    val logScroll = rememberScrollState()
    LaunchedEffect(logs.size, expanded) {
        if (expanded) logScroll.scrollTo(logScroll.maxValue)
    }
    AppCard(contentPadding = PaddingValues(0.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .semantics {
                    stateDescription = if (expanded) "Expanded" else "Collapsed"
                }
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Technical details", fontWeight = FontWeight.SemiBold)
                Text(
                    if (logs.isEmpty()) {
                        "Engine output will appear here"
                    } else {
                        "${logs.size} diagnostic lines"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Collapse log" else "Expand log",
            )
        }
        AnimatedVisibility(expanded) {
            Column {
                HorizontalDivider()
                if (logs.isEmpty()) {
                    Text(
                        "The yt-dlp and FFmpeg log is kept out of the way until you need it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp),
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 260.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .verticalScroll(logScroll)
                            .padding(14.dp),
                    ) {
                        Text(
                            logs.joinToString("\n"),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                        )
                    }
                    LogActions(logs.joinToString("\n"), onClearLogs)
                }
            }
        }
    }
}

@Composable
private fun LogActions(logText: String, onClearLogs: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(
            onClick = {
                context.getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText("TideFetch log", logText))
            },
        ) {
            Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Copy")
        }
        TextButton(onClick = onClearLogs) {
            Icon(Icons.Outlined.Clear, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Clear")
        }
    }
}

@Composable
private fun ThemeDialog(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appearance") },
        text = {
            Column(
                Modifier
                    .selectableGroup()
                    .verticalScroll(rememberScrollState()),
            ) {
                ThemeMode.entries.forEach { mode ->
                    val icon: ImageVector = when (mode) {
                        ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
                        ThemeMode.LIGHT -> Icons.Outlined.LightMode
                        ThemeMode.DARK -> Icons.Outlined.DarkMode
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = mode == selected,
                                onClick = { onSelect(mode) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(icon, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(mode.displayName, Modifier.weight(1f))
                        RadioButton(selected = mode == selected, onClick = null)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun AppCard(
    modifier: Modifier = Modifier,
    colors: androidx.compose.material3.CardColors = CardDefaults.cardColors(),
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = colors,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

private fun formatEta(seconds: Long): String = when {
    seconds < 60 -> "${seconds}s"
    seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
    else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
}

private fun openMedia(context: Context, uri: android.net.Uri, mimeType: String?): Boolean {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching { context.startActivity(intent) }.isSuccess
}

private fun shareMedia(context: Context, uri: android.net.Uri, mimeType: String?) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType ?: "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, "TideFetch media", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share media"))
}
