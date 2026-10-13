package com.threes.game.ui.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.threes.game.model.Direction
import com.threes.game.model.Tile
import com.threes.game.ui.theme.BoardPlateDark
import com.threes.game.ui.theme.BoardPlateLight
import com.threes.game.ui.theme.NextHint1
import com.threes.game.ui.theme.NextHint2
import com.threes.game.ui.theme.NextHint3
import com.threes.game.ui.theme.NextHintBonus
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun GameBoardView(
    grid: List<List<Tile?>>,
    nextTile: Tile,
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

    // Backed by MutableFloatState so pointerInput(Unit) always reads the live measured cell width
    // rather than capturing the unmeasured initial fallback value.
    var cellSizePx by remember { mutableFloatStateOf(250f) }

    // Synchronous float state for 120Hz/240Hz finger tracking — read ONLY inside graphicsLayer
    // lambdas so drag movement never triggers Composable recomposition or relayout.
    val dragOffsetPx = remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    var activeDirection by remember { mutableStateOf<Direction?>(null) }
    var movableLineIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var movableCells by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }
    var totalDragAccumulator by remember { mutableStateOf(Offset.Zero) }
    var dragOriginOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragLocked by remember { mutableStateOf(false) }
    var hasReachedThreshold by remember { mutableStateOf(false) }
    var isCommittingMove by remember { mutableStateOf(false) }

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
        else -> if (isDarkTheme) Color(0xFFFAF6EE) else NextHint3
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDarkTheme) BoardPlateDark else BoardPlateLight)
            .onSizeChanged { size ->
                if (size.width > 0) {
                    cellSizePx = size.width / 4f
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        if (isCommittingMove) return@detectDragGestures
                        settleJob?.cancel()
                        dragOffsetPx.floatValue = 0f
                        totalDragAccumulator = Offset.Zero
                        dragOriginOffset = Offset.Zero
                        isDragLocked = false
                        activeDirection = null
                        movableLineIndices = emptyList()
                        movableCells = emptySet()
                        hasReachedThreshold = false
                    },
                    onDragEnd = {
                        if (isCommittingMove) return@detectDragGestures
                        val currentDir = activeDirection
                        val currentOffset = dragOffsetPx.floatValue
                        val hasMovable = movableCells.isNotEmpty()
                        val liveCellSize = cellSizePx

                        settleJob?.cancel()
                        settleJob = coroutineScope.launch {
                            if (currentDir != null && hasMovable && abs(currentOffset) >= liveCellSize * 0.38f) {
                                isCommittingMove = true
                                currentOnSlideCommitStarted()
                                val targetFullSlide = if (currentOffset < 0f) -liveCellSize else liveCellSize
                                val remainingFraction =
                                    (1f - (abs(currentOffset) / liveCellSize)).coerceIn(0f, 1f)
                                val animDurationMs = (55f * remainingFraction).toInt().coerceIn(16, 60)
                                animate(
                                    initialValue = currentOffset,
                                    targetValue = targetFullSlide,
                                    animationSpec = tween(
                                        durationMillis = animDurationMs,
                                        easing = FastOutLinearInEasing
                                    )
                                ) { value, _ ->
                                    dragOffsetPx.floatValue = value
                                }
                                currentOnMoveRequested(currentDir)
                                dragOffsetPx.floatValue = 0f
                                isCommittingMove = false
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
                                    dragOffsetPx.floatValue = value
                                }
                            }
                            activeDirection = null
                            isDragLocked = false
                            movableLineIndices = emptyList()
                            movableCells = emptySet()
                            hasReachedThreshold = false
                        }
                    },
                    onDragCancel = {
                        if (isCommittingMove) return@detectDragGestures
                        settleJob?.cancel()
                        settleJob = coroutineScope.launch {
                            animate(
                                initialValue = dragOffsetPx.floatValue,
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 80)
                            ) { value, _ ->
                                dragOffsetPx.floatValue = value
                            }
                            activeDirection = null
                            isDragLocked = false
                            movableLineIndices = emptyList()
                            movableCells = emptySet()
                            hasReachedThreshold = false
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (isCommittingMove) return@detectDragGestures
                        totalDragAccumulator += dragAmount

                        if (!isDragLocked) {
                            val dx = totalDragAccumulator.x
                            val dy = totalDragAccumulator.y
                            val dist = totalDragAccumulator.getDistance()

                            // detectDragGestures already waits for touch slop, so lock direction immediately
                            if (dist > 2f) {
                                isDragLocked = true
                                dragOriginOffset = totalDragAccumulator
                                val dir = if (abs(dx) > abs(dy)) {
                                    if (dx > 0) Direction.RIGHT else Direction.LEFT
                                } else {
                                    if (dy > 0) Direction.DOWN else Direction.UP
                                }
                                activeDirection = dir
                                val lines = currentGetMovableLines(dir)
                                val cells = currentGetMovableCells(dir)
                                movableLineIndices = lines
                                movableCells = cells
                                if (cells.isEmpty()) {
                                    currentOnDragBlocked()
                                }
                            }
                        }

                        if (isDragLocked && activeDirection != null) {
                            val dir = activeDirection!!
                            val hasMovable = movableCells.isNotEmpty()
                            val liveCellSize = cellSizePx
                            val effectiveDelta = totalDragAccumulator - dragOriginOffset

                            val rawDelta = when (dir) {
                                Direction.LEFT -> effectiveDelta.x.coerceIn(-liveCellSize, 0f)
                                Direction.RIGHT -> effectiveDelta.x.coerceIn(0f, liveCellSize)
                                Direction.UP -> effectiveDelta.y.coerceIn(-liveCellSize, 0f)
                                Direction.DOWN -> effectiveDelta.y.coerceIn(0f, liveCellSize)
                            }

                            val targetOffset = if (hasMovable) {
                                rawDelta
                            } else {
                                rawDelta * 0.18f
                            }

                            if (hasMovable) {
                                val isPastThreshold = abs(targetOffset) >= liveCellSize * 0.38f
                                if (isPastThreshold != hasReachedThreshold) {
                                    hasReachedThreshold = isPastThreshold
                                    currentOnDragThresholdReached()
                                }
                            }

                            // Synchronous update on UI thread — zero coroutine launch overhead
                            dragOffsetPx.floatValue = targetOffset
                        }
                    }
                )
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

        // Layer 2: Incoming Card Edge Peeking & Active Tiles
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
                        val tile = grid.getOrNull(r)?.getOrNull(c)
                        val dir = activeDirection
                        val isAllBlocked = dir != null && movableCells.isEmpty()
                        val isCellMovable = dir != null && movableCells.contains(r to c)
                        val translateHoriz = dir?.isHorizontal == true && (isCellMovable || isAllBlocked)
                        val translateVert = dir?.isVertical == true && (isCellMovable || isAllBlocked)

                        val isSpawnEdgeCandidate = when (dir) {
                            Direction.LEFT -> c == 3 && movableLineIndices.contains(r)
                            Direction.RIGHT -> c == 0 && movableLineIndices.contains(r)
                            Direction.UP -> r == 3 && movableLineIndices.contains(c)
                            Direction.DOWN -> r == 0 && movableLineIndices.contains(c)
                            null -> false
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Subtle incoming card edge peek — reads dragOffsetPx ONLY inside graphicsLayer
                            if (isSpawnEdgeCandidate) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                        .graphicsLayer {
                                            val liveCell = cellSizePx.coerceAtLeast(1f)
                                            val progress =
                                                (abs(dragOffsetPx.floatValue) / liveCell).coerceIn(0f, 1f)
                                            if (progress > 0.2f) {
                                                val peekFraction = ((progress - 0.2f) / 0.8f).coerceIn(0f, 1f)
                                                val peekShift = (1f - peekFraction * 0.38f) * liveCell
                                                translationX = when (dir) {
                                                    Direction.LEFT -> peekShift
                                                    Direction.RIGHT -> -peekShift
                                                    else -> 0f
                                                }
                                                translationY = when (dir) {
                                                    Direction.UP -> peekShift
                                                    Direction.DOWN -> -peekShift
                                                    else -> 0f
                                                }
                                                alpha = peekFraction * 0.55f
                                            } else {
                                                alpha = 0f
                                            }
                                        }
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(nextPeekColor)
                                )
                            }

                            if (tile != null) {
                                key(tile.id) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                val offset = dragOffsetPx.floatValue
                                                translationX = if (translateHoriz) offset else 0f
                                                translationY = if (translateVert) offset else 0f
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        TileView(
                                            tile = tile,
                                            isDarkTheme = isDarkTheme,
                                            lookOffsetX = lookOffsetX,
                                            lookOffsetY = lookOffsetY
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
