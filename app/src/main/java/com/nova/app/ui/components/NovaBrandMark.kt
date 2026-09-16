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
    interactionProgress: Float = 1f,
) {
    Canvas(modifier = modifier) {
        val unit = min(size.width, size.height) / 108f
        val originX = (size.width - 108f * unit) / 2f
        val originY = (size.height - 108f * unit) / 2f
        val settled = interactionProgress.coerceIn(0f, 1f)
        val separation = 11f * (1f - settled)

        fun lx(value: Float) = originX + (value - separation) * unit
        fun rx(value: Float) = originX + (value + separation) * unit
        fun y(value: Float) = originY + value * unit

        val left = Path().apply {
            moveTo(lx(30f), y(70f))
            cubicTo(lx(27f), y(57f), lx(27f), y(42f), lx(31f), y(33f))
            cubicTo(lx(35f), y(24f), lx(44f), y(21f), lx(52f), y(25f))
            cubicTo(lx(60f), y(29f), lx(64f), y(38f), lx(60f), y(46f))
            cubicTo(lx(56f), y(54f), lx(48f), y(61f), lx(43f), y(68f))
            cubicTo(lx(39f), y(74f), lx(35f), y(79f), lx(32f), y(77f))
            cubicTo(lx(30f), y(76f), lx(30f), y(73f), lx(30f), y(70f))
            close()
        }
        val right = Path().apply {
            moveTo(rx(56f), y(55f))
            cubicTo(rx(62f), y(47f), rx(69f), y(40f), rx(76f), y(32f))
            cubicTo(rx(82f), y(26f), rx(87f), y(28f), rx(87f), y(36f))
            cubicTo(rx(88f), y(49f), rx(86f), y(63f), rx(82f), y(73f))
            cubicTo(rx(78f), y(83f), rx(68f), y(86f), rx(60f), y(80f))
            cubicTo(rx(52f), y(74f), rx(51f), y(64f), rx(56f), y(55f))
            close()
        }

        if (monochrome) {
            drawPath(left, color = monochromeColor)
            drawPath(right, color = monochromeColor)
        } else {
            drawPath(
                left,
                brush = Brush.linearGradient(
                    0f to NovaBrandSunrisePeach,
                    0.48f to Color(0xFFC987FF),
                    1f to NovaBrandPurple,
                    start = Offset(lx(30f), y(25f)),
                    end = Offset(lx(54f), y(75f)),
                ),
            )
            drawPath(
                right,
                brush = Brush.linearGradient(
                    colors = listOf(NovaBrandPurple, NovaBrandElectricBlue),
                    start = Offset(rx(58f), y(32f)),
                    end = Offset(rx(83f), y(82f)),
                ),
            )
        }
    }
}
