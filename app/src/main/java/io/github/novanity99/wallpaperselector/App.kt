package io.github.novanity99.wallpaperselector

import android.app.Application
import io.github.novanity99.wallpaperselector.data.Settings
import io.github.novanity99.wallpaperselector.data.WallpaperRepository

class App : Application() {
    val settings by lazy { Settings(this) }
    val repository by lazy { WallpaperRepository(this) }
}
