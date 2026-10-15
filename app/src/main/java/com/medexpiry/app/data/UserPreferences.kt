package com.medexpiry.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class ColorStyle {
    INDIGO, GREEN, TEAL, PURPLE, ORANGE, BLUE,
    RED, PINK, CYAN, AMBER, MINT, CORAL, BROWN, SLATE
}

enum class SortOrder {
    BY_EXPIRY, BY_NAME, BY_ADDED
}

enum class TextSizeOption {
    SMALL, NORMAL, LARGE
}

enum class CardStyle {
    ROUNDED,   // сильно скруглённые
    SOFT,      // обычные
    SQUARE     // почти квадратные
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorStyle: ColorStyle = ColorStyle.INDIGO,
    val sortOrder: SortOrder = SortOrder.BY_EXPIRY,
    val urgentDays: Int = 30,
    val notifyDays: Set<Int> = setOf(30, 14, 7, 3, 1, 0),
    val autoCheckUpdates: Boolean = true,
    val showQuantityOnCard: Boolean = true,
    val compactCards: Boolean = false,
    // новые (значения по умолчанию = прежнее поведение)
    val textSize: TextSizeOption = TextSizeOption.NORMAL,
    val showImages: Boolean = true,
    val showNotesOnCard: Boolean = false,
    val groupByUrgency: Boolean = true,
    val cardStyle: CardStyle = CardStyle.SOFT,
    /** ARGB цвет; 0 = использовать colorStyle */
    val customPrimaryColor: Int = 0,
    // Дополнительные параметры списка; значения по умолчанию сохраняют прежнее поведение
    val searchNotes: Boolean = true,
    val showExpiredMedicines: Boolean = true,
    val showMedicineCount: Boolean = true
)

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        save(next)
        _settings.value = next
    }

    private fun load(): AppSettings {
        fun <T : Enum<T>> enumOr(key: String, fallback: T, parser: (String) -> T): T {
            return try {
                val s = prefs.getString(key, null) ?: return fallback
                parser(s)
            } catch (_: Exception) {
                fallback
            }
        }

        val notifyStr = prefs.getString(KEY_NOTIFY, "30,14,7,3,1,0") ?: "30,14,7,3,1,0"
        val notify = notifyStr.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
            .ifEmpty { setOf(30, 14, 7, 3, 1, 0) }

        return AppSettings(
            themeMode = enumOr(KEY_THEME, ThemeMode.SYSTEM) { ThemeMode.valueOf(it) },
            colorStyle = enumOr(KEY_COLOR, ColorStyle.INDIGO) { ColorStyle.valueOf(it) },
            sortOrder = enumOr(KEY_SORT, SortOrder.BY_EXPIRY) { SortOrder.valueOf(it) },
            urgentDays = prefs.getInt(KEY_URGENT, 30).coerceIn(1, 365),
            notifyDays = notify,
            autoCheckUpdates = prefs.getBoolean(KEY_AUTO_UPDATE, true),
            showQuantityOnCard = prefs.getBoolean(KEY_SHOW_QTY, true),
            compactCards = prefs.getBoolean(KEY_COMPACT, false),
            textSize = enumOr(KEY_TEXT, TextSizeOption.NORMAL) { TextSizeOption.valueOf(it) },
            showImages = prefs.getBoolean(KEY_IMAGES, true),
            showNotesOnCard = prefs.getBoolean(KEY_NOTES, false),
            groupByUrgency = prefs.getBoolean(KEY_GROUP, true),
            cardStyle = enumOr(KEY_CARD, CardStyle.SOFT) { CardStyle.valueOf(it) },
            customPrimaryColor = prefs.getInt(KEY_CUSTOM_COLOR, 0),
            searchNotes = prefs.getBoolean(KEY_SEARCH_NOTES, true),
            showExpiredMedicines = prefs.getBoolean(KEY_SHOW_EXPIRED, true),
            showMedicineCount = prefs.getBoolean(KEY_SHOW_COUNT, true)
        )
    }

    private fun save(s: AppSettings) {
        prefs.edit()
            .putString(KEY_THEME, s.themeMode.name)
            .putString(KEY_COLOR, s.colorStyle.name)
            .putString(KEY_SORT, s.sortOrder.name)
            .putInt(KEY_URGENT, s.urgentDays)
            .putString(KEY_NOTIFY, s.notifyDays.sortedDescending().joinToString(","))
            .putBoolean(KEY_AUTO_UPDATE, s.autoCheckUpdates)
            .putBoolean(KEY_SHOW_QTY, s.showQuantityOnCard)
            .putBoolean(KEY_COMPACT, s.compactCards)
            .putString(KEY_TEXT, s.textSize.name)
            .putBoolean(KEY_IMAGES, s.showImages)
            .putBoolean(KEY_NOTES, s.showNotesOnCard)
            .putBoolean(KEY_GROUP, s.groupByUrgency)
            .putString(KEY_CARD, s.cardStyle.name)
            .putInt(KEY_CUSTOM_COLOR, s.customPrimaryColor)
            .putBoolean(KEY_SEARCH_NOTES, s.searchNotes)
            .putBoolean(KEY_SHOW_EXPIRED, s.showExpiredMedicines)
            .putBoolean(KEY_SHOW_COUNT, s.showMedicineCount)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "aptechka_settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_COLOR = "color_style"
        private const val KEY_SORT = "sort_order"
        private const val KEY_URGENT = "urgent_days"
        private const val KEY_NOTIFY = "notify_days"
        private const val KEY_AUTO_UPDATE = "auto_check_updates"
        private const val KEY_SHOW_QTY = "show_quantity"
        private const val KEY_COMPACT = "compact_cards"
        private const val KEY_TEXT = "text_size"
        private const val KEY_IMAGES = "show_images"
        private const val KEY_NOTES = "show_notes"
        private const val KEY_GROUP = "group_urgency"
        private const val KEY_CARD = "card_style"
        private const val KEY_CUSTOM_COLOR = "custom_primary"
        private const val KEY_SEARCH_NOTES = "search_notes"
        private const val KEY_SHOW_EXPIRED = "show_expired_medicines"
        private const val KEY_SHOW_COUNT = "show_medicine_count"

        @Volatile private var instance: UserPreferences? = null

        fun get(context: Context): UserPreferences {
            return instance ?: synchronized(this) {
                instance ?: UserPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
