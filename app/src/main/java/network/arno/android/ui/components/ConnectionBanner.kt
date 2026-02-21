package network.arno.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import network.arno.android.transport.ConnectionState
import network.arno.android.ui.theme.ArnoGreen
import network.arno.android.ui.theme.ArnoRed
import network.arno.android.ui.theme.ArnoYellow

@Composable
fun ConnectionBanner(state: ConnectionState, modifier: Modifier = Modifier) {
    val (text, dotColor) = when (state) {
        is ConnectionState.Connected -> "Connected" to ArnoGreen
        is ConnectionState.Connecting -> "Connecting..." to ArnoYellow
        is ConnectionState.Reconnecting -> "Reconnecting (${state.attempt})..." to ArnoYellow
        is ConnectionState.Disconnected -> "Disconnected" to ArnoRed
        is ConnectionState.Error -> "Error" to ArnoRed
    }
    val animatedColor by animateColorAsState(targetValue = dotColor, label = "dot")

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier.size(8.dp).clip(CircleShape)
            ) { drawCircle(animatedColor) }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
