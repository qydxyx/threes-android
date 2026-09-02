package com.threes.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.threes.game.model.Direction
import com.threes.game.model.Tile
import com.threes.game.ui.theme.BoardPlateDark
import com.threes.game.ui.theme.BoardPlateLight
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun GameBoardView(
    grid: Array<Array<Tile?>>,
    nextTile: Tile,
    onMoveRequested: (Direction) -> Unit,
    getMovableLines: (Direction) -> List<Int>,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val cellSizePx = if (boardSize.width > 0) (boardSize.width / 4f) else 100f

    val dragOffsetAnim = remember { Animatable(0f) }
    var activeDirection by remember { mutableStateOf<Direction?>(null) }
    var movableLineIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var totalDragAccumulator by remember { mutableStateOf(Offset.Zero) }
    var isDragLocked by remember { mutableStateOf(false) }

    val lookOffsetX = remember(dragOffsetAnim.value, activeDirection) {
        when (activeDirection) {
            Direction.LEFT -> -0.8f
            Direction.RIGHT -> 0.8f
            else -> 0f
        }
    }
    val lookOffsetY = remember(dragOffsetAnim.value, activeDirection) {
        when (activeDirection) {
            Direction.UP -> -0.8f
            Direction.DOWN -> 0.8f
            else -> 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDarkTheme) BoardPlateDark else BoardPlateLight)
            .onSizeChanged { boardSize = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        totalDragAccumulator = Offset.Zero
                        isDragLocked = false
                        activeDirection = null
                        movableLineIndices = emptyList()
                    },
                    onDragEnd = {
                        val currentDir = activeDirection
                        val currentOffset = dragOffsetAnim.value
                        val hasMovable = movableLineIndices.isNotEmpty()

                        coroutineScope.launch {
                            if (currentDir != null && hasMovable && abs(currentOffset) >= cellSizePx * 0.42f) {
                                // Commit move
                                onMoveRequested(currentDir)
                                dragOffsetAnim.snapTo(0f)
                            } else {
                                // Snap back with bouncy spring
                                dragOffsetAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                            activeDirection = null
                            isDragLocked = false
                            movableLineIndices = emptyList()
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            dragOffsetAnim.animateTo(0f)
                            activeDirection = null
                            isDragLocked = false
                            movableLineIndices = emptyList()
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragAccumulator += dragAmount

                        if (!isDragLocked) {
                            val dx = totalDragAccumulator.x
                            val dy = totalDragAccumulator.y
                            val dist = totalDragAccumulator.getDistance()

                            if (dist > 15f) {
                                isDragLocked = true
                                val dir = if (abs(dx) > abs(dy)) {
                                    if (dx > 0) Direction.RIGHT else Direction.LEFT
                                } else {
                                    if (dy > 0) Direction.DOWN else Direction.UP
                                }
                                activeDirection = dir
                                movableLineIndices = getMovableLines(dir)
                            }
                        }

                        if (isDragLocked && activeDirection != null) {
                            val dir = activeDirection!!
                            val hasMovable = movableLineIndices.isNotEmpty()

                            val rawDelta = when (dir) {
                                Direction.LEFT -> totalDragAccumulator.x.coerceIn(-cellSizePx, 0f)
                                Direction.RIGHT -> totalDragAccumulator.x.coerceIn(0f, cellSizePx)
                                Direction.UP -> totalDragAccumulator.y.coerceIn(-cellSizePx, 0f)
                                Direction.DOWN -> totalDragAccumulator.y.coerceIn(0f, cellSizePx)
                            }

                            // If blocked, apply rubber band damping
                            val targetOffset = if (hasMovable) {
                                rawDelta
                            } else {
                                val resistance = 0.2f
                                rawDelta * resistance
                            }

                            coroutineScope.launch {
                                dragOffsetAnim.snapTo(targetOffset)
                            }
                        }
                    }
                )
            }
            .padding(8.dp)
    ) {
        // 4x4 Grid
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
                        val tile = grid[r][c]

                        // Calculate translation for this tile based on drag state
                        val isMovingRow = activeDirection?.isHorizontal == true && movableLineIndices.contains(r)
                        val isMovingCol = activeDirection?.isVertical == true && movableLineIndices.contains(c)

                        val transX = if (isMovingRow) dragOffsetAnim.value else 0f
                        val transY = if (isMovingCol) dragOffsetAnim.value else 0f

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    translationX = transX
                                    translationY = transY
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
