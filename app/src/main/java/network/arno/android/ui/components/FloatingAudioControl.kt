package network.arno.android.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.arno.android.ui.theme.*

/**
 * Translucent floating audio control overlay.
 * Appears when audio is playing; provides play/pause, stop, and volume.
 */
@Composable
fun FloatingAudioControl(
    isPlaying: Boolean,
    hasActivePlayer: Boolean,
    volume: Float,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = hasActivePlayer,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(JarvisSurface.copy(alpha = 0.88f))
                .border(1.dp, JarvisBorder.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            // Play/Pause button
            IconButton(
                onClick = onTogglePause,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPlaying) JarvisGreen else JarvisCyan,
                    modifier = Modifier.size(22.dp),
                )
            }

            // Stop button
            IconButton(
                onClick = onStop,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = "Stop",
                    tint = JarvisRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp),
                )
            }

            // Volume slider
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                modifier = Modifier.width(100.dp),
                colors = SliderDefaults.colors(
                    thumbColor = JarvisCyan,
                    activeTrackColor = JarvisCyan.copy(alpha = 0.6f),
                    inactiveTrackColor = JarvisBorder,
                ),
            )

            // Volume percentage label
            Text(
                text = "${(volume * 100).toInt()}%",
                color = JarvisTextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.width(32.dp),
            )
        }
    }
}
