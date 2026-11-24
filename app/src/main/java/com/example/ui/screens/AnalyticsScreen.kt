package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ExpenseCategory
import com.example.data.local.ExpenseEntity
import com.example.ui.AnalyticsPeriod
import com.example.ui.AnalyticsSnapshot
import com.example.ui.DailySalesPoint
import com.example.ui.components.ExpenseManagementDialog
import com.example.ui.theme.GoldenYellowTextAccent
import com.example.ui.theme.MonoPriceStyle
import com.example.ui.theme.ThemeMode
import com.example.util.Formatters

@Composable
fun AnalyticsScreen(
    period: AnalyticsPeriod,
    analytics: AnalyticsSnapshot,
    expenses: List<ExpenseEntity>,
    isOwner: Boolean,
    themeMode: ThemeMode,
    onSelectPeriod: (AnalyticsPeriod) -> Unit,
    onSaveExpense: (id: Long, title: String, category: ExpenseCategory, amount: Long, notes: String, expenseDate: Long) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit
) {
    val isDark = themeMode == ThemeMode.DARK || (themeMode == ThemeMode.SYSTEM && isSystemInDarkTheme())
    val accentTextColor = if (isDark) MaterialTheme.colorScheme.primary else GoldenYellowTextAccent
    var showExpenseDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalyticsPeriod.entries.forEach { p ->
                    FilterChip(
                        selected = period == p,
                        onClick = { onSelectPeriod(p) },
                        label = { Text(p.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("analytics_period_${p.name}")
                    )
                }
            }
        }

        // 2x2 KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AnalyticsKpiCard(
                        title = "Total Omzet",
                        value = Formatters.formatCompactRupiah(analytics.totalRevenue),
                        subtitle = Formatters.formatRupiah(analytics.totalRevenue),
                        icon = Icons.Default.AttachMoney,
                        accent = accentTextColor,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsKpiCard(
                        title = "Transaksi",
                        value = analytics.totalOrders.toString(),
                        subtitle = "Rata-rata ${Formatters.formatCompactRupiah(analytics.averageOrderValue)}",
                        icon = Icons.Default.Receipt,
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AnalyticsKpiCard(
                        title = "Laba Kotor",
                        value = if (isOwner) Formatters.formatCompactRupiah(analytics.grossProfit) else "Terbatas",
                        subtitle = if (isOwner) "Omzet - HPP ${Formatters.formatCompactRupiah(analytics.totalCost)}" else "Khusus akun Owner",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsKpiCard(
                        title = "Total Pengeluaran",
                        value = if (isOwner) Formatters.formatCompactRupiah(analytics.totalExpenses) else "Terbatas",
                        subtitle = if (isOwner) Formatters.formatRupiah(analytics.totalExpenses) else "Khusus akun Owner",
                        icon = Icons.Default.MoneyOff,
                        accent = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // True Net Profit Report + Expense Breakdown (Owner only)
        if (isOwner) {
            item {
                val isPositiveNet = analytics.netProfit >= 0L
                val netMarginPct = if (analytics.totalRevenue > 0L) {
                    ((analytics.netProfit * 100.0) / analytics.totalRevenue).toInt()
                } else {
                    0
                }
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPositiveNet) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("analytics_net_profit_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = if (isPositiveNet) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                                Text(
                                    "Laporan Laba Bersih Aktual",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPositiveNet) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    }
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    "Margin $netMarginPct%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = Formatters.formatRupiah(analytics.netProfit),
                            style = MonoPriceStyle.copy(
                                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                                color = if (isPositiveNet) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                        )
                        HorizontalDivider(
                            color = (if (isPositiveNet) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }).copy(alpha = 0.2f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Laba Kotor (Omzet - HPP)", style = MaterialTheme.typography.bodySmall)
                            Text(Formatters.formatRupiah(analytics.grossProfit), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Dikurangi Pengeluaran (${period.label})", style = MaterialTheme.typography.bodySmall)
                            Text("-${Formatters.formatRupiah(analytics.totalExpenses)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item {
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("Rincian Pengeluaran (${period.label})", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        analytics.expenseBreakdowns.forEach { eb ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${eb.category.label} (${eb.count}x)",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "${Formatters.formatRupiah(eb.totalAmount)} (${(eb.percentage * 100).toInt()}%)",
                                        style = MonoPriceStyle.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { eb.percentage.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Button(
                            onClick = { showExpenseDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("analytics_manage_expenses_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Catat & Kelola Pengeluaran")
                        }
                    }
                }
            }
        }

        // Interactive Sales Trend Chart
        item {
            SalesTrendChartCard(
                title = when (period) {
                    AnalyticsPeriod.TODAY -> "Tren Penjualan Hari Ini (Per Jam)"
                    AnalyticsPeriod.WEEK -> "Tren Penjualan 7 Hari Terakhir"
                    AnalyticsPeriod.MONTH -> "Tren Penjualan 30 Hari Terakhir"
                },
                points = analytics.chartPoints,
                accentTextColor = accentTextColor
            )
        }

        // Category Sales Breakdown
        item {
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Category,
                            contentDescription = null,
                            tint = accentTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Analisis Penjualan per Kategori", style = MaterialTheme.typography.titleMedium)
                    }
                    if (analytics.categoryBreakdowns.isEmpty()) {
                        Text(
                            "Belum ada transaksi kategori pada periode ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        analytics.categoryBreakdowns.forEach { cb ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${cb.categoryName} (${cb.qtySold} item)",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "${Formatters.formatRupiah(cb.revenue)} (${(cb.percentage * 100).toInt()}%)",
                                        style = MonoPriceStyle.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { cb.percentage.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Payment Method Breakdown
        item {
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
                    Text("Metode Pembayaran", style = MaterialTheme.typography.titleMedium)
                    analytics.paymentBreakdowns.forEach { pb ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${pb.method.label} (${pb.count}x)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "${Formatters.formatRupiah(pb.totalAmount)} (${(pb.percentage * 100).toInt()}%)",
                                    style = MonoPriceStyle.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                            LinearProgressIndicator(
                                progress = { pb.percentage.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Top Selling Products
        item {
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Menu Terlaris", style = MaterialTheme.typography.titleMedium)
                    }
                    if (analytics.topProducts.isEmpty()) {
                        Text(
                            "Belum ada penjualan pada periode ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val maxQty = analytics.topProducts.maxOf { it.qtySold }.coerceAtLeast(1)
                        analytics.topProducts.forEachIndexed { idx, prod ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${idx + 1}. ${prod.itemName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "${prod.qtySold} terjual • ${Formatters.formatCompactRupiah(prod.revenue)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = accentTextColor
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { prod.qtySold.toFloat() / maxQty.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExpenseDialog && isOwner) {
        ExpenseManagementDialog(
            expenses = expenses,
            onDismiss = { showExpenseDialog = false },
            onSaveExpense = onSaveExpense,
            onDeleteExpense = onDeleteExpense
        )
    }
}

@Composable
private fun AnalyticsKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SalesTrendChartCard(
    title: String,
    points: List<DailySalesPoint>,
    accentTextColor: Color
) {
    var selectedIdx by remember(points) { mutableIntStateOf(-1) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val maxRevenue = (points.maxOfOrNull { it.revenue } ?: 0L).coerceAtLeast(1L)

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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (selectedIdx in points.indices) {
                    val sel = points[selectedIdx]
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${sel.label}: ${Formatters.formatCompactRupiah(sel.revenue)} (${sel.orderCount})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada data grafik", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .pointerInput(points) {
                            detectTapGestures { tap ->
                                if (points.isNotEmpty()) {
                                    val slotWidth = size.width.toFloat() / points.size
                                    val idx = (tap.x / slotWidth).toInt().coerceIn(0, points.lastIndex)
                                    selectedIdx = if (selectedIdx == idx) -1 else idx
                                }
                            }
                        }
                        .testTag("analytics_sales_chart")
                ) {
                    val barCount = points.size
                    val slotW = size.width / barCount
                    val barW = (slotW * 0.56f).coerceAtLeast(12f)
                    val topPad = 14f
                    val chartH = size.height - topPad - 8f

                    listOf(0.25f, 0.5f, 0.75f, 1f).forEach { ratio ->
                        val y = topPad + chartH * (1f - ratio)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }

                    points.forEachIndexed { idx, pt ->
                        val frac = if (maxRevenue > 0L) pt.revenue.toFloat() / maxRevenue.toFloat() else 0f
                        val barH = (chartH * frac).coerceAtLeast(if (pt.revenue > 0L) 10f else 4f)
                        val left = idx * slotW + (slotW - barW) / 2f
                        val top = topPad + (chartH - barH)
                        val isSelected = idx == selectedIdx

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = if (isSelected) {
                                    listOf(secondaryColor, secondaryColor.copy(alpha = 0.75f))
                                } else {
                                    listOf(primaryColor, primaryColor.copy(alpha = 0.75f))
                                }
                            ),
                            topLeft = Offset(left, top),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    points.forEachIndexed { idx, pt ->
                        Text(
                            text = pt.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (idx == selectedIdx) accentTextColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
