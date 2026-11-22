package io.github.novanity99.wallpaperselector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.novanity99.wallpaperselector.data.WallpaperItem
import io.github.novanity99.wallpaperselector.ui.AppTheme
import io.github.novanity99.wallpaperselector.ui.BrowseScreen
import io.github.novanity99.wallpaperselector.ui.DetailScreen
import io.github.novanity99.wallpaperselector.ui.MyWallpapersScreen
import io.github.novanity99.wallpaperselector.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val app = application as App
        if (savedInstanceState == null) app.repository.refresh()
        setContent {
            val settings by app.settings.state.collectAsStateWithLifecycle()
            AppTheme(settings.theme) { AppRoot(app) }
        }
    }
}

@Composable
private fun AppRoot(app: App) {
    val settings by app.settings.state.collectAsStateWithLifecycle()
    val items by app.repository.items.collectAsStateWithLifecycle()
    val status by app.repository.status.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { app.repository.errors.collect { snackbar.showSnackbar(it) } }

    val detail = items.firstOrNull { it.entry.id == detailId }
    // Keeps the last item around so the closing animation still has something to show.
    var lastDetail by remember { mutableStateOf<WallpaperItem?>(null) }
    if (detail != null) lastDetail = detail
    // Grid previews stop while a full-screen preview covers them.
    val live = settings.livePreviews && detail == null

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Rounded.Explore, null) }, label = { Text("Browse") })
                    NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Rounded.Collections, null) }, label = { Text("My Wallpapers") })
                    NavigationBarItem(tab == 2, { tab = 2 }, { Icon(Icons.Rounded.Settings, null) }, label = { Text("Settings") })
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    0 -> BrowseScreen(items, status, settings, live, app.repository::refresh) { detailId = it.entry.id }
                    1 -> MyWallpapersScreen(items, settings, live, { detailId = it.entry.id }, { app.repository.delete(it.entry.id) })
                    else -> SettingsScreen(settings)
                }
            }
        }
        AnimatedVisibility(
            visible = detail != null,
            enter = fadeIn() + scaleIn(initialScale = 0.94f),
            exit = fadeOut() + scaleOut(targetScale = 0.94f),
        ) {
            lastDetail?.let { DetailScreen(it, settings, onBack = { detailId = null }) }
        }
    }
    BackHandler(enabled = detail != null) { detailId = null }
    BackHandler(enabled = detail == null && tab != 0) { tab = 0 }
}
