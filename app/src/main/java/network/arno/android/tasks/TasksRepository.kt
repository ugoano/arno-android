package network.arno.android.tasks

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.*

class TasksRepository {

    companion object {
        private const val TAG = "TasksRepository"
    }

    private val _summary = MutableStateFlow<TasksSummary?>(null)
    val summary: StateFlow<TasksSummary?> = _summary

    fun handleTasksSummary(summaryElement: JsonElement?) {
        val obj = summaryElement as? JsonObject ?: return
        try {
            val tasksArray = obj["tasks"] as? JsonArray ?: return
            val tasks = tasksArray.mapNotNull { parseTask(it) }

            _summary.value = TasksSummary(
                totalTasks = obj["total_tasks"]?.jsonPrimitive?.intOrNull ?: tasks.size,
                completed = obj["completed"]?.jsonPrimitive?.intOrNull ?: 0,
                running = obj["running"]?.jsonPrimitive?.intOrNull ?: 0,
                pending = obj["pending"]?.jsonPrimitive?.intOrNull ?: 0,
                queued = obj["queued"]?.jsonPrimitive?.intOrNull ?: 0,
                failed = obj["failed"]?.jsonPrimitive?.intOrNull ?: 0,
                cancelled = obj["cancelled"]?.jsonPrimitive?.intOrNull ?: 0,
                totalCostUsd = obj["total_cost_usd"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                tasks = tasks,
            )
            Log.d(TAG, "Updated tasks summary: ${tasks.size} tasks")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse tasks_summary: ${e.message}")
        }
    }

    private fun parseTask(element: JsonElement): TaskInfo? {
        val obj = element as? JsonObject ?: return null
        return TaskInfo(
            id = obj["id"]?.jsonPrimitive?.intOrNull ?: return null,
            status = obj["status"]?.jsonPrimitive?.contentOrNull ?: return null,
            inputPreview = obj["input_preview"]?.jsonPrimitive?.contentOrNull ?: "",
            costUsd = obj["cost_usd"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            durationMs = obj["duration_ms"]?.jsonPrimitive?.longOrNull,
            queuePosition = obj["queue_position"]?.jsonPrimitive?.intOrNull,
        )
    }

    fun clear() {
        _summary.value = null
    }
}
