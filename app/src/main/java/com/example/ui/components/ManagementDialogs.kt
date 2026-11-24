package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.CustomizationOptionEntity
import com.example.data.local.CustomizationOptionType
import com.example.data.local.ExpenseCategory
import com.example.data.local.ExpenseEntity
import com.example.ui.theme.MonoPriceStyle
import com.example.util.Formatters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomizationOptionsManagerDialog(
    options: List<CustomizationOptionEntity>,
    initialType: CustomizationOptionType = CustomizationOptionType.DRINK_VARIANT,
    onDismiss: () -> Unit,
    onSaveOption: (CustomizationOptionEntity) -> Unit,
    onDeleteOption: (CustomizationOptionEntity) -> Unit
) {
    var selectedType by remember(initialType) { mutableStateOf(initialType) }
    var editingOption by remember { mutableStateOf<CustomizationOptionEntity?>(null) }
    var labelInput by remember { mutableStateOf("") }
    var extraPriceInput by remember { mutableStateOf("0") }

    val filteredOptions = remember(options, selectedType) {
        options.filter { it.optionType == selectedType.name }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Harga Varian, Toping & Level", style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CustomizationOptionType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                editingOption = null
                                labelInput = ""
                                extraPriceInput = "0"
                            },
                            label = { Text(type.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("custom_opt_tab_${type.name}")
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (editingOption != null) {
                                "Edit Harga / Nama: ${editingOption!!.label}"
                            } else {
                                "Tambah ${selectedType.label} Baru"
                            },
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = labelInput,
                                onValueChange = { labelInput = it },
                                label = { Text("Nama Opsi") },
                                placeholder = { Text("Contoh: Jumbo / Jelly") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("custom_opt_label_input")
                            )
                            OutlinedTextField(
                                value = extraPriceInput,
                                onValueChange = { v -> extraPriceInput = v.filter { it.isDigit() } },
                                label = { Text("Harga +Rp") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(0.9f)
                                    .testTag("custom_opt_price_input")
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val cleanLabel = labelInput.trim()
                                    if (cleanLabel.isNotEmpty()) {
                                        val price = extraPriceInput.toLongOrNull() ?: 0L
                                        val nextSort = (filteredOptions.maxOfOrNull { it.sortOrder } ?: 0) + 1
                                        val existing = editingOption
                                        onSaveOption(
                                            CustomizationOptionEntity(
                                                id = existing?.id ?: 0L,
                                                optionType = selectedType.name,
                                                label = cleanLabel,
                                                extraPrice = price,
                                                sortOrder = existing?.sortOrder ?: nextSort
                                            )
                                        )
                                        editingOption = null
                                        labelInput = ""
                                        extraPriceInput = "0"
                                    }
                                },
                                enabled = labelInput.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_opt_save_button")
                            ) {
                                Icon(
                                    if (editingOption != null) Icons.Default.Edit else Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (editingOption != null) "Simpan Harga" else "Tambah Opsi")
                            }
                            if (editingOption != null) {
                                TextButton(
                                    onClick = {
                                        editingOption = null
                                        labelInput = ""
                                        extraPriceInput = "0"
                                    }
                                ) {
                                    Text("Batal")
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()
                Text(
                    "Daftar ${selectedType.label} (${filteredOptions.size})",
                    style = MaterialTheme.typography.titleSmall
                )

                if (filteredOptions.isEmpty()) {
                    Text(
                        "Belum ada opsi untuk kategori ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredOptions.forEach { opt ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = if (editingOption?.id == opt.id) {
                                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    null
                                },
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
                                        Text(opt.label, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = if (opt.extraPrice > 0L) {
                                                "+${Formatters.formatRupiah(opt.extraPrice)}"
                                            } else {
                                                "Gratis (Rp 0)"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingOption = opt
                                                labelInput = opt.label
                                                extraPriceInput = opt.extraPrice.toString()
                                            },
                                            modifier = Modifier.testTag("edit_custom_option_${opt.id}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit harga ${opt.label}")
                                        }
                                        IconButton(
                                            onClick = {
                                                if (editingOption?.id == opt.id) {
                                                    editingOption = null
                                                    labelInput = ""
                                                    extraPriceInput = "0"
                                                }
                                                onDeleteOption(opt)
                                            },
                                            modifier = Modifier.testTag("delete_custom_option_${opt.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Hapus ${opt.label}",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Selesai") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseManagementDialog(
    expenses: List<ExpenseEntity>,
    onDismiss: () -> Unit,
    onSaveExpense: (id: Long, title: String, category: ExpenseCategory, amount: Long, notes: String, expenseDate: Long) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit
) {
    var filterCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var editingExpenseId by remember { mutableLongStateOf(0L) }
    var editingExpenseDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var titleInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.OPERATIONAL) }
    var amountInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    val filteredExpenses = remember(expenses, filterCategory) {
        if (filterCategory == null) {
            expenses
        } else {
            expenses.filter { it.category == filterCategory!!.name }
        }
    }
    val totalFiltered = remember(filteredExpenses) { filteredExpenses.sumOf { it.amount } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text("Kelola Pengeluaran Toko", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Total tercatat: ${Formatters.formatRupiah(totalFiltered)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                // Add / Edit Expense Form
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (editingExpenseId > 0L) "Edit Pengeluaran" else "Catat Pengeluaran Baru",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ExpenseCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("expense_form_cat_${cat.name}")
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = titleInput,
                                onValueChange = { titleInput = it },
                                label = { Text("Judul Pengeluaran") },
                                placeholder = { Text("Listrik, Gaji, Beli Susu...") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.25f)
                                    .testTag("expense_title_input")
                            )
                            OutlinedTextField(
                                value = amountInput,
                                onValueChange = { v -> amountInput = v.filter { it.isDigit() } },
                                label = { Text("Nominal (Rp)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(0.95f)
                                    .testTag("expense_amount_input")
                            )
                        }
                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("Catatan / Supplier / Shift (opsional)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_notes_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val parsedAmount = amountInput.toLongOrNull() ?: 0L
                            Button(
                                onClick = {
                                    if (titleInput.isNotBlank() && parsedAmount > 0L) {
                                        val dateToUse = if (editingExpenseId > 0L) {
                                            editingExpenseDate
                                        } else {
                                            System.currentTimeMillis()
                                        }
                                        onSaveExpense(
                                            editingExpenseId,
                                            titleInput,
                                            selectedCategory,
                                            parsedAmount,
                                            notesInput,
                                            dateToUse
                                        )
                                        editingExpenseId = 0L
                                        titleInput = ""
                                        amountInput = ""
                                        notesInput = ""
                                    }
                                },
                                enabled = titleInput.isNotBlank() && parsedAmount > 0L,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("expense_save_button")
                            ) {
                                Icon(
                                    if (editingExpenseId > 0L) Icons.Default.Edit else Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (editingExpenseId > 0L) "Simpan Perubahan" else "Catat Pengeluaran")
                            }
                            if (editingExpenseId > 0L) {
                                TextButton(
                                    onClick = {
                                        editingExpenseId = 0L
                                        titleInput = ""
                                        amountInput = ""
                                        notesInput = ""
                                    }
                                ) {
                                    Text("Batal")
                                }
                            }
                        }
                    }
                }

                // Filter chips for list
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = filterCategory == null,
                        onClick = { filterCategory = null },
                        label = { Text("Semua (${expenses.size})") },
                        modifier = Modifier.testTag("expense_filter_ALL")
                    )
                    ExpenseCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = filterCategory == cat,
                            onClick = { filterCategory = cat },
                            label = { Text(cat.label) },
                            modifier = Modifier.testTag("expense_filter_${cat.name}")
                        )
                    }
                }

                HorizontalDivider()

                if (filteredExpenses.isEmpty()) {
                    Text(
                        "Belum ada catatan pengeluaran pada kategori ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredExpenses.forEach { exp ->
                            val catLabel = ExpenseCategory.entries.find { it.name == exp.category }?.label ?: exp.category
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
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            exp.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "$catLabel • ${Formatters.formatDateTime(exp.expenseDate)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (exp.notes.isNotBlank()) {
                                            Text(
                                                exp.notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            Formatters.formatRupiah(exp.amount),
                                            style = MonoPriceStyle.copy(color = MaterialTheme.colorScheme.error)
                                        )
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editingExpenseId = exp.id
                                                    editingExpenseDate = exp.expenseDate
                                                    titleInput = exp.title
                                                    selectedCategory = ExpenseCategory.entries.find { it.name == exp.category }
                                                        ?: ExpenseCategory.OPERATIONAL
                                                    amountInput = exp.amount.toString()
                                                    notesInput = exp.notes
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("edit_expense_${exp.id}")
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit ${exp.title}",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (editingExpenseId == exp.id) {
                                                        editingExpenseId = 0L
                                                        titleInput = ""
                                                        amountInput = ""
                                                        notesInput = ""
                                                    }
                                                    onDeleteExpense(exp)
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("delete_expense_${exp.id}")
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Hapus ${exp.title}",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Selesai") }
                }
            }
        }
    }
}
