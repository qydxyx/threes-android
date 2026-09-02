package com.threes.game.ui.dialogs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.threes.game.model.Tile
import com.threes.game.ui.characters.CharacterInfo
import com.threes.game.ui.characters.CharacterRegistry
import com.threes.game.ui.components.TileView
import com.threes.game.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CastBookDialog(
    unlockedValues: Set<Int>,
    onDismiss: () -> Unit,
    onPlayVoice: (Int) -> Unit,
    isDarkTheme: Boolean = false
) {
    val allValues = listOf(1, 2, 3, 6, 12, 24, 48, 96, 192, 384, 768, 1536, 3072, 6144)
    var selectedValue by remember { mutableStateOf(3) }
    val selectedInfo = CharacterRegistry.getInfo(selectedValue)
    val isSelectedUnlocked = unlockedValues.contains(selectedValue)
    val coroutineScope = rememberCoroutineScope()
    val cardScaleAnim = remember { Animatable(1f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = if (isDarkTheme) BoardPlateDark else BoardBackgroundLight
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "THE CAST",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Character Details Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) EmptyCellDark else Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isSelectedUnlocked) {
                            // Interactive Tile Display
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .scale(cardScaleAnim.value)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        coroutineScope.launch {
                                            onPlayVoice(selectedValue)
                                            cardScaleAnim.animateTo(
                                                1.2f,
                                                spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                            )
                                            cardScaleAnim.animateTo(1f)
                                        }
                                    }
                            ) {
                                TileView(
                                    tile = Tile.create(selectedValue),
                                    isDarkTheme = isDarkTheme
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = selectedInfo.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight
                            )
                            Text(
                                text = selectedInfo.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Card1Bg
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "\"${selectedInfo.quote}\"",
                                fontSize = 14.sp,
                                fontStyle = FontStyle.Italic,
                                color = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = selectedInfo.description,
                                fontSize = 12.sp,
                                color = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            if (selectedValue >= 3) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Score: ${Tile.create(selectedValue).score} pts",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight
                                )
                            }
                        } else {
                            // Locked card silhouette
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDarkTheme) BoardPlateDark else EmptyCellLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "?",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Locked Character",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Combine tiles to reach $selectedValue and unlock this character's story!",
                                fontSize = 13.sp,
                                color = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Carousel of all Cards
                val listState = rememberLazyListState()
                LazyRow(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(allValues) { value ->
                        val isUnlocked = unlockedValues.contains(value)
                        val isSelected = value == selectedValue

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Card1Bg.copy(alpha = 0.3f) else Color.Transparent
                                )
                                .padding(3.dp)
                                .clickable {
                                    selectedValue = value
                                    if (isUnlocked) {
                                        onPlayVoice(value)
                                    }
                                }
                        ) {
                            if (isUnlocked) {
                                TileView(
                                    tile = Tile.create(value),
                                    isDarkTheme = isDarkTheme
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isDarkTheme) EmptyCellDark else EmptyCellLight
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "?",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight
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
