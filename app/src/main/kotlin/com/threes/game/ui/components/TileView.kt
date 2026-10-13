package com.threes.game.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.threes.game.model.Tile
import com.threes.game.ui.characters.CharacterFace
import com.threes.game.ui.theme.Card1Bg
import com.threes.game.ui.theme.Card1Edge
import com.threes.game.ui.theme.Card1Text
import com.threes.game.ui.theme.Card2Bg
import com.threes.game.ui.theme.Card2Edge
import com.threes.game.ui.theme.Card2Text
import com.threes.game.ui.theme.Card3Bg
import com.threes.game.ui.theme.Card3Edge
import com.threes.game.ui.theme.Card3Text
import com.threes.game.ui.theme.EmptyCellDark
import com.threes.game.ui.theme.EmptyCellLight
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun EmptyCellSlot(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDarkTheme) EmptyCellDark else EmptyCellLight)
    )
}

@Composable
fun TileView(
    tile: Tile?,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    lookOffsetX: Float = 0f,
    lookOffsetY: Float = 0f,
    isCompact: Boolean = false
) {
    if (tile == null) {
        EmptyCellSlot(isDarkTheme = isDarkTheme, modifier = modifier)
        return
    }

    // Key scale animation by tile.id so newly spawned tiles at a previously occupied cell
    // always start from 0.5f and play their entrance pop animation.
    val scaleAnim = remember(tile.id) {
        Animatable(if (tile.isNew) 0.5f else 1f)
    }

    LaunchedEffect(tile.id, tile.isMerged, tile.isNew) {
        if (tile.isMerged) {
            scaleAnim.snapTo(1f)
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
            if (scaleAnim.value > 0.6f) {
                scaleAnim.snapTo(0.5f)
            }
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else if (scaleAnim.value != 1f) {
            scaleAnim.snapTo(1f)
        }
    }

    // Periodic cute eye blinking
    var isBlinking by remember(tile.id) { mutableStateOf(false) }
    LaunchedEffect(tile.id) {
        while (true) {
            delay(Random.nextLong(3000, 6000))
            isBlinking = true
            delay(150)
            isBlinking = false
        }
    }

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
    val fontSize = if (isCompact) {
        when {
            valueText.length <= 2 -> 14.sp
            valueText.length == 3 -> 11.sp
            else -> 9.sp
        }
    } else {
        when {
            valueText.length <= 2 -> 30.sp
            valueText.length == 3 -> 23.sp
            else -> 18.sp
        }
    }

    val outerPadding = if (isCompact) 2.dp else 4.dp
    val cornerRadius = if (isCompact) 8.dp else 12.dp
    val innerCornerRadius = if (isCompact) 7.dp else 10.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(outerPadding)
            .graphicsLayer {
                val s = scaleAnim.value
                scaleX = s
                scaleY = s
            }
            .shadow(if (isCompact) 1.dp else 3.dp, shape = RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .background(edgeColor)
    ) {
        // Physical card body with 3D bottom edge offset
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.93f)
                .clip(RoundedCornerShape(innerCornerRadius))
                .background(bgColor)
                .align(Alignment.TopCenter)
        ) {
            // Number Text in upper portion
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.58f)
                    .padding(top = if (isCompact) 2.dp else 6.dp),
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
