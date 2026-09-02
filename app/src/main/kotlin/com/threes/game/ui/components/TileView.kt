package com.threes.game.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.threes.game.model.Tile
import com.threes.game.ui.characters.CharacterFace
import com.threes.game.ui.characters.CharacterRegistry
import com.threes.game.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun TileView(
    tile: Tile?,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    lookOffsetX: Float = 0f,
    lookOffsetY: Float = 0f
) {
    if (tile == null) {
        // Empty cell slot recess
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isDarkTheme) EmptyCellDark else EmptyCellLight)
        )
        return
    }

    // Animation state for pop/merge
    val scaleAnim = remember { Animatable(if (tile.isNew) 0.5f else 1f) }

    LaunchedEffect(tile.id, tile.isMerged, tile.isNew) {
        if (tile.isMerged) {
            scaleAnim.animateTo(
                targetValue = 1.18f,
                animationSpec = tween(100, easing = FastOutSlowInEasing)
            )
            scaleAnim.animateTo(
                targetValue = 0.94f,
                animationSpec = tween(80, easing = LinearOutSlowInEasing)
            )
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        } else if (tile.isNew) {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    // Cute periodic eye blinking
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(tile.id) {
        while (true) {
            delay(Random.nextLong(3000, 6000))
            isBlinking = true
            delay(150)
            isBlinking = false
        }
    }

    val charInfo = CharacterRegistry.getInfo(tile.value)

    val bgColor = when {
        tile.isOne -> Card1Bg
        tile.isTwo -> Card2Bg
        else -> if (isDarkTheme) Color(0xFFFAF6EE) else Card3Bg
    }

    val edgeColor = when {
        tile.isOne -> Card1Edge
        tile.isTwo -> Card2Edge
        else -> Card3Edge
    }

    val textColor = when {
        tile.isOne -> Card1Text
        tile.isTwo -> Card2Text
        else -> Card3Text
    }

    val valueText = tile.value.toString()
    val fontSize = when {
        valueText.length <= 2 -> 30.sp
        valueText.length == 3 -> 23.sp
        else -> 18.sp
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp)
            .scale(scaleAnim.value)
            .shadow(3.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(edgeColor)
    ) {
        // Physical card body with 3D bottom edge offset
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.93f)
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor)
                .align(Alignment.TopCenter)
        ) {
            // Number Text in upper portion
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.58f)
                    .padding(top = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = valueText,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    letterSpacing = (-0.5).sp
                )
            }

            // Character Face in lower portion
            CharacterFace(
                value = tile.value,
                isBlinking = isBlinking,
                lookOffsetX = lookOffsetX,
                lookOffsetY = lookOffsetY,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
