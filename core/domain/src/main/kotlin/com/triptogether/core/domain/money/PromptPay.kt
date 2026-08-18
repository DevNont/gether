package com.triptogether.core.domain.money

import com.triptogether.core.domain.model.Money

/**
 * EMVCo PromptPay payload builder (docs/04 §7). Pure logic, no I/O — rendering
 * to a QR bitmap happens in the UI layer with ZXing.
 *
 * Field order (00, 01, 29, 58, 53, 54, 63) mirrors the widely deployed
 * promptpay-qr library so the output matches QR codes Thai banking apps
 * already accept. MUST be scanned with at least 2 real bank apps before release.
 */
object PromptPay {
    private const val AID_PROMPTPAY = "A000000677010111"
    private const val CITIZEN_ID_LENGTH = 13
    private const val CRC_POLYNOMIAL = 0x1021
    private const val CRC_INITIAL = 0xFFFF

    /**
     * Builds the payload for a phone number (10 digits, leading 0) or a
     * 13-digit citizen id. Returns null when [target] is neither.
     */
    fun buildPayload(
        target: String,
        amount: Money? = null,
    ): String? {
        val digits = target.filter { it.isDigit() }
        val proxy =
            when {
                digits.length == CITIZEN_ID_LENGTH -> tlv("02", digits)
                digits.length == 10 && digits.startsWith("0") ->
                    tlv("01", "0066" + digits.drop(1))
                else -> return null
            }

        val payload =
            buildString {
                append(tlv("00", "01"))
                append(tlv("01", if (amount != null) "12" else "11"))
                append(tlv("29", tlv("00", AID_PROMPTPAY) + proxy))
                append(tlv("58", "TH"))
                append(tlv("53", "764"))
                if (amount != null) {
                    append(tlv("54", formatAmount(amount)))
                }
                append("6304")
            }
        return payload + crc16(payload)
    }

    private fun tlv(
        id: String,
        value: String,
    ): String = id + value.length.toString().padStart(2, '0') + value

    /** Two-decimal baht string from satang, never via floating point. */
    private fun formatAmount(amount: Money): String {
        val baht = amount.satang / 100
        val satang = amount.satang % 100
        return "$baht.${satang.toString().padStart(2, '0')}"
    }

    /** CRC-16/CCITT-FALSE over the payload including the trailing "6304". */
    private fun crc16(input: String): String {
        var crc = CRC_INITIAL
        for (byte in input.encodeToByteArray()) {
            crc = crc xor (byte.toInt() and 0xFF shl 8)
            repeat(8) {
                crc =
                    if (crc and 0x8000 != 0) {
                        crc shl 1 xor CRC_POLYNOMIAL
                    } else {
                        crc shl 1
                    }
                crc = crc and 0xFFFF
            }
        }
        return crc.toString(16).uppercase().padStart(4, '0')
    }
}
