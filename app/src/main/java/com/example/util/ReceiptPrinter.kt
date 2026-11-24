package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.data.local.OrderType
import com.example.data.local.OrderWithItems
import com.example.data.local.PaymentMethod
import com.example.data.prefs.StoreProfile
import java.io.File
import java.io.FileOutputStream

object ReceiptPrinter {

    fun buildPlainTextReceipt(orderWithItems: OrderWithItems, store: StoreProfile): String {
        val order = orderWithItems.order
        val typeLabel = OrderType.entries.find { it.name == order.orderType }?.label ?: order.orderType
        val payLabel = PaymentMethod.entries.find { it.name == order.paymentMethod }?.label ?: order.paymentMethod
        return buildString {
            appendLine(store.storeName)
            if (store.storeAddress.isNotBlank()) appendLine(store.storeAddress)
            if (store.storePhone.isNotBlank()) appendLine("Telp: ${store.storePhone}")
            appendLine("================================")
            appendLine("No     : ${order.orderNumber}")
            appendLine("Tanggal: ${Formatters.formatDateTime(order.createdAt)}")
            appendLine("Kasir  : ${order.cashierName}")
            appendLine("Layanan: $typeLabel")
            if (order.customerName.isNotBlank()) appendLine("Tamu   : ${order.customerName}")
            appendLine("--------------------------------")
            orderWithItems.items.forEach { item ->
                appendLine(item.itemName)
                if (item.optionsSummary.isNotBlank()) {
                    appendLine("  [${item.optionsSummary}]")
                }
                appendLine("  ${item.quantity} x ${Formatters.formatRupiah(item.unitPrice)} = ${Formatters.formatRupiah(item.lineTotal)}")
            }
            appendLine("--------------------------------")
            appendLine("Subtotal : ${Formatters.formatRupiah(order.subtotal)}")
            if (order.discountAmount > 0) {
                appendLine("Diskon   : -${Formatters.formatRupiah(order.discountAmount)}")
            }
            if (order.taxAmount > 0) {
                appendLine("Pajak    : ${Formatters.formatRupiah(order.taxAmount)}")
            }
            appendLine("TOTAL    : ${Formatters.formatRupiah(order.totalAmount)}")
            appendLine("Bayar ($payLabel): ${Formatters.formatRupiah(order.paidAmount)}")
            if (order.changeAmount > 0) {
                appendLine("Kembali  : ${Formatters.formatRupiah(order.changeAmount)}")
            }
            if (order.notes.isNotBlank()) {
                appendLine("Catatan  : ${order.notes}")
            }
            appendLine("================================")
            appendLine(store.receiptFooter)
        }
    }

    private fun createReceiptPdf(orderWithItems: OrderWithItems, store: StoreProfile): PdfDocument {
        val order = orderWithItems.order
        val typeLabel = OrderType.entries.find { it.name == order.orderType }?.label ?: order.orderType
        val payLabel = PaymentMethod.entries.find { it.name == order.paymentMethod }?.label ?: order.paymentMethod

        val extraOptionLines = orderWithItems.items.count { it.optionsSummary.isNotBlank() }
        val pageWidth = 240
        val estimatedHeight = 320 + (orderWithItems.items.size * 34) + (extraOptionLines * 12) + (store.receiptFooter.lines().size * 16)
        val pageHeight = estimatedHeight.coerceAtLeast(390)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val centerBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val centerSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 8f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val leftText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }
        val leftSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 7.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }
        val leftBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val rightText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val rightBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val linePaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 0.8f
        }

        val margin = 12f
        val rightX = pageWidth - margin
        val centerX = pageWidth / 2f
        var y = 24f

        canvas.drawText(store.storeName.take(26), centerX, y, centerBold)
        y += 12f
        if (store.storeAddress.isNotBlank()) {
            canvas.drawText(store.storeAddress.take(36), centerX, y, centerSmall)
            y += 11f
        }
        if (store.storePhone.isNotBlank()) {
            canvas.drawText("Telp: ${store.storePhone}", centerX, y, centerSmall)
            y += 12f
        }
        canvas.drawLine(margin, y, rightX, y, linePaint)
        y += 13f

        canvas.drawText("No: ${order.orderNumber}", margin, y, leftText)
        canvas.drawText(Formatters.formatTime(order.createdAt), rightX, y, rightText)
        y += 12f
        canvas.drawText("Tgl: ${Formatters.formatShortDate(order.createdAt)} | Kasir: ${order.cashierName.take(12)}", margin, y, leftText)
        y += 12f
        canvas.drawText("Layanan: $typeLabel", margin, y, leftText)
        y += 12f
        if (order.customerName.isNotBlank()) {
            canvas.drawText("Pelanggan: ${order.customerName.take(22)}", margin, y, leftText)
            y += 12f
        }
        canvas.drawLine(margin, y, rightX, y, linePaint)
        y += 13f

        orderWithItems.items.forEach { item ->
            canvas.drawText(item.itemName.take(30), margin, y, leftText)
            y += 10f
            if (item.optionsSummary.isNotBlank()) {
                canvas.drawText("  ${item.optionsSummary.take(34)}", margin, y, leftSmall)
                y += 10f
            }
            canvas.drawText("${item.quantity} x ${Formatters.formatRupiah(item.unitPrice)}", margin + 6f, y, leftText)
            canvas.drawText(Formatters.formatRupiah(item.lineTotal), rightX, y, rightText)
            y += 13f
        }

        canvas.drawLine(margin, y, rightX, y, linePaint)
        y += 13f

        canvas.drawText("Subtotal", margin, y, leftText)
        canvas.drawText(Formatters.formatRupiah(order.subtotal), rightX, y, rightText)
        y += 12f

        if (order.discountAmount > 0) {
            canvas.drawText("Diskon", margin, y, leftText)
            canvas.drawText("-${Formatters.formatRupiah(order.discountAmount)}", rightX, y, rightText)
            y += 12f
        }
        if (order.taxAmount > 0) {
            canvas.drawText("Pajak", margin, y, leftText)
            canvas.drawText(Formatters.formatRupiah(order.taxAmount), rightX, y, rightText)
            y += 12f
        }

        canvas.drawText("TOTAL", margin, y, leftBold)
        canvas.drawText(Formatters.formatRupiah(order.totalAmount), rightX, y, rightBold)
        y += 14f

        canvas.drawText("Bayar ($payLabel)", margin, y, leftText)
        canvas.drawText(Formatters.formatRupiah(order.paidAmount), rightX, y, rightText)
        y += 12f

        if (order.changeAmount > 0) {
            canvas.drawText("Kembalian", margin, y, leftText)
            canvas.drawText(Formatters.formatRupiah(order.changeAmount), rightX, y, rightText)
            y += 12f
        }

        if (order.notes.isNotBlank()) {
            y += 4f
            canvas.drawText("Catatan: ${order.notes.take(28)}", margin, y, leftText)
            y += 12f
        }

        y += 4f
        canvas.drawLine(margin, y, rightX, y, linePaint)
        y += 14f

        store.receiptFooter.lines().forEach { line ->
            if (line.isNotBlank()) {
                canvas.drawText(line.take(36), centerX, y, centerSmall)
                y += 11f
            }
        }

        document.finishPage(page)
        return document
    }

    fun printReceipt(context: Context, orderWithItems: OrderWithItems, store: StoreProfile) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "Struk_${orderWithItems.order.orderNumber}"

        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }
                val pdf = createReceiptPdf(orderWithItems, store)
                try {
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        pdf.writeTo(out)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    pdf.close()
                }
            }
        }

        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }

    fun shareReceiptPdf(context: Context, orderWithItems: OrderWithItems, store: StoreProfile) {
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(dir, "Struk_${orderWithItems.order.orderNumber}.pdf")
        val pdf = createReceiptPdf(orderWithItems, store)
        try {
            FileOutputStream(file).use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Struk ${orderWithItems.order.orderNumber} - ${store.storeName}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Struk PDF"))
    }

    fun shareReceiptText(context: Context, orderWithItems: OrderWithItems, store: StoreProfile) {
        val text = buildPlainTextReceipt(orderWithItems, store)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Struk ${orderWithItems.order.orderNumber}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Struk Teks"))
    }
}
