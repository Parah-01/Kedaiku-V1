package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.CustomizationOptionEntity
import com.example.data.local.ExpenseCategory
import com.example.data.local.ExpenseEntity
import com.example.data.local.StaffAccountEntity
import com.example.data.local.StaffRole
import com.example.data.prefs.StoreProfile
import com.example.ui.components.CustomizationOptionsManagerDialog
import com.example.ui.components.ExpenseManagementDialog
import com.example.ui.theme.ThemeMode
import com.example.util.Formatters
import com.example.util.LocalBackupInfo

@Composable
fun ReportsBackupScreen(
    isOwner: Boolean,
    storeProfile: StoreProfile,
    themeMode: ThemeMode,
    autoBackupEnabled: Boolean,
    lastAutoBackupTime: Long,
    localSnapshots: List<LocalBackupInfo>,
    allStaff: List<StaffAccountEntity>,
    currentUser: StaffAccountEntity?,
    customizationOptions: List<CustomizationOptionEntity>,
    expenses: List<ExpenseEntity>,
    needsNotificationPermission: Boolean,
    onSetThemeMode: (ThemeMode) -> Unit,
    onToggleAutoBackup: (Boolean) -> Unit,
    onRunManualBackup: () -> Unit,
    onExportExcel: (Uri) -> Unit,
    onExportCsv: (Uri) -> Unit,
    onExportPdf: (Uri) -> Unit,
    onExportTemplateCsv: (Uri) -> Unit,
    onImportInventoryCsv: (Uri) -> Unit,
    onExportBackupJson: (Uri) -> Unit,
    onRestoreBackupJson: (Uri) -> Unit,
    onRestoreLocalSnapshot: (String) -> Unit,
    onUpdateStoreProfile: (StoreProfile) -> Unit,
    onSaveCustomizationOption: (CustomizationOptionEntity) -> Unit,
    onDeleteCustomizationOption: (CustomizationOptionEntity) -> Unit,
    onSaveExpense: (id: Long, title: String, category: ExpenseCategory, amount: Long, notes: String, expenseDate: Long) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onAddStaff: (name: String, role: StaffRole, pin: String) -> Unit,
    onToggleStaffActive: (StaffAccountEntity) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onTestLowStockNotification: () -> Unit,
    onLogout: () -> Unit
) {
    // SAF CreateDocument / OpenDocument launchers (zero storage permission needed!)
    val excelLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri -> uri?.let(onExportExcel) }

    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(onExportCsv) }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> uri?.let(onExportPdf) }

    val templateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(onExportTemplateCsv) }

    val csvImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onImportInventoryCsv) }

    val backupExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(onExportBackupJson) }

    val backupRestoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onRestoreBackupJson) }

    var showAddStaffDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showCustomizationDialog by remember { mutableStateOf(false) }
    var snapshotRestoreTarget by remember { mutableStateOf<LocalBackupInfo?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Auto-Save & Offline Backup Section
        item {
            SectionCard(title = "Auto-Save & Backup Offline", icon = Icons.Default.CloudDone) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Backup Otomatis Setiap Transaksi", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (lastAutoBackupTime > 0L) {
                                "Terakhir disimpan: ${Formatters.formatDateTime(lastAutoBackupTime)}"
                            } else {
                                "Auto-save SQLite aktif (WAL mode)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Switch(
                        checked = autoBackupEnabled,
                        onCheckedChange = onToggleAutoBackup,
                        modifier = Modifier.testTag("toggle_auto_backup")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRunManualBackup,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_backup_button")
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup Sekarang")
                    }
                    if (isOwner) {
                        OutlinedButton(
                            onClick = {
                                backupExportLauncher.launch("KedaiKu_Backup_${Formatters.formatFileTimestamp()}.json")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_backup_json_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ekspor .JSON")
                        }
                    }
                }

                if (isOwner) {
                    OutlinedButton(
                        onClick = {
                            backupRestoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_backup_json_button")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pulihkan Data dari File Backup (.json)")
                    }
                }

                if (localSnapshots.isNotEmpty() && isOwner) {
                    HorizontalDivider()
                    Text("Snapshot Lokal Terbaru", style = MaterialTheme.typography.labelMedium)
                    localSnapshots.take(3).forEach { snap ->
                        Surface(
                            onClick = { snapshotRestoreTarget = snap },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(snap.fileName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${Formatters.formatDateTime(snap.timestamp)} • ${snap.sizeBytes / 1024} KB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "Pulihkan",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Ekspor, Pengeluaran & Harga Varian (Owner only)
        if (isOwner) {
            item {
                SectionCard(title = "Pengeluaran & Harga Opsi", icon = Icons.Default.Payments) {
                    Text(
                        "Catat biaya operasional, gaji karyawan, belanja bahan baku, serta atur harga varian minuman, toping & level.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showExpenseDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reports_manage_expenses_button")
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pengeluaran (${expenses.size})")
                        }
                        OutlinedButton(
                            onClick = { showCustomizationDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reports_manage_custom_prices_button")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Harga Varian")
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Ekspor & Impor Pembukuan", icon = Icons.Default.TableChart) {
                    Text(
                        "Ekspor laporan penjualan, pengeluaran, laba bersih, dan stok untuk pembukuan mandiri.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                excelLauncher.launch("Laporan_KedaiKu_${Formatters.formatFileTimestamp()}.xls")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_excel_button")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Excel (.xls)")
                        }
                        Button(
                            onClick = {
                                pdfLauncher.launch("Laporan_KedaiKu_${Formatters.formatFileTimestamp()}.pdf")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Laporan PDF")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                csvExportLauncher.launch("Penjualan_KedaiKu_${Formatters.formatFileTimestamp()}.csv")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_csv_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ekspor CSV")
                        }
                        OutlinedButton(
                            onClick = {
                                csvImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_csv_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Impor Stok CSV")
                        }
                    }
                    TextButton(
                        onClick = { templateLauncher.launch("Template_Impor_Menu_KedaiKu.csv") },
                        modifier = Modifier.testTag("download_csv_template_button")
                    ) {
                        Text("Unduh Contoh Template Impor CSV")
                    }
                }
            }
        }

        // 3. Tampilan & Notifikasi
        item {
            SectionCard(title = "Tampilan & Notifikasi Stok", icon = Icons.Default.DarkMode) {
                Text("Pilih Tema Nyaman di Mata", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("theme_mode_${mode.name}")
                        )
                    }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (needsNotificationPermission) {
                        Button(
                            onClick = onRequestNotificationPermission,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Izinkan Notif")
                        }
                    }
                    OutlinedButton(
                        onClick = onTestLowStockNotification,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_low_stock_notif_button")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cek Notif Stok")
                    }
                }
            }
        }

        // 4. Manajemen Staf (Owner only)
        if (isOwner) {
            item {
                SectionCard(title = "Manajemen Akun Owner & Karyawan", icon = Icons.Default.GroupAdd) {
                    allStaff.forEach { staff ->
                        val isMe = staff.id == currentUser?.id
                        val roleLabel = StaffRole.entries.find { it.name == staff.role }?.label ?: staff.role
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = staff.name + if (isMe) " (Aktif)" else "",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = roleLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Switch(
                                    checked = staff.isActive,
                                    enabled = !isMe,
                                    onCheckedChange = { onToggleStaffActive(staff) }
                                )
                            }
                        }
                    }
                    Button(
                        onClick = { showAddStaffDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_staff_button")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tambah Karyawan / Owner Baru")
                    }
                }
            }

            // 5. Profil Toko & Struk (Owner only)
            item {
                StoreProfileEditorCard(
                    storeProfile = storeProfile,
                    onSave = onUpdateStoreProfile
                )
            }
        }

        // 6. Ganti Shift / Logout
        item {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("settings_logout_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kunci Kasir / Ganti Shift")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showAddStaffDialog) {
        AddStaffDialog(
            onDismiss = { showAddStaffDialog = false },
            onSave = { name, role, pin ->
                onAddStaff(name, role, pin)
                showAddStaffDialog = false
            }
        )
    }

    if (showExpenseDialog && isOwner) {
        ExpenseManagementDialog(
            expenses = expenses,
            onDismiss = { showExpenseDialog = false },
            onSaveExpense = onSaveExpense,
            onDeleteExpense = onDeleteExpense
        )
    }

    if (showCustomizationDialog && isOwner) {
        CustomizationOptionsManagerDialog(
            options = customizationOptions,
            onDismiss = { showCustomizationDialog = false },
            onSaveOption = onSaveCustomizationOption,
            onDeleteOption = onDeleteCustomizationOption
        )
    }

    snapshotRestoreTarget?.let { snap ->
        AlertDialog(
            onDismissRequest = { snapshotRestoreTarget = null },
            title = { Text("Pulihkan Snapshot Lokal?") },
            text = {
                Text("Data inventaris dan pesanan saat ini akan diganti dengan snapshot ${snap.fileName} (${Formatters.formatDateTime(snap.timestamp)}).")
            },
            confirmButton = {
                Button(onClick = {
                    onRestoreLocalSnapshot(snap.filePath)
                    snapshotRestoreTarget = null
                }) {
                    Text("Pulihkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { snapshotRestoreTarget = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun StoreProfileEditorCard(
    storeProfile: StoreProfile,
    onSave: (StoreProfile) -> Unit
) {
    var name by remember(storeProfile) { mutableStateOf(storeProfile.storeName) }
    var address by remember(storeProfile) { mutableStateOf(storeProfile.storeAddress) }
    var phone by remember(storeProfile) { mutableStateOf(storeProfile.storePhone) }
    var taxTakeAwayStr by remember(storeProfile) { mutableStateOf(storeProfile.taxTakeAwayPercent.toString()) }
    var taxShopeeStr by remember(storeProfile) { mutableStateOf(storeProfile.taxShopeePercent.toString()) }
    var taxGrabStr by remember(storeProfile) { mutableStateOf(storeProfile.taxGrabPercent.toString()) }
    var footer by remember(storeProfile) { mutableStateOf(storeProfile.receiptFooter) }

    SectionCard(title = "Profil Toko, Pajak Layanan & Struk", icon = Icons.Default.Store) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nama Toko") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Alamat Toko") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("No. Telepon") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Tarif Pajak per Jenis Layanan (0 - 99%)", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = taxTakeAwayStr,
                onValueChange = { v -> taxTakeAwayStr = v.filter { it.isDigit() }.take(2) },
                label = { Text("Take Away %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("tax_take_away_input")
            )
            OutlinedTextField(
                value = taxShopeeStr,
                onValueChange = { v -> taxShopeeStr = v.filter { it.isDigit() }.take(2) },
                label = { Text("Shopee %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("tax_shopee_input")
            )
            OutlinedTextField(
                value = taxGrabStr,
                onValueChange = { v -> taxGrabStr = v.filter { it.isDigit() }.take(2) },
                label = { Text("Grab %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("tax_grab_input")
            )
        }

        OutlinedTextField(
            value = footer,
            onValueChange = { footer = it },
            label = { Text("Catatan Bawah Struk") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                onSave(
                    StoreProfile(
                        storeName = name,
                        storeAddress = address,
                        storePhone = phone,
                        receiptFooter = footer,
                        taxTakeAwayPercent = (taxTakeAwayStr.toIntOrNull() ?: 0).coerceIn(0, 99),
                        taxShopeePercent = (taxShopeeStr.toIntOrNull() ?: 0).coerceIn(0, 99),
                        taxGrabPercent = (taxGrabStr.toIntOrNull() ?: 0).coerceIn(0, 99)
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("save_store_profile_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Simpan Profil & Pajak")
        }
    }
}

@Composable
private fun AddStaffDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, role: StaffRole, pin: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(StaffRole.KARYAWAN) }
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Akun Staf") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StaffRole.entries.forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r.label) },
                            modifier = Modifier.testTag("add_staff_role_${r.name}")
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_staff_name_input")
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { v -> if (v.length <= 6 && v.all { it.isDigit() }) pin = v },
                    label = { Text("PIN Masuk (4-6 angka)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_staff_pin_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, role, pin) },
                enabled = name.isNotBlank() && pin.length >= 4,
                modifier = Modifier.testTag("add_staff_confirm_button")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
