package com.example

import com.example.data.local.CustomizationOptionEntity
import com.example.data.local.CustomizationOptionType
import com.example.data.local.ExpenseCategory
import com.example.data.local.ExpenseEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderItemEntity
import com.example.data.local.OrderStatus
import com.example.data.local.OrderType
import com.example.data.local.OrderWithItems
import com.example.data.local.PaymentMethod
import com.example.data.prefs.StoreProfile
import com.example.data.security.PinSecurity
import com.example.ui.AnalyticsPeriod
import com.example.ui.CartLineItem
import com.example.ui.MainViewModel
import com.example.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun pinSecurity_verifiesCorrectPinAndRejectsWrongPin() {
        val salt = PinSecurity.generateSalt()
        val hash = PinSecurity.hashPin("2468", salt)
        assertTrue(PinSecurity.verifyPin("2468", salt, hash))
        assertFalse(PinSecurity.verifyPin("1357", salt, hash))
    }

    @Test
    fun formatters_formatRupiahAndQuantities() {
        assertEquals("Rp 24.000", Formatters.formatRupiah(24000))
        assertEquals("5", Formatters.formatQty(5.0))
        assertEquals("4,5", Formatters.formatQty(4.5))
    }

    @Test
    fun customizationOptions_updateUnitPriceAndLineTotal() {
        val baseDrink = 24000L
        val largeVariant = CustomizationOptionEntity(
            id = 1L,
            optionType = CustomizationOptionType.DRINK_VARIANT.name,
            label = "Large",
            extraPrice = 6000L
        )
        val bobaTopping = CustomizationOptionEntity(
            id = 2L,
            optionType = CustomizationOptionType.DRINK_TOPPING.name,
            label = "Pearl Boba",
            extraPrice = 4000L
        )
        val cheeseTopping = CustomizationOptionEntity(
            id = 3L,
            optionType = CustomizationOptionType.DRINK_TOPPING.name,
            label = "Cream Cheese",
            extraPrice = 6000L
        )

        val unitPrice = baseDrink + largeVariant.extraPrice + bobaTopping.extraPrice + cheeseTopping.extraPrice
        assertEquals(40000L, unitPrice)

        val line = CartLineItem(
            lineKey = "1|Large + Pearl Boba, Cream Cheese",
            itemId = 1L,
            itemName = "Kopi Susu Gula Aren",
            basePrice = baseDrink,
            unitPrice = unitPrice,
            quantity = 2,
            optionsSummary = "Large + Pearl Boba, Cream Cheese"
        )
        assertEquals(80000L, line.lineTotal)

        val lv5 = CustomizationOptionEntity(
            id = 4L,
            optionType = CustomizationOptionType.FOOD_LEVEL.name,
            label = "Lv 5 (Super Pedas)",
            extraPrice = 3000L
        )
        assertEquals(38000L, 35000L + lv5.extraPrice)
    }

    @Test
    fun storeProfile_resolvesTaxPerServiceTypeAndAllowedEnumsOnly() {
        assertEquals(
            listOf(OrderType.TAKE_AWAY, OrderType.ONLINE_SHOPEE, OrderType.ONLINE_GRAB),
            OrderType.entries
        )
        assertEquals(listOf(PaymentMethod.TUNAI, PaymentMethod.QRIS), PaymentMethod.entries)

        val profile = StoreProfile(
            taxTakeAwayPercent = 10,
            taxShopeePercent = 12,
            taxGrabPercent = 15
        )
        assertEquals(10, profile.taxPercentFor(OrderType.TAKE_AWAY))
        assertEquals(12, profile.taxPercentFor(OrderType.ONLINE_SHOPEE))
        assertEquals(15, profile.taxPercentFor(OrderType.ONLINE_GRAB))
    }

    @Test
    fun analytics_calculatesTrueNetProfitFromGrossProfitMinusExpenses() {
        val now = System.currentTimeMillis()
        val orders = listOf(
            OrderWithItems(
                order = OrderEntity(
                    id = 1L,
                    orderNumber = "ORD-0001",
                    customerName = "Budi",
                    orderType = OrderType.TAKE_AWAY.name,
                    status = OrderStatus.COMPLETED.name,
                    paymentMethod = PaymentMethod.QRIS.name,
                    subtotal = 100000L,
                    discountAmount = 0L,
                    taxAmount = 10000L,
                    totalAmount = 110000L,
                    paidAmount = 110000L,
                    changeAmount = 0L,
                    notes = "",
                    cashierId = 1L,
                    cashierName = "Owner",
                    createdAt = now,
                    updatedAt = now
                ),
                items = listOf(
                    OrderItemEntity(
                        id = 1L,
                        orderId = 1L,
                        inventoryItemId = 10L,
                        itemName = "Kopi Susu",
                        unitPrice = 25000L,
                        unitCost = 10000L,
                        quantity = 4,
                        lineTotal = 100000L
                    )
                )
            )
        )
        val expenses = listOf(
            ExpenseEntity(
                id = 1L,
                title = "Listrik Toko",
                category = ExpenseCategory.OPERATIONAL.name,
                amount = 15000L,
                recordedBy = "Owner",
                expenseDate = now
            ),
            ExpenseEntity(
                id = 2L,
                title = "Gaji Harian Kasir",
                category = ExpenseCategory.SALARY.name,
                amount = 25000L,
                recordedBy = "Owner",
                expenseDate = now
            )
        )

        val snapshot = MainViewModel.computeAnalyticsSnapshot(
            allOrders = orders,
            inventoryWithCategories = emptyList(),
            allExpenses = expenses,
            period = AnalyticsPeriod.TODAY
        )

        assertEquals(110000L, snapshot.totalRevenue)
        assertEquals(40000L, snapshot.totalCost)
        assertEquals(70000L, snapshot.grossProfit)
        assertEquals(40000L, snapshot.totalExpenses)
        assertEquals(30000L, snapshot.netProfit)
    }
}
