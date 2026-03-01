package network.arno.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import network.arno.android.schedules.Schedule
import network.arno.android.schedules.SchedulesState
import network.arno.android.schedules.SchedulesViewModel
import network.arno.android.ui.theme.JarvisBorder
import network.arno.android.ui.theme.JarvisCyan
import network.arno.android.ui.theme.JarvisGreen
import network.arno.android.ui.theme.JarvisSurface
import network.arno.android.ui.theme.JarvisTextSecondary
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(viewModel: SchedulesViewModel) {
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    when (val current = state) {
        is SchedulesState.Loading -> LoadingView()
        is SchedulesState.Error -> ErrorView(message = current.message, onRetry = viewModel::refresh)
        is SchedulesState.Success -> {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.silentRefresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                if (current.schedules.isEmpty()) {
                    EmptySchedulesView()
                } else {
                    SchedulesListView(
                        schedules = current.schedules,
                        onToggle = { id, enabled -> viewModel.toggle(id, enabled) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = JarvisCyan)
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptySchedulesView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "\u23F0",
                style = MaterialTheme.typography.displayLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Scheduled Tasks",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Schedules will appear here",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SchedulesListView(
    schedules: List<Schedule>,
    onToggle: (String, Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(schedules, key = { it.id }) { schedule ->
            ScheduleCard(schedule = schedule, onToggle = onToggle)
        }
    }
}

@Composable
private fun ScheduleCard(
    schedule: Schedule,
    onToggle: (String, Boolean) -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val borderColor = if (schedule.enabled) JarvisGreen else JarvisBorder

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, borderColor, shape),
        color = JarvisSurface,
        shape = shape,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = schedule.command,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = { onToggle(schedule.id, schedule.enabled) },
                )
            }

            if (schedule.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = schedule.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisTextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "\u23F0",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = schedule.schedule,
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisTextSecondary,
                    )
                }
                StatusChip(enabled = schedule.enabled)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Next: ${formatNextRun(schedule.nextRun)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisTextSecondary,
                )
                Text(
                    text = "Last: ${formatLastRun(schedule.lastRun)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(enabled: Boolean) {
    val color = if (enabled) JarvisGreen else JarvisTextSecondary
    val label = if (enabled) "ENABLED" else "DISABLED"

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

private fun formatNextRun(value: String?): String {
    val instant = value?.let(::parseInstant) ?: return "-"
    val now = Instant.now()
    val duration = Duration.between(now, instant)

    if (duration.isNegative) return "-"
    if (duration.seconds < 60) return "Starting soon"
    if (duration.toMinutes() < 60) return "in ${duration.toMinutes()} minutes"
    if (duration.toHours() < 24) return "in ${duration.toHours()} hours"
    return "in ${duration.toDays()} days"
}

private fun formatLastRun(value: String?): String {
    val instant = value?.let(::parseInstant) ?: return "-"
    val now = Instant.now()
    val duration = Duration.between(instant, now)

    if (duration.isNegative) return "-"
    if (duration.toMinutes() < 1) return "just now"
    if (duration.toMinutes() < 60) return "${duration.toMinutes()} minutes ago"
    if (duration.toHours() < 24) return "${duration.toHours()} hours ago"
    return "${duration.toDays()} days ago"
}

private fun parseInstant(value: String): Instant? {
    return try {
        Instant.parse(value)
    } catch (_: Exception) {
        null
    }
}
