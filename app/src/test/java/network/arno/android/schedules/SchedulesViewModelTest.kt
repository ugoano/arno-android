package network.arno.android.schedules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `refresh emits success when repository returns schedules`() = runTest(dispatcher) {
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
    fun `toggle applies optimistic update and reverts on failure`() = runTest(dispatcher) {
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

    @Test
    fun `silentRefresh does not emit Loading state`() = runTest(dispatcher) {
        val repository = FakeSchedulesRepository(
            schedules = listOf(
                Schedule(id = "s1", command = "echo", schedule = "* * * * *", enabled = true)
            )
        )
        val viewModel = SchedulesViewModel(repository)

        // Initial load
        viewModel.refresh()
        advanceUntilIdle()
        assertTrue(viewModel.state.value is SchedulesState.Success)

        // Silent refresh should NOT go through Loading
        var sawLoading = false
        viewModel.silentRefresh()
        // Check state hasn't changed to Loading during the refresh
        if (viewModel.state.value is SchedulesState.Loading) sawLoading = true
        advanceUntilIdle()
        if (viewModel.state.value is SchedulesState.Loading) sawLoading = true

        assertFalse("silentRefresh should not emit Loading", sawLoading)
        assertTrue(viewModel.state.value is SchedulesState.Success)
    }

    @Test
    fun `silentRefresh updates schedules data`() = runTest(dispatcher) {
        val repository = FakeSchedulesRepository(
            schedules = listOf(
                Schedule(id = "s1", command = "echo", schedule = "* * * * *", enabled = true)
            )
        )
        val viewModel = SchedulesViewModel(repository)

        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(1, (viewModel.state.value as SchedulesState.Success).schedules.size)

        // Update repository data and silent refresh
        repository.updateSchedules(
            listOf(
                Schedule(id = "s1", command = "echo", schedule = "* * * * *", enabled = true),
                Schedule(id = "s2", command = "test", schedule = "0 9 * * *", enabled = false),
            )
        )
        viewModel.silentRefresh()
        advanceUntilIdle()

        assertEquals(2, (viewModel.state.value as SchedulesState.Success).schedules.size)
    }

    @Test
    fun `isRefreshing is false after silentRefresh completes`() = runTest(dispatcher) {
        val repository = FakeSchedulesRepository(
            schedules = listOf(
                Schedule(id = "s1", command = "echo", schedule = "* * * * *", enabled = true)
            )
        )
        val viewModel = SchedulesViewModel(repository)

        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.isRefreshing.value)

        viewModel.silentRefresh()
        advanceUntilIdle()

        // After completion, isRefreshing must be false
        assertFalse(viewModel.isRefreshing.value)
        assertTrue(viewModel.state.value is SchedulesState.Success)
    }

    @Test
    fun `startAutoRefresh triggers periodic fetches`() = runTest(dispatcher) {
        val repository = FakeSchedulesRepository(
            schedules = listOf(
                Schedule(id = "s1", command = "echo", schedule = "* * * * *", enabled = true)
            )
        )
        val viewModel = SchedulesViewModel(repository)

        viewModel.refresh()
        advanceUntilIdle()
        val initialCount = repository.fetchCount

        viewModel.startAutoRefresh()

        // Advance past one refresh interval (30 seconds)
        advanceTimeBy(31_000)
        // Stop BEFORE advanceUntilIdle to break the infinite loop
        viewModel.stopAutoRefresh()
        advanceUntilIdle()

        assertTrue("Auto-refresh should have fetched at least once more", repository.fetchCount > initialCount)
    }

    @Test
    fun `stopAutoRefresh stops periodic fetches`() = runTest(dispatcher) {
        val repository = FakeSchedulesRepository(
            schedules = listOf(
                Schedule(id = "s1", command = "echo", schedule = "* * * * *", enabled = true)
            )
        )
        val viewModel = SchedulesViewModel(repository)

        viewModel.refresh()
        advanceUntilIdle()

        viewModel.startAutoRefresh()
        advanceTimeBy(31_000)
        // Stop BEFORE advanceUntilIdle to break the infinite loop
        viewModel.stopAutoRefresh()
        advanceUntilIdle()

        val countAfterStop = repository.fetchCount

        // Advance another interval - no more fetches should happen
        advanceTimeBy(31_000)
        advanceUntilIdle()

        assertEquals("No fetches after stopAutoRefresh", countAfterStop, repository.fetchCount)
    }

    private class FakeSchedulesRepository(
        private var schedules: List<Schedule>,
        private val failToggle: Boolean = false,
    ) : SchedulesRepository("https://example.com") {

        var fetchCount = 0
            private set

        override suspend fun fetchSchedules(): List<Schedule> {
            fetchCount++
            return schedules
        }

        override suspend fun toggleSchedule(id: String, enabled: Boolean) {
            if (failToggle) throw RuntimeException("toggle failed")
        }

        fun updateSchedules(newSchedules: List<Schedule>) {
            schedules = newSchedules
        }
    }
}
