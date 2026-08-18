package com.triptogether.core.data.dto

import com.triptogether.core.domain.model.Activity
import com.triptogether.core.domain.model.Attachment
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PlanMapperTest {
    @Test
    @DisplayName("ActivityDto round-trips through the domain model")
    fun activityRoundTrip() {
        val activity =
            Activity(
                id = "a1",
                title = "เช็คอินโรงแรม",
                startTime = LocalTime(14, 0),
                endTime = LocalTime(15, 30),
                placeName = "โรงแรมช้างเผือก",
                lat = 18.7961,
                lng = 98.9793,
                note = "จองไว้แล้ว",
                attachments =
                    listOf(
                        Attachment(name = "booking.pdf", url = "https://x/y", mimeType = "application/pdf"),
                    ),
                sortOrder = 2,
                createdBy = "m1",
            )

        val roundTripped = activity.toDto().toDomain("a1")

        assertEquals(activity, roundTripped)
    }

    @Test
    @DisplayName("Times serialize as zero-padded HH:mm")
    fun timeFormat() {
        assertEquals("09:05", LocalTime(9, 5).toHHmm())
        assertEquals("00:00", LocalTime(0, 0).toHHmm())
        assertEquals("23:59", LocalTime(23, 59).toHHmm())
    }

    @Test
    @DisplayName("Null times survive the round trip")
    fun nullTimes() {
        val dto = ActivityDto(title = "เดินเล่น", createdBy = "m1")
        val domain = dto.toDomain("a2")
        assertNull(domain.startTime)
        assertNull(domain.endTime)
        assertEquals(dto, domain.toDto())
    }
}
