package io.github.novanity99.wallpaperselector.ui

import android.app.Activity
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.novanity99.wallpaperselector.data.Custom
import io.github.novanity99.wallpaperselector.data.SettingsState
import io.github.novanity99.wallpaperselector.data.WallpaperItem

@Composable
fun DetailScreen(item: WallpaperItem, settings: SettingsState, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = context.app.repository
    val def = rememberPreviewDef(item)
    val customs by context.app.settings.customs.collectAsStateWithLifecycle()
    val custom = customs[item.entry.id] ?: Custom()
    val shownDef = remember(def, custom) { def?.customized(custom) }
    var customizing by rememberSaveable { mutableStateOf(false) }
    var showUi by rememberSaveable { mutableStateOf(true) }
    var askTarget by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }

    // The preview is usually dark, so use light status bar icons while it is open.
    val view = LocalView.current
    val dark = LocalDarkTheme.current
    DisposableEffect(dark) {
        val controller = WindowCompat.getInsetsController((view.context as Activity).window, view)
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = !dark }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Thumbnail(item.entry, Modifier.fillMaxSize())
        if (shownDef != null) {
            LivePreview(shownDef, Modifier.fillMaxSize(), interactive = true, fps = settings.fps, speed = settings.speed)
        }

        val overlayButton = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.45f), contentColor = Color.White)
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack, colors = overlayButton) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            IconButton(onClick = { showUi = !showUi }, colors = overlayButton) {
                Icon(if (showUi) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (showUi) "Hide details" else "Show details")
            }
        }

        AnimatedVisibility(
            visible = showUi,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            ) {
                if (customizing && def != null) {
                    CustomizePanel(
                        base = def, interactive = item.entry.interactive, custom = custom,
                        onChange = { context.app.settings.setCustom(item.entry.id, it) },
                        onDone = { customizing = false },
                    )
                } else Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(20.dp)) {
                    Text(item.entry.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        buildString {
                            append(item.entry.category).append(" · ").append(formatSize(item.entry.size))
                            if (item.entry.interactive) append(" · Reacts to touch")
                            append(
                                when {
                                    item.downloaded -> " · Downloaded"
                                    item.bundled -> " · Built in"
                                    else -> ""
                                },
                            )
                        },
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    if (item.entry.description.isNotBlank()) {
                        Text(item.entry.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                    }
                    if (def == null && !item.available) {
                        Text(
                            "The live preview needs an internet connection.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    val progress = item.progress
                    if (progress != null) {
                        Text("Downloading… ${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp))
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (!item.available) {
                                Button(onClick = { repo.download(item.entry) }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.Download, null, Modifier.size(18.dp))
                                    Text("  Download")
                                }
                            } else if (item.updateAvailable) {
                                FilledTonalButton(onClick = { repo.download(item.entry) }) { Text("Update") }
                            }
                            if (item.available) {
                                Button(onClick = { askTarget = true }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.Wallpaper, null, Modifier.size(18.dp))
                                    Text("  Set as Wallpaper")
                                }
                            } else {
                                OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.weight(1f)) { Text("Set as Wallpaper") }
                            }
                            if (item.downloaded) {
                                IconButton(onClick = { askDelete = true }) { Icon(Icons.Rounded.Delete, "Delete download") }
                            }
                        }
                        if (def != null) {
                            OutlinedButton(onClick = { customizing = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Icon(Icons.Rounded.Tune, null, Modifier.size(18.dp))
                                Text(if (custom != Custom()) "  Customize (changed)" else "  Customize")
                            }
                        }
                        if (!item.available) {
                            Text(
                                "Download it first, then you can set it as your wallpaper, even offline.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (askTarget) {
        TargetDialog(
            name = item.entry.name,
            onDismiss = { askTarget = false },
            onConfirm = { home, lock ->
                askTarget = false
                context.app.settings.selectWallpaper(item.entry.id, home, lock)
                openSystemWallpaperPreview(context)
            },
        )
    }
    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            title = { Text("Delete ${item.entry.name}?") },
            text = { Text(if (item.bundled) "The downloaded update is removed. The built-in version stays." else "It's removed from this phone. You can download it again any time.") },
            confirmButton = { TextButton(onClick = { askDelete = false; repo.delete(item.entry.id) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { askDelete = false }) { Text("Cancel") } },
        )
    }
}

/** Home / Lock / Both. Separate lock screen live wallpapers exist from Android 14 (API 34). */
@Composable
private fun TargetDialog(name: String, onDismiss: () -> Unit, onConfirm: (home: Boolean, lock: Boolean) -> Unit) {
    val lockSupported = Build.VERSION.SDK_INT >= 34
    var choice by rememberSaveable { mutableIntStateOf(if (lockSupported) 2 else 0) }
    val options = listOf("Home screen", "Lock screen", "Home and lock screens")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Use $name on") },
        text = {
            Column {
                options.forEachIndexed { i, label ->
                    val enabled = i == 0 || lockSupported
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.RadioButton) { choice = i }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = choice == i, onClick = null, enabled = enabled)
                        Text(
                            label, modifier = Modifier.padding(start = 12.dp),
                            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        )
                    }
                }
                Text(
                    if (lockSupported) {
                        "Android shows a preview next. Tap \"Set wallpaper\" there and pick the same screen."
                    } else {
                        "Android shows a preview next. Tap \"Set wallpaper\" there. A separate lock screen live wallpaper needs Android 14 or newer; on this phone the lock screen follows the home screen."
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(choice != 1, choice != 0) }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
