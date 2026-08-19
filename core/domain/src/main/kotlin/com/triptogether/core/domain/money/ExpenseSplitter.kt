package com.triptogether.core.domain.money

import com.triptogether.core.domain.model.Money
import com.triptogether.core.domain.model.Share

/**
 * All split maths for the app. Implements docs/04-money-logic.md section 2.
 *
 * Two guarantees every function here upholds:
 *   1. sum(result) == total, exactly, always.
 *   2. The result is deterministic — same input gives the same output on every
 *      device, so two people looking at one bill never see different numbers.
 *
 * Determinism comes from ordering by memberId, never by list position or by
 * whatever order a query happened to return.
 */
object ExpenseSplitter {
    /**
     * Splits [total] equally between [memberIds].
     *
     * The leftover satang (total % n) are handed out one each, to the members
     * whose ids sort first. See money-logic section 2.4.
     */
    fun splitEqually(
        total: Money,
        memberIds: List<String>,
    ): List<Share> {
        require(memberIds.isNotEmpty()) { "Cannot split between zero members" }
        val ordered = memberIds.sorted()
        val n = ordered.size
        val base = total.satang / n
        val remainder = total.satang - base * n

        return ordered.mapIndexed { index, memberId ->
            val extra = if (index < remainder) 1L else 0L
            Share(memberId = memberId, amount = Money(base + extra))
        }
    }

    /**
     * Splits [total] proportionally to integer weights.
     *
     * Uses the largest-remainder method: whoever lost the most to integer
     * truncation gets the leftover satang first, ties broken by memberId.
     */
    fun splitByWeights(
        total: Money,
        weights: Map<String, Int>,
    ): List<Share> {
        require(weights.isNotEmpty()) { "Cannot split between zero members" }
        require(weights.values.all { it > 0 }) { "Weights must be positive" }

        val ordered = weights.entries.sortedBy { it.key }
        val totalWeight = ordered.sumOf { it.value.toLong() }

        val raw =
            ordered.map { (memberId, weight) ->
                val exact = total.satang * weight
                val floor = exact / totalWeight
                Triple(memberId, floor, exact - floor * totalWeight)
            }

        var remainder = total.satang - raw.sumOf { it.second }
        val bonusIds =
            raw
                .sortedWith(compareByDescending<Triple<String, Long, Long>> { it.third }.thenBy { it.first })
                .take(remainder.toInt())
                .map { it.first }
                .toSet()

        return raw.map { (memberId, floor, _) ->
            val extra = if (memberId in bonusIds) 1L else 0L
            Share(
                memberId = memberId,
                amount = Money(floor + extra),
                weight = weights.getValue(memberId),
            )
        }
    }

    /**
     * Distributes whatever is left of [total] after [enteredShares] across
     * [remainingMemberIds]. Backs the "เกลี่ยส่วนที่เหลือ" button.
     *
     * Returns the full share list: the entered ones untouched, plus the new ones.
     */
    fun distributeRemainder(
        total: Money,
        enteredShares: List<Share>,
        remainingMemberIds: List<String>,
    ): List<Share> {
        if (remainingMemberIds.isEmpty()) return enteredShares
        val entered = Money(enteredShares.sumOf { it.amount.satang })
        val leftover = total - entered
        require(!leftover.isNegative) { "Entered amounts already exceed the total" }
        return enteredShares + splitEqually(leftover, remainingMemberIds)
    }

    /**
     * Adds a service charge / VAT of [percentBasisPoints] (10% = 1000 bp) on
     * top of each share, keeping each person's proportion intact.
     *
     * The new total is `sum(shares) * (10000 + bp) / 10000` in pure Long math,
     * then redistributed with the largest-remainder method of section 2.4:
     * each raw share is truncated, and the leftover satang go one each to the
     * members who lost the most to truncation, ties broken by memberId.
     *
     * Returns the new shares together with the new total, which the caller must
     * write back to the expense — the old total no longer matches.
     */
    fun applySurcharge(
        shares: List<Share>,
        percentBasisPoints: Int,
    ): Pair<List<Share>, Money> {
        require(percentBasisPoints >= 0) { "Surcharge cannot be negative" }
        if (shares.isEmpty()) return emptyList<Share>() to Money.ZERO

        val multiplier = BASIS_POINT_SCALE + percentBasisPoints
        val ordered = shares.sortedBy { it.memberId }
        val newTotal = ordered.sumOf { it.amount.satang } * multiplier / BASIS_POINT_SCALE

        val raw =
            ordered.map { share ->
                val exact = share.amount.satang * multiplier
                val floor = exact / BASIS_POINT_SCALE
                Triple(share, floor, exact - floor * BASIS_POINT_SCALE)
            }

        val remainder = newTotal - raw.sumOf { it.second }
        val bonusIds =
            raw
                .sortedWith(
                    compareByDescending<Triple<Share, Long, Long>> { it.third }.thenBy { it.first.memberId },
                )
                .take(remainder.toInt())
                .map { it.first.memberId }
                .toSet()

        val updated =
            raw.map { (share, floor, _) ->
                val extra = if (share.memberId in bonusIds) 1L else 0L
                share.copy(amount = Money(floor + extra))
            }
        return updated to Money(newTotal)
    }

    /**
     * Total for `ITEMIZED` mode: the bill total is not entered by the user but
     * derived as the sum of the per-member amounts, so the invariant
     * `sum(shares) == totalAmount` holds by construction.
     * See docs/04-money-logic.md section 2.5.
     */
    fun itemizedTotal(shares: List<Share>): Money = Money(shares.sumOf { it.amount.satang })

    /**
     * Difference between the shares as entered and the bill total.
     * Zero means the expense may be saved; anything else must block the save
     * and be shown to the user.
     */
    fun validationDelta(
        total: Money,
        shares: List<Share>,
    ): Money = Money(shares.sumOf { it.amount.satang }) - total

    /** 100% expressed in basis points; 10% = 1000 bp. */
    private const val BASIS_POINT_SCALE = 10_000L
}
