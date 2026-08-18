package com.triptogether.feature.plan

import com.triptogether.core.domain.model.Activity
import com.triptogether.core.domain.model.DayPlan

data class DayPlanUiState(
    val isLoading: Boolean = true,
    val days: List<DayPlan> = emptyList(),
    val selectedDayId: String? = null,
    val selectedDay: DayPlan? = null,
) {
    /** Timed activities first (by startTime), untimed at the end (by sortOrder) — S06 spec. */
    val timeline: List<Activity>
        get() {
            val activities = selectedDay?.activities ?: return emptyList()
            val (timed, untimed) = activities.partition { it.startTime != null }
            return timed.sortedBy { it.startTime } + untimed.sortedBy { it.sortOrder }
        }
}
