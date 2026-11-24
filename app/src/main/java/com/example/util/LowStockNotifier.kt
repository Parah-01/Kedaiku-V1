package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.InventoryItemEntity

object LowStockNotifier {
    const val CHANNEL_ID = "low_stock_alerts"
    private const val NOTIFICATION_ID = 4102

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_low_stock_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_low_stock_desc)
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun notifyLowStock(context: Context, lowStockItems: List<InventoryItemEntity>): Boolean {
        if (lowStockItems.isEmpty()) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            return false
        }
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return false

        val outOfStockCount = lowStockItems.count { it.isOutOfStock }
        val lowCount = lowStockItems.size - outOfStockCount

        val title = when {
            outOfStockCount > 0 && lowCount > 0 ->
                "Peringatan Stok: $outOfStockCount Habis, $lowCount Menipis"
            outOfStockCount > 0 ->
                "Stok Habis ($outOfStockCount item perlu segera diisi)"
            else ->
                "Stok Menipis ($lowCount item mendekati batas)"
        }

        val summaryText = lowStockItems.take(5).joinToString(", ") {
            "${it.name} (${Formatters.formatQty(it.stockQty)} ${it.unit})"
        }

        val inboxStyle = NotificationCompat.InboxStyle().setBigContentTitle(title)
        lowStockItems.take(6).forEach { item ->
            val statusLabel = if (item.isOutOfStock) "HABIS" else "Sisa ${Formatters.formatQty(item.stockQty)} ${item.unit}"
            inboxStyle.addLine("• ${item.name}: $statusLabel (min ${Formatters.formatQty(item.minThreshold)})")
        }
        if (lowStockItems.size > 6) {
            inboxStyle.setSummaryText("+${lowStockItems.size - 6} item lainnya")
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
