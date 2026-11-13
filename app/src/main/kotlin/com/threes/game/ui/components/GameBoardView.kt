package com.threes.game.ui.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
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
    var hasReachedThreshold by remember { mutableStateOf(false) }

    val currentOnMoveRequested by rememberUpdatedState(onMoveRequested)
    val currentOnSlideCommitStarted by rememberUpdatedState(onSlideCommitStarted)
    val currentGetMovableLines by rememberUpdatedState(getMovableLines)
    val currentGetMovableCells by rememberUpdatedState(getMovableCells)
    val currentOnDragThresholdReached by rememberUpdatedState(onDragThresholdReached)
    val currentOnDragBlocked by rememberUpdatedState(onDragBlocked)
    val currentOnSnapBack by rememberUpdatedState(onSnapBack)

    // Gaze offset only depends on activeDirection (changes once per swipe, not per pixel)
    val lookOffsetX = when (activeDirection) {
        Direction.LEFT -> -0.85f
        Direction.RIGHT -> 0.85f
        else -> 0f
    }
    val lookOffsetY = when (activeDirection) {
        Direction.UP -> -0.85f
        Direction.DOWN -> 0.85f
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
                    val lockThresholdPx = 5.dp.toPx()
                    val flickUpThresholdPx = 8.dp.toPx()
                    val commitFraction = 0.34f

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
                        hasReachedThreshold = false

                        velocityTracker.resetTracking()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)

                        var lockedDir: Direction? = null
                        var lockedCells: Set<Pair<Int, Int>> = emptySet()
                        var finishedNormally = false

                        fun tryLockAndUpdate(delta: Offset, minLockPx: Float) {
                            val liveCellSize = cellSizePxState.floatValue.coerceAtLeast(1f)
                            if (lockedDir == null && delta.getDistance() >= minLockPx) {
                                val dir = if (abs(delta.x) > abs(delta.y)) {
                                    if (delta.x > 0) Direction.RIGHT else Direction.LEFT
                                } else {
                                    if (delta.y > 0) Direction.DOWN else Direction.UP
                                }
                                lockedDir = dir
                                activeDirection = dir
                                val lines = currentGetMovableLines(dir)
                                val cells = currentGetMovableCells(dir)
                                lockedCells = cells
                                movableLineIndices = lines
                                movableCells = cells
                                if (cells.isEmpty()) {
                                    currentOnDragBlocked()
                                }
                            }

                            val dir = lockedDir
                            if (dir != null) {
                                val hasMovable = lockedCells.isNotEmpty()
                                val rawDelta = when (dir) {
                                    Direction.LEFT -> delta.x.coerceIn(-liveCellSize, 0f)
                                    Direction.RIGHT -> delta.x.coerceIn(0f, liveCellSize)
                                    Direction.UP -> delta.y.coerceIn(-liveCellSize, 0f)
                                    Direction.DOWN -> delta.y.coerceIn(0f, liveCellSize)
                                }
                                val targetOffset = if (hasMovable) rawDelta else rawDelta * 0.18f
                                if (hasMovable) {
                                    val isPastThreshold = abs(targetOffset) >= liveCellSize * commitFraction
                                    if (isPastThreshold != hasReachedThreshold) {
                                        hasReachedThreshold = isPastThreshold
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
                                tryLockAndUpdate(delta, flickUpThresholdPx)
                                finishedNormally = true
                                break
                            }

                            if (change.isConsumed) {
                                break
                            }

                            tryLockAndUpdate(delta, lockThresholdPx)
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

                        val shouldCommit = finishedNormally &&
                            currentDir != null &&
                            hasMovable &&
                            (abs(currentOffset) >= liveCellSize * commitFraction ||
                                (axisVelocity >= 550f &&
                                    abs(currentOffset) >= maxOf(flickUpThresholdPx, liveCellSize * 0.08f)))

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
                                    val remainingFraction =
                                        (1f - (abs(currentOffset) / liveCellSize)).coerceIn(0f, 1f)
                                    val animDurationMs =
                                        (55f * remainingFraction).toInt().coerceIn(16, 60)
                                    animate(
                                        initialValue = currentOffset,
                                        targetValue = targetFullSlide,
                                        animationSpec = tween(
                                            durationMillis = animDurationMs,
                                            easing = FastOutLinearInEasing
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
                                    if (hasMovable && abs(currentOffset) > 10f) {
                                        currentOnSnapBack()
                                    }
                                    animate(
                                        initialValue = currentOffset,
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
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
                                    if (progress > 0.2f) {
                                        val peekFraction = ((progress - 0.2f) / 0.8f).coerceIn(0f, 1f)
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
                                    lookOffsetX = lookOffsetX,
                                    lookOffsetY = lookOffsetY,
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
