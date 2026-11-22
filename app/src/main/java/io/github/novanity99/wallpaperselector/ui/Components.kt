package io.github.novanity99.wallpaperselector.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.novanity99.wallpaperselector.App
import io.github.novanity99.wallpaperselector.data.CatalogEntry
import io.github.novanity99.wallpaperselector.data.SettingsState
import io.github.novanity99.wallpaperselector.data.WallpaperDef
import io.github.novanity99.wallpaperselector.data.WallpaperItem
import io.github.novanity99.wallpaperselector.service.LiveWallpaperService

val Context.app get() = applicationContext as App

fun formatSize(bytes: Long): String =
    if (bytes < 1024 * 1024) "${(bytes + 1023) / 1024} KB" else String.format("%.1f MB", bytes / 1048576.0)

/** Opens Android's live wallpaper preview with our service preselected; the user taps "Set wallpaper" there. */
fun openSystemWallpaperPreview(context: Context) {
    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, LiveWallpaperService::class.java))
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            context.startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            Toast.makeText(context, "Choose \"Wallpaper Selector\" in the list", Toast.LENGTH_LONG).show()
        } catch (e2: Exception) {
            Toast.makeText(context, "This device doesn't support live wallpapers", Toast.LENGTH_LONG).show()
        }
    }
}

/** Static thumbnail; also sits underneath live previews while they start up. */
@Composable
fun Thumbnail(entry: CatalogEntry, modifier: Modifier = Modifier) {
    val repo = LocalContext.current.app.repository
    val bitmap by produceState<ImageBitmap?>(null, entry.id, entry.version) {
        value = repo.loadThumbnail(entry)?.asImageBitmap()
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        bitmap?.let {
            Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

/** Loads what the renderer needs for [item]; null while loading or when it can't be fetched. */
@Composable
fun rememberPreviewDef(item: WallpaperItem): WallpaperDef? {
    val repo = LocalContext.current.app.repository
    val def by produceState<WallpaperDef?>(null, item.entry.id, item.entry.version, item.localVersion, item.downloaded) {
        value = repo.loadForPreview(item.entry)
    }
    return def
}

@Composable
private fun Badge(icon: ImageVector, description: String?, modifier: Modifier = Modifier) {
    Box(modifier.size(28.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun WallpaperCard(
    item: WallpaperItem,
    live: Boolean,
    settings: SettingsState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    Card(onClick = onClick, shape = RoundedCornerShape(18.dp), modifier = modifier.aspectRatio(0.62f)) {
        Box(Modifier.fillMaxSize()) {
            Thumbnail(item.entry, Modifier.fillMaxSize())
            if (live) {
                val customs by LocalContext.current.app.settings.customs.collectAsStateWithLifecycle()
                val custom = customs[item.entry.id]
                val base = rememberPreviewDef(item)
                remember(base, custom) { base?.customized(custom) }?.let {
                    // Small and capped at 30 fps: a screen full of previews stays cheap.
                    LivePreview(it, Modifier.fillMaxSize(), fps = 30, speed = settings.speed, scale = 0.6f)
                }
            }
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                    .padding(start = 12.dp, end = 12.dp, top = 28.dp, bottom = 10.dp),
            ) {
                Column {
                    Text(
                        item.entry.name, color = Color.White, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (item.bundled && !item.downloaded) "${item.entry.category} · Built in" else item.entry.category,
                        color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall, maxLines = 1,
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                if (item.entry.interactive) Badge(Icons.Rounded.TouchApp, "Reacts to touch") else Box(Modifier.size(28.dp))
                val progress = item.progress
                when {
                    progress != null -> Box(
                        Modifier.size(28.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            progress = { progress }, modifier = Modifier.size(18.dp), color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f), strokeWidth = 2.dp,
                        )
                    }
                    onDelete != null -> IconButton(
                        onClick = onDelete, modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.45f), contentColor = Color.White),
                    ) { Icon(Icons.Rounded.Delete, "Delete ${item.entry.name}", Modifier.size(18.dp)) }
                    !item.available -> Badge(Icons.Rounded.Download, "Not downloaded")
                }
            }
        }
    }
}

@Composable
fun WallpaperGrid(
    sections: List<Pair<String?, List<WallpaperItem>>>,
    live: Boolean,
    settings: SettingsState,
    onOpen: (WallpaperItem) -> Unit,
    modifier: Modifier = Modifier,
    onDelete: ((WallpaperItem) -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        sections.forEach { (title, list) ->
            if (title != null) {
                item(key = "header-$title", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Bottom) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "  ${list.size}", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(list, key = { it.entry.id }, contentType = { "card" }) { item ->
                WallpaperCard(
                    item, live, settings, onClick = { onOpen(item) },
                    modifier = Modifier.clip(RoundedCornerShape(18.dp)),
                    onDelete = if (onDelete != null && item.downloaded) ({ onDelete(item) }) else null,
                )
            }
        }
    }
}
