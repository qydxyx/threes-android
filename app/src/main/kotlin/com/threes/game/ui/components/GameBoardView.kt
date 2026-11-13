package com.threes.game.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.threes.game.model.Direction
import com.threes.game.model.Tile
import com.threes.game.ui.characters.DragExpressionMood
import com.threes.game.ui.theme.BoardPlateDark
import com.threes.game.ui.theme.BoardPlateLight
import com.threes.game.ui.theme.NextHint1
import com.threes.game.ui.theme.NextHint2
import com.threes.game.ui.theme.NextHint3
import com.threes.game.ui.theme.NextHint3Dark
import com.threes.game.ui.theme.NextHintBonus
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun GameBoardView(
    grid: List<List<Tile?>>,
    nextTile: Tile,
    lastMoveDirection: Direction? = null,
    onMoveRequested: (Direction) -> Unit,
    onSlideCommitStarted: () -> Unit = {},
    getMovableLines: (Direction) -> List<Int>,
    getMovableCells: (Direction) -> Set<Pair<Int, Int>>,
    getMergingCells: (Direction) -> Set<Pair<Int, Int>> = { emptySet() },
    onDragThresholdReached: () -> Unit = {},
    onDragBlocked: () -> Unit = {},
    onSnapBack: () -> Unit = {},
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Live measured width of a single inner grid cell (inside outer 16.dp and inner 8.dp padding).
    val cellSizePxState = remember { mutableFloatStateOf(250f) }

    // Synchronous float state for 120Hz/240Hz finger tracking — read ONLY inside graphicsLayer
    // lambdas so drag movement never triggers Composable recomposition or relayout.
    val dragOffsetPx = remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    var settleToken by remember { mutableIntStateOf(0) }
    var pendingCommitDir by remember { mutableStateOf<Direction?>(null) }

    var activeDirection by remember { mutableStateOf<Direction?>(null) }
    var movableLineIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var movableCells by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }
    var mergingCells by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }
    var hasReachedThreshold by remember { mutableStateOf(false) }

    val currentOnMoveRequested by rememberUpdatedState(onMoveRequested)
    val currentOnSlideCommitStarted by rememberUpdatedState(onSlideCommitStarted)
    val currentGetMovableLines by rememberUpdatedState(getMovableLines)
    val currentGetMovableCells by rememberUpdatedState(getMovableCells)
    val currentGetMergingCells by rememberUpdatedState(getMergingCells)
    val currentOnDragThresholdReached by rememberUpdatedState(onDragThresholdReached)
    val currentOnDragBlocked by rememberUpdatedState(onDragBlocked)
    val currentOnSnapBack by rememberUpdatedState(onSnapBack)

    // Gaze direction unit vector (smoothly scaled by drag progress inside Canvas draw phase)
    val lookOffsetX = when (activeDirection) {
        Direction.LEFT -> -1f
        Direction.RIGHT -> 1f
        else -> 0f
    }
    val lookOffsetY = when (activeDirection) {
        Direction.UP -> -1f
        Direction.DOWN -> 1f
        else -> 0f
    }

    val nextPeekColor = when {
        nextTile.isBonus -> NextHintBonus
        nextTile.isOne -> NextHint1
        nextTile.isTwo -> NextHint2
        else -> if (isDarkTheme) NextHint3Dark else NextHint3
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val boardSide = minOf(maxWidth, maxHeight)

        Box(
            modifier = Modifier
                .size(boardSide)
                .padding(16.dp)
                .shadow(6.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(if (isDarkTheme) BoardPlateDark else BoardPlateLight)
                .pointerInput(Unit) {
                    val velocityTracker = VelocityTracker()
                    val lockThresholdPx = SwipePhysics.LOCK_THRESHOLD_DP.dp.toPx()
                    val flickUpThresholdPx = SwipePhysics.FLICK_UP_THRESHOLD_DP.dp.toPx()
                    val blockedRecheckMaxPx = SwipePhysics.BLOCKED_RECHECK_MAX_DP.dp.toPx()
                    val blockedHapticMinPx = SwipePhysics.BLOCKED_HAPTIC_MIN_DP.dp.toPx()
                    val blockedRecheckMinPerpPx = 4.dp.toPx()
                    val snapHapticMinPx = SwipePhysics.SNAP_HAPTIC_MIN_DP.dp.toPx()
                    val flickVelocityThresholdPxPerSec =
                        SwipePhysics.FLICK_VELOCITY_DP_PER_SEC.dp.toPx()

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // Invalidate any in-flight settle/snap animation before touching shared state
                        settleToken++
                        settleJob?.cancel()
                        settleJob = null

                        // If the previous swipe's short commit slide-out was still animating,
                        // flush the state commit immediately so rapid back-to-back swipes never stall.
                        pendingCommitDir?.let { committedDir ->
                            pendingCommitDir = null
                            currentOnMoveRequested(committedDir)
                        }

                        dragOffsetPx.floatValue = 0f
                        activeDirection = null
                        movableLineIndices = emptyList()
                        movableCells = emptySet()
                        mergingCells = emptySet()
                        hasReachedThreshold = false

                        velocityTracker.resetTracking()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)

                        var lockedDir: Direction? = null
                        var lockOriginPx = 0f
                        var lockedCells: Set<Pair<Int, Int>> = emptySet()
                        var hasNotifiedBlocked = false
                        var finishedNormally = false

                        fun tryLockAndUpdate(delta: Offset, minLockPx: Float, isUp: Boolean) {
                            val liveCellSize = cellSizePxState.floatValue.coerceAtLeast(1f)
                            if (lockedDir == null && delta.getDistance() >= minLockPx) {
                                val dir = SwipePhysics.detectDirection(delta.x, delta.y)
                                lockedDir = dir
                                lockOriginPx = SwipePhysics.computeLockOrigin(
                                    dx = delta.x,
                                    dy = delta.y,
                                    dir = dir,
                                    maxOriginPx = lockThresholdPx
                                )
                                activeDirection = dir
                                val lines = currentGetMovableLines(dir)
                                val cells = currentGetMovableCells(dir)
                                lockedCells = cells
                                movableLineIndices = lines
                                movableCells = cells
                                mergingCells = currentGetMergingCells(dir)
                            }

                            val currentLocked = lockedDir
                            if (currentLocked != null && lockedCells.isEmpty() && !hasNotifiedBlocked) {
                                val currentForward = (
                                    SwipePhysics.signedAxisDelta(
                                        delta.x,
                                        delta.y,
                                        currentLocked
                                    ) - lockOriginPx
                                    ).coerceAtLeast(0f)
                                val recheckDir = SwipePhysics.shouldRecheckBlockedDirection(
                                    dx = delta.x,
                                    dy = delta.y,
                                    currentDir = currentLocked,
                                    forwardPx = currentForward,
                                    recheckMaxDistPx = blockedRecheckMaxPx,
                                    blockedHapticMinPx = blockedHapticMinPx,
                                    minPerpPx = blockedRecheckMinPerpPx
                                )
                                if (recheckDir != null) {
                                    val candidateCells = currentGetMovableCells(recheckDir)
                                    if (candidateCells.isNotEmpty()) {
                                        lockedDir = recheckDir
                                        lockOriginPx = SwipePhysics.computeLockOrigin(
                                            dx = delta.x,
                                            dy = delta.y,
                                            dir = recheckDir,
                                            maxOriginPx = lockThresholdPx
                                        )
                                        activeDirection = recheckDir
                                        movableLineIndices = currentGetMovableLines(recheckDir)
                                        movableCells = candidateCells
                                        mergingCells = currentGetMergingCells(recheckDir)
                                        lockedCells = candidateCells
                                    }
                                }
                            }

                            val dir = lockedDir
                            if (dir != null) {
                                val hasMovable = lockedCells.isNotEmpty()
                                val forwardPx = (
                                    SwipePhysics.signedAxisDelta(delta.x, delta.y, dir) -
                                        lockOriginPx
                                    ).coerceAtLeast(0f)

                                if (!hasMovable &&
                                    !hasNotifiedBlocked &&
                                    (isUp || forwardPx >= blockedHapticMinPx)
                                ) {
                                    hasNotifiedBlocked = true
                                    currentOnDragBlocked()
                                }

                                val targetOffset = SwipePhysics.computeVisualDragOffset(
                                    forwardPx = forwardPx,
                                    cellSizePx = liveCellSize,
                                    hasMovable = hasMovable,
                                    dir = dir
                                )

                                if (hasMovable) {
                                    val progress = abs(targetOffset) / liveCellSize
                                    val nextThresholdState = if (hasReachedThreshold) {
                                        progress >= SwipePhysics.UNLATCH_FRACTION
                                    } else {
                                        progress >= SwipePhysics.COMMIT_FRACTION
                                    }
                                    if (nextThresholdState != hasReachedThreshold) {
                                        hasReachedThreshold = nextThresholdState
                                        currentOnDragThresholdReached()
                                    }
                                }
                                dragOffsetPx.floatValue = targetOffset
                            }
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val delta = change.position - down.position

                            if (change.changedToUpIgnoreConsumed()) {
                                change.consume()
                                tryLockAndUpdate(delta, flickUpThresholdPx, isUp = true)
                                finishedNormally = true
                                break
                            }

                            if (change.isConsumed) {
                                break
                            }

                            tryLockAndUpdate(delta, lockThresholdPx, isUp = false)
                            if (lockedDir != null) {
                                change.consume()
                            }
                        }

                        val currentDir = lockedDir
                        val currentOffset = dragOffsetPx.floatValue
                        val hasMovable = lockedCells.isNotEmpty()
                        val liveCellSize = cellSizePxState.floatValue.coerceAtLeast(1f)
                        val velocity = velocityTracker.calculateVelocity()
                        val axisVelocity = when (currentDir) {
                            Direction.LEFT -> -velocity.x
                            Direction.RIGHT -> velocity.x
                            Direction.UP -> -velocity.y
                            Direction.DOWN -> velocity.y
                            null -> 0f
                        }

                        val flickMinDistancePx =
                            maxOf(flickUpThresholdPx, liveCellSize * SwipePhysics.FLICK_MIN_FRACTION)
                        val shouldCommit = currentDir != null && SwipePhysics.shouldCommitMove(
                            finishedNormally = finishedNormally,
                            hasMovable = hasMovable,
                            offsetPx = currentOffset,
                            cellSizePx = liveCellSize,
                            axisVelocityPxPerSec = axisVelocity,
                            flickVelocityThresholdPxPerSec = flickVelocityThresholdPxPerSec,
                            flickMinDistancePx = flickMinDistancePx
                        )

                        val myToken = ++settleToken
                        settleJob = coroutineScope.launch {
                            try {
                                if (shouldCommit && currentDir != null) {
                                    pendingCommitDir = currentDir
                                    currentOnSlideCommitStarted()
                                    val targetFullSlide = when (currentDir) {
                                        Direction.LEFT, Direction.UP -> -liveCellSize
                                        Direction.RIGHT, Direction.DOWN -> liveCellSize
                                    }
                                    val distancePx = abs(targetFullSlide - currentOffset)
                                    val animDurationMs = SwipePhysics.computeCommitAnimDurationMs(
                                        distancePx = distancePx,
                                        cellSizePx = liveCellSize,
                                        axisVelocityPxPerSec = axisVelocity
                                    )
                                    animate(
                                        initialValue = currentOffset,
                                        targetValue = targetFullSlide,
                                        animationSpec = tween(
                                            durationMillis = animDurationMs,
                                            easing = LinearOutSlowInEasing
                                        )
                                    ) { value, _ ->
                                        if (myToken == settleToken) {
                                            dragOffsetPx.floatValue = value
                                        }
                                    }
                                    if (myToken == settleToken && pendingCommitDir == currentDir) {
                                        pendingCommitDir = null
                                        currentOnMoveRequested(currentDir)
                                        dragOffsetPx.floatValue = 0f
                                    }
                                } else {
                                    if (hasMovable && abs(currentOffset) > snapHapticMinPx) {
                                        currentOnSnapBack()
                                    }
                                    val releaseVelocity = when {
                                        currentDir == null -> 0f
                                        currentDir.isHorizontal -> velocity.x
                                        else -> velocity.y
                                    }.coerceIn(-6000f, 6000f)
                                    animate(
                                        initialValue = currentOffset,
                                        targetValue = 0f,
                                        initialVelocity = releaseVelocity,
                                        animationSpec = spring(
                                            dampingRatio = 0.90f,
                                            stiffness = 3800f,
                                            visibilityThreshold = 0.75f
                                        )
                                    ) { value, _ ->
                                        if (myToken == settleToken) {
                                            dragOffsetPx.floatValue = value
                                        }
                                    }
                                }
                            } finally {
                                if (myToken == settleToken) {
                                    pendingCommitDir = null
                                    activeDirection = null
                                    movableLineIndices = emptyList()
                                    movableCells = emptySet()
                                    mergingCells = emptySet()
                                    hasReachedThreshold = false
                                }
                            }
                        }
                    }
                }
                .padding(8.dp)
        ) {
            // Layer 1: Stationary 4x4 Empty Cell Recesses
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                for (r in 0 until 4) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (c in 0 until 4) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyCellSlot(isDarkTheme = isDarkTheme)
                            }
                        }
                    }
                }
            }

            // Layer 2: Flat Z-Indexed Overlay for Incoming Edge Peeks & Active Tiles
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val cellDp = maxWidth / 4f
                val cellPx = with(density) { cellDp.toPx() }.coerceAtLeast(1f)
                SideEffect {
                    cellSizePxState.floatValue = cellPx
                }

                val dir = activeDirection
                val dragProgressProvider = remember {
                    {
                        val cell = cellSizePxState.floatValue.coerceAtLeast(1f)
                        val denom = if (activeDirection != null && movableCells.isEmpty()) {
                            cell * 0.12f
                        } else {
                            cell
                        }
                        (abs(dragOffsetPx.floatValue) / denom).coerceIn(0f, 1f)
                    }
                }

                // Incoming card edge peek previews
                if (dir != null && movableCells.isNotEmpty()) {
                    for (lineIdx in movableLineIndices) {
                        val (spawnR, spawnC) = when (dir) {
                            Direction.LEFT -> lineIdx to 3
                            Direction.RIGHT -> lineIdx to 0
                            Direction.UP -> 3 to lineIdx
                            Direction.DOWN -> 0 to lineIdx
                        }
                        Box(
                            modifier = Modifier
                                .size(cellDp)
                                .zIndex(-1f)
                                .graphicsLayer {
                                    val progress =
                                        (abs(dragOffsetPx.floatValue) / cellPx).coerceIn(0f, 1f)
                                    val baseX = spawnC * cellPx
                                    val baseY = spawnR * cellPx
                                    if (progress > 0.15f) {
                                        val peekFraction = ((progress - 0.15f) / 0.85f).coerceIn(0f, 1f)
                                        val peekShift = (1f - peekFraction * 0.38f) * cellPx
                                        translationX = baseX + when (dir) {
                                            Direction.LEFT -> peekShift
                                            Direction.RIGHT -> -peekShift
                                            else -> 0f
                                        }
                                        translationY = baseY + when (dir) {
                                            Direction.UP -> peekShift
                                            Direction.DOWN -> -peekShift
                                            else -> 0f
                                        }
                                        alpha = peekFraction * 0.55f
                                    } else {
                                        translationX = baseX
                                        translationY = baseY
                                        alpha = 0f
                                    }
                                }
                                .padding(6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(nextPeekColor)
                        )
                    }
                }

                // Active tiles keyed by tile.id across the whole board so sliding between cells
                // never destroys/recreates TileView or resets blinking state.
                val isAllBlocked = dir != null && movableCells.isEmpty()
                for (r in 0 until 4) {
                    for (c in 0 until 4) {
                        val tile = grid.getOrNull(r)?.getOrNull(c) ?: continue
                        key(tile.id) {
                            val isCellMovable = dir != null && movableCells.contains(r to c)
                            val isCellMerging = dir != null && mergingCells.contains(r to c)
                            val isStationaryMergeReceiver = isCellMerging && !isCellMovable
                            val tileMood = when {
                                dir == null -> DragExpressionMood.NEUTRAL
                                isCellMerging -> DragExpressionMood.WILL_MERGE
                                isCellMovable -> DragExpressionMood.MOVING
                                else -> DragExpressionMood.BLOCKED
                            }
                            val tileLookX = if (isStationaryMergeReceiver) -lookOffsetX else lookOffsetX
                            val tileLookY = if (isStationaryMergeReceiver) -lookOffsetY else lookOffsetY
                            val translateHoriz = dir?.isHorizontal == true && (isCellMovable || isAllBlocked)
                            val translateVert = dir?.isVertical == true && (isCellMovable || isAllBlocked)
                            val tileZIndex = if (isCellMovable || isAllBlocked) 1f else 0f

                            Box(
                                modifier = Modifier
                                    .size(cellDp)
                                    .zIndex(tileZIndex)
                                    .graphicsLayer {
                                        val offset = dragOffsetPx.floatValue
                                        translationX = c * cellPx + if (translateHoriz) offset else 0f
                                        translationY = r * cellPx + if (translateVert) offset else 0f
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                TileView(
                                    tile = tile,
                                    isDarkTheme = isDarkTheme,
                                    lookOffsetX = tileLookX,
                                    lookOffsetY = tileLookY,
                                    dragProgressProvider = dragProgressProvider,
                                    expressionMood = tileMood,
                                    spawnFromDirection = lastMoveDirection,
                                    cellSizePx = cellPx
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
