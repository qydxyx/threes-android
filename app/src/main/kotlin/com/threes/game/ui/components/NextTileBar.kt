package com.threes.game.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.threes.game.model.Tile
import com.threes.game.ui.theme.*

@Composable
fun NextTileBar(
    nextTile: Tile,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val barColor = when {
        nextTile.isBonus -> NextHintBonus
        nextTile.isOne -> NextHint1
        nextTile.isTwo -> NextHint2
        else -> if (isDarkTheme) Color(0xFFFAF6EE) else NextHint3
    }

    val animatedColor by animateColorAsState(targetValue = barColor, label = "nextColor")

    val textColor = when {
        nextTile.isBonus -> Color.White
        nextTile.isOne || nextTile.isTwo -> Color.White
        else -> TextPrimaryLight
    }

    val labelText = when {
        nextTile.isBonus -> "+ ${nextTile.value}"
        nextTile.isOne -> "1"
        nextTile.isTwo -> "2"
        else -> "3"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "NEXT",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(end = 12.dp)
        )

        Box(
            modifier = Modifier
                .width(68.dp)
                .height(30.dp)
                .shadow(2.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(animatedColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = labelText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = textColor
            )
        }
    }
}
