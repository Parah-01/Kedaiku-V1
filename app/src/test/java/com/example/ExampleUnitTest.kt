package com.example

import com.example.data.local.CustomizationOptionEntity
import com.example.data.local.CustomizationOptionType
import com.example.data.local.ExpenseCategory
import com.example.data.local.ExpenseEntity
import com.example.data.local.InventoryItemEntity
import com.example.data.local.InventoryType
import com.example.data.local.MenuRecipeEntity
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
import com.example.util.RecipeStockCalculator
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
    fun storeProfile_resolvesTaxUpTo99PercentAndAllowedEnumsOnly() {
        assertEquals(
            listOf(OrderType.TAKE_AWAY, OrderType.ONLINE_SHOPEE, OrderType.ONLINE_GRAB),
            OrderType.entries
        )
        assertEquals(listOf(PaymentMethod.TUNAI, PaymentMethod.QRIS), PaymentMethod.entries)

        val profile = StoreProfile(
            taxTakeAwayPercent = 10,
            taxShopeePercent = 50,
            taxGrabPercent = 99
        )
        assertEquals(10, profile.taxPercentFor(OrderType.TAKE_AWAY))
        assertEquals(50, profile.taxPercentFor(OrderType.ONLINE_SHOPEE))
        assertEquals(99, profile.taxPercentFor(OrderType.ONLINE_GRAB))
    }

    @Test
    fun recipeStockCalculator_computesEffectiveStockSharedCartAndPhoneticMatch() {
        val matchaMenu = InventoryItemEntity(
            id = 1L,
            sku = "MIN-003",
            name = "Matcha Latte Artisan",
            category = "Kopi & Minuman",
            itemType = InventoryType.MENU.name,
            price = 28000L,
            costPrice = 10000L,
            stockQty = 20.0,
            minThreshold = 5.0,
            unit = "cup"
        )
        val kopiSusuMenu = InventoryItemEntity(
            id = 2L,
            sku = "KOP-001",
            name = "Kopi Susu Gula Aren",
            category = "Kopi & Minuman",
            itemType = InventoryType.MENU.name,
            price = 24000L,
            costPrice = 8500L,
            stockQty = 35.0,
            minThreshold = 8.0,
            unit = "cup"
        )
        val powderMacha = InventoryItemEntity(
            id = 10L,
            sku = "BHN-004",
            name = "Powder Macha",
            category = "Bahan Baku",
            itemType = InventoryType.INGREDIENT.name,
            price = 0L,
            costPrice = 350L,
            stockQty = 150.0, // enough for 10 cups @ 15g/cup
            minThreshold = 30.0,
            unit = "gr"
        )
        val milk = InventoryItemEntity(
            id = 11L,
            sku = "BHN-002",
            name = "Susu UHT Full Cream",
            category = "Bahan Baku",
            itemType = InventoryType.INGREDIENT.name,
            price = 0L,
            costPrice = 20L,
            stockQty = 1000.0, // shared: 150ml/cup -> max 6 cups total across both menus
            minThreshold = 300.0,
            unit = "ml"
        )

        val recipes = listOf(
            MenuRecipeEntity(menuItemId = 1L, ingredientItemId = 10L, quantityRequired = 15.0),
            MenuRecipeEntity(menuItemId = 1L, ingredientItemId = 11L, quantityRequired = 150.0),
            MenuRecipeEntity(menuItemId = 2L, ingredientItemId = 11L, quantityRequired = 150.0)
        )
        val itemsById = listOf(matchaMenu, kopiSusuMenu, powderMacha, milk).associateBy { it.id }
        val recipesByMenuId = recipes.groupBy { it.menuItemId }

        // Effective stock limited by shared milk (floor(1000 / 150) = 6)
        assertEquals(6.0, RecipeStockCalculator.computeEffectiveStock(matchaMenu, recipesByMenuId, itemsById), 0.001)

        // Recipe HPP = (15 * 350) + (150 * 20) = 5250 + 3000 = 8250
        assertEquals(
            8250L,
            RecipeStockCalculator.computeRecipeCostPrice(
                listOf(10L to 15.0, 11L to 150.0),
                itemsById
            )
        )

        // If 4 cups of Kopi Susu are already in cart (consuming 600ml milk), only 400ml milk remains -> 2 Matcha Latte left
        val cart = listOf(
            CartLineItem(
                lineKey = "2|Regular",
                itemId = 2L,
                itemName = kopiSusuMenu.name,
                basePrice = kopiSusuMenu.price,
                unitPrice = kopiSusuMenu.price,
                quantity = 4,
                optionsSummary = "Regular"
            )
        )
        assertEquals(
            2,
            RecipeStockCalculator.computeRemainingAddableQty(
                targetMenuId = 1L,
                cartLines = cart,
                itemsById = itemsById,
                recipesByMenuId = recipesByMenuId
            )
        )

        // Phonetic matching: "Powder Macha" automatically suggests "Matcha Latte Artisan"
        val suggested = RecipeStockCalculator.suggestMatchingItems(
            sourceName = powderMacha.name,
            candidates = listOf(matchaMenu, kopiSusuMenu)
        )
        assertEquals(listOf(matchaMenu), suggested)
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
