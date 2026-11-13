package com.threes.game.ui.components

import com.threes.game.model.Direction
import kotlin.math.abs

internal object SwipePhysics {
    const val LOCK_THRESHOLD_DP = 3f
    const val FLICK_UP_THRESHOLD_DP = 6f
    const val BLOCKED_RECHECK_MAX_DP = 14f
    const val BLOCKED_HAPTIC_MIN_DP = 6f
    const val SNAP_HAPTIC_MIN_DP = 4f

    const val COMMIT_FRACTION = 0.45f
    const val UNLATCH_FRACTION = 0.41f
    const val FLICK_MIN_FRACTION = 0.14f
    const val FLICK_VELOCITY_DP_PER_SEC = 520f

    fun detectDirection(dx: Float, dy: Float): Direction {
        return if (abs(dx) > abs(dy)) {
            if (dx > 0f) Direction.RIGHT else Direction.LEFT
        } else {
            if (dy > 0f) Direction.DOWN else Direction.UP
        }
    }

    fun signedAxisDelta(dx: Float, dy: Float, dir: Direction): Float {
        return when (dir) {
            Direction.LEFT -> -dx
            Direction.RIGHT -> dx
            Direction.UP -> -dy
            Direction.DOWN -> dy
        }
    }

    fun computeLockOrigin(dx: Float, dy: Float, dir: Direction, maxOriginPx: Float): Float {
        return signedAxisDelta(dx, dy, dir).coerceIn(0f, maxOriginPx)
    }

    /**
     * When an initial lock lands on a completely blocked direction during the very first
     * few dp of a diagonal swipe, allow switching to the perpendicular axis once it clearly
     * dominates and before the user has committed into the blocked push.
     */
    fun shouldRecheckBlockedDirection(
        dx: Float,
        dy: Float,
        currentDir: Direction,
        forwardPx: Float,
        recheckMaxDistPx: Float,
        blockedHapticMinPx: Float,
        minPerpPx: Float
    ): Direction? {
        val totalDistSq = dx * dx + dy * dy
        if (totalDistSq > recheckMaxDistPx * recheckMaxDistPx) return null
        if (forwardPx > blockedHapticMinPx) return null

        val primaryAbs = if (currentDir.isHorizontal) abs(dx) else abs(dy)
        val perpDelta = if (currentDir.isHorizontal) dy else dx
        val perpAbs = abs(perpDelta)
        if (perpAbs < minPerpPx || perpAbs <= primaryAbs * 1.25f) return null

        return if (currentDir.isHorizontal) {
            if (perpDelta > 0f) Direction.DOWN else Direction.UP
        } else {
            if (perpDelta > 0f) Direction.RIGHT else Direction.LEFT
        }
    }

    fun computeVisualDragOffset(
        forwardPx: Float,
        cellSizePx: Float,
        hasMovable: Boolean,
        dir: Direction
    ): Float {
        val f = forwardPx.coerceAtLeast(0f)
        val magnitude = if (!hasMovable) {
            (f * 0.16f).coerceAtMost(cellSizePx * 0.14f)
        } else if (f <= cellSizePx) {
            f
        } else {
            val overshoot = f - cellSizePx
            cellSizePx + (overshoot * 0.14f).coerceAtMost(cellSizePx * 0.10f)
        }
        return when (dir) {
            Direction.LEFT, Direction.UP -> -magnitude
            Direction.RIGHT, Direction.DOWN -> magnitude
        }
    }

    fun shouldCommitMove(
        finishedNormally: Boolean,
        hasMovable: Boolean,
        offsetPx: Float,
        cellSizePx: Float,
        axisVelocityPxPerSec: Float,
        flickVelocityThresholdPxPerSec: Float,
        flickMinDistancePx: Float,
        commitFraction: Float = COMMIT_FRACTION
    ): Boolean {
        if (!finishedNormally || !hasMovable) return false
        val absOffset = abs(offsetPx)
        if (absOffset >= cellSizePx * commitFraction) return true
        return axisVelocityPxPerSec >= flickVelocityThresholdPxPerSec &&
            absOffset >= flickMinDistancePx
    }

    fun computeCommitAnimDurationMs(
        distancePx: Float,
        cellSizePx: Float,
        axisVelocityPxPerSec: Float
    ): Int {
        val safeCell = cellSizePx.coerceAtLeast(1f)
        val distanceRatio = (distancePx / safeCell).coerceIn(0f, 1f)
        val slowFallbackMs = (40f + 38f * distanceRatio).toInt()
        val forwardSpeed = axisVelocityPxPerSec.coerceAtLeast(0f)
        if (forwardSpeed <= 250f) return slowFallbackMs.coerceIn(28, 78)
        val velocityMs = ((distancePx / forwardSpeed) * 1000f * 1.15f).toInt()
        return minOf(slowFallbackMs, velocityMs).coerceIn(28, 78)
    }
}
