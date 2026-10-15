package com.medexpiry.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

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
                    containerColor = if (settings.liquidGlass) MaterialTheme.colorScheme.primary.copy(alpha = 0.78f) else MaterialTheme.colorScheme.primary,
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
            // ========== ЦВЕТ ==========
            SectionTitle("Цвет приложения")
            SettingCard {
                val preview = if (settings.customPrimaryColor != 0) {
                    Color(settings.customPrimaryColor)
                } else {
                    previewColor(settings.colorStyle)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(preview)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (settings.customPrimaryColor != 0) "Свой цвет"
                        else colorStyleName(settings.colorStyle),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Круговая палитра",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                ColorWheelPicker(
                    selected = preview,
                    onColorSelected = { c ->
                        preferences.update {
                            it.copy(customPrimaryColor = c.toArgbCompat())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )

                Spacer(Modifier.height(18.dp))
                Text(
                    "Цвета темы",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                ColorDotsRow(
                    colors = themePresetColors(),
                    selected = if (settings.customPrimaryColor == 0) preview else null,
                    onSelect = { _, style ->
                        if (style != null) {
                            preferences.update {
                                it.copy(customPrimaryColor = 0, colorStyle = style)
                            }
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    "Стандартные цвета",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                ColorDotsRow(
                    colors = standardColors().map { it to null },
                    selected = if (settings.customPrimaryColor != 0) preview else null,
                    onSelect = { c, _ ->
                        preferences.update { it.copy(customPrimaryColor = c.toArgbCompat()) }
                    }
                )
            }

            // ========== ОФОРМЛЕНИЕ ==========
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
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    "Liquid Glass",
                    "Стеклянные полупрозрачные карточки и мягкий градиентный фон",
                    settings.liquidGlass
                ) { enabled -> preferences.update { it.copy(liquidGlass = enabled) } }
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
                SoftDivider()
                SwitchRow("Искать в заметках", "Учитывать заметки при поиске лекарства",
                    settings.searchNotes) {
                    preferences.update { s -> s.copy(searchNotes = it) }
                }
                SoftDivider()
                SwitchRow("Показывать просроченные", "Можно отключить, чтобы скрыть их из списка",
                    settings.showExpiredMedicines) {
                    preferences.update { s -> s.copy(showExpiredMedicines = it) }
                }
                SoftDivider()
                SwitchRow("Счётчик препаратов", "Количество в заголовке главного экрана",
                    settings.showMedicineCount) {
                    preferences.update { s -> s.copy(showMedicineCount = it) }
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

/** HSV-колесо: по углу — оттенок, по радиусу — насыщенность */
@Composable
private fun ColorWheelPicker(
    selected: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var brightness by remember { mutableFloatStateOf(0.92f) }
    val density = LocalDensity.current

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(brightness) {
                        fun pick(pos: Offset) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val rMax = min(cx, cy) * 0.92f
                            val dx = pos.x - cx
                            val dy = pos.y - cy
                            val dist = hypot(dx, dy).coerceAtMost(rMax)
                            if (dist < 4f) return
                            var angle = atan2(dy, dx)
                            if (angle < 0) angle += (Math.PI * 2).toFloat()
                            val hue = Math.toDegrees(angle.toDouble()).toFloat()
                            val sat = (dist / rMax).coerceIn(0f, 1f)
                            onColorSelected(Color.hsv(hue, sat, brightness))
                        }
                        detectDragGestures(
                            onDragStart = { pick(it) },
                            onDrag = { change, _ ->
                                change.consume()
                                pick(change.position)
                            }
                        )
                    }
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val rMax = min(cx, cy) * 0.92f
                // Рисуем кольцами HSV
                val steps = 72
                for (i in 0 until steps) {
                    val h0 = i * 360f / steps
                    val h1 = (i + 1) * 360f / steps
                    val rad0 = Math.toRadians(h0.toDouble()).toFloat()
                    val rad1 = Math.toRadians(h1.toDouble()).toFloat()
                    // несколько колец по насыщенности
                    val rings = 12
                    for (r in 0 until rings) {
                        val sat = (r + 1f) / rings
                        val rInner = rMax * r / rings
                        val rOuter = rMax * (r + 1) / rings
                        val color = Color.hsv((h0 + h1) / 2f, sat, brightness)
                        drawArc(
                            color = color,
                            startAngle = Math.toDegrees(rad0.toDouble()).toFloat(),
                            sweepAngle = Math.toDegrees((rad1 - rad0).toDouble()).toFloat(),
                            useCenter = true,
                            topLeft = Offset(cx - rOuter, cy - rOuter),
                            size = androidx.compose.ui.geometry.Size(rOuter * 2, rOuter * 2)
                        )
                        if (rInner > 0) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.0f),
                                radius = rInner,
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }
                // центр — белый/серый
                drawCircle(Color(0xFFF5F5F5), radius = rMax * 0.08f, center = Offset(cx, cy))
                drawCircle(
                    Color.Black.copy(alpha = 0.12f),
                    radius = rMax,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Яркость", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = brightness,
            onValueChange = { brightness = it },
            valueRange = 0.25f..1f,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun ColorDotsRow(
    colors: List<Pair<Color, ColorStyle?>>,
    selected: Color?,
    onSelect: (Color, ColorStyle?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.chunked(7).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (c, style) ->
                    val isSel = selected != null && colorsClose(selected, c)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(
                                if (isSel) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier.border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
                            )
                            .clickable { onSelect(c, style) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSel) {
                            Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                repeat(7 - row.size) { Spacer(Modifier.size(36.dp)) }
            }
        }
    }
}

private fun colorsClose(a: Color, b: Color): Boolean {
    return kotlin.math.abs(a.red - b.red) < 0.08f &&
            kotlin.math.abs(a.green - b.green) < 0.08f &&
            kotlin.math.abs(a.blue - b.blue) < 0.08f
}

private fun themePresetColors(): List<Pair<Color, ColorStyle?>> =
    ColorStyle.entries.map { previewColor(it) to it }

private fun standardColors(): List<Color> = listOf(
    Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
    Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
    Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
    Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722),
    Color(0xFF795548), Color(0xFF9E9E9E), Color(0xFF607D8B), Color(0xFF000000)
)

private fun Color.toArgbCompat(): Int {
    val a = (alpha * 255).toInt() and 0xFF
    val r = (red * 255).toInt() and 0xFF
    val g = (green * 255).toInt() and 0xFF
    val b = (blue * 255).toInt() and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

@Composable private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
    )
}

@Composable private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable private fun SegmentedRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable private fun RowScope.SegmentChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge)
        },
        modifier = Modifier.weight(1f)
    )
}

@Composable private fun SoftDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 10.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable private fun SwitchRow(
    title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
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
