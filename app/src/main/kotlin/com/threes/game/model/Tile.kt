package com.threes.game.model

import kotlin.math.roundToInt

data class Tile(
    val id: Long,
    val value: Int,
    val isBonus: Boolean = false,
    val isMerged: Boolean = false,
    val isNew: Boolean = false
) {
    val isOne: Boolean get() = value == 1
    val isTwo: Boolean get() = value == 2
    val isThreePlus: Boolean get() = value >= 3

    /**
     * Score calculation for Threes:
     * 1 and 2 score 0 points.
     * For V >= 3, score = 3^(1 + log2(V / 3)).
     */
    val score: Int
        get() {
            if (value < 3) return 0
            var rank = 1
            var temp = value / 3
            while (temp > 1) {
                temp /= 2
                rank++
            }
            var result = 1
            repeat(rank) {
                result *= 3
            }
            return result
        }

    companion object {
        private var idCounter = 0L

        fun nextId(): Long = ++idCounter

        fun create(value: Int, isBonus: Boolean = false, isNew: Boolean = false): Tile {
            return Tile(
                id = nextId(),
                value = value,
                isBonus = isBonus,
                isMerged = false,
                isNew = isNew
            )
        }

        fun canMerge(a: Tile?, b: Tile?): Boolean {
            if (a == null || b == null) return false
            if (a.value == 1 && b.value == 2) return true
            if (a.value == 2 && b.value == 1) return true
            if (a.value >= 3 && a.value == b.value) return true
            return false
        }

        fun merge(a: Tile, b: Tile): Tile {
            val newValue = when {
                (a.value == 1 && b.value == 2) || (a.value == 2 && b.value == 1) -> 3
                a.value >= 3 && a.value == b.value -> a.value * 2
                else -> throw IllegalArgumentException("Cannot merge tile ${a.value} with ${b.value}")
            }
            return Tile(
                id = nextId(),
                value = newValue,
                isBonus = false,
                isMerged = true,
                isNew = false
            )
        }
    }
}
