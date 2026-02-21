package network.arno.android.tasks

data class TaskInfo(
    val id: Int,
    val status: String,
    val inputPreview: String,
    val costUsd: Double,
    val durationMs: Long?,
    val queuePosition: Int?,
)

data class TasksSummary(
    val totalTasks: Int,
    val completed: Int,
    val running: Int,
    val pending: Int,
    val queued: Int,
    val failed: Int,
    val cancelled: Int,
    val totalCostUsd: Double,
    val tasks: List<TaskInfo>,
)
