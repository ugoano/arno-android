package network.arno.android.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import network.arno.android.tasks.TaskInfo
import network.arno.android.tasks.TasksSummary
import network.arno.android.tasks.TasksViewModel
import network.arno.android.ui.theme.*

@Composable
fun TasksScreen(viewModel: TasksViewModel) {
    val summary by viewModel.summary.collectAsState()

    if (summary == null || summary?.tasks.isNullOrEmpty()) {
        EmptyTasksView()
    } else {
        TasksListView(summary!!)
    }
}

@Composable
private fun EmptyTasksView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "\u2705",
                style = MaterialTheme.typography.displayLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Tasks",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Background tasks will appear here",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TasksListView(summary: TasksSummary) {
    val pending = summary.tasks.filter { it.status == "pending" || it.status == "queued" }
    val running = summary.tasks.filter { it.status == "running" }
    val done = summary.tasks.filter { it.status in listOf("completed", "failed", "cancelled") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Summary bar
        item {
            SummaryBar(summary)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Running section
        if (running.isNotEmpty()) {
            item {
                SectionHeader("Running", running.size, MaterialTheme.colorScheme.secondary)
            }
            items(running, key = { it.id }) { task ->
                TaskCard(task, isRunning = true)
            }
            item { Spacer(modifier = Modifier.height(4.dp)) }
        }

        // Pending section
        if (pending.isNotEmpty()) {
            item {
                SectionHeader("Pending", pending.size, MaterialTheme.colorScheme.tertiary)
            }
            items(pending, key = { it.id }) { task ->
                TaskCard(task, isRunning = false)
            }
            item { Spacer(modifier = Modifier.height(4.dp)) }
        }

        // Done section
        if (done.isNotEmpty()) {
            item {
                SectionHeader("Done", done.size, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(done, key = { it.id }) { task ->
                TaskCard(task, isRunning = false)
            }
        }
    }
}

@Composable
private fun SummaryBar(summary: TasksSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${summary.totalTasks} tasks",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "$${formatCost(summary.totalCostUsd)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun TaskCard(task: TaskInfo, isRunning: Boolean) {
    val shape = RoundedCornerShape(8.dp)
    val borderColor = if (isRunning) {
        val infiniteTransition = rememberInfiniteTransition(label = "running")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse",
        )
        JarvisGreen.copy(alpha = alpha)
    } else {
        JarvisBorder
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
    ) {
        // Top row: ID + status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "#${task.id}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            StatusChip(task.status)
        }

        // Input preview
        if (task.inputPreview.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = task.inputPreview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Meta row: duration + cost
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(task.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$${formatCost(task.costUsd)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (label, color) = when (status) {
        "running" -> "RUNNING" to JarvisGreen
        "pending" -> "PENDING" to JarvisYellow
        "queued" -> "QUEUED" to JarvisYellow
        "completed" -> "DONE" to JarvisTextSecondary
        "failed" -> "FAILED" to JarvisRed
        "cancelled" -> "CANCELLED" to JarvisTextSecondary
        else -> status.uppercase() to JarvisTextSecondary
    }

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

private fun formatDuration(ms: Long?): String {
    if (ms == null || ms == 0L) return "-"
    if (ms < 1000) return "${ms}ms"
    val seconds = ms / 1000
    if (seconds < 60) return "${seconds}s"
    val minutes = seconds / 60
    val secs = seconds % 60
    return "${minutes}m ${secs}s"
}

private fun formatCost(usd: Double): String {
    return "%.4f".format(usd)
}
