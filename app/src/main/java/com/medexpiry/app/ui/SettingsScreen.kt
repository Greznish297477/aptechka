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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medexpiry.app.data.CardStyle
import com.medexpiry.app.data.ColorStyle
import com.medexpiry.app.data.SortOrder
import com.medexpiry.app.data.TextSizeOption
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
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Настройки", fontWeight = FontWeight.Bold)
                        Text(
                            "Персонализация приложения",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                },
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
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // —— Цвета (сразу сверху, заметно) ——
            SectionTitle("Цвет приложения")
            SettingCard {
                Text(
                    colorStyleName(settings.colorStyle),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(14.dp))
                ColorGrid(
                    selected = settings.colorStyle,
                    onSelect = { style -> preferences.update { it.copy(colorStyle = style) } }
                )
            }

            SectionTitle("Оформление")
            SettingCard {
                Label("Тема")
                Spacer(Modifier.height(8.dp))
                SegmentedRow {
                    listOf(
                        ThemeMode.SYSTEM to "Система",
                        ThemeMode.LIGHT to "Светлая",
                        ThemeMode.DARK to "Тёмная"
                    ).forEach { (mode, label) ->
                        SegmentChip(
                            selected = settings.themeMode == mode,
                            label = label,
                            onClick = { preferences.update { it.copy(themeMode = mode) } }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Label("Размер текста")
                Spacer(Modifier.height(8.dp))
                SegmentedRow {
                    listOf(
                        TextSizeOption.SMALL to "Мелкий",
                        TextSizeOption.NORMAL to "Обычный",
                        TextSizeOption.LARGE to "Крупный"
                    ).forEach { (opt, label) ->
                        SegmentChip(
                            selected = settings.textSize == opt,
                            label = label,
                            onClick = { preferences.update { it.copy(textSize = opt) } }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Label("Форма карточек")
                Spacer(Modifier.height(8.dp))
                SegmentedRow {
                    listOf(
                        CardStyle.ROUNDED to "Круглые",
                        CardStyle.SOFT to "Мягкие",
                        CardStyle.SQUARE to "Прямые"
                    ).forEach { (style, label) ->
                        SegmentChip(
                            selected = settings.cardStyle == style,
                            label = label,
                            onClick = { preferences.update { it.copy(cardStyle = style) } }
                        )
                    }
                }
            }

            SectionTitle("Список")
            SettingCard {
                Label("Сортировка")
                Spacer(Modifier.height(4.dp))
                listOf(
                    SortOrder.BY_EXPIRY to "По сроку годности",
                    SortOrder.BY_NAME to "По названию",
                    SortOrder.BY_ADDED to "По дате добавления"
                ).forEach { (order, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { preferences.update { it.copy(sortOrder = order) } }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.sortOrder == order,
                            onClick = { preferences.update { it.copy(sortOrder = order) } }
                        )
                        Text(label)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    "«Требуют внимания» — до ${settings.urgentDays} дн.",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
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
                SwitchRow("Группировать по срочности", "Блоки «внимание» и «в порядке»",
                    settings.groupByUrgency) {
                    preferences.update { s -> s.copy(groupByUrgency = it) }
                }
                SoftDivider()
                SwitchRow("Картинки на карточках", "Фото лекарства в списке",
                    settings.showImages) {
                    preferences.update { s -> s.copy(showImages = it) }
                }
                SoftDivider()
                SwitchRow("Количество на карточке", null, settings.showQuantityOnCard) {
                    preferences.update { s -> s.copy(showQuantityOnCard = it) }
                }
                SoftDivider()
                SwitchRow("Заметки на карточке", "Короткий текст в списке",
                    settings.showNotesOnCard) {
                    preferences.update { s -> s.copy(showNotesOnCard = it) }
                }
                SoftDivider()
                SwitchRow("Компактные карточки", "Меньше отступы",
                    settings.compactCards) {
                    preferences.update { s -> s.copy(compactCards = it) }
                }
            }

            SectionTitle("Уведомления")
            SettingCard {
                Text(
                    "За сколько дней напоминать",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
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
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }

            SectionTitle("Обновления")
            SettingCard {
                SwitchRow("Проверять при запуске", "GitHub Releases",
                    settings.autoCheckUpdates) {
                    preferences.update { s -> s.copy(autoCheckUpdates = it) }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Версия ${AppUpdater.currentVersionName(context)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ColorGrid(
    selected: ColorStyle,
    onSelect: (ColorStyle) -> Unit
) {
    val styles = ColorStyle.entries
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        styles.chunked(7).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { style ->
                    val c = previewColor(style)
                    val isSelected = selected == style
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (isSelected)
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else
                                        Modifier.border(1.5.dp, Color.Black.copy(alpha = 0.08f), CircleShape)
                                )
                                .clickable { onSelect(style) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            colorStyleShort(style),
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
                // pad incomplete rows
                repeat(7 - row.size) {
                    Spacer(Modifier.size(40.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
    )
}

@Composable
private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SegmentedRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun RowScope.SegmentChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                label,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge
            )
        },
        modifier = Modifier.weight(1f)
    )
}

@Composable
private fun SoftDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 10.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!subtitle.isNullOrBlank()) {
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
    ColorStyle.RED -> Color(0xFFC62828)
    ColorStyle.PINK -> Color(0xFFC2185B)
    ColorStyle.CYAN -> Color(0xFF00838F)
    ColorStyle.AMBER -> Color(0xFFFF8F00)
    ColorStyle.MINT -> Color(0xFF26A69A)
    ColorStyle.CORAL -> Color(0xFFE64A19)
    ColorStyle.BROWN -> Color(0xFF6D4C41)
    ColorStyle.SLATE -> Color(0xFF455A64)
}

private fun colorStyleName(style: ColorStyle): String = when (style) {
    ColorStyle.INDIGO -> "Индиго"
    ColorStyle.GREEN -> "Зелёный"
    ColorStyle.TEAL -> "Бирюзовый"
    ColorStyle.PURPLE -> "Фиолетовый"
    ColorStyle.ORANGE -> "Оранжевый"
    ColorStyle.BLUE -> "Синий"
    ColorStyle.RED -> "Красный"
    ColorStyle.PINK -> "Розовый"
    ColorStyle.CYAN -> "Голубой"
    ColorStyle.AMBER -> "Янтарный"
    ColorStyle.MINT -> "Мятный"
    ColorStyle.CORAL -> "Коралловый"
    ColorStyle.BROWN -> "Коричневый"
    ColorStyle.SLATE -> "Серый"
}

private fun colorStyleShort(style: ColorStyle): String = when (style) {
    ColorStyle.INDIGO -> "индиго"
    ColorStyle.GREEN -> "зел."
    ColorStyle.TEAL -> "бирюз."
    ColorStyle.PURPLE -> "фиол."
    ColorStyle.ORANGE -> "оранж."
    ColorStyle.BLUE -> "син."
    ColorStyle.RED -> "красн."
    ColorStyle.PINK -> "роз."
    ColorStyle.CYAN -> "голуб."
    ColorStyle.AMBER -> "янт."
    ColorStyle.MINT -> "мята"
    ColorStyle.CORAL -> "коралл"
    ColorStyle.BROWN -> "корич."
    ColorStyle.SLATE -> "серый"
}
