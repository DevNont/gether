package com.triptogether.core.data.dto

import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.MemberRole
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MapperTest {
    @Test
    @DisplayName("TripDto maps yyyy-MM-dd strings to LocalDate and carries all fields")
    fun tripDtoToDomain() {
        val dto =
            TripDto(
                name = "ทริปเชียงใหม่",
                coverUrl = null,
                startDate = "2026-12-05",
                endDate = "2026-12-07",
                ownerId = "uid-1",
                memberIds = listOf("uid-1"),
                inviteCode = "AB23CD",
                archived = false,
            )

        val trip = dto.toDomain("trip-1")

        assertEquals("trip-1", trip.id)
        assertEquals(LocalDate(2026, 12, 5), trip.startDate)
        assertEquals(LocalDate(2026, 12, 7), trip.endDate)
        assertEquals(3, trip.dayCount)
        assertEquals("AB23CD", trip.inviteCode)
        assertEquals("uid-1", trip.ownerId)
    }

    @Test
    @DisplayName("MemberDto role strings map to MemberRole and back")
    fun memberRoleRoundTrip() {
        assertEquals(MemberRole.OWNER, MemberDto(role = ROLE_OWNER).toDomain("m1").role)
        assertEquals(MemberRole.MEMBER, MemberDto(role = ROLE_MEMBER).toDomain("m1").role)
        assertEquals(MemberRole.MEMBER, MemberDto(role = "garbage").toDomain("m1").role)

        val owner =
            Member(
                id = "m1",
                userId = "uid-1",
                displayName = "สมชาย",
                role = MemberRole.OWNER,
            )
        assertEquals(ROLE_OWNER, owner.toDto().role)
    }

    @Test
    @DisplayName("Guest member has null userId and isGuest true")
    fun guestMember() {
        val guest = MemberDto(userId = null, displayName = "น้องแมว").toDomain("m2")
        assertNull(guest.userId)
        assertEquals(true, guest.isGuest)
    }
}
