package network.arno.android.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SchedulesViewModel(
    private val repository: SchedulesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<SchedulesState>(SchedulesState.Loading)
    val state: StateFlow<SchedulesState> = _state

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private var autoRefreshJob: Job? = null

    companion object {
        const val AUTO_REFRESH_INTERVAL_MS = 30_000L
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = SchedulesState.Loading
            try {
                val schedules = repository.fetchSchedules()
                _state.value = SchedulesState.Success(schedules)
            } catch (e: Exception) {
                _state.value = SchedulesState.Error(e.message ?: "Failed to fetch schedules")
            }
        }
    }

    fun silentRefresh() {
        viewModelScope.launch { doSilentRefresh() }
    }

    private suspend fun doSilentRefresh() {
        _isRefreshing.value = true
        try {
            val schedules = repository.fetchSchedules()
            _state.value = SchedulesState.Success(schedules)
        } catch (_: Exception) {
            // Silent refresh ignores errors - keeps current state
        } finally {
            _isRefreshing.value = false
        }
    }

    fun startAutoRefresh() {
        stopAutoRefresh()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                doSilentRefresh()
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    fun toggle(id: String, currentEnabled: Boolean) {
        val currentState = _state.value as? SchedulesState.Success ?: return
        val optimistic = currentState.schedules.map {
            if (it.id == id) it.copy(enabled = !currentEnabled) else it
        }
        _state.value = SchedulesState.Success(optimistic)

        viewModelScope.launch {
            try {
                repository.toggleSchedule(id, !currentEnabled)
            } catch (_: Exception) {
                val reverted = (_state.value as? SchedulesState.Success)?.schedules?.map {
                    if (it.id == id) it.copy(enabled = currentEnabled) else it
                } ?: currentState.schedules
                _state.value = SchedulesState.Success(reverted)
            }
        }
    }

    class Factory(
        private val repository: SchedulesRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SchedulesViewModel(repository) as T
        }
    }
}
