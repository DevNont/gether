package com.triptogether.core.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Covers Money parsing and formatting per docs/04-money-logic.md sections 1
 * and 6: pure Long math, fixed locale-independent output, U+2212 for signed
 * negatives.
 */
class MoneyTest {
    @Nested
    @DisplayName("parse")
    inner class Parse {
        @Test fun `rejects negative amounts`() {
            assertNull(Money.parse("-5"))
            assertNull(Money.parse("-1850.00"))
        }

        @Test fun `rejects more than two decimal places`() {
            assertNull(Money.parse("1.234"))
        }

        @Test fun `rejects blank input`() {
            assertNull(Money.parse(""))
            assertNull(Money.parse("   "))
        }

        @Test fun `rejects garbage`() {
            assertNull(Money.parse("abc"))
            assertNull(Money.parse("1.2.3"))
            assertNull(Money.parse("5."))
            assertNull(Money.parse("฿100"))
            assertNull(Money.parse("๑๘๕๐"))
        }

        @Test fun `accepts whole baht`() {
            assertEquals(Money(185_000), Money.parse("1850"))
        }

        @Test fun `single decimal digit means tens of satang`() {
            // "(\d{1,2})" allows one fractional digit; it is scaled by ten,
            // so "1850.5" is 1,850.50 baht = 185050 satang.
            assertEquals(Money(185_050), Money.parse("1850.5"))
        }

        @Test fun `two decimal digits are satang`() {
            assertEquals(Money(185_075), Money.parse("1850.75"))
            assertEquals(Money(1), Money.parse("0.01"))
        }

        @Test fun `thousands separators are stripped before matching`() {
            // parse() removes commas before applying the regex, so grouped
            // input — including format() output — round-trips.
            assertEquals(Money(185_050), Money.parse("1,850.5"))
            assertEquals(Money(185_000), Money.parse("1,850.00"))
        }
    }

    @Nested
    @DisplayName("format")
    inner class Format {
        @Test fun `always two decimals with ascii digits`() {
            assertEquals("0.00", Money.ZERO.format())
            assertEquals("0.01", Money(1).format())
            assertEquals("0.99", Money(99).format())
            assertEquals("1.00", Money(100).format())
            assertEquals("1,850.00", Money(185_000).format())
        }

        @Test fun `groups thousands with commas`() {
            assertEquals("1,234,567.89", Money(123_456_789).format())
            assertEquals("10,000,000,000.00", Money(1_000_000_000_000).format())
        }

        @Test fun `negative amounts use an ascii hyphen`() {
            assertEquals("-0.01", Money(-1).format())
            assertEquals("-1,850.00", Money(-185_000).format())
        }

        @Test fun `round-trips through parse`() {
            val values = listOf(0L, 1L, 99L, 100L, 185_000L, 123_456_789L, 999_999_999_999L)
            for (satang in values) {
                val money = Money(satang)
                assertEquals(money, Money.parse(money.format()), "round-trip failed for $satang")
            }
        }

        @Test fun `formatWithSymbol prefixes the baht sign`() {
            assertEquals("฿1,850.00", Money(185_000).formatWithSymbol())
        }
    }

    @Nested
    @DisplayName("formatSigned")
    inner class FormatSigned {
        @Test fun `positive is prefixed with plus`() {
            assertEquals("+1,240.00", Money(124_000).formatSigned())
        }

        @Test fun `negative uses unicode minus not hyphen`() {
            val text = Money(-83_000).formatSigned()
            assertEquals("−830.00", text)
            assertFalse(text.contains('-'), "formatSigned must use U+2212, not an ASCII hyphen")
        }

        @Test fun `zero has no sign`() {
            assertEquals("0.00", Money.ZERO.formatSigned())
        }
    }
}
