package com.nova.app.feature.plants

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaPointCloudTest {
    private val header = """
        ply
        format ascii 1.0
        comment plantupia_sparse_sfm_unscaled
        element vertex 30
        property float x
        property float y
        property float z
        property uchar red
        property uchar green
        property uchar blue
        end_header
    """.trimIndent() + "\n"
    private val rows = (1..30).joinToString("\n") { "$it ${it % 6} ${it % 7} 20 100 30" }

    @Test fun opensBoundedRelativeSceneAndPreservesItsColours() {
        val raw = parsePlantupiaPointCloud((header + rows).toByteArray())
        assertEquals(30, raw.size)
        assertEquals(SpatialPoint(1.0, 1.0, 1.0, 20, 100, 30), raw.first())
        val scene = readPlantupiaPointCloud(ByteArrayInputStream((header + rows).toByteArray()))
        assertEquals(30, scene.size)
        assertTrue(scene.all { it.x in -2.5..2.5 && it.green == 100 })
    }

    @Test fun rejectsMalformedTruncatedOverLimitAndDegenerateScenes() {
        val bad = listOf(
            header + rows.substringBeforeLast('\n'),
            header + rows + "\nadditional element",
            header + rows.replaceFirst("1 1 1", "NaN 1 1"),
            header.replace("vertex 30", "vertex 6001") + rows,
            header + ("0 0 0 20 100 30\n").repeat(30),
        )
        for (text in bad) {
            assertThrows(IllegalArgumentException::class.java) {
                readPlantupiaPointCloud(ByteArrayInputStream(text.toByteArray()))
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            readPlantupiaPointCloud(ByteArrayInputStream(ByteArray(PLANTUPIA_PLY_MAX_BYTES + 1)))
        }
    }
}
