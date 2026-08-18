package com.triptogether.feature.plan

import com.triptogether.core.domain.model.Activity
import com.triptogether.core.domain.model.ActivityType
import com.triptogether.core.domain.model.DayPlan

data class DayPlanUiState(
    val isLoading: Boolean = true,
    val days: List<DayPlan> = emptyList(),
    val selectedDayId: String? = null,
    val selectedDay: DayPlan? = null,
) {
    /** The day's stay(s), pinned above the timeline instead of inside it. */
    val stays: List<Activity>
        get() = selectedDay?.activities?.filter { it.type == ActivityType.STAY } ?: emptyList()

    /** Timed activities first (by startTime), untimed at the end (by sortOrder) — S06 spec. */
    val timeline: List<Activity>
        get() {
            val activities =
                selectedDay?.activities?.filterNot { it.type == ActivityType.STAY } ?: return emptyList()
            val (timed, untimed) = activities.partition { it.startTime != null }
            return timed.sortedBy { it.startTime } + untimed.sortedBy { it.sortOrder }
        }

    val isDayEmpty: Boolean get() = stays.isEmpty() && timeline.isEmpty()
}
