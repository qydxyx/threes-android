package com.threes.game.ui.dialogs

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.threes.game.model.Tile
import com.threes.game.ui.theme.AccentToddYellow
import com.threes.game.ui.theme.BoardBackgroundLight
import com.threes.game.ui.theme.BoardPlateDark
import com.threes.game.ui.theme.Card1Bg
import com.threes.game.ui.theme.EmptyCellDark
import com.threes.game.ui.theme.EmptyCellLight
import com.threes.game.ui.theme.TextPrimaryDark
import com.threes.game.ui.theme.TextPrimaryLight
import com.threes.game.ui.theme.TextSecondaryDark
import com.threes.game.ui.theme.TextSecondaryLight

@Composable
fun GameOverDialog(
    score: Int,
    highScore: Int,
    isNewRecord: Boolean,
    grid: List<List<Tile?>>,
    canUndo: Boolean = false,
    onUndoLastMove: () -> Unit = {},
    onDismissToViewBoard: () -> Unit = {},
    onPlayAgain: () -> Unit,
    isDarkTheme: Boolean = false
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismissToViewBoard,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = if (isDarkTheme) BoardPlateDark else BoardBackgroundLight
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "GAME OVER",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight,
                        letterSpacing = 1.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    IconButton(
                        onClick = onDismissToViewBoard,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "View Board",
                            tint = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight
                        )
                    }
                }

                if (isNewRecord) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentToddYellow)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "★ NEW BEST! ★",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5A4400)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Score Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) EmptyCellDark else Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "FINAL SCORE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = score.toString(),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "All-Time Best:",
                                fontSize = 13.sp,
                                color = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight
                            )
                            Text(
                                text = highScore.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (canUndo) {
                    FilledTonalButton(
                        onClick = onUndoLastMove,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isDarkTheme) EmptyCellDark else EmptyCellLight
                        )
                    ) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = "Undo Last Move",
                            modifier = Modifier.size(18.dp),
                            tint = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Undo Last Move",
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            shareGameResult(context, score, grid)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Share", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onPlayAgain,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Card1Bg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Play Again", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Play Again", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

private fun shareGameResult(context: Context, score: Int, grid: List<List<Tile?>>) {
    val sb = StringBuilder()
    sb.append("🎮 I scored $score in Threes! for Android!\n\n")
    for (r in 0 until 4) {
        for (c in 0 until 4) {
            val v = grid.getOrNull(r)?.getOrNull(c)?.value ?: 0
            val emoji = when (v) {
                0 -> "⬛"
                1 -> "🟦"
                2 -> "🟥"
                3 -> "⬜"
                in 6..24 -> "🟨"
                in 48..192 -> "🟪"
                else -> "⭐"
            }
            sb.append(emoji)
        }
        sb.append("\n")
    }
    sb.append("\nCan you beat my score?")

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, sb.toString())
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Threes Score"))
}
