package com.nova.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.unit.dp
import com.nova.app.ui.theme.NovaTheme
import org.junit.Rule
import org.junit.Test

class PlantupiaPortraitInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun everyOriginalAvatarLoadsAtMicroAndHeroSizesWithoutCrashing() {
        compose.setContent {
            NovaTheme {
                Box {
                    for (variant in 0..7) {
                        PlantupiaAvatar(variant + 1L, "Synthetic portrait $variant hero", "", 96.dp, chosenVariant = variant)
                        PlantupiaAvatar(variant + 1L, "Synthetic portrait $variant micro", "", 32.dp, chosenVariant = variant)
                    }
                }
            }
        }
        compose.onAllNodes(hasContentDescription("Profile illustration for Synthetic portrait", substring = true))
            .assertCountEquals(16)
    }
}
