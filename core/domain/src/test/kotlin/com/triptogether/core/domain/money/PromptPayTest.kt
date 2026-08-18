package com.triptogether.core.domain.money

import com.triptogether.core.domain.model.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PromptPayTest {
    @Test
    @DisplayName("Reference vector: phone 000-000-0000 without amount; CRC cross-checked independently")
    fun knownVector() {
        // CRC 8956 verified against a separate CRC-16/CCITT-FALSE implementation.
        assertEquals(
            "00020101021129370016A000000677010111011300660000000005802TH530376463048956",
            PromptPay.buildPayload("000-000-0000"),
        )
    }

    @Test
    @DisplayName("Phone proxy becomes 0066 + number without the leading zero")
    fun phoneProxy() {
        val payload = PromptPay.buildPayload("081-234-5678", Money(185_000))
        checkNotNull(payload)
        assertTrue("01130066812345678" in payload)
        // Dynamic QR marker when an amount is present.
        assertTrue(payload.startsWith("000201010212"))
        assertTrue("54071850.00" in payload)
    }

    @Test
    @DisplayName("13-digit citizen id uses proxy tag 02")
    fun citizenIdProxy() {
        val payload = PromptPay.buildPayload("1234567890123")
        checkNotNull(payload)
        assertTrue("02131234567890123" in payload)
    }

    @Test
    @DisplayName("Amount always prints two decimals from satang, no floating point")
    fun amountFormat() {
        val payload = PromptPay.buildPayload("0812345678", Money(1))
        checkNotNull(payload)
        assertTrue("54040.01" in payload)
    }

    @Test
    @DisplayName("Garbage targets return null")
    fun invalidTargets() {
        assertNull(PromptPay.buildPayload("abc"))
        assertNull(PromptPay.buildPayload("12345"))
        assertNull(PromptPay.buildPayload("9812345678"))
    }

    @Test
    @DisplayName("CRC changes when any character changes")
    fun crcCoversPayload() {
        val a = PromptPay.buildPayload("0812345678", Money(10_000))
        val b = PromptPay.buildPayload("0812345679", Money(10_000))
        checkNotNull(a)
        checkNotNull(b)
        assertTrue(a.takeLast(4) != b.takeLast(4))
    }
}
