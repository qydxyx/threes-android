package com.threes.game.ui.characters

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.threes.game.ui.theme.*

@Composable
fun CharacterFace(
    value: Int,
    isBlinking: Boolean,
    lookOffsetX: Float,
    lookOffsetY: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val eyeLook = Offset(lookOffsetX * w * 0.04f, lookOffsetY * h * 0.04f)

        when (value) {
            1 -> drawUnoFace(w, h)
            2 -> drawDosFace(w, h)
            3 -> drawTreyFace(w, h, isBlinking, eyeLook)
            6 -> drawToddFace(w, h, isBlinking, eyeLook)
            12 -> drawTerryFace(w, h)
            24 -> drawTorvalFace(w, h, isBlinking, eyeLook)
            48 -> drawTweeFace(w, h, isBlinking, eyeLook)
            96 -> drawThelmaFace(w, h)
            192 -> drawThurstonFace(w, h, isBlinking, eyeLook)
            384 -> drawTorranceFace(w, h, isBlinking, eyeLook)
            768 -> drawTychoFace(w, h, eyeLook)
            1536 -> drawTerrenceFace(w, h, isBlinking, eyeLook)
            3072 -> drawTheronFace(w, h, isBlinking, eyeLook)
            6144 -> drawTrinityFace(w, h, isBlinking, eyeLook)
            else -> drawTreyFace(w, h, isBlinking, eyeLook)
        }
    }
}

private fun DrawScope.drawBlinkLine(
    center: Offset,
    halfWidth: Float,
    strokeWidth: Float,
    color: Color = TextPrimaryLight
) {
    drawLine(
        color = color,
        start = Offset(center.x - halfWidth, center.y),
        end = Offset(center.x + halfWidth, center.y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawUnoFace(w: Float, h: Float) {
    drawRoundRect(
        color = Color.White.copy(alpha = 0.8f),
        topLeft = Offset(w * 0.35f, h * 0.78f),
        size = Size(w * 0.3f, h * 0.05f),
        cornerRadius = CornerRadius(h * 0.025f)
    )
}

private fun DrawScope.drawDosFace(w: Float, h: Float) {
    val r = w * 0.035f
    drawCircle(
        color = Color.White.copy(alpha = 0.8f),
        radius = r,
        center = Offset(w * 0.42f, h * 0.8f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.8f),
        radius = r,
        center = Offset(w * 0.58f, h * 0.8f)
    )
}

private fun DrawScope.drawTreyFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset
) {
    val eyeY = h * 0.74f
    val eyeLeftX = w * 0.38f
    val eyeRightX = w * 0.62f
    val eyeR = w * 0.045f
    val strokeW = w * 0.03f

    if (isBlinking) {
        drawBlinkLine(Offset(eyeLeftX, eyeY), eyeR, strokeW)
        drawBlinkLine(Offset(eyeRightX, eyeY), eyeR, strokeW)
    } else {
        drawCircle(
            color = TextPrimaryLight,
            radius = eyeR,
            center = Offset(eyeLeftX + eyeLook.x, eyeY + eyeLook.y)
        )
        drawCircle(
            color = TextPrimaryLight,
            radius = eyeR,
            center = Offset(eyeRightX + eyeLook.x, eyeY + eyeLook.y)
        )
    }

    val mouthPath = Path().apply {
        moveTo(w * 0.42f, h * 0.84f)
        quadraticBezierTo(w * 0.5f, h * 0.90f, w * 0.58f, h * 0.84f)
    }
    drawPath(mouthPath, TextPrimaryLight, style = Stroke(width = w * 0.035f, cap = StrokeCap.Round))
}

private fun DrawScope.drawToddFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset
) {
    drawRoundRect(
        color = AccentToddYellow,
        topLeft = Offset(w * 0.15f, h * 0.62f),
        size = Size(w * 0.7f, h * 0.07f),
        cornerRadius = CornerRadius(w * 0.035f)
    )

    val eyeY = h * 0.76f
    val eyeLeftX = w * 0.36f
    val eyeRightX = w * 0.64f
    val glassR = w * 0.075f
    val strokeW = w * 0.028f

    drawCircle(TextPrimaryLight, radius = glassR, center = Offset(eyeLeftX, eyeY), style = Stroke(width = strokeW))
    drawCircle(TextPrimaryLight, radius = glassR, center = Offset(eyeRightX, eyeY), style = Stroke(width = strokeW))
    drawLine(TextPrimaryLight, start = Offset(eyeLeftX + glassR, eyeY), end = Offset(eyeRightX - glassR, eyeY), strokeWidth = strokeW)

    if (isBlinking) {
        drawBlinkLine(Offset(eyeLeftX, eyeY), w * 0.035f, strokeW)
        drawBlinkLine(Offset(eyeRightX, eyeY), w * 0.035f, strokeW)
    } else {
        drawCircle(TextPrimaryLight, radius = w * 0.035f, center = Offset(eyeLeftX + eyeLook.x, eyeY + eyeLook.y))
        drawCircle(TextPrimaryLight, radius = w * 0.035f, center = Offset(eyeRightX + eyeLook.x, eyeY + eyeLook.y))
    }

    val mouthPath = Path().apply {
        moveTo(w * 0.44f, h * 0.88f)
        quadraticBezierTo(w * 0.52f, h * 0.91f, w * 0.58f, h * 0.87f)
    }
    drawPath(mouthPath, TextPrimaryLight, style = Stroke(width = strokeW, cap = StrokeCap.Round))
}

private fun DrawScope.drawTerryFace(
    w: Float,
    h: Float
) {
    val eyeY = h * 0.74f
    val shadePath = Path().apply {
        moveTo(w * 0.25f, eyeY - h * 0.03f)
        lineTo(w * 0.75f, eyeY - h * 0.03f)
        lineTo(w * 0.68f, eyeY + h * 0.05f)
        lineTo(w * 0.53f, eyeY + h * 0.02f)
        lineTo(w * 0.47f, eyeY + h * 0.02f)
        lineTo(w * 0.32f, eyeY + h * 0.05f)
        close()
    }
    drawPath(shadePath, Color(0xFF2C3E50))

    val hairPath = Path().apply {
        moveTo(w * 0.3f, h * 0.62f)
        lineTo(w * 0.4f, h * 0.54f)
        lineTo(w * 0.5f, h * 0.61f)
        lineTo(w * 0.6f, h * 0.52f)
        lineTo(w * 0.7f, h * 0.62f)
    }
    drawPath(hairPath, AccentTerryRed, style = Stroke(width = w * 0.038f, cap = StrokeCap.Round))

    val mouthPath = Path().apply {
        moveTo(w * 0.42f, h * 0.85f)
        quadraticBezierTo(w * 0.52f, h * 0.88f, w * 0.62f, h * 0.83f)
    }
    drawPath(mouthPath, TextPrimaryLight, style = Stroke(width = w * 0.03f, cap = StrokeCap.Round))
}

private fun DrawScope.drawTorvalFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset
) {
    val eyeY = h * 0.72f
    val eyeLeftX = w * 0.35f
    val eyeRightX = w * 0.65f
    val eyeR = w * 0.045f
    val strokeW = w * 0.03f

    if (isBlinking) {
        drawBlinkLine(Offset(eyeLeftX, eyeY), eyeR, strokeW)
        drawBlinkLine(Offset(eyeRightX, eyeY), eyeR, strokeW)
    } else {
        drawCircle(TextPrimaryLight, eyeR, Offset(eyeLeftX + eyeLook.x, eyeY + eyeLook.y))
        drawCircle(TextPrimaryLight, eyeR, Offset(eyeRightX + eyeLook.x, eyeY + eyeLook.y))
    }

    drawCircle(AccentTorvalBlush.copy(alpha = 0.6f), radius = w * 0.06f, center = Offset(w * 0.25f, h * 0.78f))
    drawCircle(AccentTorvalBlush.copy(alpha = 0.6f), radius = w * 0.06f, center = Offset(w * 0.75f, h * 0.78f))

    val mouthRect = Rect(w * 0.4f, h * 0.78f, w * 0.6f, h * 0.89f)
    drawArc(TextPrimaryLight, 0f, 180f, true, topLeft = mouthRect.topLeft, size = mouthRect.size)
}

private fun DrawScope.drawTweeFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset
) {
    drawCircle(AccentTweeFlower, radius = w * 0.05f, center = Offset(w * 0.25f, h * 0.63f))
    drawCircle(Color.Yellow, radius = w * 0.02f, center = Offset(w * 0.25f, h * 0.63f))

    val eyeY = h * 0.74f
    val eyeLeftX = w * 0.37f
    val eyeRightX = w * 0.63f
    val eyeR = w * 0.04f
    val strokeW = w * 0.028f

    if (isBlinking) {
        drawBlinkLine(Offset(eyeLeftX, eyeY), eyeR, strokeW)
        drawBlinkLine(Offset(eyeRightX, eyeY), eyeR, strokeW)
    } else {
        drawCircle(TextPrimaryLight, eyeR, Offset(eyeLeftX + eyeLook.x, eyeY + eyeLook.y))
        drawCircle(TextPrimaryLight, eyeR, Offset(eyeRightX + eyeLook.x, eyeY + eyeLook.y))
        val lashStroke = w * 0.024f
        drawLine(TextPrimaryLight, Offset(eyeLeftX - eyeR, eyeY - eyeR), Offset(eyeLeftX - eyeR * 1.5f, eyeY - eyeR * 1.5f), lashStroke, StrokeCap.Round)
        drawLine(TextPrimaryLight, Offset(eyeRightX + eyeR, eyeY - eyeR), Offset(eyeRightX + eyeR * 1.5f, eyeY - eyeR * 1.5f), lashStroke, StrokeCap.Round)
    }

    val mouthPath = Path().apply {
        moveTo(w * 0.45f, h * 0.84f)
        quadraticBezierTo(w * 0.5f, h * 0.88f, w * 0.55f, h * 0.84f)
    }
    drawPath(mouthPath, TextPrimaryLight, style = Stroke(width = strokeW, cap = StrokeCap.Round))
}

private fun DrawScope.drawThelmaFace(
    w: Float,
    h: Float
) {
    val eyeY = h * 0.72f
    val leftLens = Path().apply {
        moveTo(w * 0.24f, eyeY - h * 0.03f)
        lineTo(w * 0.46f, eyeY - h * 0.01f)
        lineTo(w * 0.44f, eyeY + h * 0.04f)
        lineTo(w * 0.26f, eyeY + h * 0.02f)
        close()
    }
    val rightLens = Path().apply {
        moveTo(w * 0.76f, eyeY - h * 0.03f)
        lineTo(w * 0.54f, eyeY - h * 0.01f)
        lineTo(w * 0.56f, eyeY + h * 0.04f)
        lineTo(w * 0.74f, eyeY + h * 0.02f)
        close()
    }
    drawPath(leftLens, AccentThelmaPurple)
    drawPath(rightLens, AccentThelmaPurple)

    val pearlR = w * 0.025f
    val pearlY = h * 0.91f
    listOf(0.35f, 0.42f, 0.5f, 0.58f, 0.65f).forEach { px ->
        drawCircle(Color(0xFFEDE7DC), radius = pearlR, center = Offset(w * px, pearlY))
    }

    val mouthPath = Path().apply {
        moveTo(w * 0.43f, h * 0.83f)
        quadraticBezierTo(w * 0.5f, h * 0.86f, w * 0.57f, h * 0.83f)
    }
    drawPath(mouthPath, Color(0xFFC0392B), style = Stroke(width = w * 0.03f, cap = StrokeCap.Round))
}

private fun DrawScope.drawThurstonFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset
) {
    val eyeY = h * 0.70f
    val eyeLeftX = w * 0.36f
    val eyeRightX = w * 0.64f
    val eyeR = w * 0.035f
    val strokeW = w * 0.024f

    drawCircle(AccentThurstonGold, radius = w * 0.075f, center = Offset(eyeRightX, eyeY), style = Stroke(width = strokeW))
    drawLine(AccentThurstonGold, Offset(eyeRightX + w * 0.075f, eyeY), Offset(w * 0.8f, eyeY + h * 0.1f), strokeWidth = w * 0.02f)

    if (isBlinking) {
        drawBlinkLine(Offset(eyeLeftX, eyeY), eyeR, strokeW)
        drawBlinkLine(Offset(eyeRightX, eyeY), eyeR, strokeW)
    } else {
        drawCircle(TextPrimaryLight, radius = eyeR, center = Offset(eyeLeftX + eyeLook.x, eyeY + eyeLook.y))
        drawCircle(TextPrimaryLight, radius = eyeR, center = Offset(eyeRightX + eyeLook.x, eyeY + eyeLook.y))
    }

    val mustachePath = Path().apply {
        moveTo(w * 0.3f, h * 0.80f)
        quadraticBezierTo(w * 0.4f, h * 0.77f, w * 0.5f, h * 0.82f)
        quadraticBezierTo(w * 0.6f, h * 0.77f, w * 0.7f, h * 0.80f)
        quadraticBezierTo(w * 0.6f, h * 0.86f, w * 0.5f, h * 0.84f)
        quadraticBezierTo(w * 0.4f, h * 0.86f, w * 0.3f, h * 0.80f)
        close()
    }
    drawPath(mustachePath, Color(0xFF4A3B32))
}

private fun DrawScope.drawTorranceFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset
) {
    val eyeY = h * 0.72f
    val eyeRightX = w * 0.64f
    val eyeR = w * 0.045f

    val patchPath = Path().apply {
        moveTo(w * 0.25f, eyeY - h * 0.04f)
        lineTo(w * 0.45f, eyeY - h * 0.02f)
        lineTo(w * 0.43f, eyeY + h * 0.05f)
        lineTo(w * 0.27f, eyeY + h * 0.04f)
        close()
    }
    drawPath(patchPath, AccentTorranceEyepatch)
    drawLine(AccentTorranceEyepatch, Offset(w * 0.15f, eyeY - h * 0.06f), Offset(w * 0.85f, eyeY + h * 0.02f), w * 0.024f)

    if (isBlinking) {
        drawBlinkLine(Offset(eyeRightX, eyeY), eyeR, w * 0.03f)
    } else {
        drawCircle(TextPrimaryLight, radius = eyeR, center = Offset(eyeRightX + eyeLook.x, eyeY + eyeLook.y))
    }

    val mouthPath = Path().apply {
        moveTo(w * 0.38f, h * 0.84f)
        lineTo(w * 0.62f, h * 0.84f)
    }
    drawPath(mouthPath, TextPrimaryLight, style = Stroke(width = w * 0.034f, cap = StrokeCap.Round))
    drawRect(AccentThurstonGold, topLeft = Offset(w * 0.52f, h * 0.83f), size = Size(w * 0.04f, h * 0.04f))
}

private fun DrawScope.drawTychoFace(
    w: Float,
    h: Float,
    eyeLook: Offset
) {
    val eyeY = h * 0.72f
    drawRoundRect(
        color = AccentTychoCyan,
        topLeft = Offset(w * 0.22f, eyeY - h * 0.04f),
        size = Size(w * 0.56f, h * 0.07f),
        cornerRadius = CornerRadius(w * 0.035f)
    )
    drawLine(
        Color.White,
        Offset(w * 0.28f + eyeLook.x, eyeY),
        Offset(w * 0.72f + eyeLook.x, eyeY),
        strokeWidth = w * 0.028f,
        cap = StrokeCap.Round
    )

    drawRect(Color(0xFF2C3E50), topLeft = Offset(w * 0.44f, h * 0.84f), size = Size(w * 0.12f, h * 0.025f))
}

private fun DrawScope.drawTerrenceFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset
) {
    val crownPath = Path().apply {
        moveTo(w * 0.28f, h * 0.62f)
        lineTo(w * 0.32f, h * 0.52f)
        lineTo(w * 0.42f, h * 0.58f)
        lineTo(w * 0.5f, h * 0.48f)
        lineTo(w * 0.58f, h * 0.58f)
        lineTo(w * 0.68f, h * 0.52f)
        lineTo(w * 0.72f, h * 0.62f)
        close()
    }
    drawPath(crownPath, AccentTerrenceCrown)

    val eyeY = h * 0.72f
    val eyeR = w * 0.04f
    if (isBlinking) {
        drawBlinkLine(Offset(w * 0.38f, eyeY), eyeR, w * 0.028f)
        drawBlinkLine(Offset(w * 0.62f, eyeY), eyeR, w * 0.028f)
    } else {
        drawCircle(TextPrimaryLight, radius = eyeR, center = Offset(w * 0.38f + eyeLook.x, eyeY + eyeLook.y))
        drawCircle(TextPrimaryLight, radius = eyeR, center = Offset(w * 0.62f + eyeLook.x, eyeY + eyeLook.y))
    }

    val beardPath = Path().apply {
        moveTo(w * 0.35f, h * 0.80f)
        quadraticBezierTo(w * 0.5f, h * 0.94f, w * 0.65f, h * 0.80f)
        close()
    }
    drawPath(beardPath, Color(0xFF635343))
}

private fun DrawScope.drawTheronFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset
) {
    drawCircle(
        color = AccentTheronCosmic.copy(alpha = 0.25f),
        radius = w * 0.3f,
        center = Offset(w * 0.5f, h * 0.75f)
    )

    val eyeY = h * 0.72f
    val eyeR = w * 0.05f
    if (isBlinking) {
        drawBlinkLine(Offset(w * 0.36f, eyeY), eyeR, w * 0.032f, AccentTheronCosmic)
        drawBlinkLine(Offset(w * 0.64f, eyeY), eyeR, w * 0.032f, AccentTheronCosmic)
    } else {
        drawCircle(AccentTheronCosmic, radius = eyeR, center = Offset(w * 0.36f + eyeLook.x, eyeY + eyeLook.y))
        drawCircle(Color.White, radius = w * 0.02f, center = Offset(w * 0.36f + eyeLook.x, eyeY + eyeLook.y))

        drawCircle(AccentTheronCosmic, radius = eyeR, center = Offset(w * 0.64f + eyeLook.x, eyeY + eyeLook.y))
        drawCircle(Color.White, radius = w * 0.02f, center = Offset(w * 0.64f + eyeLook.x, eyeY + eyeLook.y))
    }

    val mouthPath = Path().apply {
        moveTo(w * 0.42f, h * 0.84f)
        quadraticBezierTo(w * 0.5f, h * 0.89f, w * 0.58f, h * 0.84f)
    }
    drawPath(mouthPath, AccentTheronCosmic, style = Stroke(width = w * 0.034f, cap = StrokeCap.Round))
}

private fun DrawScope.drawTrinityFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset
) {
    val leftWing = Path().apply {
        moveTo(w * 0.25f, h * 0.65f)
        quadraticBezierTo(w * 0.05f, h * 0.55f, w * 0.15f, h * 0.75f)
        close()
    }
    val rightWing = Path().apply {
        moveTo(w * 0.75f, h * 0.65f)
        quadraticBezierTo(w * 0.95f, h * 0.55f, w * 0.85f, h * 0.75f)
        close()
    }
    drawPath(leftWing, AccentTrinityRainbow.copy(alpha = 0.8f))
    drawPath(rightWing, AccentTrinityRainbow.copy(alpha = 0.8f))

    val eyeY = h * 0.72f
    val eyeR = w * 0.05f
    if (isBlinking) {
        drawBlinkLine(Offset(w * 0.36f, eyeY), eyeR, w * 0.032f, AccentTerrenceCrown)
        drawBlinkLine(Offset(w * 0.64f, eyeY), eyeR, w * 0.032f, AccentTerrenceCrown)
    } else {
        drawCircle(AccentTerrenceCrown, radius = eyeR, center = Offset(w * 0.36f + eyeLook.x, eyeY + eyeLook.y))
        drawCircle(AccentTerrenceCrown, radius = eyeR, center = Offset(w * 0.64f + eyeLook.x, eyeY + eyeLook.y))
    }

    val mouthPath = Path().apply {
        moveTo(w * 0.4f, h * 0.83f)
        quadraticBezierTo(w * 0.5f, h * 0.88f, w * 0.6f, h * 0.83f)
    }
    drawPath(mouthPath, AccentTerrenceCrown, style = Stroke(width = w * 0.038f, cap = StrokeCap.Round))
}
