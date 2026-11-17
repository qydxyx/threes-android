package com.threes.game.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.threes.game.R
import com.threes.game.model.Tile
import com.threes.game.ui.characters.CharacterFace
import com.threes.game.ui.theme.Card1Bg
import com.threes.game.ui.theme.Card1Edge
import com.threes.game.ui.theme.Card2Bg
import com.threes.game.ui.theme.Card2Edge
import com.threes.game.ui.theme.Card3Bg
import com.threes.game.ui.theme.Card3BgDark
import com.threes.game.ui.theme.Card3Edge
import com.threes.game.ui.theme.Card3Text
import com.threes.game.ui.theme.NextHint1
import com.threes.game.ui.theme.NextHint2
import com.threes.game.ui.theme.NextHint3
import com.threes.game.ui.theme.NextHint3Dark
import com.threes.game.ui.theme.NextHintBonus
import com.threes.game.ui.theme.TextPrimaryLight
import com.threes.game.ui.theme.TextSecondaryDark
import com.threes.game.ui.theme.TextSecondaryLight

@Composable
fun NextTileBadge(
    nextTile: Tile,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val barColor = when {
        nextTile.isBonus -> NextHintBonus
        nextTile.isOne -> NextHint1
        nextTile.isTwo -> NextHint2
        else -> if (isDarkTheme) NextHint3Dark else NextHint3
    }

    val edgeColor = when {
        nextTile.isBonus -> Color(0xFFB8860B)
        nextTile.isOne -> Card1Edge
        nextTile.isTwo -> Card2Edge
        else -> Card3Edge
    }

    val animatedColor by animateColorAsState(targetValue = barColor, label = "nextColor")

    val textColor = when {
        nextTile.isBonus -> Color.White
        nextTile.isOne || nextTile.isTwo -> Color.White
        else -> TextPrimaryLight
    }

    val labelText = when {
        nextTile.isBonus -> {
            val candidates = nextTile.bonusCandidates
            if (candidates.size > 1) {
                "+ " + candidates.joinToString(" · ")
            } else {
                "+ ${nextTile.value}"
            }
        }
        nextTile.isOne -> "1"
        nextTile.isTwo -> "2"
        else -> "3"
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 52.dp)
            .height(30.dp)
            .shadow(2.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(edgeColor)
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 52.dp)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(7.dp))
                .background(animatedColor)
                .padding(horizontal = 10.dp)
                .align(Alignment.TopCenter),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = labelText,
                fontSize = if (nextTile.isBonus && nextTile.bonusCandidates.size > 1) 12.sp else 15.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun NextTileBar(
    nextTile: Tile,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.next),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(end = 12.dp)
        )

        NextTileBadge(
            nextTile = nextTile,
            isDarkTheme = isDarkTheme,
            modifier = Modifier.defaultMinSize(minWidth = 68.dp)
        )
    }
}

@Composable
fun NextTileCardStage(
    nextTile: Tile,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val trayColor = if (isDarkTheme) Color(0xFF23262A) else Color(0xFFF1EDE4)
    val labelColor = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight

    val cardColor = when {
        nextTile.isBonus -> NextHintBonus
        nextTile.isOne -> Card1Bg
        nextTile.isTwo -> Card2Bg
        else -> if (isDarkTheme) Card3BgDark else Card3Bg
    }
    val edgeColor = when {
        nextTile.isBonus -> Color(0xFFD07C14)
        nextTile.isOne -> Card1Edge
        nextTile.isTwo -> Card2Edge
        else -> Card3Edge
    }
    val textColor = when {
        nextTile.isBonus || nextTile.isOne || nextTile.isTwo -> Color.White
        else -> Card3Text
    }
    val animatedCardColor by animateColorAsState(targetValue = cardColor, label = "nextStageColor")
    val animatedEdgeColor by animateColorAsState(targetValue = edgeColor, label = "nextStageEdge")

    val labelText = when {
        nextTile.isBonus -> {
            val candidates = nextTile.bonusCandidates
            if (candidates.size > 1) {
                "+ " + candidates.joinToString(" · ")
            } else {
                "+ ${nextTile.value}"
            }
        }
        nextTile.isOne -> "1"
        nextTile.isTwo -> "2"
        else -> "3"
    }

    val isMultiBonus = nextTile.isBonus && nextTile.bonusCandidates.size > 1
    val cardWidth = if (isMultiBonus) 92.dp else 52.dp

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(trayColor)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = cardWidth, height = 62.dp)
                .shadow(3.dp, RoundedCornerShape(11.dp))
                .clip(RoundedCornerShape(11.dp))
                .background(animatedEdgeColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.91f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(animatedCardColor)
                    .align(Alignment.TopCenter)
            ) {
                if (nextTile.isBonus) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labelText,
                            fontSize = if (isMultiBonus) 13.sp else 18.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.58f)
                            .padding(top = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labelText,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    CharacterFace(
                        value = nextTile.value,
                        isBlinking = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.next),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = labelColor
        )
    }
}

