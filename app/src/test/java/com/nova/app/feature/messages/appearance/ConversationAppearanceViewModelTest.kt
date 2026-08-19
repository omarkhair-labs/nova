package com.nova.app.feature.messages.appearance

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.appearance.data.ConversationAppearanceRepository
import com.nova.app.feature.messages.appearance.model.ConversationPreference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


class ConversationAppearanceViewModelTest {
    @Test
    fun initialPreferenceUsesTheUiThemeResolver() = runBlocking {
        val fake = FakeConversationAppearanceRepository().apply {
            preferenceResult = ApiResult.Success(ConversationPreference(false, "unsupported"))
        }
        val viewModel = viewModel(fake, this)
        yield()

        assertEquals("nova", viewModel.state.themeKey)
        assertNull(viewModel.state.errorMessage)
        assertEquals(listOf(CONVERSATION_ID), fake.preferenceCalls)
    }

    @Test
    fun pickerCannotDismissWhileOptimisticThemeSaveIsInFlight() = runBlocking {
        val save = CompletableDeferred<ApiResult<ConversationPreference>>()
        val fake = FakeConversationAppearanceRepository().apply {
            setThemeBlock = { _, _ -> save.await() }
        }
        val viewModel = viewModel(fake, this)
        yield()

        viewModel.openPicker()
        viewModel.selectTheme("rose")
        yield()

        assertTrue(viewModel.state.pickerOpen)
        assertEquals("rose", viewModel.state.themeKey)
        assertEquals("rose", viewModel.state.savingThemeKey)
        viewModel.dismissPicker()
        assertTrue(viewModel.state.pickerOpen)

        save.complete(ApiResult.Success(ConversationPreference(false, "midnight")))
        yield()

        assertEquals("midnight", viewModel.state.themeKey)
        assertNull(viewModel.state.savingThemeKey)
        viewModel.dismissPicker()
        assertFalse(viewModel.state.pickerOpen)
    }

    @Test
    fun failedThemeSaveRollsBackAndShowsTheExistingInlineError() = runBlocking {
        val fake = FakeConversationAppearanceRepository().apply {
            setThemeResult = ApiResult.Failure("could not save", 500)
        }
        val viewModel = viewModel(fake, this)
        yield()

        viewModel.selectTheme("ocean")
        yield()

        assertEquals("nova", viewModel.state.themeKey)
        assertEquals("could not save", viewModel.state.errorMessage)
        assertNull(viewModel.state.savingThemeKey)
    }

    @Test
    fun terminal401ProducesSessionEffectWithoutInlineError() = runBlocking {
        val fake = FakeConversationAppearanceRepository().apply {
            preferenceResult = ApiResult.Failure("expired", 401)
        }
        val viewModel = viewModel(fake, this)
        yield()

        assertEquals(1, viewModel.state.sessionExpiryVersion)
        assertNull(viewModel.state.errorMessage)
    }

    @Test
    fun selectingCurrentThemeDoesNotWrite() = runBlocking {
        val fake = FakeConversationAppearanceRepository()
        val viewModel = viewModel(fake, this)
        yield()

        viewModel.selectTheme("nova")
        yield()

        assertTrue(fake.setThemeCalls.isEmpty())
    }

    private fun viewModel(
        repository: ConversationAppearanceRepository,
        scope: CoroutineScope,
    ) = ConversationAppearanceViewModel(
        conversationId = CONVERSATION_ID,
        repository = repository,
        resolveThemeKey = { key ->
            key?.trim()?.lowercase()?.takeIf { it in setOf("nova", "midnight", "aurora", "ocean", "rose", "ember") }
                ?: "nova"
        },
        workScope = scope,
    )

    private class FakeConversationAppearanceRepository : ConversationAppearanceRepository {
        var preferenceResult: ApiResult<ConversationPreference> =
            ApiResult.Success(ConversationPreference(false, "nova"))
        var setThemeResult: ApiResult<ConversationPreference> =
            ApiResult.Success(ConversationPreference(false, "nova"))
        var setThemeBlock: (suspend (Long, String) -> ApiResult<ConversationPreference>)? = null

        val preferenceCalls = mutableListOf<Long>()
        val setThemeCalls = mutableListOf<ThemeCall>()

        override suspend fun preference(conversationId: Long): ApiResult<ConversationPreference> {
            preferenceCalls += conversationId
            return preferenceResult
        }

        override suspend fun setTheme(
            conversationId: Long,
            themeKey: String,
        ): ApiResult<ConversationPreference> {
            setThemeCalls += ThemeCall(conversationId, themeKey)
            return setThemeBlock?.invoke(conversationId, themeKey) ?: setThemeResult
        }
    }

    private data class ThemeCall(val conversationId: Long, val themeKey: String)

    private companion object {
        const val CONVERSATION_ID = 42L
    }
}
