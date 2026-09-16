package com.nova.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.nova.app.ui.theme.NovaBaseInk
import com.nova.app.ui.theme.NovaBrandElectricBlue
import com.nova.app.ui.theme.NovaBrandPurple
import com.nova.app.ui.theme.NovaBrandSunrisePeach
import kotlin.math.min

@Composable
fun NovaBrandMark(
    modifier: Modifier = Modifier,
    monochrome: Boolean = false,
    monochromeColor: Color = NovaBaseInk,
) {
    Canvas(modifier = modifier) {
        val unit = min(size.width, size.height) / 108f
        val originX = (size.width - 108f * unit) / 2f
        val originY = (size.height - 108f * unit) / 2f
        fun x(value: Float) = originX + value * unit
        fun y(value: Float) = originY + value * unit

        val left = Path().apply {
            moveTo(x(30f), y(70f))
            cubicTo(x(27f), y(57f), x(27f), y(42f), x(31f), y(33f))
            cubicTo(x(35f), y(24f), x(44f), y(21f), x(52f), y(25f))
            cubicTo(x(60f), y(29f), x(64f), y(38f), x(60f), y(46f))
            cubicTo(x(56f), y(54f), x(48f), y(61f), x(43f), y(68f))
            cubicTo(x(39f), y(74f), x(35f), y(79f), x(32f), y(77f))
            cubicTo(x(30f), y(76f), x(30f), y(73f), x(30f), y(70f))
            close()
        }
        val right = Path().apply {
            moveTo(x(56f), y(55f))
            cubicTo(x(62f), y(47f), x(69f), y(40f), x(76f), y(32f))
            cubicTo(x(82f), y(26f), x(87f), y(28f), x(87f), y(36f))
            cubicTo(x(88f), y(49f), x(86f), y(63f), x(82f), y(73f))
            cubicTo(x(78f), y(83f), x(68f), y(86f), x(60f), y(80f))
            cubicTo(x(52f), y(74f), x(51f), y(64f), x(56f), y(55f))
            close()
        }

        if (monochrome) {
            drawPath(left, color = monochromeColor)
            drawPath(right, color = monochromeColor)
        } else {
            drawPath(left, brush = Brush.linearGradient(
                0f to NovaBrandSunrisePeach,
                0.48f to Color(0xFFC987FF),
                1f to NovaBrandPurple,
                start = Offset(x(30f), y(25f)), end = Offset(x(54f), y(75f)),
            ))
            drawPath(right, brush = Brush.linearGradient(
                colors = listOf(NovaBrandPurple, NovaBrandElectricBlue),
                start = Offset(x(58f), y(32f)), end = Offset(x(83f), y(82f)),
            ))
        }
    }
}
