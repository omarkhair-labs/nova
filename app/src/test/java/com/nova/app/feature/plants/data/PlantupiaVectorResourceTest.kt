package com.nova.app.feature.plants.data

import androidx.compose.ui.graphics.vector.PathParser
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** AAPT accepts some path strings that crash Compose only when a rare avatar is displayed. */
class PlantupiaVectorResourceTest {
    @Test fun allExistingPlantupiaVectorsParseWithTheActualComposePathParser() {
        val folder = listOf(File("src/main/res/drawable"), File("app/src/main/res/drawable")).first { it.isDirectory }
        val resources = folder.listFiles()!!.filter { it.name.startsWith("plantupia_") && it.extension == "xml" }
        assertTrue(resources.isNotEmpty())
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        resources.forEach { file ->
            val document = factory.newDocumentBuilder().parse(file)
            val nodes = document.getElementsByTagName("*")
            for (index in 0 until nodes.length) {
                val data = nodes.item(index).attributes.getNamedItemNS("http://schemas.android.com/apk/res/android", "pathData")?.nodeValue ?: continue
                val parsed = try { PathParser().parsePathString(data).toNodes() }
                catch (failure: IllegalArgumentException) { throw AssertionError("Invalid vector ${file.name}", failure) }
                assertFalse(file.name, parsed.toString().contains("NaN"))
                assertFalse(file.name, parsed.toString().contains("Infinity"))
            }
        }
    }
}
