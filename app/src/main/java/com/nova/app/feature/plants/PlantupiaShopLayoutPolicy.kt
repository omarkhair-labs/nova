package com.nova.app.feature.plants

/** After the 20dp inset; large text or narrow phones use readable full-width cards. */
internal fun plantupiaShopProductColumns(innerWidthDp: Float, fontScale: Float): Int =
    if (innerWidthDp < 330f || fontScale >= 1.3f) 1 else 2

/** Visible family groups are intentionally incremental, without hiding older SKUs. */
internal fun plantupiaVisibleFamilies(total: Int, requested: Int): Int =
    requested.coerceAtLeast(8).coerceAtMost(total.coerceAtLeast(0))

/** A narrow viewport or accessibility text scale gets a two-level, unclipped Shop header. */
internal fun plantupiaShopNavigationStacked(innerWidthDp: Float, fontScale: Float): Boolean =
    innerWidthDp < 330f || fontScale >= 1.35f

/**
 * On narrow phones the hero must not push product name, variant and action below
 * a second screen. This is the real *inner* content width after horizontal insets.
 */
internal fun plantupiaShopHeroHeightDp(innerWidthDp: Float, fontScale: Float): Int =
    when {
        fontScale >= 1.5f -> 204
        innerWidthDp < 330f -> 216
        innerWidthDp < 480f -> 244
        else -> 292
    }

/** Catalog photos should have calm, useful ratios, not fixed 174dp on every size. */
internal fun plantupiaShopTilePhotoHeightDp(columns: Int, innerWidthDp: Float): Int =
    if (columns <= 1) {
        if (innerWidthDp < 350f) 192 else 224
    } else {
        ((innerWidthDp - 12f) / 2f * 0.89f).toInt().coerceIn(125, 180)
    }

/** Flow pages may use a shorter heading while the Collection remains editorial. */
internal fun plantupiaShopIsFlowPage(screen: String): Boolean =
    screen in setOf("product", "cart", "checkout", "orders")
