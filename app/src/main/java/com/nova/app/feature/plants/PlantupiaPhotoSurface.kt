package com.nova.app.feature.plants

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.sin

internal data class PhotoSurface(val faces: List<FloatArray>, val texture: Bitmap)
internal data class PhotoSceneGeometry(val points: List<SpatialPoint>, val surface: PhotoSurface?)

internal fun decodePhotoScene(body: JSONObject): PhotoSceneGeometry {
    val encoded = body.getString("ply")
    require(encoded.length <= 940000)
    val points = Base64.decode(encoded, Base64.DEFAULT).inputStream().use(::readPlantupiaPointCloud)
    val surface = body.optJSONObject("surface") ?: return PhotoSceneGeometry(points, null)
    val rows = surface.optJSONArray("triangles") ?: return PhotoSceneGeometry(points, null)
    require(rows.length() in 10..2000)
    val faces = List(rows.length()) { index ->
        val row = rows.getJSONArray(index)
        require(row.length() == 15)
        FloatArray(15) { i ->
            row.getDouble(i).toFloat().also { value ->
                require(if (i % 5 < 3) value in -2.51f..2.51f else value in 0f..1024f)
            }
        }
    }
    val image = surface.getString("texture")
    require(image.length in 1..2000000)
    val bytes = Base64.decode(image, Base64.DEFAULT)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth == 1024 && bounds.outHeight == 1024)
    val texture = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
    return PhotoSceneGeometry(points, PhotoSurface(faces, texture))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PhotoSurfaceStage(surface: PhotoSurface) {
    var yaw by remember(surface) { mutableFloatStateOf(0f) }
    var pitch by remember(surface) { mutableFloatStateOf(0f) }
    var zoom by remember(surface) { mutableFloatStateOf(1f) }
    val gesture = rememberTransformableState { factor, pan, _ ->
        yaw += pan.x * .009f
        pitch = (pitch - pan.y * .009f).coerceIn(-1.5f, 1.5f)
        zoom = (zoom * factor).coerceIn(.5f, 3f)
    }
    val texture = remember(surface) { BitmapShader(surface.texture, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
    val paint = remember(texture) { Paint(Paint.FILTER_BITMAP_FLAG).apply { shader = texture } }
    Canvas(Modifier.fillMaxWidth().aspectRatio(1.25f).clipToBounds().background(Color(0xFF142820))
        .semantics { contentDescription = "Photographic reconstruction. Drag to turn and pinch to zoom." }
        .transformable(gesture)) {
        val cy = cos(yaw)
        val sy = sin(yaw)
        val cp = cos(pitch)
        val sp = sin(pitch)
        val scale = size.minDimension * .46f * zoom
        val order = surface.faces.sortedByDescending { f ->
            (0..2).sumOf { i ->
                val p = i * 5
                (f[p+1] * sp + (f[p] * sy + f[p+2] * cy) * cp).toDouble()
            }
        }
        val src = FloatArray(6)
        val dst = FloatArray(6)
        val transform = Matrix()
        val outline = Path()
        for (face in order) {
            for (i in 0..2) {
                val p = i * 5
                val depth = face[p] * sy + face[p+2] * cy
                val perspective = 4.5f / (4.5f + face[p+1] * sp + depth * cp)
                dst[i*2] = center.x + (face[p] * cy - face[p+2] * sy) * scale * perspective
                dst[i*2+1] = center.y + (face[p+1] * cp - depth * sp) * scale * perspective
                src[i*2] = face[p+3]
                src[i*2+1] = face[p+4]
            }
            if (!transform.setPolyToPoly(src, 0, dst, 0, 3)) continue
            texture.setLocalMatrix(transform)
            outline.rewind()
            outline.moveTo(dst[0], dst[1])
            outline.lineTo(dst[2], dst[3])
            outline.lineTo(dst[4], dst[5])
            outline.close()
            drawContext.canvas.nativeCanvas.drawPath(outline, paint)
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TextButton(onClick = { yaw -= .30f }) { Text("Turn left") }
        TextButton(onClick = { yaw += .30f }) { Text("Turn right") }
        TextButton(onClick = { zoom = if (zoom > 1f) 1f else 1.7f }) { Text("Zoom") }
        TextButton(onClick = { yaw = 0f; pitch = 0f; zoom = 1f }) { Text("Reset view") }
    }
}
