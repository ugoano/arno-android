package network.arno.android.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.StateFlow

class TasksViewModel(
    private val tasksRepository: TasksRepository,
) : ViewModel() {

    val summary: StateFlow<TasksSummary?> = tasksRepository.summary

    class Factory(
        private val tasksRepository: TasksRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TasksViewModel(tasksRepository) as T
        }
    }
}
