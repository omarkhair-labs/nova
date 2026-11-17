package com.nova.app.feature.plants

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * This collection contains only what the *commercial* catalog can truthfully
 * classify today. Legacy QA care subcategories require separate reviewed
 * CommerceProduct metadata before they can return as filters.
 */
internal data class ShopDiscoveryCategory(
    val id: String, val name: String, val index: String,
)

internal val shopDiscoveryCategories = listOf(
    ShopDiscoveryCategory("all", "All", "01"),
    ShopDiscoveryCategory("plant", "Plants", "02"),
    ShopDiscoveryCategory("planter", "Pots", "03"),
    ShopDiscoveryCategory("care", "Care", "04"),
)

/** One counted product family, never one count per purchasable variant. */
internal fun shopDiscoveryCategoryCount(kinds: List<String>, selected: String): Int =
    if (selected == "all") kinds.size else kinds.count { it == selected }

/** The count is a user-owned cart quantity, not reserved warehouse stock. */
internal fun shopCartButtonLabel(quantity: Int?): String =
    if (quantity == null) "Cart" else "Cart · ${quantity.coerceAtLeast(0)}"

@Composable
private fun ShopCartDestination(
    selected: Boolean,
    quantity: Int?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) PlantupiaColors.forest else PlantupiaColors.softSage,
    ) {
        Box(
            Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = quantity, animationSpec = tween(190),
                label = "saved-cart-quantity") { storedQuantity ->
                Text(
                    shopCartButtonLabel(storedQuantity),
                    style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selected) PlantupiaColors.paper else PlantupiaColors.forest,
                )
            }
        }
    }
}

/** The one Shop header stays legible at 320dp and 200% font sizes. */
@Composable
internal fun PlantupiaShopNavigation(
    screen: String,
    cartQuantity: Int?,
    enabled: Boolean,
    onCollection: () -> Unit,
    onOrders: () -> Unit,
    onCart: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = plantupiaShopNavigationStacked(maxWidth.value, LocalDensity.current.fontScale)
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onCollection, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(if (screen == "catalog") "Collection" else "← Collection", color = PlantupiaColors.forest)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onOrders, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Orders", color = PlantupiaColors.forest)
                    }
                }
                ShopCartDestination(
                    selected = screen == "cart" || screen == "checkout",
                    quantity = cartQuantity, enabled = enabled,
                    modifier = Modifier.fillMaxWidth(), onClick = onCart,
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCollection, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (screen == "catalog") "Collection" else "← Collection", color = PlantupiaColors.forest)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onOrders, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Orders", color = PlantupiaColors.forest)
                }
                ShopCartDestination(
                    selected = screen == "cart" || screen == "checkout",
                    quantity = cartQuantity, enabled = enabled, onClick = onCart,
                )
            }
        }
    }
}

@Composable
internal fun PlantupiaShopCategoryRail(
    current: String,
    productKinds: List<String>,
    enabled: Boolean,
    onChoose: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        shopDiscoveryCategories.forEach { category ->
            val active = category.id == current
            val count = shopDiscoveryCategoryCount(productKinds, category.id)
            val surfaceColor by animateColorAsState(
                if (active) PlantupiaColors.forest else PlantupiaColors.paper,
                animationSpec = tween(180), label = "collection-category",
            )
            Surface(
                modifier = Modifier.heightIn(min = 50.dp)
                    .semantics { selected = active },
                enabled = enabled,
                onClick = { onChoose(category.id) },
                color = surfaceColor,
                shape = RoundedCornerShape(11.dp),
                border = BorderStroke(1.dp, if (active) PlantupiaColors.forest else PlantupiaColors.border),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        category.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        color = if (active) PlantupiaColors.paper else PlantupiaColors.forestText,
                    )
                    Text(
                        count.toString(), fontSize = 11.sp,
                        color = if (active) PlantupiaColors.paper.copy(alpha = .8f)
                            else PlantupiaColors.secondaryText,
                    )
                }
            }
        }
    }
}

@Composable
internal fun PlantupiaShopObjectAwaitingPhoto(kind: String, height: androidx.compose.ui.unit.Dp = 174.dp) {
    val category = when(kind) {
        "plant" -> "PLANT STUDY"
        "planter" -> "VESSEL STUDY"
        "care" -> "CARE OBJECT"
        else -> "OBJECT STUDY"
    }
    val compact = height <= 120.dp
    Box(
        modifier = Modifier.fillMaxWidth().height(height)
            .background(PlantupiaColors.softSage),
    ) {
        // Architectural registration lines: NOT an image of the product.
        Canvas(Modifier.fillMaxSize()) {
            val ink = Color(0xFF8EA492)
            val mid = size.width * .5f
            drawLine(ink, Offset(mid, 16f), Offset(mid, size.height - 16f), 1f)
            drawLine(ink, Offset(16f, size.height * .57f), Offset(size.width - 16f, size.height * .57f), 1f)
            drawLine(ink, Offset(20f, 20f), Offset(20f, size.height - 20f), 1f)
            drawLine(ink, Offset(size.width - 20f, 20f), Offset(size.width - 20f, size.height - 20f), 1f)
        }
        Column(
            Modifier.align(Alignment.BottomStart).padding(if (compact) 8.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 1.dp else 4.dp),
        ) {
            if (!compact) {
                Text(category, fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp, color = PlantupiaColors.secondaryText)
            }
            Text(
                if (compact) "PHOTO PENDING" else "Product photo pending",
                fontFamily = if (compact) FontFamily.Monospace else FontFamily.Default,
                fontSize = if (compact) 9.sp else 12.sp,
                color = PlantupiaColors.forestText,
            )
        }
    }
}
