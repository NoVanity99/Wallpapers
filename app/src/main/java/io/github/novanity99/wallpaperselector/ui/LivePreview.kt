package io.github.novanity99.wallpaperselector.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.github.novanity99.wallpaperselector.App
import io.github.novanity99.wallpaperselector.data.WallpaperDef
import io.github.novanity99.wallpaperselector.gl.RenderThread

/** A view that plays a wallpaper with the same renderer the live wallpaper service uses. */
@SuppressLint("ViewConstructor")
class LiveWallpaperView(context: Context, header: String) : TextureView(context), TextureView.SurfaceTextureListener {
    private val renderer = RenderThread("preview-gl", header)
    private var def: WallpaperDef? = null
    var interactive = false

    init {
        surfaceTextureListener = this
    }

    fun bind(def: WallpaperDef, fps: Int, speed: Float, scale: Float) {
        renderer.fps = fps
        renderer.speed = speed
        renderer.setRenderScale(scale)
        if (def !== this.def) {
            this.def = def
            renderer.setWallpaper(def)
        }
    }

    fun release() = renderer.quit()

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) =
        renderer.setSurface(surface, width, height)

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) =
        renderer.setSurface(surface, width, height)

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        renderer.releaseSurface()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    // Stop drawing while the app is in the background.
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        renderer.setVisible(visibility == View.VISIBLE)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!interactive) return false
        renderer.touch.onEvent(event, width, height)
        return true
    }
}

@Composable
fun LivePreview(
    def: WallpaperDef,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    fps: Int = 60,
    speed: Float = 1f,
    scale: Float = 1f,
) {
    val header = (LocalContext.current.applicationContext as App).repository.shaderHeader
    AndroidView(
        factory = { LiveWallpaperView(it, header) },
        modifier = modifier,
        update = {
            it.interactive = interactive
            it.bind(def, fps, speed, scale)
        },
        onRelease = { it.release() },
    )
}
