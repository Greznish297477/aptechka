package com.medexpiry.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class ColorStyle {
    INDIGO,   // индиго + бирюза
    GREEN,    // медицинский зелёный
    TEAL,     // бирюзовый
    PURPLE,   // фиолетовый
    ORANGE,   // тёплый оранжевый
    BLUE      // классический синий
}

enum class SortOrder {
    BY_EXPIRY,   // по сроку годности
    BY_NAME,     // по названию
    BY_ADDED     // по дате добавления
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorStyle: ColorStyle = ColorStyle.INDIGO,
    val sortOrder: SortOrder = SortOrder.BY_EXPIRY,
    val urgentDays: Int = 30,           // «требуют внимания» если ≤ N дней
    val notifyDays: Set<Int> = setOf(30, 14, 7, 3, 1, 0),
    val autoCheckUpdates: Boolean = true,
    val showQuantityOnCard: Boolean = true,
    val compactCards: Boolean = false
)

/**
 * Настройки пользователя (SharedPreferences + StateFlow для Compose).
 */
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
        val theme = try {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!)
        } catch (_: Exception) { ThemeMode.SYSTEM }

        val color = try {
            ColorStyle.valueOf(prefs.getString(KEY_COLOR, ColorStyle.INDIGO.name)!!)
        } catch (_: Exception) { ColorStyle.INDIGO }

        val sort = try {
            SortOrder.valueOf(prefs.getString(KEY_SORT, SortOrder.BY_EXPIRY.name)!!)
        } catch (_: Exception) { SortOrder.BY_EXPIRY }

        val notifyStr = prefs.getString(KEY_NOTIFY, "30,14,7,3,1,0") ?: "30,14,7,3,1,0"
        val notify = notifyStr.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
            .ifEmpty { setOf(30, 14, 7, 3, 1, 0) }

        return AppSettings(
            themeMode = theme,
            colorStyle = color,
            sortOrder = sort,
            urgentDays = prefs.getInt(KEY_URGENT, 30).coerceIn(1, 365),
            notifyDays = notify,
            autoCheckUpdates = prefs.getBoolean(KEY_AUTO_UPDATE, true),
            showQuantityOnCard = prefs.getBoolean(KEY_SHOW_QTY, true),
            compactCards = prefs.getBoolean(KEY_COMPACT, false)
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

        @Volatile private var instance: UserPreferences? = null

        fun get(context: Context): UserPreferences {
            return instance ?: synchronized(this) {
                instance ?: UserPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
