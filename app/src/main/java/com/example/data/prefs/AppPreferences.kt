package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.OrderType
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kedaiku_prefs")

data class StoreProfile(
    val storeName: String = "KedaiKu Bistro & Kopi",
    val storeAddress: String = "Jl. Kemang Raya No. 28, Jakarta",
    val storePhone: String = "0812-3456-7890",
    val receiptFooter: String = "Terima kasih telah berkunjung!\nSelamat menikmati hidangan kami.",
    val taxTakeAwayPercent: Int = 10,
    val taxShopeePercent: Int = 10,
    val taxGrabPercent: Int = 10
) {
    fun taxPercentFor(orderType: OrderType): Int {
        return when (orderType) {
            OrderType.TAKE_AWAY -> taxTakeAwayPercent
            OrderType.ONLINE_SHOPEE -> taxShopeePercent
            OrderType.ONLINE_GRAB -> taxGrabPercent
        }
    }
}

data class AppPrefsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val storeProfile: StoreProfile = StoreProfile(),
    val autoBackupEnabled: Boolean = true,
    val lastAutoBackupTime: Long = 0L,
    val activeStaffId: Long = -1L,
    val draftCartJson: String = ""
)

class AppPreferences(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val STORE_NAME = stringPreferencesKey("store_name")
        val STORE_ADDRESS = stringPreferencesKey("store_address")
        val STORE_PHONE = stringPreferencesKey("store_phone")
        val RECEIPT_FOOTER = stringPreferencesKey("receipt_footer")
        val TAX_TAKE_AWAY_PERCENT = intPreferencesKey("tax_take_away_percent")
        val TAX_SHOPEE_PERCENT = intPreferencesKey("tax_shopee_percent")
        val TAX_GRAB_PERCENT = intPreferencesKey("tax_grab_percent")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val LAST_AUTO_BACKUP_TIME = longPreferencesKey("last_auto_backup_time")
        val ACTIVE_STAFF_ID = longPreferencesKey("active_staff_id")
        val DRAFT_CART_JSON = stringPreferencesKey("draft_cart_json")
    }

    val prefsFlow: Flow<AppPrefsState> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = ThemeMode.entries.find { it.name == themeStr } ?: ThemeMode.SYSTEM
        AppPrefsState(
            themeMode = themeMode,
            storeProfile = StoreProfile(
                storeName = prefs[Keys.STORE_NAME] ?: "KedaiKu Bistro & Kopi",
                storeAddress = prefs[Keys.STORE_ADDRESS] ?: "Jl. Kemang Raya No. 28, Jakarta",
                storePhone = prefs[Keys.STORE_PHONE] ?: "0812-3456-7890",
                receiptFooter = prefs[Keys.RECEIPT_FOOTER]
                    ?: "Terima kasih telah berkunjung!\nSelamat menikmati hidangan kami.",
                taxTakeAwayPercent = prefs[Keys.TAX_TAKE_AWAY_PERCENT] ?: 10,
                taxShopeePercent = prefs[Keys.TAX_SHOPEE_PERCENT] ?: 10,
                taxGrabPercent = prefs[Keys.TAX_GRAB_PERCENT] ?: 10
            ),
            autoBackupEnabled = prefs[Keys.AUTO_BACKUP_ENABLED] ?: true,
            lastAutoBackupTime = prefs[Keys.LAST_AUTO_BACKUP_TIME] ?: 0L,
            activeStaffId = prefs[Keys.ACTIVE_STAFF_ID] ?: -1L,
            draftCartJson = prefs[Keys.DRAFT_CART_JSON] ?: ""
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateStoreProfile(profile: StoreProfile) {
        context.dataStore.edit {
            it[Keys.STORE_NAME] = profile.storeName.trim().ifEmpty { "KedaiKu Bistro & Kopi" }
            it[Keys.STORE_ADDRESS] = profile.storeAddress.trim()
            it[Keys.STORE_PHONE] = profile.storePhone.trim()
            it[Keys.RECEIPT_FOOTER] = profile.receiptFooter.trim()
            it[Keys.TAX_TAKE_AWAY_PERCENT] = profile.taxTakeAwayPercent.coerceIn(0, 99)
            it[Keys.TAX_SHOPEE_PERCENT] = profile.taxShopeePercent.coerceIn(0, 99)
            it[Keys.TAX_GRAB_PERCENT] = profile.taxGrabPercent.coerceIn(0, 99)
        }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun updateLastAutoBackupTime(timestamp: Long) {
        context.dataStore.edit { it[Keys.LAST_AUTO_BACKUP_TIME] = timestamp }
    }

    suspend fun setActiveStaffId(staffId: Long) {
        context.dataStore.edit { it[Keys.ACTIVE_STAFF_ID] = staffId }
    }

    suspend fun saveDraftCartJson(json: String) {
        context.dataStore.edit { it[Keys.DRAFT_CART_JSON] = json }
    }
}
