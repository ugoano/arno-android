package network.arno.android.schedules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SchedulesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refresh emits success when repository returns schedules`() = runTest {
        val repository = FakeSchedulesRepository(
            schedules = listOf(
                Schedule(
                    id = "s1",
                    command = "echo",
                    schedule = "* * * * *",
                    enabled = true,
                )
            )
        )
        val viewModel = SchedulesViewModel(repository)

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is SchedulesState.Success)
        assertEquals(1, (state as SchedulesState.Success).schedules.size)
    }

    @Test
    fun `toggle applies optimistic update and reverts on failure`() = runTest {
        val repository = FakeSchedulesRepository(
            schedules = listOf(
                Schedule(
                    id = "s1",
                    command = "echo",
                    schedule = "* * * * *",
                    enabled = true,
                )
            ),
            failToggle = true,
        )
        val viewModel = SchedulesViewModel(repository)

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.toggle("s1", currentEnabled = true)
        advanceUntilIdle()

        val state = viewModel.state.value as SchedulesState.Success
        assertTrue(state.schedules.first().enabled)
    }

    private class FakeSchedulesRepository(
        private val schedules: List<Schedule>,
        private val failToggle: Boolean = false,
    ) : SchedulesRepository("https://example.com") {

        override suspend fun fetchSchedules(): List<Schedule> = schedules

        override suspend fun toggleSchedule(id: String, enabled: Boolean) {
            if (failToggle) throw RuntimeException("toggle failed")
        }
    }
}
