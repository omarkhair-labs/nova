package com.nova.app.core.update

import org.junit.Assert.assertEquals
import org.junit.Test

class NovaUpdatePolicyTest {
    @Test
    fun highPriorityPrefersImmediateWhenAllowed() {
        assertEquals(
            NovaUpdateMode.Immediate,
            NovaUpdatePolicy.chooseMode(
                updatePriority = 5,
                flexibleAllowed = true,
                immediateAllowed = true,
            ),
        )
    }

    @Test
    fun normalPriorityUsesFlexibleWhenAllowed() {
        assertEquals(
            NovaUpdateMode.Flexible,
            NovaUpdatePolicy.chooseMode(
                updatePriority = 2,
                flexibleAllowed = true,
                immediateAllowed = true,
            ),
        )
    }

    @Test
    fun immediateIsFallbackWhenFlexibleIsUnavailable() {
        assertEquals(
            NovaUpdateMode.Immediate,
            NovaUpdatePolicy.chooseMode(
                updatePriority = 1,
                flexibleAllowed = false,
                immediateAllowed = true,
            ),
        )
    }

    @Test
    fun noAllowedFlowReturnsNone() {
        assertEquals(
            NovaUpdateMode.None,
            NovaUpdatePolicy.chooseMode(
                updatePriority = 5,
                flexibleAllowed = false,
                immediateAllowed = false,
            ),
        )
    }

    @Test
    fun immediatePromptsSoonerThanFlexible() {
        val immediate = NovaUpdatePolicy.promptCooldownMs(NovaUpdateMode.Immediate)
        val flexible = NovaUpdatePolicy.promptCooldownMs(NovaUpdateMode.Flexible)
        assert(immediate < flexible)
    }
}
