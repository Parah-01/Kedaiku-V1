package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.CategoryEntity
import com.example.data.local.CustomizationGroup
import com.example.data.local.CustomizationOptionEntity
import com.example.data.local.InventoryItemEntity
import com.example.data.local.InventoryItemWithCategories
import com.example.data.local.InventoryType
import com.example.data.local.StockMovementEntity
import com.example.ui.components.CustomizationOptionsManagerDialog
import com.example.ui.theme.GoldenYellowTextAccent
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.theme.ThemeMode
import com.example.util.Formatters

private enum class InventoryFilter(val label: String) {
    ALL("Semua"),
    MENU("Menu Jual"),
    INGREDIENT("Bahan Baku"),
    LOW("Menipis")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InventoryScreen(
    inventoryWithCategories: List<InventoryItemWithCategories>,
    categories: List<CategoryEntity>,
    customizationOptions: List<CustomizationOptionEntity>,
    movements: List<StockMovementEntity>,
    isOwner: Boolean,
    themeMode: ThemeMode,
    onSaveItem: (InventoryItemEntity, List<Long>) -> Unit,
    onAdjustStock: (InventoryItemEntity, Double, String) -> Unit,
    onDeleteItem: (InventoryItemEntity) -> Unit,
    onSaveCategory: (id: Long, name: String, description: String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onSaveCustomizationOption: (CustomizationOptionEntity) -> Unit,
    onDeleteCustomizationOption: (CustomizationOptionEntity) -> Unit,
    onCheckLowStockNotifications: () -> Unit
) {
    var filter by rememberSaveable { mutableStateOf(InventoryFilter.ALL) }
    var selectedCategoryId by rememberSaveable { mutableStateOf(-1L) }
    var editingEntry by remember { mutableStateOf<InventoryItemWithCategories?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showCategoryManager by remember { mutableStateOf(false) }
    var showCustomizationManager by remember { mutableStateOf(false) }
    var adjustingItem by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var showMovementsSheet by remember { mutableStateOf(false) }

    val isDark = themeMode == ThemeMode.DARK || (themeMode == ThemeMode.SYSTEM && isSystemInDarkTheme())
    val priceTextColor = if (isDark) MaterialTheme.colorScheme.primary else GoldenYellowTextAccent

    val lowCount = inventoryWithCategories.count { it.item.isLowStock }
    val outCount = inventoryWithCategories.count { it.item.isOutOfStock }

    val filtered = remember(inventoryWithCategories, filter, selectedCategoryId) {
        inventoryWithCategories.filter { entry ->
            val item = entry.item
            val matchType = when (filter) {
                InventoryFilter.ALL -> true
                InventoryFilter.MENU -> item.itemType == InventoryType.MENU.name
                InventoryFilter.INGREDIENT -> item.itemType == InventoryType.INGREDIENT.name
                InventoryFilter.LOW -> item.stockQty <= item.minThreshold
            }
            val matchCat = selectedCategoryId == -1L || entry.categories.any { it.id == selectedCategoryId }
            matchType && matchCat
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Summary Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InventoryStatChip(
                    label = "Total Item",
                    value = inventoryWithCategories.size.toString(),
                    container = MaterialTheme.colorScheme.surfaceContainer,
                    content = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                InventoryStatChip(
                    label = "Menipis",
                    value = lowCount.toString(),
                    container = MaterialTheme.colorScheme.tertiaryContainer,
                    content = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
                InventoryStatChip(
                    label = "Habis",
                    value = outCount.toString(),
                    container = MaterialTheme.colorScheme.errorContainer,
                    content = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            // Action strip
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isOwner) {
                    OutlinedButton(
                        onClick = { showCategoryManager = true },
                        modifier = Modifier.testTag("manage_categories_button")
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kategori")
                    }
                    OutlinedButton(
                        onClick = { showCustomizationManager = true },
                        modifier = Modifier.testTag("manage_customization_prices_button")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Harga Varian & Toping")
                    }
                }
                OutlinedButton(
                    onClick = { showMovementsSheet = true },
                    modifier = Modifier.testTag("inventory_history_button")
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Riwayat")
                }
                OutlinedButton(
                    onClick = onCheckLowStockNotifications,
                    modifier = Modifier.testTag("inventory_check_notif_button")
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Notif")
                }
            }

            // Type Filters
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(InventoryFilter.entries) { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(f.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("inventory_filter_${f.name}")
                    )
                }
            }

            // Multi-category Filters
            if (categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == -1L,
                            onClick = { selectedCategoryId = -1L },
                            label = { Text("Semua Kategori") },
                            modifier = Modifier.testTag("inventory_cat_filter_all")
                        )
                    }
                    items(categories, key = { it.id }) { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.name) },
                            modifier = Modifier.testTag("inventory_cat_filter_${cat.id}")
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tidak ada item pada filter ini", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.item.id }) { entry ->
                        InventoryItemCard(
                            entry = entry,
                            isOwner = isOwner,
                            priceTextColor = priceTextColor,
                            onAdjustClick = { adjustingItem = entry.item },
                            onEditClick = { editingEntry = entry },
                            onDeleteClick = { deleteTarget = entry.item }
                        )
                    }
                }
            }
        }

        if (isOwner) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("inventory_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah menu atau bahan")
            }
        }
    }

    if (showCategoryManager) {
        CategoryManagementDialog(
            categories = categories,
            onDismiss = { showCategoryManager = false },
            onSaveCategory = onSaveCategory,
            onDeleteCategory = onDeleteCategory
        )
    }

    if (showCustomizationManager && isOwner) {
        CustomizationOptionsManagerDialog(
            options = customizationOptions,
            onDismiss = { showCustomizationManager = false },
            onSaveOption = onSaveCustomizationOption,
            onDeleteOption = onDeleteCustomizationOption
        )
    }

    if (showAddDialog) {
        AddEditInventoryDialog(
            existing = null,
            categories = categories,
            onDismiss = { showAddDialog = false },
            onSave = { item, catIds ->
                onSaveItem(item, catIds)
                showAddDialog = false
            }
        )
    }

    editingEntry?.let { entry ->
        AddEditInventoryDialog(
            existing = entry,
            categories = categories,
            onDismiss = { editingEntry = null },
            onSave = { updated, catIds ->
                onSaveItem(updated, catIds)
                editingEntry = null
            }
        )
    }

    adjustingItem?.let { item ->
        AdjustStockDialog(
            item = item,
            onDismiss = { adjustingItem = null },
            onConfirm = { delta, reason ->
                onAdjustStock(item, delta, reason)
                adjustingItem = null
            }
        )
    }

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus ${item.name}?") },
            text = { Text("Item ini akan dihapus dari daftar menu/inventaris.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteItem(item)
                        deleteTarget = null
                    },
                    modifier = Modifier.testTag("confirm_delete_item_button")
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Batal") }
            }
        )
    }

    if (showMovementsSheet) {
        ModalBottomSheet(onDismissRequest = { showMovementsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text("Riwayat Mutasi Stok", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(10.dp))
                if (movements.isEmpty()) {
                    Text(
                        "Belum ada mutasi stok tercatat.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(movements, key = { it.id }) { m ->
                            val isPlus = m.delta >= 0
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(m.itemName, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            "${m.reason} • ${m.actorName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            Formatters.formatDateTime(m.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = (if (isPlus) "+" else "") + Formatters.formatQty(m.delta),
                                            style = MonoPriceStyle,
                                            color = if (isPlus) {
                                                MaterialTheme.colorScheme.secondary
                                            } else {
                                                MaterialTheme.colorScheme.error
                                            }
                                        )
                                        Text(
                                            "Jadi ${Formatters.formatQty(m.newQty)}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryManagementDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSaveCategory: (id: Long, name: String, description: String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit
) {
    var editingId by remember { mutableStateOf(0L) }
    var nameInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kelola Kategori Produk") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(if (editingId > 0) "Edit Nama Kategori" else "Nama Kategori Baru") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input")
                )
                OutlinedTextField(
                    value = descInput,
                    onValueChange = { descInput = it },
                    label = { Text("Keterangan (opsional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_desc_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                onSaveCategory(editingId, nameInput, descInput)
                                editingId = 0L
                                nameInput = ""
                                descInput = ""
                            }
                        },
                        enabled = nameInput.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("category_save_button")
                    ) {
                        Text(if (editingId > 0) "Simpan Perubahan" else "Tambah Kategori")
                    }
                    if (editingId > 0) {
                        TextButton(
                            onClick = {
                                editingId = 0L
                                nameInput = ""
                                descInput = ""
                            }
                        ) {
                            Text("Batal")
                        }
                    }
                }

                HorizontalDivider()
                Text("Daftar Kategori (${categories.size})", style = MaterialTheme.typography.titleSmall)

                if (categories.isEmpty()) {
                    Text(
                        "Belum ada kategori.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    categories.forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cat.name, style = MaterialTheme.typography.titleSmall)
                                    if (cat.description.isNotBlank()) {
                                        Text(
                                            cat.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingId = cat.id
                                            nameInput = cat.name
                                            descInput = cat.description
                                        },
                                        modifier = Modifier.testTag("edit_category_${cat.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit kategori")
                                    }
                                    IconButton(
                                        onClick = { onDeleteCategory(cat) },
                                        modifier = Modifier.testTag("delete_category_${cat.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Hapus kategori",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Selesai") }
        }
    )
}

@Composable
private fun InventoryStatChip(
    label: String,
    value: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = container,
        contentColor = content
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InventoryItemCard(
    entry: InventoryItemWithCategories,
    isOwner: Boolean,
    priceTextColor: androidx.compose.ui.graphics.Color,
    onAdjustClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val item = entry.item
    val typeLabel = InventoryType.entries.find { it.name == item.itemType }?.label ?: item.itemType
    val statusContainer = when {
        item.isOutOfStock -> MaterialTheme.colorScheme.errorContainer
        item.isLowStock -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val statusContent = when {
        item.isOutOfStock -> MaterialTheme.colorScheme.onErrorContainer
        item.isLowStock -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val statusText = when {
        item.isOutOfStock -> "HABIS"
        item.isLowStock -> "MENIPIS"
        else -> "AMAN"
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_card_${item.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${item.sku} • $typeLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusContainer,
                    contentColor = statusContent
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (entry.categoryNames.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    entry.categoryNames.forEach { catName ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = catName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Stok: ${Formatters.formatQty(item.stockQty)} ${item.unit} (Min: ${Formatters.formatQty(item.minThreshold)})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.itemType == InventoryType.MENU.name) {
                        val hppInfo = if (isOwner) " • HPP ${Formatters.formatRupiah(item.costPrice)}" else ""
                        Text(
                            "Jual: ${Formatters.formatRupiah(item.price)}$hppInfo",
                            style = MaterialTheme.typography.bodySmall,
                            color = priceTextColor
                        )
                    } else if (isOwner) {
                        Text(
                            "Harga Modal: ${Formatters.formatRupiah(item.costPrice)}/${item.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onAdjustClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("inventory_adjust_${item.id}")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stok")
                    }
                    if (isOwner) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.testTag("inventory_edit_${item.id}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit item")
                        }
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.testTag("inventory_delete_${item.id}")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Hapus item",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustStockDialog(
    item: InventoryItemEntity,
    onDismiss: () -> Unit,
    onConfirm: (delta: Double, reason: String) -> Unit
) {
    var isAdd by remember { mutableStateOf(true) }
    var amountStr by remember { mutableStateOf("5") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sesuaikan Stok: ${item.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Stok saat ini: ${Formatters.formatQty(item.stockQty)} ${item.unit}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isAdd,
                        onClick = { isAdd = true },
                        label = { Text("+ Stok Masuk") },
                        modifier = Modifier.testTag("adjust_mode_add")
                    )
                    FilterChip(
                        selected = !isAdd,
                        onClick = { isAdd = false },
                        label = { Text("- Kurangi / Rusak") },
                        modifier = Modifier.testTag("adjust_mode_sub")
                    )
                }
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { v -> amountStr = v.filter { it.isDigit() || it == '.' } },
                    label = { Text("Jumlah (${item.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("adjust_amount_input")
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Keterangan") },
                    placeholder = { Text(if (isAdd) "Restok supplier" else "Terpakai dapur / basi") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("adjust_reason_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = amountStr.toDoubleOrNull() ?: 0.0
                    if (qty > 0.0) {
                        onConfirm(if (isAdd) qty else -qty, reason)
                    }
                },
                modifier = Modifier.testTag("adjust_confirm_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddEditInventoryDialog(
    existing: InventoryItemWithCategories?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (InventoryItemEntity, List<Long>) -> Unit
) {
    val existingItem = existing?.item
    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var sku by remember { mutableStateOf(existingItem?.sku ?: "") }
    var selectedCategoryIds by remember {
        mutableStateOf(
            existing?.categories?.map { it.id }?.toSet()
                ?: categories.firstOrNull()?.let { setOf(it.id) }
                ?: emptySet()
        )
    }
    var itemType by remember {
        mutableStateOf(
            InventoryType.entries.find { it.name == existingItem?.itemType } ?: InventoryType.MENU
        )
    }
    var customGroup by remember {
        mutableStateOf(
            CustomizationGroup.entries.find { it.name == existingItem?.customizationGroup }
                ?: CustomizationGroup.NONE
        )
    }
    var priceStr by remember { mutableStateOf((existingItem?.price ?: 0L).takeIf { it > 0 }?.toString() ?: "") }
    var costStr by remember { mutableStateOf((existingItem?.costPrice ?: 0L).takeIf { it > 0 }?.toString() ?: "") }
    var stockStr by remember { mutableStateOf(existingItem?.let { Formatters.formatQty(it.stockQty) } ?: "10") }
    var minStr by remember { mutableStateOf(existingItem?.let { Formatters.formatQty(it.minThreshold) } ?: "5") }
    var unit by remember { mutableStateOf(existingItem?.unit ?: "pcs") }
    var available by remember { mutableStateOf(existingItem?.isAvailable ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingItem == null) "Tambah Item Baru" else "Edit Item") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InventoryType.entries.forEach { t ->
                        FilterChip(
                            selected = itemType == t,
                            onClick = { itemType = t },
                            label = { Text(t.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Item") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_name_input")
                )
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("Kode SKU") },
                    placeholder = { Text("KOP-01") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Pilih Kategori (Bisa >1)", style = MaterialTheme.typography.labelMedium)
                if (categories.isEmpty()) {
                    Text(
                        "Belum ada kategori. Tambahkan melalui menu Kelola Kategori.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.forEach { cat ->
                            val selected = cat.id in selectedCategoryIds
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedCategoryIds = if (selected) {
                                        selectedCategoryIds - cat.id
                                    } else {
                                        selectedCategoryIds + cat.id
                                    }
                                },
                                label = { Text(cat.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("item_cat_select_${cat.id}")
                            )
                        }
                    }
                }

                if (itemType == InventoryType.MENU) {
                    Text("Opsi Kustomisasi Kasir", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CustomizationGroup.entries.forEach { g ->
                            FilterChip(
                                selected = customGroup == g,
                                onClick = { customGroup = g },
                                label = { Text(g.label) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (itemType == InventoryType.MENU) {
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { v -> priceStr = v.filter { it.isDigit() } },
                            label = { Text("Harga Jual") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("item_price_input")
                        )
                    }
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { v -> costStr = v.filter { it.isDigit() } },
                        label = { Text("Modal / HPP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { v -> stockStr = v.filter { it.isDigit() || it == '.' } },
                        label = { Text("Stok Awal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minStr,
                        onValueChange = { v -> minStr = v.filter { it.isDigit() || it == '.' } },
                        label = { Text("Batas Minim") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Satuan") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }
                if (itemType == InventoryType.MENU) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tampilkan di Kasir", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = available, onCheckedChange = { available = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val fallbackCat = categories.filter { it.id in selectedCategoryIds }
                            .joinToString(", ") { it.name }.ifBlank { "Umum" }
                        onSave(
                            InventoryItemEntity(
                                id = existingItem?.id ?: 0L,
                                sku = sku.trim().ifEmpty { "ITM-${System.currentTimeMillis() % 10000}" },
                                name = name.trim(),
                                category = fallbackCat,
                                itemType = itemType.name,
                                customizationGroup = if (itemType == InventoryType.MENU) customGroup.name else CustomizationGroup.NONE.name,
                                price = if (itemType == InventoryType.MENU) priceStr.toLongOrNull() ?: 0L else 0L,
                                costPrice = costStr.toLongOrNull() ?: 0L,
                                stockQty = stockStr.toDoubleOrNull() ?: 0.0,
                                minThreshold = minStr.toDoubleOrNull() ?: 5.0,
                                unit = unit.trim().ifEmpty { "pcs" },
                                isAvailable = available
                            ),
                            selectedCategoryIds.toList()
                        )
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("item_save_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
