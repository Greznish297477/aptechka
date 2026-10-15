package com.medexpiry.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.medexpiry.app.data.AppSettings
import com.medexpiry.app.data.Medicine
import com.medexpiry.app.data.MedicineStatus
import com.medexpiry.app.data.CardStyle
import com.medexpiry.app.data.SortOrder
import com.medexpiry.app.util.AppUpdater
import com.medexpiry.app.util.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineListScreen(
    medicines: List<Medicine>,
    settings: AppSettings,
    onAddClick: () -> Unit,
    onItemClick: (Medicine) -> Unit,
    onDelete: (Medicine) -> Unit,
    onImport: (List<Medicine>, Boolean, (Int) -> Unit) -> Unit,
    getAllMedicines: suspend () -> List<Medicine>,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AppUpdater.UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateChecking by remember { mutableStateOf(false) }
    var updateDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var updateMessage by remember { mutableStateOf<String?>(null) }

    // Автопроверка обновлений при открытии
    LaunchedEffect(Unit) {
        AppUpdater.cleanupDownloadedApks(context)
        if (!settings.autoCheckUpdates) return@LaunchedEffect
        updateChecking = true
        when (val r = AppUpdater.checkForUpdate(context)) {
            is AppUpdater.CheckResult.Available -> {
                updateInfo = r.info
                showUpdateDialog = true
            }
            is AppUpdater.CheckResult.UpToDate -> { }
            is AppUpdater.CheckResult.Error -> { }
        }
        updateChecking = false
    }

    fun runUpdateCheck(manual: Boolean = true) {
        scope.launch {
            updateChecking = true
            updateMessage = null
            when (val r = AppUpdater.checkForUpdate(context)) {
                is AppUpdater.CheckResult.Available -> {
                    updateInfo = r.info
                    showUpdateDialog = true
                }
                is AppUpdater.CheckResult.UpToDate -> {
                    if (manual) {
                        Toast.makeText(context, "У вас актуальная версия (${AppUpdater.currentVersionName(context)})", Toast.LENGTH_SHORT).show()
                    }
                }
                is AppUpdater.CheckResult.Error -> {
                    if (manual) {
                        Toast.makeText(context, r.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
            updateChecking = false
        }
    }

    fun startDownloadAndInstall(info: AppUpdater.UpdateInfo) {
        scope.launch {
            if (!AppUpdater.canInstallPackages(context)) {
                Toast.makeText(context, "Разрешите установку из этого источника", Toast.LENGTH_LONG).show()
                AppUpdater.openInstallPermissionSettings(context)
                return@launch
            }
            updateDownloading = true
            downloadProgress = 0f
            val file = AppUpdater.downloadApk(context, info) { p -> downloadProgress = p }
            updateDownloading = false
            if (file == null) {
                Toast.makeText(context, "Не удалось скачать обновление", Toast.LENGTH_LONG).show()
                return@launch
            }
            showUpdateDialog = false
            val ok = AppUpdater.installApk(context, file)
            if (ok) {
                Toast.makeText(
                    context,
                    "Откройте установщик и нажмите «Обновить» / «Установить»",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(context, "Не удалось запустить установку", Toast.LENGTH_LONG).show()
                AppUpdater.cleanupDownloadedApks(context)
            }
        }
    }

    val filtered = remember(
        medicines, searchQuery, settings.sortOrder,
        settings.searchNotes, settings.showExpiredMedicines
    ) {
        val searched = if (searchQuery.isBlank()) medicines
        else medicines.filter { medicine ->
            medicine.name.contains(searchQuery, ignoreCase = true) ||
                (settings.searchNotes && medicine.notes.contains(searchQuery, ignoreCase = true))
        }
        val visible = if (settings.showExpiredMedicines) searched
            else searched.filter { it.daysUntilExpiration() >= 0 }
        when (settings.sortOrder) {
            SortOrder.BY_NAME -> visible.sortedBy { it.name.lowercase() }
            SortOrder.BY_ADDED -> visible.sortedByDescending { it.createdAt }
            SortOrder.BY_EXPIRY -> visible // уже по сроку из БД
        }
    }

    val urgent = remember(filtered, settings.urgentDays) {
        filtered.filter { it.daysUntilExpiration() <= settings.urgentDays }
    }
    val normal = remember(filtered, settings.urgentDays) {
        filtered.filter { it.daysUntilExpiration() > settings.urgentDays }
    }

    // Сохранение бэкапа
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isBusy = true
            val all = getAllMedicines()
            val ok = withContext(Dispatchers.IO) {
                BackupManager.export(context, all, uri)
            }
            isBusy = false
            Toast.makeText(
                context,
                if (ok) "Резервная копия сохранена (${all.size} препаратов)"
                else "Ошибка при сохранении",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Выбор файла для импорта
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingImportUri = uri
        showImportDialog = true
    }

    fun doImport(replace: Boolean) {
        val uri = pendingImportUri ?: return
        showImportDialog = false
        scope.launch {
            isBusy = true
            val result = withContext(Dispatchers.IO) {
                BackupManager.import(context, uri)
            }
            isBusy = false
            if (result == null) {
                Toast.makeText(context, "Не удалось прочитать файл", Toast.LENGTH_LONG).show()
            } else {
                onImport(result.medicines, replace) { count ->
                    Toast.makeText(
                        context,
                        "Импортировано: $count препаратов",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            pendingImportUri = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Аптечка",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (settings.showMedicineCount && medicines.isNotEmpty()) {
                            Text(
                                "${medicines.size} препаратов",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Меню",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Сохранить копию") },
                                onClick = {
                                    menuExpanded = false
                                    val name = "aptechka_backup_${System.currentTimeMillis()}.zip"
                                    exportLauncher.launch(name)
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.FileUpload, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Загрузить копию") },
                                onClick = {
                                    menuExpanded = false
                                    importLauncher.launch(arrayOf("application/zip", "*/*"))
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.FileDownload, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Настройки") },
                                onClick = {
                                    menuExpanded = false
                                    onOpenSettings()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Проверить обновления") },
                                onClick = {
                                    menuExpanded = false
                                    runUpdateCheck(true)
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.SystemUpdate, contentDescription = null)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Добавить") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    placeholder = { Text("Найти лекарство...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                if (filtered.isEmpty()) {
                    EmptyState(hasAny = medicines.isNotEmpty())
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(if (settings.compactCards) 6.dp else 10.dp)
                    ) {
                        if (settings.groupByUrgency) {
                            if (urgent.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "Требуют внимания",
                                        count = urgent.size,
                                        icon = Icons.Outlined.WarningAmber,
                                        color = StatusColors.Critical
                                    )
                                }
                                items(urgent, key = { it.id }) { medicine ->
                                    MedicineCard(
                                        medicine = medicine,
                                        showQuantity = settings.showQuantityOnCard,
                                        showImage = settings.showImages,
                                        showNotes = settings.showNotesOnCard,
                                        compact = settings.compactCards,
                                        cardStyle = settings.cardStyle,
                                        onClick = { onItemClick(medicine) },
                                        onDelete = { onDelete(medicine) }
                                    )
                                }
                            }
                            if (normal.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "Всё в порядке",
                                        count = normal.size,
                                        icon = Icons.Outlined.Inventory2,
                                        color = StatusColors.Safe
                                    )
                                }
                                items(normal, key = { it.id }) { medicine ->
                                    MedicineCard(
                                        medicine = medicine,
                                        showQuantity = settings.showQuantityOnCard,
                                        showImage = settings.showImages,
                                        showNotes = settings.showNotesOnCard,
                                        compact = settings.compactCards,
                                        cardStyle = settings.cardStyle,
                                        onClick = { onItemClick(medicine) },
                                        onDelete = { onDelete(medicine) }
                                    )
                                }
                            }
                        } else {
                            items(filtered, key = { it.id }) { medicine ->
                                MedicineCard(
                                    medicine = medicine,
                                    showQuantity = settings.showQuantityOnCard,
                                    showImage = settings.showImages,
                                    showNotes = settings.showNotesOnCard,
                                    compact = settings.compactCards,
                                    cardStyle = settings.cardStyle,
                                    onClick = { onItemClick(medicine) },
                                    onDelete = { onDelete(medicine) }
                                )
                            }
                        }
                    }
                }
            }

            if (isBusy) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Подождите...")
                        }
                    }
                }
            }
        }
    }

    // Диалог обновления
    if (showUpdateDialog && updateInfo != null) {
        val info = updateInfo!!
        AlertDialog(
            onDismissRequest = {
                if (!updateDownloading) showUpdateDialog = false
            },
            title = { Text("Доступно обновление", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Версия ${info.versionName} (сейчас ${AppUpdater.currentVersionName(context)})")
                    if (info.releaseNotes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(info.releaseNotes.take(300), style = MaterialTheme.typography.bodySmall)
                    }
                    if (updateDownloading) {
                        Spacer(Modifier.height(16.dp))
                        Text("Скачивание… ${(downloadProgress * 100).toInt()}%")
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { startDownloadAndInstall(info) },
                    enabled = !updateDownloading
                ) {
                    Text(if (updateDownloading) "Скачивание…" else "Обновить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUpdateDialog = false },
                    enabled = !updateDownloading
                ) { Text("Позже") }
            }
        )
    }

    if (updateChecking && !showUpdateDialog) {
        // лёгкий индикатор не блокирует UI
    }

    // Диалог: заменить или добавить к существующим
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                pendingImportUri = null
            },
            title = { Text("Загрузка копии", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Как импортировать данные?\n\n" +
                            "• Добавить — сохранить текущие лекарства и добавить из файла\n" +
                            "• Заменить — удалить всё и загрузить только из файла"
                )
            },
            confirmButton = {
                TextButton(onClick = { doImport(replace = false) }) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { doImport(replace = true) }) {
                        Text("Заменить", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = {
                        showImportDialog = false
                        pendingImportUri = null
                    }) {
                        Text("Отмена")
                    }
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f)
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun EmptyState(hasAny: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Medication,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = if (hasAny) "Ничего не найдено" else "Аптечка пуста",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (hasAny)
                    "Попробуйте изменить поисковый запрос"
                else
                    "Добавьте первое лекарство\nили загрузите копию из меню ⋮",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MedicineCard(
    medicine: Medicine,
    showQuantity: Boolean = true,
    showImage: Boolean = true,
    showNotes: Boolean = false,
    compact: Boolean = false,
    cardStyle: CardStyle = CardStyle.SOFT,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val status = medicine.statusColor()
    val statusColor by animateColorAsState(
        targetValue = when (status) {
            MedicineStatus.SAFE -> StatusColors.Safe
            MedicineStatus.WARNING -> StatusColors.Warning
            MedicineStatus.CRITICAL -> StatusColors.Critical
            MedicineStatus.EXPIRED -> StatusColors.Expired
        },
        label = "statusColor"
    )
    val days = medicine.daysUntilExpiration()
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val corner = when (cardStyle) {
        CardStyle.ROUNDED -> if (compact) 16.dp else 22.dp
        CardStyle.SOFT -> if (compact) 12.dp else 18.dp
        CardStyle.SQUARE -> if (compact) 6.dp else 8.dp
    }
    val imgCorner = when (cardStyle) {
        CardStyle.ROUNDED -> if (compact) 12.dp else 16.dp
        CardStyle.SOFT -> if (compact) 10.dp else 14.dp
        CardStyle.SQUARE -> if (compact) 4.dp else 6.dp
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(corner),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showImage) {
                MedicineImage(
                    imageUrl = medicine.imageUrl,
                    modifier = Modifier
                        .size(if (compact) 52.dp else 68.dp)
                        .clip(RoundedCornerShape(imgCorner))
                )
                Spacer(Modifier.width(14.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medicine.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = when {
                            days < 0 -> "Срок истёк"
                            days == 0L -> "Истекает сегодня"
                            days == 1L -> "Остался 1 день"
                            days <= 7 -> "Осталось $days дней"
                            days <= 30 -> "Осталось $days дней"
                            else -> "До ${medicine.expirationDate.format(dateFormatter)}"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }

                if (showQuantity && medicine.quantity.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = medicine.quantity,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (showNotes && medicine.notes.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = medicine.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MedicineImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    if (imageUrl.isBlank()) {
        Box(
            modifier = modifier.background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(14.dp)
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Medication,
                contentDescription = contentDescription,
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    } else {
        val model: Any = if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            imageUrl
        } else {
            java.io.File(imageUrl)
        }

        SubcomposeAsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Medication,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        )
    }
}
