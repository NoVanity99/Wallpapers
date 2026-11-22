package io.github.novanity99.wallpaperselector.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.novanity99.wallpaperselector.data.Custom
import io.github.novanity99.wallpaperselector.data.WallpaperDef

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, suffix: String = "") {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            val shown = if (range.endInclusive - range.start >= 20f) "%.0f".format(value) else "%.2f".format(value)
            Text(shown + suffix, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value.coerceIn(range), onValueChange = onChange, valueRange = range, modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp),
    )
}

/**
 * Per-wallpaper settings. Every change is saved immediately and shows up live, both in the
 * preview behind this panel and on the home screen if this wallpaper is the active one.
 * [base] is the wallpaper as designed, [custom] the user's current changes.
 */
@Composable
fun CustomizePanel(base: WallpaperDef, interactive: Boolean, custom: Custom, onChange: (Custom?) -> Unit, onDone: () -> Unit) {
    var editingColor by remember { mutableIntStateOf(-1) }
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.52f).dp

    Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Customize", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { onChange(null) }, enabled = custom != Custom()) { Text("Reset") }
            TextButton(onClick = onDone) { Text("Done") }
        }
        Column(Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState())) {
            if (interactive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("React to touch", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Off: the wallpaper ignores taps and swipes.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = custom.touch, onCheckedChange = { onChange(custom.copy(touch = it)) })
                }
            }

            SectionLabel("Colours")
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                for (i in 0 until 4) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape)
                            .background(Color(custom.colors.getOrNull(i) ?: base.colorInt(i)))
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { editingColor = i },
                    )
                }
            }

            SectionLabel("Motion")
            LabeledSlider("Speed", custom.speed, 0f..3f, { onChange(custom.copy(speed = it)) }, "×")

            if (base.controls.isNotEmpty()) {
                SectionLabel("This wallpaper")
                base.controls.forEach { c ->
                    LabeledSlider(c.label, custom.params[c.key] ?: c.default, c.min..c.max, { onChange(custom.copy(params = custom.params + (c.key to it))) })
                }
            }

            SectionLabel("Look")
            LabeledSlider("Brightness", custom.brightness, 0.2f..1.6f, { onChange(custom.copy(brightness = it)) })
            LabeledSlider("Contrast", custom.contrast, 0.5f..1.6f, { onChange(custom.copy(contrast = it)) })
            LabeledSlider("Saturation", custom.saturation, 0f..2f, { onChange(custom.copy(saturation = it)) })
            LabeledSlider("Dark corners", custom.vignette, 0f..1.5f, { onChange(custom.copy(vignette = it)) })

            SectionLabel("Performance")
            LabeledSlider("Sharpness", custom.quality, 0.4f..1f, { onChange(custom.copy(quality = it)) })
            Text(
                "Lower sharpness draws fewer pixels, which saves battery and helps slower phones.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
    }

    if (editingColor >= 0) {
        val slot = editingColor
        ColorDialog(
            initial = custom.colors.getOrNull(slot) ?: base.colorInt(slot),
            onDismiss = { editingColor = -1 },
            onDefault = {
                onChange(custom.copy(colors = custom.colors.toMutableList().also { it[slot] = null }))
                editingColor = -1
            },
            onPick = { argb -> onChange(custom.copy(colors = custom.colors.toMutableList().also { it[slot] = argb })) },
        )
    }
}

/** Hue / saturation / brightness sliders. The wallpaper updates while you drag. */
@Composable
private fun ColorDialog(initial: Int, onDismiss: () -> Unit, onDefault: () -> Unit, onPick: (Int) -> Unit) {
    val hsv = remember { FloatArray(3).also { AndroidColor.colorToHSV(initial, it) } }
    var h by remember { mutableFloatStateOf(hsv[0]) }
    var s by remember { mutableFloatStateOf(hsv[1]) }
    var v by remember { mutableFloatStateOf(hsv[2]) }
    fun current() = AndroidColor.HSVToColor(floatArrayOf(h, s, v))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a colour") },
        text = {
            Column {
                Box(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(current()))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                )
                LabeledSlider("Hue", h, 0f..360f, { h = it; onPick(current()) }, "°")
                LabeledSlider("Saturation", s, 0f..1f, { s = it; onPick(current()) })
                LabeledSlider("Brightness", v, 0f..1f, { v = it; onPick(current()) })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDefault) { Text("Use original") } },
    )
}
