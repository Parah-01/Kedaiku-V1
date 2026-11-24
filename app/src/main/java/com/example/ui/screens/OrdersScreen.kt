package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.OrderStatus
import com.example.data.local.OrderType
import com.example.data.local.OrderWithItems
import com.example.data.local.PaymentMethod
import com.example.ui.theme.GoldenYellowTextAccent
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.theme.ThemeMode
import com.example.util.Formatters

@Composable
fun OrdersScreen(
    orders: List<OrderWithItems>,
    themeMode: ThemeMode,
    onEditDraft: (OrderWithItems) -> Unit,
    onDeleteDraft: (Long) -> Unit,
    onUpdateStatus: (OrderWithItems, OrderStatus) -> Unit,
    onOpenReceipt: (OrderWithItems) -> Unit
) {
    var selectedFilter by rememberSaveable { mutableStateOf("ALL") }
    var cancelTarget by remember { mutableStateOf<OrderWithItems?>(null) }
    var deleteDraftTarget by remember { mutableStateOf<OrderWithItems?>(null) }

    val isDark = themeMode == ThemeMode.DARK || (themeMode == ThemeMode.SYSTEM && isSystemInDarkTheme())
    val priceTextColor = if (isDark) MaterialTheme.colorScheme.primary else GoldenYellowTextAccent

    val filteredOrders = remember(orders, selectedFilter) {
        if (selectedFilter == "ALL") {
            orders
        } else {
            orders.filter { it.order.status == selectedFilter }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("Semua (${orders.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("orders_filter_ALL")
                )
            }
            items(OrderStatus.entries) { status ->
                val count = orders.count { it.order.status == status.name }
                FilterChip(
                    selected = selectedFilter == status.name,
                    onClick = { selectedFilter = status.name },
                    label = { Text("${status.label} ($count)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("orders_filter_${status.name}")
                )
            }
        }

        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.img_empty_orders),
                        contentDescription = "Belum ada pesanan",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(190.dp, 140.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Belum ada pesanan", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Draf dan pesanan dari menu Kasir akan muncul secara real-time di sini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredOrders, key = { it.order.id }) { orderWithItems ->
                    OrderCard(
                        orderWithItems = orderWithItems,
                        priceTextColor = priceTextColor,
                        onEditDraft = { onEditDraft(orderWithItems) },
                        onDeleteDraftRequest = { deleteDraftTarget = orderWithItems },
                        onAdvanceStatus = { nextStatus -> onUpdateStatus(orderWithItems, nextStatus) },
                        onCancelRequest = { cancelTarget = orderWithItems },
                        onOpenReceipt = { onOpenReceipt(orderWithItems) }
                    )
                }
            }
        }
    }

    cancelTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("Batalkan Pesanan ${target.order.orderNumber}?") },
            text = {
                Text("Stok menu yang sudah terpakai pada pesanan ini akan otomatis dikembalikan ke inventaris.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateStatus(target, OrderStatus.CANCELLED)
                        cancelTarget = null
                    },
                    modifier = Modifier.testTag("confirm_cancel_order_button")
                ) {
                    Text("Ya, Batalkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) { Text("Kembali") }
            }
        )
    }

    deleteDraftTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteDraftTarget = null },
            title = { Text("Hapus Draf ${target.order.orderNumber}?") },
            text = { Text("Draf pesanan yang belum diselesaikan ini akan dihapus permanen.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteDraft(target.order.id)
                        deleteDraftTarget = null
                    },
                    modifier = Modifier.testTag("confirm_delete_draft_button")
                ) {
                    Text("Hapus Draf")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDraftTarget = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun OrderCard(
    orderWithItems: OrderWithItems,
    priceTextColor: androidx.compose.ui.graphics.Color,
    onEditDraft: () -> Unit,
    onDeleteDraftRequest: () -> Unit,
    onAdvanceStatus: (OrderStatus) -> Unit,
    onCancelRequest: () -> Unit,
    onOpenReceipt: () -> Unit
) {
    val o = orderWithItems.order
    val status = OrderStatus.entries.find { it.name == o.status } ?: OrderStatus.PENDING
    val typeLabel = OrderType.entries.find { it.name == o.orderType }?.label ?: o.orderType
    val payLabel = PaymentMethod.entries.find { it.name == o.paymentMethod }?.label ?: o.paymentMethod

    val statusColor = when (status) {
        OrderStatus.DRAFT -> MaterialTheme.colorScheme.primaryContainer
        OrderStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
        OrderStatus.PREPARING -> MaterialTheme.colorScheme.primaryContainer
        OrderStatus.READY -> MaterialTheme.colorScheme.secondaryContainer
        OrderStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
    }
    val statusContentColor = when (status) {
        OrderStatus.DRAFT -> MaterialTheme.colorScheme.onPrimaryContainer
        OrderStatus.PENDING -> MaterialTheme.colorScheme.onTertiaryContainer
        OrderStatus.PREPARING -> MaterialTheme.colorScheme.onPrimaryContainer
        OrderStatus.READY -> MaterialTheme.colorScheme.onSecondaryContainer
        OrderStatus.COMPLETED -> MaterialTheme.colorScheme.onSurfaceVariant
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (status == OrderStatus.DRAFT) 1.5.dp else 1.dp,
            color = if (status == OrderStatus.DRAFT) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${o.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${o.orderNumber} • $typeLabel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = buildString {
                            append(Formatters.formatDateTime(o.createdAt))
                            if (o.customerName.isNotBlank()) append(" • ${o.customerName}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor,
                    contentColor = statusContentColor
                ) {
                    Text(
                        text = if (status == OrderStatus.DRAFT) "Draf (Belum Bayar)" else status.label,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                orderWithItems.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "${item.quantity}x ${item.itemName}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (item.optionsSummary.isNotBlank()) {
                                Text(
                                    text = "  ${item.optionsSummary}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = Formatters.formatRupiah(item.lineTotal),
                            style = MonoPriceStyle.copy(fontWeight = FontWeight.Normal)
                        )
                    }
                }
            }

            if (o.notes.isNotBlank()) {
                Text(
                    text = "Catatan: ${o.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = priceTextColor
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (status == OrderStatus.DRAFT) {
                            "Estimasi • Kasir: ${o.cashierName}"
                        } else {
                            "Total ($payLabel) • Kasir: ${o.cashierName}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Formatters.formatRupiah(o.totalAmount),
                        style = MonoPriceStyle,
                        color = priceTextColor
                    )
                }

                if (status == OrderStatus.DRAFT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDeleteDraftRequest,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("order_delete_draft_${o.id}")
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Hapus draf",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Button(
                            onClick = onEditDraft,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("order_edit_draft_${o.id}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit / Finis")
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onOpenReceipt,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("order_receipt_${o.id}")
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Struk")
                        }

                        val nextAction: Pair<String, OrderStatus>? = when (status) {
                            OrderStatus.PENDING -> "Proses" to OrderStatus.PREPARING
                            OrderStatus.PREPARING -> "Siap" to OrderStatus.READY
                            OrderStatus.READY -> "Selesai" to OrderStatus.COMPLETED
                            else -> null
                        }
                        if (nextAction != null) {
                            Button(
                                onClick = { onAdvanceStatus(nextAction.second) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("order_advance_${o.id}")
                            ) {
                                Icon(
                                    imageVector = if (nextAction.second == OrderStatus.COMPLETED) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.LocalDining
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(nextAction.first)
                            }
                        }

                        if (status != OrderStatus.COMPLETED && status != OrderStatus.CANCELLED) {
                            OutlinedButton(
                                onClick = onCancelRequest,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("order_cancel_${o.id}")
                            ) {
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = "Batalkan pesanan",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
