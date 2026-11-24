package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.local.ExpenseCategory
import com.example.data.local.ExpenseEntity
import com.example.data.local.InventoryItemEntity
import com.example.data.local.InventoryItemWithCategories
import com.example.data.local.InventoryType
import com.example.data.local.OrderStatus
import com.example.data.local.OrderType
import com.example.data.local.OrderWithItems
import com.example.data.local.PaymentMethod
import com.example.data.prefs.StoreProfile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class CsvImportResult(
    val importedItems: List<InventoryItemEntity>,
    val skippedRows: Int,
    val errorSummary: String? = null
)

object ReportExportImport {

    fun exportSalesAndInventoryExcelXml(
        context: Context,
        uri: Uri,
        store: StoreProfile,
        orders: List<OrderWithItems>,
        inventory: List<InventoryItemWithCategories>,
        expenses: List<ExpenseEntity>
    ) {
        val completedOrders = orders.filter {
            it.order.status != OrderStatus.CANCELLED.name && it.order.status != OrderStatus.DRAFT.name
        }
        val totalRevenue = completedOrders.sumOf { it.order.totalAmount }
        val totalCost = completedOrders.sumOf { o -> o.items.sumOf { it.unitCost * it.quantity } }
        val grossProfit = totalRevenue - totalCost
        val totalExpenses = expenses.sumOf { it.amount }
        val netProfit = grossProfit - totalExpenses

        val xml = buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<?mso-application progid="Excel.Sheet"?>""")
            appendLine("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""")
            appendLine("""  <Styles>""")
            appendLine("""    <Style ss:ID="Header"><Font ss:Bold="1" ss:Color="#1F1916"/><Interior ss:Color="#FFC000" ss:Pattern="Solid"/></Style>""")
            appendLine("""    <Style ss:ID="Title"><Font ss:Bold="1" ss:Size="14"/></Style>""")
            appendLine("""  </Styles>""")

            // Sheet 1: Ringkasan
            appendLine("""  <Worksheet ss:Name="Ringkasan">""")
            appendLine("""    <Table>""")
            appendRow(listOf(store.storeName), style = "Title")
            appendRow(listOf("Dicetak pada", Formatters.formatDateTime(System.currentTimeMillis())))
            appendRow(listOf("Total Pesanan Valid", completedOrders.size.toString()))
            appendRow(listOf("Total Omzet (IDR)", totalRevenue.toString()))
            appendRow(listOf("Estimasi Modal / HPP (IDR)", totalCost.toString()))
            appendRow(listOf("Laba Kotor (IDR)", grossProfit.toString()))
            appendRow(listOf("Total Pengeluaran Operasional & Gaji (IDR)", totalExpenses.toString()))
            appendRow(listOf("Laba Bersih Aktual / Net Profit (IDR)", netProfit.toString()))
            appendRow(listOf("Total Item Inventaris", inventory.size.toString()))
            appendRow(listOf("Item Stok Menipis / Habis", inventory.count { it.item.stockQty <= it.item.minThreshold }.toString()))
            appendLine("""    </Table>""")
            appendLine("""  </Worksheet>""")

            // Sheet 2: Transaksi
            appendLine("""  <Worksheet ss:Name="Transaksi">""")
            appendLine("""    <Table>""")
            appendRow(
                listOf("No Order", "Tanggal", "Pelanggan", "Layanan", "Status", "Pembayaran", "Subtotal", "Pajak", "Total", "Kasir", "Detail Menu"),
                style = "Header"
            )
            orders.forEach { owi ->
                val o = owi.order
                val detail = owi.items.joinToString("; ") { line ->
                    val opts = if (line.optionsSummary.isNotBlank()) " (${line.optionsSummary})" else ""
                    "${line.itemName}$opts x${line.quantity}"
                }
                val typeLabel = OrderType.entries.find { it.name == o.orderType }?.label ?: o.orderType
                val statusLabel = OrderStatus.entries.find { it.name == o.status }?.label ?: o.status
                val payLabel = PaymentMethod.entries.find { it.name == o.paymentMethod }?.label ?: o.paymentMethod
                appendRow(
                    listOf(
                        o.orderNumber,
                        Formatters.formatDateTime(o.createdAt),
                        o.customerName.ifBlank { "-" },
                        typeLabel,
                        statusLabel,
                        payLabel,
                        o.subtotal.toString(),
                        o.taxAmount.toString(),
                        o.totalAmount.toString(),
                        o.cashierName,
                        detail
                    )
                )
            }
            appendLine("""    </Table>""")
            appendLine("""  </Worksheet>""")

            // Sheet 3: Pengeluaran
            appendLine("""  <Worksheet ss:Name="Pengeluaran">""")
            appendLine("""    <Table>""")
            appendRow(
                listOf("Tanggal", "Judul Pengeluaran", "Kategori", "Nominal (IDR)", "Catatan", "Dicatat Oleh"),
                style = "Header"
            )
            expenses.forEach { exp ->
                val catLabel = ExpenseCategory.entries.find { it.name == exp.category }?.label ?: exp.category
                appendRow(
                    listOf(
                        Formatters.formatDateTime(exp.expenseDate),
                        exp.title,
                        catLabel,
                        exp.amount.toString(),
                        exp.notes.ifBlank { "-" },
                        exp.recordedBy
                    )
                )
            }
            appendLine("""    </Table>""")
            appendLine("""  </Worksheet>""")

            // Sheet 4: Inventaris
            appendLine("""  <Worksheet ss:Name="Inventaris">""")
            appendLine("""    <Table>""")
            appendRow(
                listOf("SKU", "Nama", "Kategori", "Jenis", "Harga Jual", "Harga Modal", "Stok", "Batas Minim", "Satuan", "Status Stok"),
                style = "Header"
            )
            inventory.forEach { entry ->
                val item = entry.item
                val typeLabel = InventoryType.entries.find { it.name == item.itemType }?.label ?: item.itemType
                val stockStatus = when {
                    item.isOutOfStock -> "HABIS"
                    item.isLowStock -> "MENIPIS"
                    else -> "AMAN"
                }
                appendRow(
                    listOf(
                        item.sku,
                        item.name,
                        entry.categoryNames.joinToString(", "),
                        typeLabel,
                        item.price.toString(),
                        item.costPrice.toString(),
                        Formatters.formatQty(item.stockQty),
                        Formatters.formatQty(item.minThreshold),
                        item.unit,
                        stockStatus
                    )
                )
            }
            appendLine("""    </Table>""")
            appendLine("""  </Worksheet>""")
            appendLine("""</Workbook>""")
        }

        context.contentResolver.openOutputStream(uri)?.use { os ->
            OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                writer.write(xml)
            }
        } ?: error("Gagal membuka lokasi penyimpanan file Excel.")
    }

    fun exportSalesCsv(
        context: Context,
        uri: Uri,
        orders: List<OrderWithItems>
    ) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                writer.write("\uFEFF")
                writer.write("No Order,Tanggal,Pelanggan,Layanan,Status,Metode Bayar,Subtotal,Pajak,Total,Kasir,Item\n")
                orders.forEach { owi ->
                    val o = owi.order
                    val itemsDesc = owi.items.joinToString("; ") { line ->
                        val opts = if (line.optionsSummary.isNotBlank()) " (${line.optionsSummary})" else ""
                        "${line.itemName}$opts x${line.quantity}"
                    }
                    writer.write(
                        listOf(
                            o.orderNumber,
                            Formatters.formatDateTime(o.createdAt),
                            o.customerName,
                            o.orderType,
                            o.status,
                            o.paymentMethod,
                            o.subtotal.toString(),
                            o.taxAmount.toString(),
                            o.totalAmount.toString(),
                            o.cashierName,
                            itemsDesc
                        ).joinToString(",") { csvEscape(it) } + "\n"
                    )
                }
            }
        } ?: error("Gagal membuka file CSV.")
    }

    fun exportInventoryTemplateCsv(context: Context, uri: Uri) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                writer.write("\uFEFF")
                writer.write("sku,nama,kategori,jenis,harga_jual,harga_modal,stok,batas_minim,satuan\n")
                writer.write("MN-101,Es Kopi Pandan,Kopi & Minuman;Best Seller,MENU,24000,9000,25,5,cup\n")
                writer.write("BH-201,Sirup Pandan,Bahan Baku,INGREDIENT,0,42000,6,2,botol\n")
            }
        } ?: error("Gagal menyimpan template CSV.")
    }

    fun exportBusinessReportPdf(
        context: Context,
        uri: Uri,
        store: StoreProfile,
        orders: List<OrderWithItems>,
        inventory: List<InventoryItemWithCategories>,
        expenses: List<ExpenseEntity>
    ) {
        val completedOrders = orders.filter {
            it.order.status != OrderStatus.CANCELLED.name && it.order.status != OrderStatus.DRAFT.name
        }
        val totalRevenue = completedOrders.sumOf { it.order.totalAmount }
        val totalCost = completedOrders.sumOf { owi -> owi.items.sumOf { it.unitCost * it.quantity } }
        val grossProfit = totalRevenue - totalCost
        val totalExpenses = expenses.sumOf { it.amount }
        val netProfit = grossProfit - totalExpenses

        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNum = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(31, 25, 22)
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(107, 94, 85)
            textSize = 10f
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(168, 120, 0)
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(31, 25, 22)
            textSize = 9.5f
        }
        val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(31, 25, 22)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val dividerPaint = Paint().apply {
            color = Color.rgb(220, 209, 196)
            strokeWidth = 1f
        }

        val left = 36f
        val right = pageWidth - 36f
        var y = 46f

        fun nextPageIfNeeded(neededHeight: Float) {
            if (y + neededHeight > pageHeight - 40f) {
                document.finishPage(page)
                pageNum++
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                y = 44f
            }
        }

        canvas.drawText("Laporan Bisnis & Laba Bersih - ${store.storeName}", left, y, titlePaint)
        y += 16f
        canvas.drawText(
            "${store.storeAddress} | Telp: ${store.storePhone} | Dicetak: ${Formatters.formatDateTime(System.currentTimeMillis())}",
            left,
            y,
            subtitlePaint
        )
        y += 14f
        canvas.drawLine(left, y, right, y, dividerPaint)
        y += 22f

        canvas.drawText("1. Ringkasan Keuangan & Laba Bersih", left, y, sectionPaint)
        y += 18f
        canvas.drawText("Total Transaksi Valid : ${completedOrders.size} transaksi", left, y, bodyPaint)
        y += 15f
        canvas.drawText("Total Omzet Penjualan : ${Formatters.formatRupiah(totalRevenue)}", left, y, boldBodyPaint)
        y += 15f
        canvas.drawText("Estimasi Modal (HPP)  : ${Formatters.formatRupiah(totalCost)}", left, y, bodyPaint)
        y += 15f
        canvas.drawText("Laba Kotor (Gross)    : ${Formatters.formatRupiah(grossProfit)}", left, y, boldBodyPaint)
        y += 15f
        canvas.drawText("Total Pengeluaran     : ${Formatters.formatRupiah(totalExpenses)}", left, y, bodyPaint)
        y += 15f
        canvas.drawText("LABA BERSIH AKTUAL    : ${Formatters.formatRupiah(netProfit)}", left, y, boldBodyPaint)
        y += 22f

        canvas.drawText("2. Catatan Pengeluaran (${expenses.size})", left, y, sectionPaint)
        y += 16f
        if (expenses.isEmpty()) {
            canvas.drawText("Belum ada pengeluaran tercatat.", left, y, bodyPaint)
            y += 18f
        } else {
            expenses.take(25).forEach { exp ->
                nextPageIfNeeded(18f)
                val catLabel = ExpenseCategory.entries.find { it.name == exp.category }?.label ?: exp.category
                canvas.drawText("${Formatters.formatShortDate(exp.expenseDate)} • ${exp.title.take(26)} ($catLabel)", left, y, bodyPaint)
                canvas.drawText(Formatters.formatRupiah(exp.amount), left + 400f, y, boldBodyPaint)
                y += 15f
            }
            y += 10f
        }

        nextPageIfNeeded(50f)
        canvas.drawText("3. Riwayat Transaksi Terbaru", left, y, sectionPaint)
        y += 16f
        canvas.drawText("No Order", left, y, boldBodyPaint)
        canvas.drawText("Tanggal", left + 90f, y, boldBodyPaint)
        canvas.drawText("Metode", left + 220f, y, boldBodyPaint)
        canvas.drawText("Status", left + 305f, y, boldBodyPaint)
        canvas.drawText("Total", left + 400f, y, boldBodyPaint)
        y += 8f
        canvas.drawLine(left, y, right, y, dividerPaint)
        y += 14f

        orders.take(30).forEach { owi ->
            nextPageIfNeeded(18f)
            val o = owi.order
            val statusLabel = OrderStatus.entries.find { it.name == o.status }?.label ?: o.status
            val payLabel = PaymentMethod.entries.find { it.name == o.paymentMethod }?.label ?: o.paymentMethod
            canvas.drawText(o.orderNumber, left, y, bodyPaint)
            canvas.drawText(Formatters.formatDateTime(o.createdAt), left + 90f, y, bodyPaint)
            canvas.drawText(payLabel, left + 220f, y, bodyPaint)
            canvas.drawText(statusLabel, left + 305f, y, bodyPaint)
            canvas.drawText(Formatters.formatRupiah(o.totalAmount), left + 400f, y, boldBodyPaint)
            y += 15f
        }

        y += 14f
        nextPageIfNeeded(50f)
        canvas.drawText("4. Kondisi Inventaris & Kategori", left, y, sectionPaint)
        y += 16f
        canvas.drawText("Nama Item", left, y, boldBodyPaint)
        canvas.drawText("Kategori", left + 180f, y, boldBodyPaint)
        canvas.drawText("Stok", left + 310f, y, boldBodyPaint)
        canvas.drawText("Harga Jual", left + 400f, y, boldBodyPaint)
        y += 8f
        canvas.drawLine(left, y, right, y, dividerPaint)
        y += 14f

        inventory.forEach { entry ->
            val item = entry.item
            nextPageIfNeeded(18f)
            val statusTag = if (item.stockQty <= item.minThreshold) " (!)" else ""
            canvas.drawText("${item.name.take(28)}$statusTag", left, y, bodyPaint)
            canvas.drawText(entry.categoryNames.joinToString(", ").take(20), left + 180f, y, bodyPaint)
            canvas.drawText("${Formatters.formatQty(item.stockQty)} ${item.unit}", left + 310f, y, bodyPaint)
            canvas.drawText(
                if (item.price > 0) Formatters.formatRupiah(item.price) else "Bahan Baku",
                left + 400f,
                y,
                bodyPaint
            )
            y += 15f
        }

        document.finishPage(page)

        try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                document.writeTo(os)
            } ?: error("Gagal menulis laporan PDF.")
        } finally {
            document.close()
        }
    }

    fun parseInventoryCsv(context: Context, uri: Uri): CsvImportResult {
        val imported = mutableListOf<InventoryItemEntity>()
        var skipped = 0

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
                lines.forEachIndexed { index, rawLine ->
                    val line = rawLine.removePrefix("\uFEFF").trim()
                    if (line.isEmpty()) return@forEachIndexed
                    val cols = parseCsvLine(line)
                    if (index == 0 && (cols.firstOrNull()?.equals("sku", true) == true || cols.firstOrNull()?.equals("nama", true) == true)) {
                        return@forEachIndexed
                    }
                    if (cols.size < 8) {
                        skipped++
                        return@forEachIndexed
                    }
                    try {
                        val sku = cols[0].trim().ifEmpty { "IMP-${1000 + index}" }
                        val name = cols[1].trim()
                        val category = cols[2].trim().ifEmpty { "Umum" }.replace(";", ",")
                        val rawType = cols[3].trim().uppercase(java.util.Locale.US)
                        val itemType = if (rawType.contains("BAHAN") || rawType == "INGREDIENT") {
                            InventoryType.INGREDIENT.name
                        } else {
                            InventoryType.MENU.name
                        }
                        val price = cols[4].trim().toLongOrNull() ?: 0L
                        val cost = cols[5].trim().toLongOrNull() ?: 0L
                        val stock = cols[6].trim().replace(',', '.').toDoubleOrNull() ?: 0.0
                        val minThreshold = cols[7].trim().replace(',', '.').toDoubleOrNull() ?: 5.0
                        val unit = cols.getOrNull(8)?.trim()?.ifEmpty { "pcs" } ?: "pcs"

                        if (name.isBlank()) {
                            skipped++
                        } else {
                            imported.add(
                                InventoryItemEntity(
                                    sku = sku,
                                    name = name,
                                    category = category,
                                    itemType = itemType,
                                    price = price.coerceAtLeast(0L),
                                    costPrice = cost.coerceAtLeast(0L),
                                    stockQty = stock.coerceAtLeast(0.0),
                                    minThreshold = minThreshold.coerceAtLeast(0.0),
                                    unit = unit,
                                    isAvailable = true,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    } catch (_: Exception) {
                        skipped++
                    }
                }
            }
        } ?: return CsvImportResult(emptyList(), 0, "Tidak dapat membaca file yang dipilih.")

        return CsvImportResult(importedItems = imported, skippedRows = skipped)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    private fun csvEscape(value: String): String {
        val safe = value.replace("\"", "\"\"")
        return if (safe.contains(',') || safe.contains('"') || safe.contains('\n')) {
            "\"$safe\""
        } else {
            safe
        }
    }

    private fun StringBuilder.appendRow(cells: List<String>, style: String? = null) {
        appendLine("      <Row>")
        cells.forEach { cell ->
            val styleAttr = if (style != null) """ ss:StyleID="$style"""" else ""
            val escaped = cell
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
            appendLine("""        <Cell$styleAttr><Data ss:Type="String">$escaped</Data></Cell>""")
        }
        appendLine("      </Row>")
    }
}
