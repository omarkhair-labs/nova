package com.nova.app.feature.plants

/** After the 20dp inset; large text or narrow phones use readable full-width cards. */
internal fun plantupiaShopProductColumns(innerWidthDp: Float, fontScale: Float): Int =
    if (innerWidthDp < 330f || fontScale >= 1.3f) 1 else 2

/** Visible family groups are intentionally incremental, without hiding older SKUs. */
internal fun plantupiaVisibleFamilies(total: Int, requested: Int): Int =
    requested.coerceAtLeast(8).coerceAtMost(total.coerceAtLeast(0))
