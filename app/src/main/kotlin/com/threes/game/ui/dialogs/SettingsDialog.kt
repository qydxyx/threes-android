package com.threes.game.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.threes.game.R
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
    var showResetConfirm by remember { mutableStateOf(false) }

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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = if (isNightMode) TextPrimaryDark else TextPrimaryLight
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = if (isNightMode) TextSecondaryDark else TextSecondaryLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SettingSwitchRow(
                    title = stringResource(R.string.night_mode),
                    checked = isNightMode,
                    onCheckedChange = onNightModeChange,
                    isDarkTheme = isNightMode
                )

                HorizontalDivider(
                    color = if (isNightMode) EmptyCellDark else EmptyCellLight,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                SettingSwitchRow(
                    title = stringResource(R.string.sound_effects),
                    checked = isSoundEnabled,
                    onCheckedChange = onSoundEnabledChange,
                    isDarkTheme = isNightMode
                )

                HorizontalDivider(
                    color = if (isNightMode) EmptyCellDark else EmptyCellLight,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                SettingSwitchRow(
                    title = stringResource(R.string.haptics),
                    checked = isHapticsEnabled,
                    onCheckedChange = onHapticsEnabledChange,
                    isDarkTheme = isNightMode
                )

                HorizontalDivider(
                    color = if (isNightMode) EmptyCellDark else EmptyCellLight,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                SettingSwitchRow(
                    title = stringResource(R.string.undo_toggle),
                    checked = isUndoEnabled,
                    onCheckedChange = onUndoEnabledChange,
                    isDarkTheme = isNightMode
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Card2Bg
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = stringResource(R.string.reset_high_score), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.reset_high_score_title)) },
            text = { Text(stringResource(R.string.reset_high_score_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetHighScore()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Card2Bg)
                ) {
                    Text(stringResource(R.string.reset), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = if (isNightMode) BoardPlateDark else BoardBackgroundLight,
            titleContentColor = if (isNightMode) TextPrimaryDark else TextPrimaryLight,
            textContentColor = if (isNightMode) TextSecondaryDark else TextSecondaryLight
        )
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
            color = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
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
