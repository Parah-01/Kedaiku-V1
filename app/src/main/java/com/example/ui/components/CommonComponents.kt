package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.OrderType
import com.example.data.local.OrderWithItems
import com.example.data.local.PaymentMethod
import com.example.data.local.StaffAccountEntity
import com.example.data.local.StaffRole
import com.example.data.prefs.StoreProfile
import com.example.ui.theme.MonoReceiptStyle
import com.example.ui.theme.ThemeMode
import com.example.util.Formatters
import com.example.util.ReceiptPrinter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KedaiKuTopBar(
    storeName: String,
    currentUser: StaffAccountEntity?,
    lowStockCount: Int,
    lastAutoBackupTime: Long,
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onLowStockClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val isDarkActive = themeMode == ThemeMode.DARK
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
            Column {
                Text(
                    text = storeName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (currentUser != null) {
                        val roleLabel = StaffRole.entries.find { it.name == currentUser.role }?.label ?: currentUser.role
                        Text(
                            text = "${currentUser.name} • $roleLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (lastAutoBackupTime > 0L) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Text(
                            text = "Auto-Save",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        },
        actions = {
            if (lowStockCount > 0) {
                IconButton(
                    onClick = onLowStockClick,
                    modifier = Modifier.testTag("top_bar_low_stock_button")
                ) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ) {
                                Text(lowStockCount.toString())
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Stok menipis",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier.testTag("top_bar_theme_toggle")
            ) {
                Icon(
                    imageVector = if (isDarkActive) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Ganti mode gelap atau terang"
                )
            }
            IconButton(
                onClick = onLogoutClick,
                modifier = Modifier.testTag("top_bar_logout_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Keluar atau ganti shift"
                )
            }
        }
    )
}

@Composable
fun StockAlertBanner(
    lowStockCount: Int,
    outOfStockCount: Int,
    needsNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onInspectInventory: () -> Unit
) {
    if (lowStockCount == 0 && !needsNotificationPermission) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onInspectInventory)
            .testTag("low_stock_banner"),
        shape = RoundedCornerShape(14.dp),
        color = if (outOfStockCount > 0) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = "Peringatan stok",
                tint = if (outOfStockCount > 0) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (lowStockCount > 0) {
                        "$lowStockCount item stok menipis ($outOfStockCount habis)"
                    } else {
                        "Aktifkan notifikasi push stok menipis"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (outOfStockCount > 0) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    }
                )
                Text(
                    text = if (needsNotificationPermission) {
                        "Ketuk Aktifkan agar peringatan stok muncul otomatis."
                    } else {
                        "Ketuk untuk cek dan isi ulang stok sekarang."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (outOfStockCount > 0) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    }
                )
            }
            if (needsNotificationPermission) {
                OutlinedButton(
                    onClick = onRequestNotificationPermission,
                    modifier = Modifier.testTag("enable_notification_button")
                ) {
                    Text("Aktifkan")
                }
            }
        }
    }
}

@Composable
fun ReceiptPreviewDialog(
    orderWithItems: OrderWithItems,
    storeProfile: StoreProfile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val order = orderWithItems.order
    val typeLabel = OrderType.entries.find { it.name == order.orderType }?.label ?: order.orderType
    val payLabel = PaymentMethod.entries.find { it.name == order.paymentMethod }?.label ?: order.paymentMethod

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Struk Transaksi",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))

                // Thermal paper card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFDAD2C8), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFDF9),
                    contentColor = Color(0xFF1F1916)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = storeProfile.storeName.uppercase(),
                            style = MonoReceiptStyle.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        if (storeProfile.storeAddress.isNotBlank()) {
                            Text(
                                text = storeProfile.storeAddress,
                                style = MonoReceiptStyle,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (storeProfile.storePhone.isNotBlank()) {
                            Text(
                                text = "Telp: ${storeProfile.storePhone}",
                                style = MonoReceiptStyle,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFCBC2B8))
                        Spacer(modifier = Modifier.height(8.dp))

                        ReceiptLine("No Order", order.orderNumber)
                        ReceiptLine("Tanggal", Formatters.formatDateTime(order.createdAt))
                        ReceiptLine("Kasir", order.cashierName)
                        ReceiptLine("Layanan", typeLabel)
                        if (order.customerName.isNotBlank()) {
                            ReceiptLine("Pelanggan", order.customerName)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFCBC2B8))
                        Spacer(modifier = Modifier.height(8.dp))

                        orderWithItems.items.forEach { line ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = line.itemName,
                                    style = MonoReceiptStyle.copy(fontWeight = FontWeight.Medium)
                                )
                                if (line.optionsSummary.isNotBlank()) {
                                    Text(
                                        text = "  ${line.optionsSummary}",
                                        style = MonoReceiptStyle.copy(color = Color(0xFF5C5248))
                                    )
                                }
                                ReceiptLine(
                                    "  ${line.quantity} x ${Formatters.formatRupiah(line.unitPrice)}",
                                    Formatters.formatRupiah(line.lineTotal)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = Color(0xFFCBC2B8))
                        Spacer(modifier = Modifier.height(8.dp))

                        ReceiptLine("Subtotal", Formatters.formatRupiah(order.subtotal))
                        if (order.discountAmount > 0) {
                            ReceiptLine("Diskon", "-${Formatters.formatRupiah(order.discountAmount)}")
                        }
                        if (order.taxAmount > 0) {
                            ReceiptLine("Pajak", Formatters.formatRupiah(order.taxAmount))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        ReceiptLine(
                            "TOTAL",
                            Formatters.formatRupiah(order.totalAmount),
                            bold = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ReceiptLine("Bayar ($payLabel)", Formatters.formatRupiah(order.paidAmount))
                        if (order.changeAmount > 0) {
                            ReceiptLine("Kembalian", Formatters.formatRupiah(order.changeAmount))
                        }
                        if (order.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Catatan: ${order.notes}",
                                style = MonoReceiptStyle,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFCBC2B8))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = storeProfile.receiptFooter,
                            style = MonoReceiptStyle,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { ReceiptPrinter.printReceipt(context, orderWithItems, storeProfile) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("print_receipt_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cetak")
                    }
                    OutlinedButton(
                        onClick = { ReceiptPrinter.shareReceiptPdf(context, orderWithItems, storeProfile) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_receipt_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { ReceiptPrinter.shareReceiptText(context, orderWithItems, storeProfile) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_receipt_text_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bagikan Teks")
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("close_receipt_button")
                    ) {
                        Text("Selesai")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptLine(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (bold) MonoReceiptStyle.copy(fontWeight = FontWeight.Bold) else MonoReceiptStyle
        )
        Text(
            text = value,
            style = if (bold) MonoReceiptStyle.copy(fontWeight = FontWeight.Bold) else MonoReceiptStyle
        )
    }
}
