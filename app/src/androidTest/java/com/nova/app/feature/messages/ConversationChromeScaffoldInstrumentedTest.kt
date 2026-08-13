package com.nova.app.feature.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConversationChromeScaffoldInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun headerContentAndComposerRemainVisibleInAFullViewport() {
        assertChromeLayout(viewportHeight = 700.dp)
    }

    @Test
    fun headerContentAndComposerRemainVisibleInAnAdjustResizeViewport() {
        assertChromeLayout(viewportHeight = 320.dp)
    }

    private fun assertChromeLayout(viewportHeight: Dp) {
        composeRule.setContent {
            NovaTheme {
                ConversationChromeScaffold(
                    modifier = Modifier.requiredSize(width = 360.dp, height = viewportHeight),
                    topBar = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .background(NovaSurface),
                        )
                    },
                    bottomBar = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .background(NovaAccent),
                        )
                    },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .background(NovaBackground)
                            .testTag(ContentTag),
                    )
                }
            }
        }

        composeRule.onNodeWithTag(ConversationChromeTags.Header).assertIsDisplayed()
        composeRule.onNodeWithTag(ContentTag).assertIsDisplayed()
        composeRule.onNodeWithTag(ConversationChromeTags.Composer).assertIsDisplayed()

        val header = bounds(ConversationChromeTags.Header)
        val content = bounds(ContentTag)
        val composer = bounds(ConversationChromeTags.Composer)

        assertTrue("content must begin below the header", header.bottom <= content.top)
        assertTrue("content must end above the composer", content.bottom <= composer.top)
        assertTrue("the resized viewport must retain message space", content.height > 0f)
    }

    private fun bounds(tag: String): Rect =
        composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private companion object {
        const val ContentTag = "conversation_content"
    }
}
