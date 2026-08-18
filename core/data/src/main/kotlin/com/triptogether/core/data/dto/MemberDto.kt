package com.triptogether.core.data.dto

import com.triptogether.core.domain.model.Member
import com.triptogether.core.domain.model.MemberRole

/** Firestore shape of trips/{tripId}/members/{memberId}. Profile fields are denormalized from users/. */
data class MemberDto(
    val userId: String? = null,
    val displayName: String = "",
    val photoUrl: String? = null,
    val promptpayId: String? = null,
    val role: String = ROLE_MEMBER,
)

fun MemberDto.toDomain(id: String): Member =
    Member(
        id = id,
        userId = userId,
        displayName = displayName,
        photoUrl = photoUrl,
        promptpayId = promptpayId,
        role = if (role == ROLE_OWNER) MemberRole.OWNER else MemberRole.MEMBER,
    )

fun Member.toDto(): MemberDto =
    MemberDto(
        userId = userId,
        displayName = displayName,
        photoUrl = photoUrl,
        promptpayId = promptpayId,
        role = if (role == MemberRole.OWNER) ROLE_OWNER else ROLE_MEMBER,
    )

const val ROLE_OWNER = "owner"
const val ROLE_MEMBER = "member"
