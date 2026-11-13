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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.threes.game.ui.theme.*

enum class DragExpressionMood {
    NEUTRAL,
    MOVING,
    WILL_MERGE,
    BLOCKED
}

@Composable
fun CharacterFace(
    value: Int,
    isBlinking: Boolean,
    lookOffsetX: Float = 0f,
    lookOffsetY: Float = 0f,
    dragProgressProvider: () -> Float = { 0f },
    expressionMood: DragExpressionMood = DragExpressionMood.NEUTRAL,
    isCelebrating: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val dragProgress = dragProgressProvider().coerceIn(0f, 1f)
        val gazeScale = if (expressionMood == DragExpressionMood.NEUTRAL) {
            0f
        } else {
            (dragProgress / 0.45f).coerceIn(0f, 1f)
        }
        val eyeLook = Offset(
            x = lookOffsetX * gazeScale * w * 0.048f,
            y = lookOffsetY * gazeScale * h * 0.048f
        )
        val moodIntensity = when (expressionMood) {
            DragExpressionMood.WILL_MERGE -> ((dragProgress - 0.14f) / 0.31f).coerceIn(0f, 1f)
            DragExpressionMood.BLOCKED -> ((dragProgress - 0.08f) / 0.24f).coerceIn(0f, 1f)
            else -> 0f
        }

        when (value) {
            1 -> drawUnoFace(w, h, eyeLook, expressionMood, moodIntensity)
            2 -> drawDosFace(w, h, isBlinking, eyeLook, expressionMood, moodIntensity)
            3 -> drawTreyFace(w, h, isBlinking, eyeLook, expressionMood, moodIntensity, isCelebrating)
            6 -> drawToddFace(w, h, isBlinking, eyeLook, expressionMood, moodIntensity, isCelebrating)
            12 -> drawTerryFace(w, h, eyeLook, expressionMood, moodIntensity, isCelebrating)
            24 -> drawTorvalFace(w, h, isBlinking, eyeLook, expressionMood, moodIntensity, isCelebrating)
            48 -> drawTweeFace(w, h, isBlinking, eyeLook, expressionMood, moodIntensity, isCelebrating)
            96 -> drawThelmaFace(w, h, eyeLook, expressionMood, moodIntensity, isCelebrating)
            192 -> drawThurstonFace(w, h, isBlinking, eyeLook, expressionMood, moodIntensity, isCelebrating)
            384 -> drawTorranceFace(w, h, isBlinking, eyeLook, expressionMood, moodIntensity, isCelebrating)
            768 -> drawTychoFace(w, h, eyeLook, expressionMood, moodIntensity, isCelebrating)
            1536 -> drawTerrenceFace(w, h, isBlinking, eyeLook, expressionMood, moodIntensity, isCelebrating)
            3072 -> drawTheronFace(w, h, isBlinking, eyeLook, expressionMood, moodIntensity, isCelebrating)
            6144 -> drawTrinityFace(w, h, isBlinking, eyeLook, expressionMood, moodIntensity, isCelebrating)
            else -> drawTreyFace(w, h, isBlinking, eyeLook, expressionMood, moodIntensity, isCelebrating)
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

private fun DrawScope.drawHappyArcEye(
    center: Offset,
    radius: Float,
    strokeWidth: Float,
    color: Color = TextPrimaryLight
) {
    val path = Path().apply {
        moveTo(center.x - radius * 1.15f, center.y + radius * 0.25f)
        quadraticBezierTo(center.x, center.y - radius * 1.15f, center.x + radius * 1.15f, center.y + radius * 0.25f)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawSquintChevron(
    center: Offset,
    radius: Float,
    strokeWidth: Float,
    isLeftEye: Boolean,
    color: Color = TextPrimaryLight
) {
    val dirSign = if (isLeftEye) 1f else -1f
    val path = Path().apply {
        moveTo(center.x - dirSign * radius * 0.75f, center.y - radius * 0.75f)
        lineTo(center.x + dirSign * radius * 0.65f, center.y)
        lineTo(center.x - dirSign * radius * 0.75f, center.y + radius * 0.75f)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawExpressiveEye(
    center: Offset,
    radius: Float,
    strokeWidth: Float,
    color: Color = TextPrimaryLight,
    eyeLook: Offset,
    isBlinking: Boolean,
    isLeftEye: Boolean,
    mood: DragExpressionMood,
    moodIntensity: Float,
    isCelebrating: Boolean,
    sparkleColor: Color = Color.White
) {
    when {
        isCelebrating -> drawHappyArcEye(center, radius, strokeWidth, color)
        mood == DragExpressionMood.BLOCKED && moodIntensity > 0.25f ->
            drawSquintChevron(center, radius, strokeWidth, isLeftEye, color)
        isBlinking -> drawBlinkLine(center, radius, strokeWidth, color)
        mood == DragExpressionMood.WILL_MERGE && moodIntensity > 0.18f -> {
            val excitedRadius = radius * (1f + 0.24f * moodIntensity)
            val shifted = center + eyeLook
            drawCircle(color = color, radius = excitedRadius, center = shifted)
            drawCircle(
                color = sparkleColor.copy(alpha = 0.9f * moodIntensity),
                radius = excitedRadius * 0.32f,
                center = Offset(shifted.x - excitedRadius * 0.28f, shifted.y - excitedRadius * 0.28f)
            )
        }
        else -> drawCircle(color = color, radius = radius, center = center + eyeLook)
    }
}

private fun DrawScope.drawReactiveMouth(
    w: Float,
    h: Float,
    color: Color,
    strokeWidth: Float,
    mood: DragExpressionMood,
    moodIntensity: Float,
    isCelebrating: Boolean,
    mouthCenterY: Float = h * 0.85f,
    defaultMouth: DrawScope.() -> Unit
) {
    when {
        isCelebrating -> {
            val smileRect = Rect(w * 0.41f, mouthCenterY - h * 0.055f, w * 0.59f, mouthCenterY + h * 0.055f)
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = smileRect.topLeft,
                size = smileRect.size
            )
        }
        mood == DragExpressionMood.WILL_MERGE && moodIntensity > 0.22f -> {
            val rx = w * (0.022f + 0.028f * moodIntensity)
            val ry = h * (0.018f + 0.026f * moodIntensity)
            drawOval(
                color = color,
                topLeft = Offset(w * 0.5f - rx, mouthCenterY - ry),
                size = Size(rx * 2f, ry * 2f)
            )
        }
        mood == DragExpressionMood.BLOCKED && moodIntensity > 0.25f -> {
            drawLine(
                color = color,
                start = Offset(w * 0.43f, mouthCenterY),
                end = Offset(w * 0.57f, mouthCenterY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
        else -> defaultMouth()
    }
}

private fun DrawScope.drawUnoFace(
    w: Float,
    h: Float,
    eyeLook: Offset,
    mood: DragExpressionMood,
    moodIntensity: Float
) {
    val barColor = Color.White.copy(alpha = 0.88f)
    val shift = Offset(eyeLook.x * 0.75f, eyeLook.y * 0.75f)
    when {
        mood == DragExpressionMood.WILL_MERGE && moodIntensity > 0.22f -> {
            val pillW = w * (0.30f - 0.12f * moodIntensity)
            val pillH = h * (0.05f + 0.04f * moodIntensity)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(w * 0.5f - pillW * 0.5f + shift.x, h * 0.805f - pillH * 0.5f + shift.y),
                size = Size(pillW, pillH),
                cornerRadius = CornerRadius(pillH * 0.5f)
            )
        }
        mood == DragExpressionMood.BLOCKED && moodIntensity > 0.25f -> {
            val pillH = h * (0.05f - 0.018f * moodIntensity)
            val pillW = w * (0.30f + 0.05f * moodIntensity)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(w * 0.5f - pillW * 0.5f + shift.x * 0.4f, h * 0.805f - pillH * 0.5f),
                size = Size(pillW, pillH),
                cornerRadius = CornerRadius(pillH * 0.5f)
            )
        }
        else -> {
            drawRoundRect(
                color = barColor,
                topLeft = Offset(w * 0.35f + shift.x, h * 0.78f + shift.y),
                size = Size(w * 0.3f, h * 0.05f),
                cornerRadius = CornerRadius(h * 0.025f)
            )
        }
    }
}

private fun DrawScope.drawDosFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset,
    mood: DragExpressionMood,
    moodIntensity: Float
) {
    val r = w * 0.036f
    val eyeY = h * 0.8f
    val color = Color.White.copy(alpha = 0.88f)
    val strokeW = w * 0.028f
    drawExpressiveEye(
        center = Offset(w * 0.42f, eyeY),
        radius = r,
        strokeWidth = strokeW,
        color = color,
        eyeLook = eyeLook,
        isBlinking = isBlinking,
        isLeftEye = true,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = false,
        sparkleColor = Card2Bg
    )
    drawExpressiveEye(
        center = Offset(w * 0.58f, eyeY),
        radius = r,
        strokeWidth = strokeW,
        color = color,
        eyeLook = eyeLook,
        isBlinking = isBlinking,
        isLeftEye = false,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = false,
        sparkleColor = Card2Bg
    )
}

private fun DrawScope.drawTreyFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset,
    mood: DragExpressionMood,
    moodIntensity: Float,
    isCelebrating: Boolean
) {
    val eyeY = h * 0.74f
    val eyeLeftX = w * 0.38f
    val eyeRightX = w * 0.62f
    val eyeR = w * 0.045f
    val strokeW = w * 0.03f

    drawExpressiveEye(
        center = Offset(eyeLeftX, eyeY),
        radius = eyeR,
        strokeWidth = strokeW,
        eyeLook = eyeLook,
        isBlinking = isBlinking,
        isLeftEye = true,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating
    )
    drawExpressiveEye(
        center = Offset(eyeRightX, eyeY),
        radius = eyeR,
        strokeWidth = strokeW,
        eyeLook = eyeLook,
        isBlinking = isBlinking,
        isLeftEye = false,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating
    )

    drawReactiveMouth(
        w = w,
        h = h,
        color = TextPrimaryLight,
        strokeWidth = w * 0.034f,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating,
        mouthCenterY = h * 0.85f
    ) {
        val mouthPath = Path().apply {
            moveTo(w * 0.42f, h * 0.84f)
            quadraticBezierTo(w * 0.5f, h * 0.90f, w * 0.58f, h * 0.84f)
        }
        drawPath(mouthPath, TextPrimaryLight, style = Stroke(width = w * 0.035f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawToddFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset,
    mood: DragExpressionMood,
    moodIntensity: Float,
    isCelebrating: Boolean
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

    val glassLook = Offset(eyeLook.x * 0.75f, eyeLook.y * 0.75f)
    drawExpressiveEye(
        center = Offset(eyeLeftX, eyeY),
        radius = w * 0.035f,
        strokeWidth = strokeW,
        eyeLook = glassLook,
        isBlinking = isBlinking,
        isLeftEye = true,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating
    )
    drawExpressiveEye(
        center = Offset(eyeRightX, eyeY),
        radius = w * 0.035f,
        strokeWidth = strokeW,
        eyeLook = glassLook,
        isBlinking = isBlinking,
        isLeftEye = false,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating
    )

    drawReactiveMouth(
        w = w,
        h = h,
        color = TextPrimaryLight,
        strokeWidth = strokeW,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating,
        mouthCenterY = h * 0.88f
    ) {
        val mouthPath = Path().apply {
            moveTo(w * 0.44f, h * 0.88f)
            quadraticBezierTo(w * 0.52f, h * 0.91f, w * 0.58f, h * 0.87f)
        }
        drawPath(mouthPath, TextPrimaryLight, style = Stroke(width = strokeW, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawTerryFace(
    w: Float,
    h: Float,
    eyeLook: Offset,
    mood: DragExpressionMood,
    moodIntensity: Float,
    isCelebrating: Boolean
) {
    val frameShift = Offset(eyeLook.x * 0.45f, eyeLook.y * 0.45f)
    val eyeY = h * 0.74f + frameShift.y
    val shadePath = Path().apply {
        moveTo(w * 0.25f + frameShift.x, eyeY - h * 0.03f)
        lineTo(w * 0.75f + frameShift.x, eyeY - h * 0.03f)
        lineTo(w * 0.68f + frameShift.x, eyeY + h * 0.05f)
        lineTo(w * 0.53f + frameShift.x, eyeY + h * 0.02f)
        lineTo(w * 0.47f + frameShift.x, eyeY + h * 0.02f)
        lineTo(w * 0.32f + frameShift.x, eyeY + h * 0.05f)
        close()
    }
    drawPath(shadePath, Color(0xFF2C3E50))

    // Glint sweeping across sunglasses lens in gaze direction
    val glintAlpha = if (mood == DragExpressionMood.WILL_MERGE || isCelebrating) 0.65f else 0.38f
    val glintX = w * 0.37f + eyeLook.x * 1.4f
    val glintX2 = w * 0.61f + eyeLook.x * 1.4f
    drawLine(
        color = Color.White.copy(alpha = glintAlpha),
        start = Offset(glintX - w * 0.02f, eyeY + h * 0.025f),
        end = Offset(glintX + w * 0.025f, eyeY - h * 0.018f),
        strokeWidth = w * 0.018f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color.White.copy(alpha = glintAlpha),
        start = Offset(glintX2 - w * 0.02f, eyeY + h * 0.025f),
        end = Offset(glintX2 + w * 0.025f, eyeY - h * 0.018f),
        strokeWidth = w * 0.018f,
        cap = StrokeCap.Round
    )

    val hairPath = Path().apply {
        moveTo(w * 0.3f, h * 0.62f)
        lineTo(w * 0.4f, h * 0.54f)
        lineTo(w * 0.5f, h * 0.61f)
        lineTo(w * 0.6f, h * 0.52f)
        lineTo(w * 0.7f, h * 0.62f)
    }
    drawPath(hairPath, AccentTerryRed, style = Stroke(width = w * 0.038f, cap = StrokeCap.Round))

    drawReactiveMouth(
        w = w,
        h = h,
        color = TextPrimaryLight,
        strokeWidth = w * 0.03f,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating,
        mouthCenterY = h * 0.85f
    ) {
        val mouthPath = Path().apply {
            moveTo(w * 0.42f, h * 0.85f)
            quadraticBezierTo(w * 0.52f, h * 0.88f, w * 0.62f, h * 0.83f)
        }
        drawPath(mouthPath, TextPrimaryLight, style = Stroke(width = w * 0.03f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawTorvalFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset,
    mood: DragExpressionMood,
    moodIntensity: Float,
    isCelebrating: Boolean
) {
    val eyeY = h * 0.72f
    val eyeLeftX = w * 0.35f
    val eyeRightX = w * 0.65f
    val eyeR = w * 0.045f
    val strokeW = w * 0.03f

    drawExpressiveEye(
        center = Offset(eyeLeftX, eyeY),
        radius = eyeR,
        strokeWidth = strokeW,
        eyeLook = eyeLook,
        isBlinking = isBlinking,
        isLeftEye = true,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating
    )
    drawExpressiveEye(
        center = Offset(eyeRightX, eyeY),
        radius = eyeR,
        strokeWidth = strokeW,
        eyeLook = eyeLook,
        isBlinking = isBlinking,
        isLeftEye = false,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating
    )

    val blushAlpha = if (isCelebrating || mood == DragExpressionMood.WILL_MERGE) 0.78f else 0.6f
    drawCircle(AccentTorvalBlush.copy(alpha = blushAlpha), radius = w * 0.06f, center = Offset(w * 0.25f, h * 0.78f))
    drawCircle(AccentTorvalBlush.copy(alpha = blushAlpha), radius = w * 0.06f, center = Offset(w * 0.75f, h * 0.78f))

    drawReactiveMouth(
        w = w,
        h = h,
        color = TextPrimaryLight,
        strokeWidth = strokeW,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating,
        mouthCenterY = h * 0.835f
    ) {
        val mouthRect = Rect(w * 0.4f, h * 0.78f, w * 0.6f, h * 0.89f)
        drawArc(TextPrimaryLight, 0f, 180f, true, topLeft = mouthRect.topLeft, size = mouthRect.size)
    }
}

private fun DrawScope.drawTweeFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset,
    mood: DragExpressionMood,
    moodIntensity: Float,
    isCelebrating: Boolean
) {
    drawCircle(AccentTweeFlower, radius = w * 0.05f, center = Offset(w * 0.25f, h * 0.63f))
    drawCircle(Color.Yellow, radius = w * 0.02f, center = Offset(w * 0.25f, h * 0.63f))

    val eyeY = h * 0.74f
    val eyeLeftX = w * 0.37f
    val eyeRightX = w * 0.63f
    val eyeR = w * 0.04f
    val strokeW = w * 0.028f

    drawExpressiveEye(
        center = Offset(eyeLeftX, eyeY),
        radius = eyeR,
        strokeWidth = strokeW,
        eyeLook = eyeLook,
        isBlinking = isBlinking,
        isLeftEye = true,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating
    )
    drawExpressiveEye(
        center = Offset(eyeRightX, eyeY),
        radius = eyeR,
        strokeWidth = strokeW,
        eyeLook = eyeLook,
        isBlinking = isBlinking,
        isLeftEye = false,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating
    )

    if (!isBlinking && !isCelebrating && !(mood == DragExpressionMood.BLOCKED && moodIntensity > 0.25f)) {
        val lashStroke = w * 0.024f
        drawLine(TextPrimaryLight, Offset(eyeLeftX - eyeR, eyeY - eyeR), Offset(eyeLeftX - eyeR * 1.5f, eyeY - eyeR * 1.5f), lashStroke, StrokeCap.Round)
        drawLine(TextPrimaryLight, Offset(eyeRightX + eyeR, eyeY - eyeR), Offset(eyeRightX + eyeR * 1.5f, eyeY - eyeR * 1.5f), lashStroke, StrokeCap.Round)
    }

    drawReactiveMouth(
        w = w,
        h = h,
        color = TextPrimaryLight,
        strokeWidth = strokeW,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating,
        mouthCenterY = h * 0.85f
    ) {
        val mouthPath = Path().apply {
            moveTo(w * 0.45f, h * 0.84f)
            quadraticBezierTo(w * 0.5f, h * 0.88f, w * 0.55f, h * 0.84f)
        }
        drawPath(mouthPath, TextPrimaryLight, style = Stroke(width = strokeW, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawThelmaFace(
    w: Float,
    h: Float,
    eyeLook: Offset,
    mood: DragExpressionMood,
    moodIntensity: Float,
    isCelebrating: Boolean
) {
    val shift = Offset(eyeLook.x * 0.4f, eyeLook.y * 0.4f)
    val eyeY = h * 0.72f + shift.y
    val leftLens = Path().apply {
        moveTo(w * 0.24f + shift.x, eyeY - h * 0.03f)
        lineTo(w * 0.46f + shift.x, eyeY - h * 0.01f)
        lineTo(w * 0.44f + shift.x, eyeY + h * 0.04f)
        lineTo(w * 0.26f + shift.x, eyeY + h * 0.02f)
        close()
    }
    val rightLens = Path().apply {
        moveTo(w * 0.76f + shift.x, eyeY - h * 0.03f)
        lineTo(w * 0.54f + shift.x, eyeY - h * 0.01f)
        lineTo(w * 0.56f + shift.x, eyeY + h * 0.04f)
        lineTo(w * 0.74f + shift.x, eyeY + h * 0.02f)
        close()
    }
    drawPath(leftLens, AccentThelmaPurple)
    drawPath(rightLens, AccentThelmaPurple)

    val glintAlpha = if (mood == DragExpressionMood.WILL_MERGE || isCelebrating) 0.7f else 0.4f
    drawLine(
        color = Color.White.copy(alpha = glintAlpha),
        start = Offset(w * 0.31f + eyeLook.x * 1.2f, eyeY + h * 0.015f),
        end = Offset(w * 0.35f + eyeLook.x * 1.2f, eyeY - h * 0.012f),
        strokeWidth = w * 0.016f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color.White.copy(alpha = glintAlpha),
        start = Offset(w * 0.63f + eyeLook.x * 1.2f, eyeY + h * 0.015f),
        end = Offset(w * 0.67f + eyeLook.x * 1.2f, eyeY - h * 0.012f),
        strokeWidth = w * 0.016f,
        cap = StrokeCap.Round
    )

    val pearlR = w * 0.025f
    val pearlY = h * 0.91f
    listOf(0.35f, 0.42f, 0.5f, 0.58f, 0.65f).forEach { px ->
        drawCircle(Color(0xFFEDE7DC), radius = pearlR, center = Offset(w * px, pearlY))
    }

    val lipColor = Color(0xFFC0392B)
    drawReactiveMouth(
        w = w,
        h = h,
        color = lipColor,
        strokeWidth = w * 0.03f,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating,
        mouthCenterY = h * 0.835f
    ) {
        val mouthPath = Path().apply {
            moveTo(w * 0.43f, h * 0.83f)
            quadraticBezierTo(w * 0.5f, h * 0.86f, w * 0.57f, h * 0.83f)
        }
        drawPath(mouthPath, lipColor, style = Stroke(width = w * 0.03f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawThurstonFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset,
    mood: DragExpressionMood,
    moodIntensity: Float,
    isCelebrating: Boolean
) {
    val eyeY = h * 0.70f
    val eyeLeftX = w * 0.36f
    val eyeRightX = w * 0.64f
    val eyeR = w * 0.035f
    val strokeW = w * 0.024f

    drawCircle(AccentThurstonGold, radius = w * 0.075f, center = Offset(eyeRightX, eyeY), style = Stroke(width = strokeW))
    drawLine(AccentThurstonGold, Offset(eyeRightX + w * 0.075f, eyeY), Offset(w * 0.8f, eyeY + h * 0.1f), strokeWidth = w * 0.02f)

    val monocleLook = Offset(eyeLook.x * 0.8f, eyeLook.y * 0.8f)
    drawExpressiveEye(
        center = Offset(eyeLeftX, eyeY),
        radius = eyeR,
        strokeWidth = strokeW,
        eyeLook = monocleLook,
        isBlinking = isBlinking,
        isLeftEye = true,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating
    )
    drawExpressiveEye(
        center = Offset(eyeRightX, eyeY),
        radius = eyeR,
        strokeWidth = strokeW,
        eyeLook = monocleLook,
        isBlinking = isBlinking,
        isLeftEye = false,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating
    )

    val mustacheDrop = if (isCelebrating) h * 0.016f else if (mood == DragExpressionMood.WILL_MERGE) h * 0.012f * moodIntensity else 0f
    val mustachePath = Path().apply {
        moveTo(w * 0.3f, h * 0.80f + mustacheDrop)
        quadraticBezierTo(w * 0.4f, h * 0.77f + mustacheDrop, w * 0.5f, h * 0.82f + mustacheDrop)
        quadraticBezierTo(w * 0.6f, h * 0.77f + mustacheDrop, w * 0.7f, h * 0.80f + mustacheDrop)
        quadraticBezierTo(w * 0.6f, h * 0.86f + mustacheDrop, w * 0.5f, h * 0.84f + mustacheDrop)
        quadraticBezierTo(w * 0.4f, h * 0.86f + mustacheDrop, w * 0.3f, h * 0.80f + mustacheDrop)
        close()
    }
    drawPath(mustachePath, Color(0xFF4A3B32))
}

private fun DrawScope.drawTorranceFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset,
    mood: DragExpressionMood,
    moodIntensity: Float,
    isCelebrating: Boolean
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

    drawExpressiveEye(
        center = Offset(eyeRightX, eyeY),
        radius = eyeR,
        strokeWidth = w * 0.03f,
        eyeLook = eyeLook,
        isBlinking = isBlinking,
        isLeftEye = false,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating
    )

    drawReactiveMouth(
        w = w,
        h = h,
        color = TextPrimaryLight,
        strokeWidth = w * 0.034f,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating,
        mouthCenterY = h * 0.84f
    ) {
        val mouthPath = Path().apply {
            moveTo(w * 0.38f, h * 0.84f)
            lineTo(w * 0.62f, h * 0.84f)
        }
        drawPath(mouthPath, TextPrimaryLight, style = Stroke(width = w * 0.034f, cap = StrokeCap.Round))
        drawRect(AccentThurstonGold, topLeft = Offset(w * 0.52f, h * 0.83f), size = Size(w * 0.04f, h * 0.04f))
    }
}

private fun DrawScope.drawTychoFace(
    w: Float,
    h: Float,
    eyeLook: Offset,
    mood: DragExpressionMood,
    moodIntensity: Float,
    isCelebrating: Boolean
) {
    val eyeY = h * 0.72f
    drawRoundRect(
        color = AccentTychoCyan,
        topLeft = Offset(w * 0.22f, eyeY - h * 0.04f),
        size = Size(w * 0.56f, h * 0.07f),
        cornerRadius = CornerRadius(w * 0.035f)
    )
    val beamStroke = if (mood == DragExpressionMood.WILL_MERGE) w * (0.028f + 0.012f * moodIntensity) else w * 0.028f
    when {
        isCelebrating -> {
            val happyBeam = Path().apply {
                moveTo(w * 0.30f, eyeY + h * 0.008f)
                quadraticBezierTo(w * 0.50f, eyeY - h * 0.028f, w * 0.70f, eyeY + h * 0.008f)
            }
            drawPath(happyBeam, Color.White, style = Stroke(width = beamStroke, cap = StrokeCap.Round))
        }
        mood == DragExpressionMood.BLOCKED && moodIntensity > 0.25f -> {
            drawSquintChevron(Offset(w * 0.37f, eyeY - h * 0.004f), w * 0.035f, beamStroke * 0.85f, true, Color.White)
            drawSquintChevron(Offset(w * 0.63f, eyeY - h * 0.004f), w * 0.035f, beamStroke * 0.85f, false, Color.White)
        }
        else -> {
            drawLine(
                Color.White,
                Offset(w * 0.28f + eyeLook.x, eyeY + eyeLook.y * 0.45f),
                Offset(w * 0.72f + eyeLook.x, eyeY + eyeLook.y * 0.45f),
                strokeWidth = beamStroke,
                cap = StrokeCap.Round
            )
        }
    }

    drawReactiveMouth(
        w = w,
        h = h,
        color = Color(0xFF2C3E50),
        strokeWidth = w * 0.026f,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating,
        mouthCenterY = h * 0.85f
    ) {
        drawRect(Color(0xFF2C3E50), topLeft = Offset(w * 0.44f, h * 0.84f), size = Size(w * 0.12f, h * 0.025f))
    }
}

private fun DrawScope.drawTerrenceFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset,
    mood: DragExpressionMood,
    moodIntensity: Float,
    isCelebrating: Boolean
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
    val strokeW = w * 0.028f
    drawExpressiveEye(
        center = Offset(w * 0.38f, eyeY),
        radius = eyeR,
        strokeWidth = strokeW,
        eyeLook = eyeLook,
        isBlinking = isBlinking,
        isLeftEye = true,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating
    )
    drawExpressiveEye(
        center = Offset(w * 0.62f, eyeY),
        radius = eyeR,
        strokeWidth = strokeW,
        eyeLook = eyeLook,
        isBlinking = isBlinking,
        isLeftEye = false,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating
    )

    val beardPath = Path().apply {
        moveTo(w * 0.35f, h * 0.80f)
        quadraticBezierTo(w * 0.5f, h * 0.94f, w * 0.65f, h * 0.80f)
        close()
    }
    drawPath(beardPath, Color(0xFF635343))

    if (isCelebrating) {
        val smileRect = Rect(w * 0.43f, h * 0.81f, w * 0.57f, h * 0.87f)
        drawArc(Color.White, 0f, 180f, true, topLeft = smileRect.topLeft, size = smileRect.size)
    } else if (mood == DragExpressionMood.WILL_MERGE && moodIntensity > 0.25f) {
        drawCircle(Color(0xFF2C1E14), radius = w * 0.032f * moodIntensity, center = Offset(w * 0.5f, h * 0.84f))
    }
}

private fun DrawScope.drawTheronFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset,
    mood: DragExpressionMood,
    moodIntensity: Float,
    isCelebrating: Boolean
) {
    drawCircle(
        color = AccentTheronCosmic.copy(alpha = 0.25f),
        radius = w * 0.3f,
        center = Offset(w * 0.5f, h * 0.75f)
    )

    val eyeY = h * 0.72f
    val eyeR = w * 0.05f
    val strokeW = w * 0.032f
    if (isCelebrating || (mood == DragExpressionMood.BLOCKED && moodIntensity > 0.25f) || isBlinking) {
        drawExpressiveEye(
            center = Offset(w * 0.36f, eyeY),
            radius = eyeR,
            strokeWidth = strokeW,
            color = AccentTheronCosmic,
            eyeLook = eyeLook,
            isBlinking = isBlinking,
            isLeftEye = true,
            mood = mood,
            moodIntensity = moodIntensity,
            isCelebrating = isCelebrating
        )
        drawExpressiveEye(
            center = Offset(w * 0.64f, eyeY),
            radius = eyeR,
            strokeWidth = strokeW,
            color = AccentTheronCosmic,
            eyeLook = eyeLook,
            isBlinking = isBlinking,
            isLeftEye = false,
            mood = mood,
            moodIntensity = moodIntensity,
            isCelebrating = isCelebrating
        )
    } else {
        val r = if (mood == DragExpressionMood.WILL_MERGE) eyeR * (1f + 0.18f * moodIntensity) else eyeR
        drawCircle(AccentTheronCosmic, radius = r, center = Offset(w * 0.36f + eyeLook.x, eyeY + eyeLook.y))
        drawCircle(Color.White, radius = w * 0.02f, center = Offset(w * 0.36f + eyeLook.x, eyeY + eyeLook.y))
        drawCircle(AccentTheronCosmic, radius = r, center = Offset(w * 0.64f + eyeLook.x, eyeY + eyeLook.y))
        drawCircle(Color.White, radius = w * 0.02f, center = Offset(w * 0.64f + eyeLook.x, eyeY + eyeLook.y))
    }

    drawReactiveMouth(
        w = w,
        h = h,
        color = AccentTheronCosmic,
        strokeWidth = w * 0.034f,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating,
        mouthCenterY = h * 0.85f
    ) {
        val mouthPath = Path().apply {
            moveTo(w * 0.42f, h * 0.84f)
            quadraticBezierTo(w * 0.5f, h * 0.89f, w * 0.58f, h * 0.84f)
        }
        drawPath(mouthPath, AccentTheronCosmic, style = Stroke(width = w * 0.034f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawTrinityFace(
    w: Float,
    h: Float,
    isBlinking: Boolean,
    eyeLook: Offset,
    mood: DragExpressionMood,
    moodIntensity: Float,
    isCelebrating: Boolean
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
    val strokeW = w * 0.032f
    drawExpressiveEye(
        center = Offset(w * 0.36f, eyeY),
        radius = eyeR,
        strokeWidth = strokeW,
        color = AccentTerrenceCrown,
        eyeLook = eyeLook,
        isBlinking = isBlinking,
        isLeftEye = true,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating
    )
    drawExpressiveEye(
        center = Offset(w * 0.64f, eyeY),
        radius = eyeR,
        strokeWidth = strokeW,
        color = AccentTerrenceCrown,
        eyeLook = eyeLook,
        isBlinking = isBlinking,
        isLeftEye = false,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating
    )

    drawReactiveMouth(
        w = w,
        h = h,
        color = AccentTerrenceCrown,
        strokeWidth = w * 0.038f,
        mood = mood,
        moodIntensity = moodIntensity,
        isCelebrating = isCelebrating,
        mouthCenterY = h * 0.84f
    ) {
        val mouthPath = Path().apply {
            moveTo(w * 0.4f, h * 0.83f)
            quadraticBezierTo(w * 0.5f, h * 0.88f, w * 0.6f, h * 0.83f)
        }
        drawPath(mouthPath, AccentTerrenceCrown, style = Stroke(width = w * 0.038f, cap = StrokeCap.Round))
    }
}
