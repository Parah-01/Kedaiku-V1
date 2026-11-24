package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CategoryEntity
import com.example.data.local.CustomizationGroup
import com.example.data.local.CustomizationOptionEntity
import com.example.data.local.CustomizationOptionType
import com.example.data.local.InventoryItemEntity
import com.example.data.local.InventoryItemWithCategories
import com.example.data.local.OrderType
import com.example.data.local.PaymentMethod
import com.example.data.prefs.StoreProfile
import com.example.ui.CartLineItem
import com.example.ui.DraftEditContext
import com.example.ui.components.CustomizationOptionsManagerDialog
import com.example.ui.theme.GoldenYellowTextAccent
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.theme.ThemeMode
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    menuItems: List<InventoryItemWithCategories>,
    categories: List<CategoryEntity>,
    customizationOptions: List<CustomizationOptionEntity>,
    cartLines: List<CartLineItem>,
    draftEditContext: DraftEditContext?,
    storeProfile: StoreProfile,
    isOwner: Boolean,
    themeMode: ThemeMode,
    onAddCustomizedLine: (item: InventoryItemEntity, qty: Int, unitPrice: Long, optionsSummary: String) -> Unit,
    onUpdateCartLineQty: (lineKey: String, delta: Int) -> Unit,
    onClearCart: () -> Unit,
    onCancelDraftEdit: () -> Unit,
    onSaveCustomizationOption: (CustomizationOptionEntity) -> Unit,
    onDeleteCustomizationOption: (CustomizationOptionEntity) -> Unit,
    onSubmitOrder: (
        customerName: String,
        orderType: OrderType,
        paymentMethod: PaymentMethod,
        discountAmount: Long,
        paidAmount: Long,
        notes: String,
        saveAsDraft: Boolean
    ) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("Semua") }
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var customizingItem by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var showCustomPriceEditor by remember { mutableStateOf(false) }
    var customPriceEditorInitialType by remember { mutableStateOf(CustomizationOptionType.DRINK_VARIANT) }

    val isDark = themeMode == ThemeMode.DARK || (themeMode == ThemeMode.SYSTEM && isSystemInDarkTheme())
    val priceTextColor = if (isDark) MaterialTheme.colorScheme.primary else GoldenYellowTextAccent

    val categoryLabels = remember(categories, menuItems) {
        val fromDb = categories.map { it.name }
        val fromItems = menuItems.flatMap { it.categoryNames }
        listOf("Semua") + (fromDb + fromItems).distinct().filter { it != "Bahan Baku" }
    }

    val filteredMenu = remember(menuItems, searchQuery, selectedCategory) {
        menuItems.filter { entry ->
            val item = entry.item
            val matchCategory = selectedCategory == "Semua" ||
                entry.categoryNames.any { it.equals(selectedCategory, ignoreCase = true) }
            val matchQuery = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.sku.contains(searchQuery, ignoreCase = true)
            matchCategory && matchQuery
        }
    }

    val totalCartCount = cartLines.sumOf { it.quantity }
    val cartSubtotal = cartLines.sumOf { it.lineTotal }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Draft Editing Banner
            if (draftEditContext != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("draft_edit_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null)
                            Column {
                                Text(
                                    "Mengedit Draf ${draftEditContext.orderNumber}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Ubah menu atau varian, lalu simpan draf atau bayar.",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        IconButton(
                            onClick = onCancelDraftEdit,
                            modifier = Modifier.testTag("cancel_draft_edit_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Batal edit draf")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari menu kopi, makanan, atau SKU...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("pos_search_input")
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categoryLabels) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("pos_category_$category")
                    )
                }
            }

            if (filteredMenu.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Coffee,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Menu tidak ditemukan", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tambahkan menu di tab Stok atau ubah kata kunci pencarian.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    val columns = when {
                        maxWidth >= 840.dp -> 3
                        maxWidth >= 560.dp -> 2
                        else -> 1
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 6.dp,
                            bottom = if (totalCartCount > 0) 96.dp else 16.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredMenu, key = { it.item.id }) { entry ->
                            val item = entry.item
                            val linesForItem = cartLines.filter { it.itemId == item.id }
                            val qtyInCart = linesForItem.sumOf { it.quantity }
                            PosMenuItemCard(
                                entry = entry,
                                qtyInCart = qtyInCart,
                                priceTextColor = priceTextColor,
                                onAddClick = {
                                    if (item.customizationGroup == CustomizationGroup.DRINK.name ||
                                        item.customizationGroup == CustomizationGroup.FOOD.name
                                    ) {
                                        customizingItem = item
                                    } else {
                                        onAddCustomizedLine(item, 1, item.price, "")
                                    }
                                },
                                onDecreaseSimple = {
                                    linesForItem.lastOrNull()?.let { lastLine ->
                                        onUpdateCartLineQty(lastLine.lineKey, -1)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Sticky Golden Yellow (#FFC000) Cart Bar
        AnimatedVisibility(
            visible = totalCartCount > 0,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pos_cart_summary_bar")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (draftEditContext != null) {
                                "$totalCartCount item • Edit ${draftEditContext.orderNumber}"
                            } else {
                                "$totalCartCount item di keranjang"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = Formatters.formatRupiah(cartSubtotal),
                            style = MonoPriceStyle.copy(color = MaterialTheme.colorScheme.onPrimary)
                        )
                    }
                    IconButton(
                        onClick = onClearCart,
                        modifier = Modifier.testTag("pos_clear_cart_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Kosongkan keranjang",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Button(
                        onClick = { showCheckoutSheet = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("pos_checkout_button")
                    ) {
                        Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bayar / Draf")
                    }
                }
            }
        }
    }

    // Customization Sheet for Drink (Variant + Toppings) or Food (Level 1-5)
    customizingItem?.let { item ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { customizingItem = null },
            sheetState = sheetState
        ) {
            ItemCustomizationSheetContent(
                item = item,
                customizationOptions = customizationOptions,
                isOwner = isOwner,
                priceTextColor = priceTextColor,
                onOpenPriceEditor = { initialType ->
                    customPriceEditorInitialType = initialType
                    showCustomPriceEditor = true
                },
                onDismiss = { customizingItem = null },
                onConfirm = { qty, finalUnitPrice, summary ->
                    onAddCustomizedLine(item, qty, finalUnitPrice, summary)
                    customizingItem = null
                }
            )
        }
    }

    if (showCustomPriceEditor && isOwner) {
        CustomizationOptionsManagerDialog(
            options = customizationOptions,
            initialType = customPriceEditorInitialType,
            onDismiss = { showCustomPriceEditor = false },
            onSaveOption = onSaveCustomizationOption,
            onDeleteOption = onDeleteCustomizationOption
        )
    }

    // Checkout & Draft Bottom Sheet
    if (showCheckoutSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCheckoutSheet = false },
            sheetState = sheetState
        ) {
            CheckoutBottomSheetContent(
                cartLines = cartLines,
                draftEditContext = draftEditContext,
                subtotal = cartSubtotal,
                storeProfile = storeProfile,
                priceTextColor = priceTextColor,
                onUpdateLineQty = onUpdateCartLineQty,
                onDismiss = { showCheckoutSheet = false },
                onConfirmOrder = { customer, type, method, discount, paid, notes, saveAsDraft ->
                    showCheckoutSheet = false
                    onSubmitOrder(customer, type, method, discount, paid, notes, saveAsDraft)
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PosMenuItemCard(
    entry: InventoryItemWithCategories,
    qtyInCart: Int,
    priceTextColor: androidx.compose.ui.graphics.Color,
    onAddClick: () -> Unit,
    onDecreaseSimple: () -> Unit
) {
    val item = entry.item
    val isOut = item.isOutOfStock
    val isLow = item.isLowStock
    val hasCustomOptions = item.customizationGroup == CustomizationGroup.DRINK.name ||
        item.customizationGroup == CustomizationGroup.FOOD.name

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (qtyInCart > 0) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pos_menu_card_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    entry.categoryNames.forEach { catName ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Text(
                                text = catName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isOut -> MaterialTheme.colorScheme.errorContainer
                            isLow -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Text(
                            text = if (isOut) {
                                "Habis"
                            } else {
                                "Stok ${Formatters.formatQty(item.stockQty)} ${item.unit}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                isOut -> MaterialTheme.colorScheme.onErrorContainer
                                isLow -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = Formatters.formatRupiah(item.price),
                        style = MonoPriceStyle,
                        color = priceTextColor
                    )
                    if (hasCustomOptions) {
                        Text(
                            text = if (item.customizationGroup == CustomizationGroup.DRINK.name) {
                                "• Varian & Toping"
                            } else {
                                "• Level 1-5"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (qtyInCart == 0) {
                Button(
                    onClick = onAddClick,
                    enabled = !isOut,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("pos_add_item_${item.id}")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isOut) "Habis" else "Tambah")
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = onDecreaseSimple,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("pos_dec_item_${item.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Remove, contentDescription = "Kurangi")
                        }
                    }
                    Text(
                        text = qtyInCart.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Surface(
                        onClick = onAddClick,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("pos_inc_item_${item.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemCustomizationSheetContent(
    item: InventoryItemEntity,
    customizationOptions: List<CustomizationOptionEntity>,
    isOwner: Boolean,
    priceTextColor: androidx.compose.ui.graphics.Color,
    onOpenPriceEditor: (CustomizationOptionType) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (qty: Int, unitPrice: Long, optionsSummary: String) -> Unit
) {
    val isDrink = item.customizationGroup == CustomizationGroup.DRINK.name

    val drinkVariants = remember(customizationOptions) {
        customizationOptions
            .filter { it.optionType == CustomizationOptionType.DRINK_VARIANT.name }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .ifEmpty {
                listOf(
                    CustomizationOptionEntity(
                        id = -1L,
                        optionType = CustomizationOptionType.DRINK_VARIANT.name,
                        label = "Regular",
                        extraPrice = 0L
                    )
                )
            }
    }
    val drinkToppings = remember(customizationOptions) {
        customizationOptions
            .filter { it.optionType == CustomizationOptionType.DRINK_TOPPING.name }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
    }
    val foodLevels = remember(customizationOptions) {
        customizationOptions
            .filter { it.optionType == CustomizationOptionType.FOOD_LEVEL.name }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .ifEmpty {
                listOf(
                    CustomizationOptionEntity(
                        id = -2L,
                        optionType = CustomizationOptionType.FOOD_LEVEL.name,
                        label = "Lv 1 (Tidak Pedas)",
                        extraPrice = 0L
                    )
                )
            }
    }

    var selectedVariantId by remember(item.id) { mutableStateOf(drinkVariants.first().id) }
    var selectedToppingIds by remember(item.id) { mutableStateOf(setOf<Long>()) }
    var selectedLevelId by remember(item.id) { mutableStateOf(foodLevels.first().id) }
    var qty by remember(item.id) { mutableIntStateOf(1) }

    val selectedVariant = drinkVariants.find { it.id == selectedVariantId } ?: drinkVariants.first()
    val activeToppings = drinkToppings.filter { it.id in selectedToppingIds }
    val selectedFoodLevel = foodLevels.find { it.id == selectedLevelId } ?: foodLevels.first()

    val extraPrice = if (isDrink) {
        selectedVariant.extraPrice + activeToppings.sumOf { it.extraPrice }
    } else {
        selectedFoodLevel.extraPrice
    }

    val unitPrice = item.price + extraPrice
    val totalPrice = unitPrice * qty

    val summaryText = if (isDrink) {
        buildString {
            append(selectedVariant.label)
            if (activeToppings.isNotEmpty()) {
                append(" + ")
                append(activeToppings.joinToString(", ") { it.label })
            }
        }
    } else {
        selectedFoodLevel.label
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Harga dasar: ${Formatters.formatRupiah(item.price)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isOwner) {
                    OutlinedButton(
                        onClick = {
                            onOpenPriceEditor(
                                if (isDrink) CustomizationOptionType.DRINK_VARIANT
                                else CustomizationOptionType.FOOD_LEVEL
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("custom_edit_price_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Harga")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                TextButton(onClick = onDismiss) { Text("Tutup") }
            }
        }

        if (isDrink) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pilih Varian Minuman", style = MaterialTheme.typography.titleSmall)
                if (isOwner) {
                    TextButton(
                        onClick = { onOpenPriceEditor(CustomizationOptionType.DRINK_VARIANT) },
                        modifier = Modifier.testTag("edit_variant_price_link")
                    ) {
                        Text("Ubah Harga")
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                drinkVariants.forEach { variant ->
                    val label = if (variant.extraPrice > 0) {
                        "${variant.label} (+${Formatters.formatRupiah(variant.extraPrice)})"
                    } else {
                        variant.label
                    }
                    FilterChip(
                        selected = selectedVariant.id == variant.id,
                        onClick = { selectedVariantId = variant.id },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("custom_variant_${variant.id}")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pilih Toping (Boleh Lebih dari 1)", style = MaterialTheme.typography.titleSmall)
                if (isOwner) {
                    TextButton(
                        onClick = { onOpenPriceEditor(CustomizationOptionType.DRINK_TOPPING) },
                        modifier = Modifier.testTag("edit_topping_price_link")
                    ) {
                        Text("Ubah Harga")
                    }
                }
            }
            if (drinkToppings.isEmpty()) {
                Text(
                    "Belum ada opsi toping.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    drinkToppings.forEach { topping ->
                        val selected = topping.id in selectedToppingIds
                        val toppingLabel = if (topping.extraPrice > 0L) {
                            "${topping.label} (+${Formatters.formatRupiah(topping.extraPrice)})"
                        } else {
                            topping.label
                        }
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedToppingIds = if (selected) {
                                    selectedToppingIds - topping.id
                                } else {
                                    selectedToppingIds + topping.id
                                }
                            },
                            label = { Text(toppingLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("custom_topping_${topping.id}")
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pilih Level Pedas (1 - 5)", style = MaterialTheme.typography.titleSmall)
                if (isOwner) {
                    TextButton(
                        onClick = { onOpenPriceEditor(CustomizationOptionType.FOOD_LEVEL) },
                        modifier = Modifier.testTag("edit_level_price_link")
                    ) {
                        Text("Ubah Harga")
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                foodLevels.forEach { lv ->
                    val label = if (lv.extraPrice > 0) {
                        "${lv.label} (+${Formatters.formatRupiah(lv.extraPrice)})"
                    } else {
                        lv.label
                    }
                    FilterChip(
                        selected = selectedFoodLevel.id == lv.id,
                        onClick = { selectedLevelId = lv.id },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("custom_level_${lv.id}")
                    )
                }
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Harga Satuan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Formatters.formatRupiah(unitPrice), style = MonoPriceStyle, color = priceTextColor)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { if (qty > 1) qty-- },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Remove, contentDescription = "Kurangi jumlah")
                    }
                }
                Text(qty.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    onClick = { if (qty < item.stockQty.toInt().coerceAtLeast(1)) qty++ },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah jumlah")
                    }
                }
            }
        }

        Button(
            onClick = { onConfirm(qty, unitPrice, summaryText) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("custom_confirm_add_button")
        ) {
            Text("Tambah ke Keranjang • ${Formatters.formatRupiah(totalPrice)}")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CheckoutBottomSheetContent(
    cartLines: List<CartLineItem>,
    draftEditContext: DraftEditContext?,
    subtotal: Long,
    storeProfile: StoreProfile,
    priceTextColor: androidx.compose.ui.graphics.Color,
    onUpdateLineQty: (String, Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirmOrder: (
        customerName: String,
        orderType: OrderType,
        paymentMethod: PaymentMethod,
        discountAmount: Long,
        paidAmount: Long,
        notes: String,
        saveAsDraft: Boolean
    ) -> Unit
) {
    var orderType by remember(draftEditContext) {
        mutableStateOf(draftEditContext?.orderType ?: OrderType.TAKE_AWAY)
    }
    var paymentMethod by remember(draftEditContext) {
        mutableStateOf(draftEditContext?.paymentMethod ?: PaymentMethod.TUNAI)
    }
    var customerName by remember(draftEditContext) {
        mutableStateOf(draftEditContext?.customerName ?: "")
    }
    var notes by remember(draftEditContext) {
        mutableStateOf(draftEditContext?.notes ?: "")
    }
    var discountInput by remember(draftEditContext) {
        mutableStateOf(
            draftEditContext?.discountAmount?.takeIf { it > 0L }?.toString() ?: ""
        )
    }
    var cashInput by remember { mutableStateOf("") }

    val taxPercent = storeProfile.taxPercentFor(orderType)
    val discount = discountInput.toLongOrNull()?.coerceIn(0L, subtotal) ?: 0L
    val taxable = (subtotal - discount).coerceAtLeast(0L)
    val taxAmount = (taxable * taxPercent) / 100L
    val grandTotal = taxable + taxAmount

    val cashPaid = if (paymentMethod == PaymentMethod.TUNAI) {
        cashInput.toLongOrNull() ?: grandTotal
    } else {
        grandTotal
    }
    val change = (cashPaid - grandTotal).coerceAtLeast(0L)
    val canPay = paymentMethod != PaymentMethod.TUNAI || cashPaid >= grandTotal

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (draftEditContext != null) {
                    "Edit Draf ${draftEditContext.orderNumber}"
                } else {
                    "Konfirmasi Pesanan"
                },
                style = MaterialTheme.typography.headlineSmall
            )
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }

        // Editable cart lines inside Checkout sheet
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cartLines.forEach { line ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(line.itemName, style = MaterialTheme.typography.titleSmall)
                            if (line.optionsSummary.isNotBlank()) {
                                Text(
                                    line.optionsSummary,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "${Formatters.formatRupiah(line.unitPrice)} x ${line.quantity} = ${Formatters.formatRupiah(line.lineTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = priceTextColor
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                onClick = { onUpdateLineQty(line.lineKey, -1) },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Remove, contentDescription = "Kurangi", modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(line.quantity.toString(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 4.dp))
                            Surface(
                                onClick = { onUpdateLineQty(line.lineKey, 1) },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Text("Jenis Layanan & Pajak", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OrderType.entries.forEach { type ->
                val serviceTax = storeProfile.taxPercentFor(type)
                FilterChip(
                    selected = orderType == type,
                    onClick = { orderType = type },
                    label = { Text("${type.label} (${serviceTax}%)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("checkout_type_${type.name}")
                )
            }
        }

        OutlinedTextField(
            value = customerName,
            onValueChange = { customerName = it },
            label = { Text("Nama Pelanggan / Kode Order Online") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_customer_input")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = discountInput,
                onValueChange = { v -> discountInput = v.filter { it.isDigit() } },
                label = { Text("Diskon (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(0.45f)
                    .testTag("checkout_discount_input")
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan Dapur") },
                placeholder = { Text("Less sugar, pisah saus...") },
                singleLine = true,
                modifier = Modifier
                    .weight(0.55f)
                    .testTag("checkout_notes_input")
            )
        }

        Text("Metode Pembayaran", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PaymentMethod.entries.forEach { method ->
                FilterChip(
                    selected = paymentMethod == method,
                    onClick = { paymentMethod = method },
                    label = { Text(method.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("checkout_pay_${method.name}")
                )
            }
        }

        if (paymentMethod == PaymentMethod.TUNAI) {
            OutlinedTextField(
                value = cashInput,
                onValueChange = { v -> cashInput = v.filter { it.isDigit() } },
                label = { Text("Uang Diterima (kosong = Uang Pas)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_cash_input")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { cashInput = grandTotal.toString() }, label = { Text("Uang Pas") })
                AssistChip(onClick = { cashInput = "50000" }, label = { Text("Rp 50 rb") })
                AssistChip(onClick = { cashInput = "100000" }, label = { Text("Rp 100 rb") })
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                Text(Formatters.formatRupiah(subtotal), style = MaterialTheme.typography.bodyMedium)
            }
            if (discount > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Diskon", style = MaterialTheme.typography.bodyMedium)
                    Text("-${Formatters.formatRupiah(discount)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (taxAmount > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pajak ${orderType.label} ($taxPercent%)", style = MaterialTheme.typography.bodyMedium)
                    Text(Formatters.formatRupiah(taxAmount), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Tagihan", style = MaterialTheme.typography.titleMedium)
                Text(
                    Formatters.formatRupiah(grandTotal),
                    style = MonoPriceStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
                    color = priceTextColor
                )
            }
            if (paymentMethod == PaymentMethod.TUNAI) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Kembalian", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (canPay) Formatters.formatRupiah(change) else "Uang kurang",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (canPay) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onConfirmOrder(customerName, orderType, paymentMethod, discount, cashPaid, notes, true)
                },
                enabled = cartLines.isNotEmpty(),
                modifier = Modifier
                    .weight(0.42f)
                    .height(52.dp)
                    .testTag("checkout_save_draft_button")
            ) {
                Icon(Icons.Default.SaveAs, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan Draf")
            }
            Button(
                onClick = {
                    onConfirmOrder(customerName, orderType, paymentMethod, discount, cashPaid, notes, false)
                },
                enabled = canPay && cartLines.isNotEmpty(),
                modifier = Modifier
                    .weight(0.58f)
                    .height(52.dp)
                    .testTag("checkout_confirm_button")
            ) {
                Text("Bayar & Cetak Struk")
            }
        }
    }
}
