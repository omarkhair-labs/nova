package com.nova.app.core.reels

import android.content.Context
import android.content.Intent
import com.nova.app.ReelsActivity


object NovaReelsNavigator {
    fun open(context: Context) {
        context.startActivity(Intent(context, ReelsActivity::class.java))
    }
}
