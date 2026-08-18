package com.triptogether.core.data.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.random.Random

class InviteCodesTest {
    @Test
    @DisplayName("Codes are 6 chars drawn only from the unambiguous alphabet")
    fun codeShape() {
        val random = Random(42)
        repeat(1_000) {
            val code = InviteCodes.random(random)
            assertEquals(InviteCodes.LENGTH, code.length)
            assertTrue(code.all { it in InviteCodes.ALPHABET }) { "Unexpected char in $code" }
        }
    }

    @Test
    @DisplayName("Alphabet excludes the confusable O, 0, I, 1")
    fun alphabetExcludesConfusables() {
        val forbidden = setOf('O', '0', 'I', '1')
        assertTrue(InviteCodes.ALPHABET.none { it in forbidden })
        assertEquals(32, InviteCodes.ALPHABET.length)
    }

    @Test
    @DisplayName("Seeded generators produce differing codes across draws")
    fun notConstant() {
        val random = Random(7)
        val codes = List(100) { InviteCodes.random(random) }.toSet()
        assertTrue(codes.size > 90)
    }
}
