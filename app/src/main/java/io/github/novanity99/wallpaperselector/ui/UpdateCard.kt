package io.github.novanity99.wallpaperselector.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.novanity99.wallpaperselector.BuildConfig
import io.github.novanity99.wallpaperselector.data.UpdateState

/**
 * App update status and actions. With [compact] it only appears when there is something to do
 * (used on the Browse screen); otherwise it always shows (Settings).
 */
@Composable
fun UpdateCard(modifier: Modifier = Modifier, compact: Boolean = false) {
    val context = LocalContext.current
    val updater = context.app.updater
    val state by updater.state.collectAsStateWithLifecycle()
    val busy = state is UpdateState.Available || state is UpdateState.Downloading || state is UpdateState.Ready
    if (compact && !busy) return

    // Open the installer as soon as the download finishes.
    LaunchedEffect(state) { (state as? UpdateState.Ready)?.let { updater.install(context, it) } }

    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            when (val s = state) {
                is UpdateState.Available -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Version ${s.version} is available", style = MaterialTheme.typography.titleSmall)
                        Text("You have ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { updater.download(s) }) { Text("Update") }
                }
                is UpdateState.Downloading -> {
                    Text("Downloading ${s.version}… ${(s.progress * 100).toInt()}%", style = MaterialTheme.typography.titleSmall)
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
                }
                is UpdateState.Ready -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Version ${s.version} is ready", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Button(onClick = { updater.install(context, s) }) { Text("Install") }
                }
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleSmall)
                        Text(
                            when (s) {
                                is UpdateState.Checking -> "Checking for updates…"
                                is UpdateState.UpToDate -> "You have the latest version."
                                is UpdateState.Failed -> s.message
                                else -> "Updates come from this app's GitHub releases."
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    OutlinedButton(onClick = { updater.check() }, enabled = s !is UpdateState.Checking) { Text("Check") }
                }
            }
        }
    }
}
