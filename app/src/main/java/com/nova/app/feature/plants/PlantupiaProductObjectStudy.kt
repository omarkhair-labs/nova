package com.nova.app.feature.plants

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nova.app.feature.plants.data.PlantupiaRepository
import org.json.JSONObject

/** Product-level photographs and exact commercial SKU decisions never get conflated. */
@Composable
internal fun StoreProductDetails(
    productId: String,
    variants: List<JSONObject>,
    cart: JSONObject?,
    repository: PlantupiaRepository,
    busy: Boolean,
    onAdd: (String, Int) -> Unit,
    adding: Boolean = false,
    addErrorForSku: Pair<String, String>? = null,
    onViewCart: () -> Unit = {},
) {
    val product = variants.firstOrNull() ?: return
    var selectedSku by rememberSaveable(productId) {
        mutableStateOf((variants.firstOrNull {
            it.optBoolean("in_stock") && it.optInt("available_units") > 0
        } ?: product).optString("id"))
    }
    val selected = shopSelectVariant(variants, selectedSku) ?: return
    val id = selected.optString("id")
    val available = selected.optInt("available_units").coerceAtLeast(0)
    val inCart = cart?.demoRows()?.firstOrNull {
        it.optJSONObject("product")?.optString("id") == id
    }?.optInt("quantity") ?: 0
    val photos = remember(productId, product.toString()) { shopProductPhotos(product) }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 660.dp && LocalDensity.current.fontScale < 1.3f
        val heroHeight = plantupiaShopHeroHeightDp(maxWidth.value,
            LocalDensity.current.fontScale).dp
        if (wide) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Box(Modifier.weight(1.05f)) {
                    ShopObjectGallery(productId, photos, repository, product.optString("kind"),
                        heroHeight = heroHeight)
                }
                Box(Modifier.weight(.95f)) {
                    ShopObjectDecision(product, variants, id, available, inCart, cart != null,
                        busy, onVariant = { selectedSku = it }, onAdd = onAdd,
                        adding = adding && addErrorForSku == null,
                        addError = addErrorForSku?.takeIf { it.first == id }?.second,
                        onViewCart = onViewCart)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // The name and real selected price precede a large image on phones.
                // This fixes the first-screen viewport without inventing a mini cart.
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(product.optString("name"),
                        style = PlantupiaText.editorialHeading)
                    Text("EGP " + selected.optString("price"),
                        style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
                }
                ShopObjectGallery(productId, photos, repository, product.optString("kind"),
                    heroHeight = heroHeight)
                ShopObjectDecision(product, variants, id, available, inCart, cart != null,
                    busy, onVariant = { selectedSku = it }, onAdd = onAdd,
                    adding = adding && addErrorForSku == null,
                    addError = addErrorForSku?.takeIf { it.first == id }?.second,
                    onViewCart = onViewCart, showIdentity = false)
            }
        }
    }
}

@Composable
private fun ShopObjectGallery(
    productId: String,
    photos: List<ShopProductPhoto>,
    repository: PlantupiaRepository,
    kind: String,
    heroHeight: androidx.compose.ui.unit.Dp,
) {
    var chosenPhoto by rememberSaveable(productId) { mutableIntStateOf(0) }
    val index = chosenPhoto.coerceIn(0, (photos.size - 1).coerceAtLeast(0))
    var inspecting by rememberSaveable(productId) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(
            Modifier.fillMaxWidth().height(heroHeight)
                .background(PlantupiaColors.softSage)
                .then(if (photos.size > 1) Modifier.pointerInput(productId, photos.size, index) {
                    var dragged = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragged = 0f },
                        onHorizontalDrag = { change, distance -> dragged += distance; change.consume() },
                        onDragEnd = { chosenPhoto = shopPhotoIndexAfterSwipe(index, photos.size, dragged) },
                        onDragCancel = { dragged = 0f },
                    )
                } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (photos.isEmpty()) {
                PlantupiaShopObjectAwaitingPhoto(kind, height = heroHeight)
            } else {
                Crossfade(targetState = index, animationSpec = tween(180), label = "product-photo") { photoIndex ->
                    val photo = photos[photoIndex]
                    PlantupiaCatalogPhoto(
                        productId, repository, photoId = photo.id,
                        photoAlt = photo.description, catalogPath = "commerce/catalog",
                        fullObject = true, displayHeight = heroHeight,
                    )
                }
                Text(
                    "PHOTO " + (index + 1) + " / " + photos.size,
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                    style = PlantupiaText.sectionLabel,
                )
                TextButton(
                    onClick = { inspecting = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp).heightIn(min = 48.dp),
                ) { Text("Inspect photo  ↗", color = PlantupiaColors.forest) }
            }
        }
        if (photos.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 3.dp)) {
                itemsIndexed(photos, key = { n, photo -> photo.id ?: "cover-" + n }) { n, photo ->
                    Surface(
                        onClick = { chosenPhoto = n },
                        modifier = Modifier.width(88.dp).height(96.dp).semantics {
                            selected = index == n
                            contentDescription = "View product photo " + (n + 1) + " of " + photos.size
                        },
                        color = PlantupiaColors.paper,
                        border = BorderStroke(
                            if (index == n) 2.dp else 1.dp,
                            if (index == n) PlantupiaColors.forest else PlantupiaColors.border),
                        shape = RoundedCornerShape(9.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PlantupiaCatalogPhoto(
                                productId, repository, thumbnail = true,
                                photoId = photo.id, photoAlt = photo.description,
                                catalogPath = "commerce/catalog",
                                fullObject = true, displayHeight = 84.dp,
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    enabled = index > 0,
                    onClick = { chosenPhoto = (index - 1).coerceAtLeast(0) },
                ) { Text("← Previous", color = PlantupiaColors.forest) }
                TextButton(
                    enabled = index < photos.lastIndex,
                    onClick = { chosenPhoto = (index + 1).coerceAtMost(photos.lastIndex) },
                ) { Text("Next →", color = PlantupiaColors.forest) }
            }
        }
        if (photos.size > 1) {
            Text(photos.size.toString() + " actual catalogue views",
                style = PlantupiaText.supporting)
        }
    }

    if (inspecting && photos.isNotEmpty()) {
        Dialog(
            onDismissRequest = { inspecting = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            var zoom by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            LaunchedEffect(index) { zoom = 1f; offset = Offset.Zero }
            Surface(
                Modifier.fillMaxWidth().fillMaxHeight(.94f),
                shape = RoundedCornerShape(14.dp), color = PlantupiaColors.paper,
            ) {
                Column(Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("OBJECT / PHOTO " + (index + 1) + " OF " + photos.size,
                            style = PlantupiaText.sectionLabel)
                        TextButton(onClick = { inspecting = false }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Close ×", color = PlantupiaColors.forest)
                        }
                    }
                    BoxWithConstraints(
                        Modifier.fillMaxWidth().weight(1f).clipToBounds()
                            .background(PlantupiaColors.softSage)
                            .pointerInput(index) {
                                detectTransformGestures { _, pan, scaleChange, _ ->
                                    zoom = (zoom * scaleChange).coerceIn(1f, 3.5f)
                                    val maxX = size.width * (zoom - 1f) / 2f
                                    val maxY = size.height * (zoom - 1f) / 2f
                                    offset = if (zoom == 1f) Offset.Zero else Offset(
                                        (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        (offset.y + pan.y).coerceIn(-maxY, maxY),
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        val height = maxHeight
                        Box(Modifier.fillMaxSize().graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = offset.x
                            translationY = offset.y
                        }, contentAlignment = Alignment.Center) {
                            val photo = photos[index]
                            PlantupiaCatalogPhoto(productId, repository, photoId = photo.id,
                                photoAlt = photo.description, catalogPath = "commerce/catalog",
                                fullObject = true, displayHeight = height)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            enabled = zoom > 1f,
                            onClick = { zoom = (zoom - .5f).coerceAtLeast(1f); if (zoom == 1f) offset = Offset.Zero },
                        ) { Text("Zoom −", color = PlantupiaColors.forest) }
                        TextButton(
                            enabled = zoom < 3.5f,
                            onClick = { zoom = (zoom + .5f).coerceAtMost(3.5f) },
                        ) { Text("Zoom +", color = PlantupiaColors.forest) }
                        if (photos.size > 1) {
                            TextButton(onClick = { chosenPhoto = (index + 1) % photos.size }) {
                                Text("Next →", color = PlantupiaColors.forest)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopObjectDecision(
    product: JSONObject,
    variants: List<JSONObject>,
    selectedId: String,
    available: Int,
    inCart: Int,
    cartLoaded: Boolean,
    busy: Boolean,
    onVariant: (String) -> Unit,
    onAdd: (String, Int) -> Unit,
    adding: Boolean,
    addError: String?,
    onViewCart: () -> Unit,
    showIdentity: Boolean = true,
) {
    val selected = variants.firstOrNull { it.optString("id") == selectedId } ?: return
    val kind = product.optString("kind")
    val family = when(kind) {
        "plant" -> "LIVING OBJECT"
        "planter" -> "THE VESSEL"
        "care" -> "CARE ESSENTIAL"
        else -> "OBJECT STUDY"
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (showIdentity) {
            Text("PLANTUPIA / " + family, style = PlantupiaText.sectionLabel)
            Text(product.optString("name"), style = PlantupiaText.pageHeading)
        }
        product.optString("description").takeIf { it.isNotBlank() }?.let {
            Text(it, style = PlantupiaText.body)
        }
        val species = product.optJSONObject("reference_species")?.optString("scientific_name").orEmpty()
        if (kind == "plant" && species.isNotBlank()) {
            Text("Species reference · " + species, style = PlantupiaText.supporting)
            Text("A reference only, not a verified identity for an individual delivered plant.",
                style = PlantupiaText.supporting)
        }
        HorizontalDivider(color = PlantupiaColors.border)
        if (variants.size > 1) {
            Text("CHOOSE YOUR OPTION", style = PlantupiaText.sectionLabel)
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                variants.forEach { option ->
                    val optionId = option.optString("id")
                    val chosen = selectedId == optionId
                    val units = option.optInt("available_units").coerceAtLeast(0)
                    Surface(
                        modifier = Modifier.fillMaxWidth().selectable(
                            selected = chosen, role = Role.RadioButton,
                            enabled = !busy, onClick = { onVariant(optionId) },
                        ),
                        shape = RoundedCornerShape(12.dp),
                        color = if (chosen) PlantupiaColors.softSage else PlantupiaColors.paper,
                        border = BorderStroke(
                            if (chosen) 2.dp else 1.dp,
                            if (chosen) PlantupiaColors.forest else PlantupiaColors.border),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RadioButton(selected = chosen, onClick = null)
                            Column(Modifier.weight(1f)) {
                                Text(storeVariantName(option), style = PlantupiaText.body,
                                    fontWeight = FontWeight.SemiBold)
                                Text(shopStockDescription(units, option.optBoolean("in_stock")),
                                    style = PlantupiaText.supporting)
                            }
                            Text("EGP " + option.optString("price"), style = PlantupiaText.body)
                        }
                    }
                }
            }
            Text("Photos show this product family, not necessarily the chosen option.",
                style = PlantupiaText.supporting)
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("YOUR SELECTION", style = PlantupiaText.sectionLabel)
            Text(storeVariantName(selected), style = PlantupiaText.body,
                fontWeight = FontWeight.SemiBold)
            if (showIdentity) {
                Text("EGP " + selected.optString("price"),
                    modifier = Modifier.semantics { contentDescription = "Selected price EGP " + selected.optString("price") },
                    style = PlantupiaText.editorialHeading)
            }
            Text(shopStockDescription(available, selected.optBoolean("in_stock")),
                style = PlantupiaText.supporting)
            selected.optString("sku").takeIf { it.isNotBlank() }?.let {
                Text("SKU / " + it, style = PlantupiaText.sectionLabel)
            }
        }
        Button(
            onClick = { onAdd(selectedId, inCart + 1) },
            enabled = shopDetailCanAdd(cartLoaded, busy, available,
                selected.optBoolean("in_stock"), inCart),
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest),
        ) {
            if (adding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                    color = PlantupiaColors.paper,
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                when {
                    adding -> "Saving your selection…"
                    inCart == 0 -> "Add to cart"
                    else -> "Add one more · " + inCart + " in cart"
                },
            )
        }
        if (addError != null) {
            Column(
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(addError, style = PlantupiaText.supporting,
                    color = PlantupiaColors.errorText)
                TextButton(onClick = onViewCart) {
                    Text("Check saved cart  ↗", color = PlantupiaColors.forest)
                }
            }
        }
        if (!cartLoaded) {
            Text("The cart is unavailable right now. Return to Collection to retry.",
                style = PlantupiaText.supporting)
        } else if (inCart >= 10) {
            Text("Maximum 10 of this option per cart.", style = PlantupiaText.supporting)
        }
        Text("Your cart does not reserve stock. Delivery is calculated at checkout.",
            style = PlantupiaText.supporting)
    }
}
