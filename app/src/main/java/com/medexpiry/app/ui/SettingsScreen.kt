package com.medexpiry.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medexpiry.app.data.ColorStyle
import com.medexpiry.app.data.SortOrder
import com.medexpiry.app.data.ThemeMode
import com.medexpiry.app.data.UserPreferences
import com.medexpiry.app.util.AppUpdater

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onBack: () -> Unit
) {
    val settings by preferences.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle("Оформление")

            SettingCard {
                Text("Тема", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ThemeMode.SYSTEM to "Система",
                        ThemeMode.LIGHT to "Светлая",
                        ThemeMode.DARK to "Тёмная"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { preferences.update { it.copy(themeMode = mode) } },
                            label = { Text(label) }
                        )
                    }
                }
            }

            SettingCard {
                Text("Цветовой стиль", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ColorStyle.entries.forEach { style ->
                        val c = previewColor(style)
                        val selected = settings.colorStyle == style
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                )
                                .clickable { preferences.update { it.copy(colorStyle = style) } },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    colorStyleName(settings.colorStyle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionTitle("Список")

            SettingCard {
                Text("Сортировка", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                listOf(
                    SortOrder.BY_EXPIRY to "По сроку годности",
                    SortOrder.BY_NAME to "По названию",
                    SortOrder.BY_ADDED to "По дате добавления"
                ).forEach { (order, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { preferences.update { it.copy(sortOrder = order) } }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.sortOrder == order,
                            onClick = { preferences.update { it.copy(sortOrder = order) } }
                        )
                        Text(label)
                    }
                }
            }

            SettingCard {
                Text(
                    "Секция «Требуют внимания»",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Лекарства с остатком ≤ ${settings.urgentDays} дней",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = settings.urgentDays.toFloat(),
                    onValueChange = { v ->
                        preferences.update { it.copy(urgentDays = v.toInt().coerceIn(3, 90)) }
                    },
                    valueRange = 3f..90f,
                    steps = 28
                )
            }

            SettingCard {
                SwitchRow(
                    title = "Количество на карточке",
                    subtitle = "Показывать поле «количество»",
                    checked = settings.showQuantityOnCard,
                    onCheckedChange = { checked ->
                        preferences.update { it.copy(showQuantityOnCard = checked) }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SwitchRow(
                    title = "Компактные карточки",
                    subtitle = "Меньше отступы в списке",
                    checked = settings.compactCards,
                    onCheckedChange = { checked ->
                        preferences.update { it.copy(compactCards = checked) }
                    }
                )
            }

            SectionTitle("Уведомления")

            SettingCard {
                Text(
                    "За сколько дней напоминать",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                val options = listOf(30, 14, 7, 3, 1, 0)
                options.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { day ->
                            val selected = day in settings.notifyDays
                            val label = if (day == 0) "В день X" else "за $day дн."
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    preferences.update { s ->
                                        val next = s.notifyDays.toMutableSet()
                                        if (selected) next.remove(day) else next.add(day)
                                        if (next.isEmpty()) next.add(7)
                                        s.copy(notifyDays = next)
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            SectionTitle("Обновления")

            SettingCard {
                SwitchRow(
                    title = "Проверять обновления при запуске",
                    subtitle = "GitHub Releases",
                    checked = settings.autoCheckUpdates,
                    onCheckedChange = { checked ->
                        preferences.update { it.copy(autoCheckUpdates = checked) }
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Версия приложения: ${AppUpdater.currentVersionName(androidx.compose.ui.platform.LocalContext.current)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun previewColor(style: ColorStyle): Color = when (style) {
    ColorStyle.INDIGO -> Color(0xFF3F51B5)
    ColorStyle.GREEN -> Color(0xFF2E7D32)
    ColorStyle.TEAL -> Color(0xFF00897B)
    ColorStyle.PURPLE -> Color(0xFF7B1FA2)
    ColorStyle.ORANGE -> Color(0xFFEF6C00)
    ColorStyle.BLUE -> Color(0xFF1565C0)
}

private fun colorStyleName(style: ColorStyle): String = when (style) {
    ColorStyle.INDIGO -> "Индиго"
    ColorStyle.GREEN -> "Зелёный"
    ColorStyle.TEAL -> "Бирюзовый"
    ColorStyle.PURPLE -> "Фиолетовый"
    ColorStyle.ORANGE -> "Оранжевый"
    ColorStyle.BLUE -> "Синий"
}
