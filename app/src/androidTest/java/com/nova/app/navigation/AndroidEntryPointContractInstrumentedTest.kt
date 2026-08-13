package com.nova.app.navigation

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nova.app.MainActivity
import com.nova.app.MessagesActivity
import com.nova.app.ReelsActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidEntryPointContractInstrumentedTest {
    private val targetContext: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun launcherAndSpecialEntriesPreserveExportPolicy() {
        assertTrue(activityInfo(MainActivity::class.java).exported)
        assertFalse(activityInfo(MessagesActivity::class.java).exported)
        assertFalse(activityInfo(ReelsActivity::class.java).exported)
    }

    @Test
    fun allMessageAndReelHostWindowsUseAdjustResize() {
        listOf(
            MainActivity::class.java,
            MessagesActivity::class.java,
            ReelsActivity::class.java,
        ).forEach { activityClass ->
            val adjustMode = activityInfo(activityClass).softInputMode and
                WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST
            assertEquals(
                "${activityClass.simpleName} must preserve adjustResize",
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
                adjustMode,
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun activityInfo(activityClass: Class<out Activity>): ActivityInfo =
        targetContext.packageManager.getActivityInfo(
            ComponentName(targetContext, activityClass),
            0,
        )
}
