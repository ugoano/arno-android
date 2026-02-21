package network.arno.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import network.arno.android.transport.ConnectionState
import network.arno.android.ui.theme.*

@Composable
fun ConnectionBanner(state: ConnectionState, modifier: Modifier = Modifier) {
    val (text, dotColor) = when (state) {
        is ConnectionState.Connected -> "CONNECTED" to JarvisGreen
        is ConnectionState.Connecting -> "CONNECTING..." to JarvisYellow
        is ConnectionState.Reconnecting -> "RECONNECTING (${state.attempt})..." to JarvisYellow
        is ConnectionState.Disconnected -> "DISCONNECTED" to JarvisRed
        is ConnectionState.Error -> "ERROR" to JarvisRed
    }
    val animatedColor by animateColorAsState(targetValue = dotColor, label = "dot")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(JarvisBg)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(6.dp).clip(CircleShape)
        ) { drawCircle(animatedColor) }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = animatedColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
