package io.github.novanity99.wallpaperselector.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.novanity99.wallpaperselector.BuildConfig
import io.github.novanity99.wallpaperselector.data.CATEGORIES
import io.github.novanity99.wallpaperselector.data.CatalogStatus
import io.github.novanity99.wallpaperselector.data.SettingsState
import io.github.novanity99.wallpaperselector.data.THEME_DARK
import io.github.novanity99.wallpaperselector.data.THEME_LIGHT
import io.github.novanity99.wallpaperselector.data.THEME_SYSTEM
import io.github.novanity99.wallpaperselector.data.WallpaperItem
import kotlin.math.roundToInt

@Composable
private fun ScreenTitle(text: String) {
    Text(
        text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    items: List<WallpaperItem>,
    status: CatalogStatus,
    settings: SettingsState,
    live: Boolean,
    onRefresh: () -> Unit,
    onOpen: (WallpaperItem) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current

    val sections = remember(items, query, category) {
        val q = query.trim()
        val filtered = items.filter {
            (category == null || it.entry.category == category) &&
                (q.isEmpty() || it.entry.name.contains(q, true) || it.entry.category.contains(q, true) ||
                    it.entry.description.contains(q, true))
        }
        val known = CATEGORIES.map { c -> c to filtered.filter { it.entry.category == c } }
        val other = filtered.filter { it.entry.category !in CATEGORIES }
        (known + ("Other" to other)).filter { it.second.isNotEmpty() }.map { (t, l) -> (t as String?) to l }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Wallpaper Selector")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search wallpapers") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear search") }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = category == null, onClick = { category = null }, label = { Text("All") })
            CATEGORIES.forEach { c ->
                FilterChip(selected = category == c, onClick = { category = if (category == c) null else c }, label = { Text(c) })
            }
        }
        UpdateCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), compact = true)
        if (status == CatalogStatus.Offline) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CloudOff, null)
                    Text(
                        LocalContext.current.app.repository.offlineReason + " Showing the wallpapers on this phone.",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                    )
                    TextButton(onClick = onRefresh) { Text("Retry") }
                }
            }
        }
        PullToRefreshBox(isRefreshing = status == CatalogStatus.Loading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
            if (sections.isEmpty()) {
                EmptyMessage(if (items.isEmpty()) "No wallpapers yet" else "No wallpapers match your search")
            } else {
                WallpaperGrid(sections, live, settings, onOpen, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    // verticalScroll keeps pull-to-refresh working on an empty list
    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(48.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MyWallpapersScreen(
    items: List<WallpaperItem>,
    settings: SettingsState,
    live: Boolean,
    onOpen: (WallpaperItem) -> Unit,
    onDelete: (WallpaperItem) -> Unit,
) {
    val mine = remember(items) { items.filter { it.available } }
    var confirm by remember { mutableStateOf<WallpaperItem?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("My Wallpapers")
        Text(
            "${mine.size} on this phone, ready to use offline",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        WallpaperGrid(listOf(null to mine), live, settings, onOpen, Modifier.fillMaxSize(), onDelete = { confirm = it })
    }

    confirm?.let { item ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Delete ${item.entry.name}?") },
            text = {
                Text(
                    if (item.bundled) "The downloaded update is removed. The built-in version stays."
                    else "It's removed from this phone. You can download it again any time.",
                )
            },
            confirmButton = { TextButton(onClick = { onDelete(item); confirm = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingBlock(title: String, description: String? = null, trailing: (@Composable () -> Unit)? = null, content: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (trailing != null) {
                Spacer(Modifier.padding(start = 12.dp))
                trailing()
            }
        }
        if (content != null) {
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Choice(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = value == selected, onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label) }
        }
    }
}

@Composable
fun SettingsScreen(settings: SettingsState) {
    val store = LocalContext.current.app.settings
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTitle("Settings")
        Column(Modifier.widthIn(max = 640.dp)) {
            var speed by remember(settings.speed) { mutableFloatStateOf(settings.speed) }
            SettingBlock("Animation speed", "${"%.2f".format(speed)}× · applies to the wallpaper and previews") {
                Slider(
                    value = speed,
                    onValueChange = { speed = (it * 20).roundToInt() / 20f },
                    onValueChangeFinished = { store.setSpeed(speed) },
                    valueRange = 0.25f..2f,
                )
            }
            SettingBlock("Frame rate limit", "30 fps uses roughly half the power of 60 fps.") {
                Choice(listOf(30 to "30 fps", 60 to "60 fps"), settings.fps, store::setFps)
            }
            SettingBlock(
                "Battery saver",
                "Freeze the wallpaper when the battery is at 15% or lower, or when Android's Battery Saver is on. " +
                    "The wallpaper always stops drawing while the screen is off or it's hidden behind an app.",
                trailing = { Switch(checked = settings.batterySaver, onCheckedChange = store::setBatterySaver) },
            )
            SettingBlock(
                "Live previews in lists",
                "Turn off to show still thumbnails in the grid instead.",
                trailing = { Switch(checked = settings.livePreviews, onCheckedChange = store::setLivePreviews) },
            )
            UpdateCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            SettingBlock(
                "Check for updates automatically",
                "Looks for a newer build on GitHub each time the app starts.",
                trailing = { Switch(checked = settings.autoUpdateCheck, onCheckedChange = store::setAutoUpdateCheck) },
            )
            SettingBlock("Theme") {
                Choice(listOf(THEME_SYSTEM to "System", THEME_LIGHT to "Light", THEME_DARK to "Dark"), settings.theme, store::setTheme)
            }
            SettingBlock(
                "About",
                "Wallpaper Selector ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
                    "No ads, no tracking, no accounts. The internet is only used to load the wallpaper catalog from:\n" +
                    BuildConfig.CATALOG_BASE_URL,
            )
        }
    }
}
