package com.threes.game.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.threes.game.R
import com.threes.game.ui.components.GameBoardView
import com.threes.game.ui.components.NextTileBar
import com.threes.game.ui.components.NextTileCardStage
import com.threes.game.ui.dialogs.CastBookDialog
import com.threes.game.ui.dialogs.GameOverDialog
import com.threes.game.ui.dialogs.SettingsDialog
import com.threes.game.ui.dialogs.ShuffleRescueDialog
import com.threes.game.ui.theme.BoardBackgroundDark
import com.threes.game.ui.theme.BoardBackgroundLight
import com.threes.game.ui.theme.BoardPlateDark
import com.threes.game.ui.theme.Card1Bg
import com.threes.game.ui.theme.Card2Bg
import com.threes.game.ui.theme.EmptyCellDark
import com.threes.game.ui.theme.EmptyCellLight
import com.threes.game.ui.theme.TextPrimaryDark
import com.threes.game.ui.theme.TextPrimaryLight
import com.threes.game.ui.theme.TextSecondaryDark
import com.threes.game.ui.theme.TextSecondaryLight
import com.threes.game.ui.viewmodel.GameUiState
import com.threes.game.ui.viewmodel.ThreesViewModel

@Composable
fun GameScreen(
    viewModel: ThreesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDarkTheme = uiState.isNightMode

    var showNewGameConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkTheme) BoardBackgroundDark else BoardBackgroundLight)
            .safeDrawingPadding()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            val isWideLayout = maxWidth > maxHeight * 1.15f
            val availableHeight = maxHeight

            if (isWideLayout) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = availableHeight),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        TopHeaderBar(
                            isDarkTheme = isDarkTheme,
                            onOpenSettings = { viewModel.openSettings() },
                            onOpenCastBook = { viewModel.openCastBook() }
                        )
                        ScoreDashboard(
                            score = uiState.score,
                            highScore = uiState.highScore,
                            isDarkTheme = isDarkTheme
                        )
                        NextOrGameOverBanner(
                            uiState = uiState,
                            isDarkTheme = isDarkTheme,
                            onShowGameOver = { viewModel.showGameOverDialog() }
                        )
                        BottomActionBar(
                            uiState = uiState,
                            isDarkTheme = isDarkTheme,
                            onUndo = { viewModel.onUndo() },
                            onNewGameClick = {
                                if (uiState.isGameOver) {
                                    viewModel.onStartNewGame()
                                } else {
                                    showNewGameConfirm = true
                                }
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.25f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        GameBoardView(
                            grid = uiState.grid,
                            nextTile = uiState.nextTile,
                            lastMoveDirection = uiState.lastMoveDirection,
                            onMoveRequested = { direction -> viewModel.onMove(direction) },
                            onSlideCommitStarted = { viewModel.onSlideCommitStarted() },
                            getMovableLines = { direction -> viewModel.getMovableLines(direction) },
                            getMovableCells = { direction -> viewModel.getMovableCells(direction) },
                            getMergingCells = { direction -> viewModel.getMergingCells(direction) },
                            onDragThresholdReached = { viewModel.onDragThresholdReached() },
                            onDragBlocked = { viewModel.onDragBlocked() },
                            onSnapBack = { viewModel.onSnapBack() },
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                val boardOuterPad = 6.dp
                val boardContainerSide = minOf(maxWidth, maxHeight * 0.51f)
                val alignedContentWidth = (boardContainerSide - boardOuterPad * 2).coerceAtLeast(220.dp)

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(
                        modifier = Modifier
                            .weight(0.14f)
                            .heightIn(min = 4.dp)
                    )

                    // Upper Display Zone: Wordmark + Borderless Score Row
                    ThreesWordmark(
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    PortraitEditorialScoreRow(
                        score = uiState.score,
                        highScore = uiState.highScore,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.width(alignedContentWidth)
                    )

                    // Centered upright 3D Next Card stage bridging the tall screen gracefully
                    Box(
                        modifier = Modifier
                            .weight(0.76f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isGameOver && uiState.isGameOverDialogDismissed) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Card2Bg)
                                    .clickable(onClick = { viewModel.showGameOverDialog() })
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.out_of_moves_banner),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else {
                            NextTileCardStage(
                                nextTile = uiState.nextTile,
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }

                    // Lower Single-Hand Thumb Zone: 4x4 Board + Clean Two-Tier Thumb Controls
                    GameBoardView(
                        grid = uiState.grid,
                        nextTile = uiState.nextTile,
                        lastMoveDirection = uiState.lastMoveDirection,
                        onMoveRequested = { direction -> viewModel.onMove(direction) },
                        onSlideCommitStarted = { viewModel.onSlideCommitStarted() },
                        getMovableLines = { direction -> viewModel.getMovableLines(direction) },
                        getMovableCells = { direction -> viewModel.getMovableCells(direction) },
                        getMergingCells = { direction -> viewModel.getMergingCells(direction) },
                        onDragThresholdReached = { viewModel.onDragThresholdReached() },
                        onDragBlocked = { viewModel.onDragBlocked() },
                        onSnapBack = { viewModel.onSnapBack() },
                        isDarkTheme = isDarkTheme,
                        outerPadding = boardOuterPad,
                        modifier = Modifier.size(boardContainerSide)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PortraitPrimaryActionRow(
                        uiState = uiState,
                        isDarkTheme = isDarkTheme,
                        onUndo = { viewModel.onUndo() },
                        onNewGameClick = {
                            if (uiState.isGameOver) {
                                viewModel.onStartNewGame()
                            } else {
                                showNewGameConfirm = true
                            }
                        },
                        modifier = Modifier.width(alignedContentWidth)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PortraitSecondaryUtilityRow(
                        isDarkTheme = isDarkTheme,
                        onOpenSettings = { viewModel.openSettings() },
                        onOpenCastBook = { viewModel.openCastBook() },
                        modifier = Modifier.width(alignedContentWidth)
                    )

                    Spacer(
                        modifier = Modifier
                            .weight(0.10f)
                            .heightIn(min = 4.dp)
                    )
                }
            }
        }

        // Dialog: Shuffle Rescue Prompt or Game Over
        if (uiState.isGameOver && !uiState.isGameOverDialogDismissed) {
            if (uiState.canShuffleRescue) {
                ShuffleRescueDialog(
                    lockedTileValues = uiState.lockedShuffleTileValues,
                    onConfirmShuffle = { viewModel.onConfirmShuffle() },
                    onDeclineToGameOver = { viewModel.onDeclineShuffle() },
                    onDismissToViewBoard = { viewModel.dismissGameOverDialog() },
                    isDarkTheme = isDarkTheme
                )
            } else {
                GameOverDialog(
                    score = uiState.score,
                    highScore = uiState.highScore,
                    isNewRecord = uiState.isNewRecord,
                    grid = uiState.grid,
                    canUndo = uiState.isUndoEnabled && uiState.canUndo,
                    onUndoLastMove = { viewModel.onUndo() },
                    onDismissToViewBoard = { viewModel.dismissGameOverDialog() },
                    onPlayAgain = { viewModel.onStartNewGame() },
                    isDarkTheme = isDarkTheme
                )
            }
        }

        // Dialog: Cast Book
        if (uiState.isCastBookOpen) {
            CastBookDialog(
                unlockedValues = uiState.unlockedCharacters,
                onDismiss = { viewModel.closeCastBook() },
                onPlayVoice = { valId -> viewModel.playCharacterVoice(valId) },
                isDarkTheme = isDarkTheme
            )
        }

        // Dialog: Settings
        if (uiState.isSettingsOpen) {
            SettingsDialog(
                isNightMode = uiState.isNightMode,
                onNightModeChange = { viewModel.setNightMode(it) },
                isSoundEnabled = uiState.isSoundEnabled,
                onSoundEnabledChange = { viewModel.setSoundEnabled(it) },
                isHapticsEnabled = uiState.isHapticsEnabled,
                onHapticsEnabledChange = { viewModel.setHapticsEnabled(it) },
                isUndoEnabled = uiState.isUndoEnabled,
                onUndoEnabledChange = { viewModel.setUndoEnabled(it) },
                isPortraitLocked = uiState.isPortraitLocked,
                onPortraitLockedChange = { viewModel.setPortraitLocked(it) },
                isFastMode = uiState.isFastMode,
                onFastModeChange = { viewModel.setFastMode(it) },
                onResetHighScore = { viewModel.resetHighScore() },
                onDismiss = { viewModel.closeSettings() }
            )
        }

        // Confirm Dialog for Restarting
        if (showNewGameConfirm) {
            AlertDialog(
                onDismissRequest = { showNewGameConfirm = false },
                title = { Text(stringResource(R.string.restart_game_title)) },
                text = { Text(stringResource(R.string.restart_game_message)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showNewGameConfirm = false
                            viewModel.onStartNewGame()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Card2Bg)
                    ) {
                        Text(stringResource(R.string.restart), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewGameConfirm = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                containerColor = if (isDarkTheme) BoardPlateDark else BoardBackgroundLight,
                titleContentColor = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight,
                textContentColor = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight
            )
        }
    }
}

@Composable
private fun TopHeaderBar(
    isDarkTheme: Boolean,
    onOpenSettings: () -> Unit,
    onOpenCastBook: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight,
                modifier = Modifier.size(26.dp)
            )
        }

        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Card1Bg)) { append("T") }
                withStyle(SpanStyle(color = Card2Bg)) { append("H") }
                withStyle(SpanStyle(color = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight)) {
                    append("REES!")
                }
            },
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        IconButton(
            onClick = onOpenCastBook,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                Icons.Default.AutoStories,
                contentDescription = stringResource(R.string.cast_book),
                tint = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun ScoreDashboard(
    score: Int,
    highScore: Int,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = stringResource(R.string.score),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight
            )
            Text(
                text = score.toString(),
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight,
                letterSpacing = (-0.5).sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.best),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight
            )
            Text(
                text = highScore.toString(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight
            )
        }
    }
}

@Composable
private fun NextOrGameOverBanner(
    uiState: GameUiState,
    isDarkTheme: Boolean,
    onShowGameOver: () -> Unit
) {
    if (uiState.isGameOver && uiState.isGameOverDialogDismissed) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Card2Bg)
                .clickable(onClick = onShowGameOver)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.out_of_moves_banner),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = Color.White
            )
        }
    } else {
        NextTileBar(
            nextTile = uiState.nextTile,
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
private fun BottomActionBar(
    uiState: GameUiState,
    isDarkTheme: Boolean,
    onUndo: () -> Unit,
    onNewGameClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (uiState.isUndoEnabled) {
            OutlinedButton(
                onClick = onUndo,
                enabled = uiState.canUndo,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight
                ),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(
                    Icons.Default.Undo,
                    contentDescription = stringResource(R.string.undo),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    stringResource(R.string.undo),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }
        } else {
            Spacer(modifier = Modifier.width(1.dp))
        }

        FilledTonalButton(
            onClick = onNewGameClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isDarkTheme) BoardPlateDark else EmptyCellLight
            ),
            modifier = Modifier.height(44.dp)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(R.string.new_game),
                modifier = Modifier.size(18.dp),
                tint = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                stringResource(R.string.new_game),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                color = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight
            )
        }
    }
}

@Composable
private fun ThreesWordmark(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = Card1Bg)) { append("T") }
            withStyle(SpanStyle(color = Card2Bg)) { append("H") }
            withStyle(SpanStyle(color = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight)) {
                append("REES!")
            }
        },
        fontSize = 24.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = modifier
    )
}

@Composable
private fun PortraitEditorialScoreRow(
    score: Int,
    highScore: Int,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val labelColor = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight
    val primaryColor = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight
    val bestPillColor = if (isDarkTheme) Color(0xFF25292D) else Color(0xFFEFECE4)

    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = stringResource(R.string.score),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = labelColor
            )
            Text(
                text = score.toString(),
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = primaryColor,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                softWrap = false
            )
        }

        Surface(
            modifier = Modifier.padding(bottom = 3.dp),
            shape = RoundedCornerShape(50),
            color = bestPillColor
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.best),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = labelColor
                )
                Text(
                    text = highScore.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun PortraitPrimaryActionRow(
    uiState: GameUiState,
    isDarkTheme: Boolean,
    onUndo: () -> Unit,
    onNewGameClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val undoBg = if (isDarkTheme) Color(0xFF2C3035) else Color(0xFFEFEBE2)
    val newGameBg = if (isDarkTheme) Color(0xFF343940) else Color(0xFFE6E0D3)
    val edgeColor = if (isDarkTheme) Color(0xFF3C4249) else Color(0xFFD5CEC0)
    val contentColor = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (uiState.isUndoEnabled) {
            CardstockActionButton(
                icon = Icons.Default.Undo,
                label = stringResource(R.string.undo),
                enabled = uiState.canUndo,
                faceColor = undoBg,
                edgeColor = edgeColor,
                contentColor = contentColor,
                onClick = onUndo,
                modifier = Modifier.weight(1f)
            )
        }

        CardstockActionButton(
            icon = Icons.Default.Refresh,
            label = stringResource(R.string.new_game),
            enabled = true,
            faceColor = newGameBg,
            edgeColor = edgeColor,
            contentColor = contentColor,
            onClick = onNewGameClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CardstockActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    faceColor: Color,
    edgeColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outerShape = RoundedCornerShape(14.dp)
    val innerShape = RoundedCornerShape(13.dp)

    Box(
        modifier = modifier
            .height(46.dp)
            .alpha(if (enabled) 1f else 0.50f)
            .shadow(if (enabled) 2.dp else 0.dp, outerShape)
            .clip(outerShape)
            .background(edgeColor)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clip(innerShape)
                .background(faceColor)
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = contentColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun PortraitSecondaryUtilityRow(
    isDarkTheme: Boolean,
    onOpenSettings: () -> Unit,
    onOpenCastBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mutedColor = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight
    val dividerColor = if (isDarkTheme) EmptyCellDark else Color(0xFFD6CFC2)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenSettings)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = mutedColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.settings),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = mutedColor,
                maxLines = 1,
                softWrap = false
            )
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .width(1.dp)
                .height(14.dp)
                .background(dividerColor)
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenCastBook)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoStories,
                contentDescription = stringResource(R.string.cast_book),
                tint = mutedColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.cast_book),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = mutedColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

