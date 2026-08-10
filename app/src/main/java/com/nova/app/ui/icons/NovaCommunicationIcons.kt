package com.nova.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp


object NovaCommunicationIcons {
    val Mic: ImageVector by lazy {
        ImageVector.Builder(
            name = "NovaMic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 4f)
                curveTo(10.35f, 4f, 9f, 5.35f, 9f, 7f)
                lineTo(9f, 11f)
                curveTo(9f, 12.65f, 10.35f, 14f, 12f, 14f)
                curveTo(13.65f, 14f, 15f, 12.65f, 15f, 11f)
                lineTo(15f, 7f)
                curveTo(15f, 5.35f, 13.65f, 4f, 12f, 4f)
                close()
                moveTo(6.5f, 11f)
                curveTo(6.5f, 14.04f, 8.96f, 16.5f, 12f, 16.5f)
                curveTo(15.04f, 16.5f, 17.5f, 14.04f, 17.5f, 11f)
                moveTo(12f, 16.5f)
                lineTo(12f, 20f)
                moveTo(9f, 20f)
                lineTo(15f, 20f)
            }
        }.build()
    }

    val VolumeUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "NovaVolumeUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 10f)
                lineTo(8f, 10f)
                lineTo(12f, 6f)
                lineTo(12f, 18f)
                lineTo(8f, 14f)
                lineTo(4f, 14f)
                close()
                moveTo(15.5f, 9f)
                curveTo(16.5f, 9.75f, 17f, 10.75f, 17f, 12f)
                curveTo(17f, 13.25f, 16.5f, 14.25f, 15.5f, 15f)
                moveTo(18f, 6.5f)
                curveTo(20f, 8f, 21f, 9.8f, 21f, 12f)
                curveTo(21f, 14.2f, 20f, 16f, 18f, 17.5f)
            }
        }.build()
    }

    val Video: ImageVector by lazy {
        ImageVector.Builder(
            name = "NovaVideo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 7f)
                lineTo(15f, 7f)
                curveTo(16.1f, 7f, 17f, 7.9f, 17f, 9f)
                lineTo(17f, 15f)
                curveTo(17f, 16.1f, 16.1f, 17f, 15f, 17f)
                lineTo(5f, 17f)
                curveTo(3.9f, 17f, 3f, 16.1f, 3f, 15f)
                lineTo(3f, 9f)
                curveTo(3f, 7.9f, 3.9f, 7f, 5f, 7f)
                close()
                moveTo(17f, 10f)
                lineTo(21f, 8f)
                lineTo(21f, 16f)
                lineTo(17f, 14f)
            }
        }.build()
    }

    val CallEnd: ImageVector by lazy {
        ImageVector.Builder(
            name = "NovaCallEnd",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 16f)
                lineTo(2.8f, 13.8f)
                curveTo(5.4f, 11.5f, 8.5f, 10.4f, 12f, 10.4f)
                curveTo(15.5f, 10.4f, 18.6f, 11.5f, 21.2f, 13.8f)
                lineTo(20f, 16f)
                lineTo(16.6f, 14.3f)
                lineTo(16.6f, 12.4f)
                curveTo(13.7f, 11.5f, 10.3f, 11.5f, 7.4f, 12.4f)
                lineTo(7.4f, 14.3f)
                close()
            }
        }.build()
    }
}
