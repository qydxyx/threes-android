package com.threes.game.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.threes.game.ui.theme.*

@Composable
fun SettingsDialog(
    isNightMode: Boolean,
    onNightModeChange: (Boolean) -> Unit,
    isSoundEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    isHapticsEnabled: Boolean,
    onHapticsEnabledChange: (Boolean) -> Unit,
    isUndoEnabled: Boolean,
    onUndoEnabledChange: (Boolean) -> Unit,
    onResetHighScore: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = if (isNightMode) BoardPlateDark else BoardBackgroundLight
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SETTINGS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = if (isNightMode) TextPrimaryDark else TextPrimaryLight
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isNightMode) TextSecondaryDark else TextSecondaryLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Night Mode Toggle
                SettingSwitchRow(
                    title = "Night Threes (Dark Mode)",
                    checked = isNightMode,
                    onCheckedChange = onNightModeChange,
                    isDarkTheme = isNightMode
                )

                Divider(
                    color = if (isNightMode) EmptyCellDark else EmptyCellLight,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Sound Effects Toggle
                SettingSwitchRow(
                    title = "Sound Effects & Voices",
                    checked = isSoundEnabled,
                    onCheckedChange = onSoundEnabledChange,
                    isDarkTheme = isNightMode
                )

                Divider(
                    color = if (isNightMode) EmptyCellDark else EmptyCellLight,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Haptic Feedback Toggle
                SettingSwitchRow(
                    title = "Haptic Vibration",
                    checked = isHapticsEnabled,
                    onCheckedChange = onHapticsEnabledChange,
                    isDarkTheme = isNightMode
                )

                Divider(
                    color = if (isNightMode) EmptyCellDark else EmptyCellLight,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Undo Move Toggle
                SettingSwitchRow(
                    title = "Enable Undo Move",
                    checked = isUndoEnabled,
                    onCheckedChange = onUndoEnabledChange,
                    isDarkTheme = isNightMode
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Reset High Score Button
                OutlinedButton(
                    onClick = onResetHighScore,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Card2Bg
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Reset High Score", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Card1Bg
            )
        )
    }
}
