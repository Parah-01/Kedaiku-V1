package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object Formatters {
    private val localeId = Locale("in", "ID")
    private val currencyFormat = NumberFormat.getNumberInstance(localeId)
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", localeId)
    private val shortTimeFormat = SimpleDateFormat("HH:mm", localeId)
    private val shortDateFormat = SimpleDateFormat("dd MMM", localeId)
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun formatRupiah(amount: Long): String {
        val sign = if (amount < 0) "-" else ""
        return "${sign}Rp ${currencyFormat.format(abs(amount))}"
    }

    fun formatCompactRupiah(amount: Long): String {
        val absVal = abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            absVal >= 1_000_000_000 -> "${sign}Rp %.1f M".format(localeId, absVal / 1_000_000_000.0)
            absVal >= 1_000_000 -> "${sign}Rp %.1f jt".format(localeId, absVal / 1_000_000.0)
            absVal >= 1_000 -> "${sign}Rp ${absVal / 1_000} rb"
            else -> formatRupiah(amount)
        }
    }

    fun formatQty(qty: Double): String {
        return if (qty % 1.0 == 0.0) {
            qty.toLong().toString()
        } else {
            String.format(localeId, "%.1f", qty)
        }
    }

    fun formatDateTime(timestamp: Long): String {
        if (timestamp <= 0L) return "-"
        return dateTimeFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String = shortTimeFormat.format(Date(timestamp))

    fun formatShortDate(timestamp: Long): String = shortDateFormat.format(Date(timestamp))

    fun formatFileTimestamp(timestamp: Long = System.currentTimeMillis()): String =
        fileTimestampFormat.format(Date(timestamp))
}
