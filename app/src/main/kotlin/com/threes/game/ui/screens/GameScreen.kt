package com.threes.game.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.threes.game.ui.components.GameBoardView
import com.threes.game.ui.components.NextTileBar
import com.threes.game.ui.dialogs.CastBookDialog
import com.threes.game.ui.dialogs.GameOverDialog
import com.threes.game.ui.dialogs.SettingsDialog
import com.threes.game.ui.theme.*
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
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Settings, Logo, Cast Book
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.openSettings() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Styled THREES! Logo
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
                    onClick = { viewModel.openCastBook() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.AutoStories,
                        contentDescription = "The Cast",
                        tint = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Score Dashboard Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "SCORE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight
                    )
                    Text(
                        text = uiState.score.toString(),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight,
                        letterSpacing = (-0.5).sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "BEST",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight
                    )
                    Text(
                        text = uiState.highScore.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight
                    )
                }
            }

            // Next Tile Bar Indicator
            NextTileBar(
                nextTile = uiState.nextTile,
                isDarkTheme = isDarkTheme
            )

            // Game Board Component
            GameBoardView(
                grid = uiState.grid,
                nextTile = uiState.nextTile,
                onMoveRequested = { direction -> viewModel.onMove(direction) },
                getMovableLines = { direction -> viewModel.getMovableLines(direction) },
                onDragThresholdReached = { viewModel.onDragThresholdReached() },
                onDragBlocked = { viewModel.onDragBlocked() },
                onSnapBack = { viewModel.onSnapBack() },
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(1f, fill = false)
            )

            // Bottom Actions: Undo & New Game
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.isUndoEnabled) {
                    OutlinedButton(
                        onClick = { viewModel.onUndo() },
                        enabled = uiState.canUndo,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight
                        ),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Undo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                FilledTonalButton(
                    onClick = { showNewGameConfirm = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isDarkTheme) BoardPlateDark else EmptyCellLight
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "New Game",
                        modifier = Modifier.size(18.dp),
                        tint = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "New Game",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight
                    )
                }
            }
        }

        // Dialog: Game Over
        if (uiState.isGameOver) {
            GameOverDialog(
                score = uiState.score,
                highScore = uiState.highScore,
                isNewRecord = uiState.isNewRecord,
                grid = uiState.grid,
                onPlayAgain = { viewModel.onStartNewGame() },
                isDarkTheme = isDarkTheme
            )
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
                onResetHighScore = { viewModel.resetHighScore() },
                onDismiss = { viewModel.closeSettings() }
            )
        }

        // Confirm Dialog for Restarting
        if (showNewGameConfirm) {
            AlertDialog(
                onDismissRequest = { showNewGameConfirm = false },
                title = { Text("Restart Game?") },
                text = { Text("Are you sure you want to abandon the current game and start a new one?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showNewGameConfirm = false
                            viewModel.onStartNewGame()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Card2Bg)
                    ) {
                        Text("Restart", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewGameConfirm = false }) {
                        Text("Cancel")
                    }
                },
                containerColor = if (isDarkTheme) BoardPlateDark else BoardBackgroundLight,
                titleContentColor = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight,
                textContentColor = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight
            )
        }
    }
}
