package com.threes.game.ui.components

import com.threes.game.model.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipePhysicsTest {

    @Test
    fun computeLockOriginPreventsInitialJumpWhilePreservingFastFlick() {
        // Normal slow lock just past 3dp (9px on 3x screen): origin absorbs lock threshold
        val slowOrigin = SwipePhysics.computeLockOrigin(
            dx = 9.5f,
            dy = 1f,
            dir = Direction.RIGHT,
            maxOriginPx = 9f
        )
        assertEquals(9f, slowOrigin, 0.001f)
        assertEquals(0.5f, 9.5f - slowOrigin, 0.001f)

        // Single-frame fast swipe jumping 120px left: caps lockOrigin at 9px so flick distance remains intact
        val fastOrigin = SwipePhysics.computeLockOrigin(
            dx = -120f,
            dy = 5f,
            dir = Direction.LEFT,
            maxOriginPx = 9f
        )
        assertEquals(9f, fastOrigin, 0.001f)
        val forward = SwipePhysics.signedAxisDelta(-120f, 5f, Direction.LEFT) - fastOrigin
        assertEquals(111f, forward, 0.001f)
    }

    @Test
    fun shouldRecheckBlockedDirectionRescuesDiagonalSlantWithinWindow() {
        // Slightly diagonal swipe initially locked UP (blocked), now moving clearly RIGHT
        val recheck = SwipePhysics.shouldRecheckBlockedDirection(
            dx = 22f,
            dy = -10f,
            currentDir = Direction.UP,
            forwardPx = 1f,
            recheckMaxDistPx = 42f,
            blockedHapticMinPx = 18f,
            minPerpPx = 12f
        )
        assertEquals(Direction.RIGHT, recheck)

        // Once user pushes firmly into the blocked direction, do not hijack to perpendicular axis
        val deliberateBlocked = SwipePhysics.shouldRecheckBlockedDirection(
            dx = 26f,
            dy = -22f,
            currentDir = Direction.UP,
            forwardPx = 19f,
            recheckMaxDistPx = 42f,
            blockedHapticMinPx = 18f,
            minPerpPx = 12f
        )
        assertNull(deliberateBlocked)
    }

    @Test
    fun computeVisualDragOffsetIsOneToOneInsideCellAndRubberBandsPastEnd() {
        val cell = 240f
        assertEquals(-100f, SwipePhysics.computeVisualDragOffset(100f, cell, true, Direction.LEFT), 0.001f)
        assertEquals(240f, SwipePhysics.computeVisualDragOffset(240f, cell, true, Direction.RIGHT), 0.001f)

        // Beyond 100%, rubber-band adds 14% cushion capped at +10% cell width
        val slightOvershoot = SwipePhysics.computeVisualDragOffset(340f, cell, true, Direction.DOWN)
        assertEquals(254f, slightOvershoot, 0.001f)

        val maxOvershoot = SwipePhysics.computeVisualDragOffset(1000f, cell, true, Direction.DOWN)
        assertEquals(264f, maxOvershoot, 0.001f)

        // Blocked direction uses heavy 16% resistance capped at 14% cell width
        val blockedNudge = SwipePhysics.computeVisualDragOffset(50f, cell, false, Direction.UP)
        assertEquals(-8f, blockedNudge, 0.001f)
    }

    @Test
    fun shouldCommitMoveRequiresFull45PercentOrGenuineHighSpeedFlick() {
        val cell = 240f
        val flickVelThreshold = 1560f // ~520dp/s on 3x density
        val flickMinDist = cell * SwipePhysics.FLICK_MIN_FRACTION // 33.6px

        // Gentle push to 38% with moderate drift velocity (old 550px/s would falsely commit!)
        assertFalse(
            SwipePhysics.shouldCommitMove(
                finishedNormally = true,
                hasMovable = true,
                offsetPx = cell * 0.38f,
                cellSizePx = cell,
                axisVelocityPxPerSec = 700f,
                flickVelocityThresholdPxPerSec = flickVelThreshold,
                flickMinDistancePx = flickMinDist
            )
        )

        // Reaching 45% commits even at 0 release velocity
        assertTrue(
            SwipePhysics.shouldCommitMove(
                finishedNormally = true,
                hasMovable = true,
                offsetPx = -cell * 0.45f,
                cellSizePx = cell,
                axisVelocityPxPerSec = 0f,
                flickVelocityThresholdPxPerSec = flickVelThreshold,
                flickMinDistancePx = flickMinDist
            )
        )

        // Crisp flick past 14% with >= 520dp/s velocity commits cleanly
        assertTrue(
            SwipePhysics.shouldCommitMove(
                finishedNormally = true,
                hasMovable = true,
                offsetPx = cell * 0.16f,
                cellSizePx = cell,
                axisVelocityPxPerSec = 1650f,
                flickVelocityThresholdPxPerSec = flickVelThreshold,
                flickMinDistancePx = flickMinDist
            )
        )
    }

    @Test
    fun computeCommitAnimDurationAdaptsToReleaseVelocity() {
        val fastMs = SwipePhysics.computeCommitAnimDurationMs(
            distancePx = 120f,
            cellSizePx = 240f,
            axisVelocityPxPerSec = 3200f
        )
        val slowMs = SwipePhysics.computeCommitAnimDurationMs(
            distancePx = 120f,
            cellSizePx = 240f,
            axisVelocityPxPerSec = 100f
        )
        assertTrue("Fast flick should settle briskly ($fastMs < $slowMs)", fastMs < slowMs)
        assertTrue(fastMs in 28..78)
        assertTrue(slowMs in 28..78)
    }
}
