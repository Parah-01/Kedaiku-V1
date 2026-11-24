package com.example.util

import com.example.data.local.InventoryItemEntity
import com.example.data.local.MenuRecipeEntity
import com.example.ui.CartLineItem
import kotlin.math.floor
import kotlin.math.roundToLong

object RecipeStockCalculator {

    /**
     * Computes effective servable stock for an inventory item:
     * - If the item is a MENU with linked recipe ingredients, servable portions equal the minimum
     *   of floor(ingredient.stockQty / recipe.quantityRequired) across all required ingredients.
     * - Otherwise, returns the item's direct stockQty.
     */
    fun computeEffectiveStock(
        item: InventoryItemEntity,
        recipesByMenuId: Map<Long, List<MenuRecipeEntity>>,
        itemsById: Map<Long, InventoryItemEntity>
    ): Double {
        val recipes = recipesByMenuId[item.id].orEmpty()
        if (recipes.isEmpty()) return item.stockQty

        var minServable = Double.MAX_VALUE
        var hasValidRecipe = false
        for (recipe in recipes) {
            if (recipe.quantityRequired <= 0.0) continue
            val ingredient = itemsById[recipe.ingredientItemId] ?: continue
            hasValidRecipe = true
            val possible = floor(ingredient.stockQty / recipe.quantityRequired).coerceAtLeast(0.0)
            if (possible < minServable) {
                minServable = possible
            }
        }
        return if (hasValidRecipe) minServable else item.stockQty
    }

    /**
     * Calculates total recipe cost (HPP) per 1 portion of a menu item from its linked ingredients.
     */
    fun computeRecipeCostPrice(
        recipeLines: List<Pair<Long, Double>>,
        itemsById: Map<Long, InventoryItemEntity>
    ): Long {
        var total = 0.0
        for ((ingredientId, qtyRequired) in recipeLines) {
            if (qtyRequired <= 0.0) continue
            val ingredient = itemsById[ingredientId] ?: continue
            total += ingredient.costPrice.toDouble() * qtyRequired
        }
        return total.roundToLong().coerceAtLeast(0L)
    }

    /**
     * Computes how many additional portions of [targetMenuId] can still be added to the cart,
     * taking into account shared raw ingredients already reserved by items currently in [cartLines].
     */
    fun computeRemainingAddableQty(
        targetMenuId: Long,
        cartLines: List<CartLineItem>,
        itemsById: Map<Long, InventoryItemEntity>,
        recipesByMenuId: Map<Long, List<MenuRecipeEntity>>
    ): Int {
        val targetItem = itemsById[targetMenuId] ?: return 0
        val targetRecipes = recipesByMenuId[targetMenuId].orEmpty().filter { it.quantityRequired > 0.0 }
        val inCartQtyForTarget = cartLines.filter { it.itemId == targetMenuId }.sumOf { it.quantity }

        if (targetRecipes.isEmpty()) {
            val maxAllowed = targetItem.stockQty.toInt().coerceAtLeast(0)
            return (maxAllowed - inCartQtyForTarget).coerceAtLeast(0)
        }

        // Accumulate total ingredient consumption already reserved by all cart lines
        val reservedByIngredient = mutableMapOf<Long, Double>()
        for (line in cartLines) {
            val lineRecipes = recipesByMenuId[line.itemId].orEmpty()
            for (r in lineRecipes) {
                if (r.quantityRequired > 0.0) {
                    reservedByIngredient[r.ingredientItemId] =
                        (reservedByIngredient[r.ingredientItemId] ?: 0.0) + (r.quantityRequired * line.quantity)
                }
            }
        }

        var minRemaining = Int.MAX_VALUE
        for (recipe in targetRecipes) {
            val ingredient = itemsById[recipe.ingredientItemId] ?: return 0
            val reserved = reservedByIngredient[recipe.ingredientItemId] ?: 0.0
            val availableIngredientQty = (ingredient.stockQty - reserved).coerceAtLeast(0.0)
            val possible = floor(availableIngredientQty / recipe.quantityRequired).toInt().coerceAtLeast(0)
            if (possible < minRemaining) {
                minRemaining = possible
            }
        }
        return if (minRemaining == Int.MAX_VALUE) 0 else minRemaining
    }

    /**
     * Suggests candidate items whose name shares meaningful tokens (including common Indonesian/F&B
     * phonetic variants like matcha/macha, chocolate/coklat/cokelat) so owners can connect
     * e.g. "Powder Macha" <-> "Matcha Latte Artisan" with one tap.
     */
    fun suggestMatchingItems(
        sourceName: String,
        candidates: List<InventoryItemEntity>
    ): List<InventoryItemEntity> {
        val sourceTokens = tokenizeForMatch(sourceName)
        if (sourceTokens.isEmpty()) return emptyList()
        return candidates.filter { candidate ->
            val candidateTokens = tokenizeForMatch(candidate.name)
            sourceTokens.any { st ->
                candidateTokens.any { ct ->
                    st == ct || (st.length >= 4 && ct.length >= 4 && (st.contains(ct) || ct.contains(st)))
                }
            }
        }
    }

    private val stopWords = setOf(
        "bubuk", "powder", "sirup", "syrup", "saus", "sauce", "biji", "daun",
        "segar", "fresh", "blend", "premium", "artisan", "dingin", "panas", "ice", "hot",
        "latte", "tea", "kopi", "coffee", "es", "ayam", "nasi", "goreng", "full", "cream"
    )

    private fun tokenizeForMatch(raw: String): Set<String> {
        val normalized = raw.lowercase()
            .replace("macha", "matcha")
            .replace("cokelat", "coklat")
            .replace("chocolate", "coklat")
            .replace("choco", "coklat")
            .replace("stroberi", "strawberry")
            .replace("karamel", "caramel")
            .replace("vanila", "vanilla")
        return normalized.split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 && it !in stopWords }
            .toSet()
    }
}
