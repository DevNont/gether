package com.triptogether.core.data.util

import kotlin.random.Random

/** 6-digit numeric invite codes — easy to read out loud and type on a number pad. */
object InviteCodes {
    const val LENGTH = 6
    const val ALPHABET = "0123456789"

    fun random(random: Random = Random.Default): String =
        buildString(LENGTH) {
            repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
        }
}
