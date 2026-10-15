package com.medexpiry.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.medexpiry.app.data.Medicine
import com.medexpiry.app.util.ImageSearchService
import com.medexpiry.app.util.ImageStorage
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicineScreen(
    medicine: Medicine? = null,
    onSave: (Medicine) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(medicine?.name ?: "") }
    var quantity by remember { mutableStateOf(medicine?.quantity ?: "") }
    var notes by remember { mutableStateOf(medicine?.notes ?: "") }
    var imageUrl by remember { mutableStateOf(medicine?.imageUrl ?: "") }
    var expirationDate by remember {
        mutableStateOf(medicine?.expirationDate ?: LocalDate.now().plusMonths(6))
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var showManualUrl by remember { mutableStateOf(false) }

    // Онлайн-поиск
    var searchResults by remember { mutableStateOf<List<ImageSearchService.ImageResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val isEdit = medicine != null

    // ——— Выбор из галереи ———
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                // Удаляем старую локальную картинку, если была
                ImageStorage.deleteIfLocal(imageUrl)
                val path = ImageStorage.saveFromUri(context, uri)
                if (path != null) {
                    imageUrl = path
                }
            }
        }
    }

    // Fallback для старых устройств / если PickVisualMedia недоступен
    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                ImageStorage.deleteIfLocal(imageUrl)
                val path = ImageStorage.saveFromUri(context, uri)
                if (path != null) {
                    imageUrl = path
                }
            }
        }
    }

    fun openGallery() {
        try {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (e: Exception) {
            // На очень старых/кастомных прошивках
            getContentLauncher.launch("image/*")
        }
    }

    fun startImageSearch() {
        if (name.isBlank()) {
            nameError = true
            return
        }
        scope.launch {
            isSearching = true
            searchError = null
            searchResults = emptyList()
            showSearchDialog = true
            val results = ImageSearchService.searchImages(name.trim())
            searchResults = results
            isSearching = false
            if (results.isEmpty()) {
                searchError = "Картинки не найдены. Выберите из галереи или вставьте ссылку."
            }
        }
    }

    fun clearImage() {
        ImageStorage.deleteIfLocal(imageUrl)
        imageUrl = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEdit) "Редактировать" else "Новое лекарство",
                        fontWeight = FontWeight.Bold
                    )
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ===== Превью =====
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MedicineImage(
                        imageUrl = imageUrl,
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentDescription = "Превью"
                    )
                    // Кнопка очистки
                    if (imageUrl.isNotBlank()) {
                        IconButton(
                            onClick = { clearImage() },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Убрать картинку",
                                    modifier = Modifier.padding(6.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // ===== Три способа добавить картинку =====
            Text(
                "Картинка лекарства",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Из галереи
                OutlinedButton(
                    onClick = { openGallery() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Галерея", style = MaterialTheme.typography.labelLarge)
                }

                // Поиск в интернете
                OutlinedButton(
                    onClick = { startImageSearch() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ImageSearch, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Поиск", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Ссылка вручную
            if (!showManualUrl) {
                TextButton(
                    onClick = { showManualUrl = true },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(Icons.Default.Link, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Вставить ссылку на картинку")
                }
            } else {
                OutlinedTextField(
                    value = if (imageUrl.startsWith("http")) imageUrl else "",
                    onValueChange = { imageUrl = it },
                    label = { Text("Ссылка на картинку") },
                    placeholder = { Text("https://...") },
                    leadingIcon = { Icon(Icons.Default.Link, null) },
                    trailingIcon = {
                        IconButton(onClick = { showManualUrl = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Скрыть")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // ===== Название =====
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text("Название лекарства *") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Введите название") }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // ===== Дата =====
            OutlinedTextField(
                value = expirationDate.format(dateFormatter),
                onValueChange = {},
                readOnly = true,
                label = { Text("Срок годности *") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Выбрать дату")
                    }
                },
                shape = RoundedCornerShape(14.dp)
            )

            // ===== Количество =====
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Количество") },
                placeholder = { Text("20 таблеток, 100 мл...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // ===== Заметки =====
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Заметки") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                maxLines = 4,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(4.dp))

            // ===== Сохранить =====
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    onSave(
                        Medicine(
                            id = medicine?.id ?: 0,
                            name = name.trim(),
                            expirationDate = expirationDate,
                            quantity = quantity.trim(),
                            notes = notes.trim(),
                            imageUrl = imageUrl.trim(),
                            createdAt = medicine?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (isEdit) "Сохранить изменения" else "Добавить в аптечку",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // ===== Диалог онлайн-поиска =====
    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = {
                Text("Выберите картинку", fontWeight = FontWeight.Bold)
            },
            text = {
                when {
                    isSearching -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Ищем картинки для «$name»...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    searchError != null -> {
                        Text(
                            searchError!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults) { result ->
                                val isSelected = imageUrl == result.url
                                Card(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clickable {
                                            // Онлайн-URL — локальный файл не трогаем
                                            if (!imageUrl.startsWith("/") &&
                                                !imageUrl.startsWith("http")
                                            ) {
                                                ImageStorage.deleteIfLocal(imageUrl)
                                            }
                                            imageUrl = result.url
                                            showSearchDialog = false
                                        }
                                        .then(
                                            if (isSelected) Modifier.border(
                                                3.dp,
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(12.dp)
                                            ) else Modifier
                                        ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    SubcomposeAsyncImage(
                                        model = result.thumbUrl.ifBlank { result.url },
                                        contentDescription = result.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        loading = {
                                            Box(
                                                Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    // ===== DatePicker =====
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = expirationDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        expirationDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
