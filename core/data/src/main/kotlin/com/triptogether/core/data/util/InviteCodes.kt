package com.triptogether.core.data.util

import kotlin.random.Random

/** 6-char invite codes from A-Z 0-9 minus the confusable O, 0, I, 1 (docs/02). */
object InviteCodes {
    const val LENGTH = 6
    const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun random(random: Random = Random.Default): String =
        buildString(LENGTH) {
            repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
        }
}
