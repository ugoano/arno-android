package network.arno.android.schedules

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Schedule(
    val id: String,
    val command: String,
    val schedule: String,
    val description: String = "",
    val enabled: Boolean = false,
    @SerialName("last_run") val lastRun: String? = null,
    @SerialName("next_run") val nextRun: String? = null,
    val created: String? = null,
)

sealed class SchedulesState {
    data object Loading : SchedulesState()
    data class Success(val schedules: List<Schedule>) : SchedulesState()
    data class Error(val message: String) : SchedulesState()
}
