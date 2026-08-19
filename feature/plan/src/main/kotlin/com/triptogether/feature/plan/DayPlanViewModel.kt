package com.triptogether.feature.plan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.triptogether.core.domain.model.DayPlan
import com.triptogether.core.domain.repository.PlanRepository
import com.triptogether.feature.plan.navigation.DayPlanRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DayPlanViewModel
    @Inject
    constructor(
        private val planRepository: PlanRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<DayPlanRoute>()
        private val selectedDayId = MutableStateFlow<String?>(null)

        val uiState: StateFlow<DayPlanUiState> =
            combine(planRepository.observeDayPlans(route.tripId), selectedDayId) { days, selected ->
                days to (selected ?: defaultDayId(days))
            }.flatMapLatest { (days, selected) ->
                if (selected == null) {
                    // observeDayPlans has emitted by now, so an empty list is a real empty state,
                    // not a loading one — spinning forever here would hide the empty-day hint.
                    flowOf(DayPlanUiState(isLoading = false, days = days))
                } else {
                    // Per-day snapshot listener only — never the whole trip (S06 spec).
                    planRepository.observeDayPlan(route.tripId, selected).map { day ->
                        DayPlanUiState(
                            isLoading = false,
                            days = days,
                            selectedDayId = selected,
                            selectedDay = day,
                        )
                    }
                }
            }.catch {
                // A failed listener (e.g. PERMISSION_DENIED) must not render as a legit empty plan forever.
                emit(DayPlanUiState(isLoading = false))
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = DayPlanUiState(),
            )

        fun selectDay(dayId: String) {
            selectedDayId.value = dayId
        }

        /** Today when the trip is underway, otherwise the first day. */
        private fun defaultDayId(days: List<DayPlan>): String? {
            if (days.isEmpty()) return null
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            return days.firstOrNull { it.date == today }?.id ?: days.first().id
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
