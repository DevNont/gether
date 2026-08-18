package com.triptogether.core.domain.model

import java.text.DecimalFormat
import kotlin.jvm.JvmInline

/**
 * Money stored as an integer number of satang (1/100 THB).
 *
 * Floating point is never used for money anywhere in this codebase. See
 * docs/04-money-logic.md section 1 for the rationale.
 */
@JvmInline
value class Money(val satang: Long) : Comparable<Money> {
    val isZero: Boolean get() = satang == 0L
    val isPositive: Boolean get() = satang > 0L
    val isNegative: Boolean get() = satang < 0L

    operator fun plus(other: Money) = Money(satang + other.satang)

    operator fun minus(other: Money) = Money(satang - other.satang)

    operator fun times(n: Int) = Money(satang * n)

    operator fun unaryMinus() = Money(-satang)

    override fun compareTo(other: Money): Int = satang.compareTo(other.satang)

    fun abs() = Money(kotlin.math.abs(satang))

    /** "1,850.00" — no currency symbol. */
    fun format(): String = FORMAT.format(satang / 100.0)

    /** "฿1,850.00" */
    fun formatWithSymbol(): String = "฿" + format()

    /** "+1,240.00" / "−830.00" (U+2212 minus, not hyphen) / "0.00" */
    fun formatSigned(): String =
        when {
            satang > 0 -> "+" + format()
            satang < 0 -> "\u2212" + Money(-satang).format()
            else -> format()
        }

    override fun toString(): String = "Money(${format()})"

    companion object {
        val ZERO = Money(0)

        private val FORMAT = DecimalFormat("#,##0.00")

        fun fromBaht(baht: Long) = Money(baht * 100)

        /**
         * Parses user input like "1850", "1,850.5", "1850.75".
         * Returns null when the text is not a valid non-negative amount.
         */
        fun parse(input: String): Money? {
            val cleaned = input.trim().replace(",", "").replace("\u0020", "")
            if (cleaned.isEmpty()) return null
            val match = Regex("""^(\d+)(?:\.(\d{1,2}))?$""").matchEntire(cleaned) ?: return null
            val whole = match.groupValues[1].toLongOrNull() ?: return null
            val fracText = match.groupValues[2]
            val frac =
                when (fracText.length) {
                    0 -> 0L
                    1 -> fracText.toLong() * 10
                    else -> fracText.toLong()
                }
            return Money(whole * 100 + frac)
        }
    }
}

fun Iterable<Money>.sum(): Money = Money(sumOf { it.satang })
