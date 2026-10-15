package com.medexpiry.app.util

import android.content.Context
import android.net.Uri
import com.medexpiry.app.data.Medicine
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Экспорт / импорт всей аптечки в один ZIP-файл.
 *
 * Структура архива:
 *   medicines.json  — данные всех лекарств
 *   images/xxx.jpg  — локальные картинки (если есть)
 */
object BackupManager {

    private const val JSON_NAME = "medicines.json"
    private const val IMAGES_DIR = "images/"

    data class ImportResult(
        val medicines: List<Medicine>,
        val count: Int
    )

    // ─────────────────── ЭКСПОРТ ───────────────────

    fun export(context: Context, medicines: List<Medicine>, outputUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(outputUri)?.use { outStream ->
                ZipOutputStream(BufferedOutputStream(outStream)).use { zip ->
                    // 1. JSON с данными
                    val jsonArray = JSONArray()
                    medicines.forEachIndexed { index, med ->
                        val obj = JSONObject().apply {
                            put("name", med.name)
                            put("expirationDate", med.expirationDate.toString())
                            put("quantity", med.quantity)
                            put("notes", med.notes)
                            put("createdAt", med.createdAt)

                            // Локальный файл → кладём в images/, в JSON пишем имя файла
                            // Интернет-URL → оставляем как есть
                            val img = med.imageUrl
                            if (img.isNotBlank() && !img.startsWith("http")) {
                                val file = File(img)
                                if (file.exists()) {
                                    val entryName = "img_$index.${file.extension.ifBlank { "jpg" }}"
                                    put("imageFile", entryName)
                                    put("imageUrl", "")
                                    // Добавляем файл в ZIP
                                    zip.putNextEntry(ZipEntry(IMAGES_DIR + entryName))
                                    FileInputStream(file).use { it.copyTo(zip) }
                                    zip.closeEntry()
                                } else {
                                    put("imageUrl", "")
                                    put("imageFile", "")
                                }
                            } else {
                                put("imageUrl", img)
                                put("imageFile", "")
                            }
                        }
                        jsonArray.put(obj)
                    }

                    val root = JSONObject().apply {
                        put("version", 1)
                        put("app", "Aptechka")
                        put("exportedAt", System.currentTimeMillis())
                        put("medicines", jsonArray)
                    }

                    zip.putNextEntry(ZipEntry(JSON_NAME))
                    zip.write(root.toString(2).toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ─────────────────── ИМПОРТ ───────────────────

    fun import(context: Context, inputUri: Uri): ImportResult? {
        return try {
            val tempDir = File(context.cacheDir, "import_tmp").apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }
            val imagesOutDir = File(context.filesDir, "medicine_images").apply {
                if (!exists()) mkdirs()
            }

            var jsonText: String? = null
            val extractedImages = mutableMapOf<String, File>() // entryName → file

            context.contentResolver.openInputStream(inputUri)?.use { inStream ->
                ZipInputStream(BufferedInputStream(inStream)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == JSON_NAME || entry.name.endsWith("/$JSON_NAME") -> {
                                jsonText = zip.bufferedReader().readText()
                            }
                            entry.name.startsWith(IMAGES_DIR) && !entry.isDirectory -> {
                                val name = entry.name.removePrefix(IMAGES_DIR)
                                val outFile = File(tempDir, name)
                                FileOutputStream(outFile).use { zip.copyTo(it) }
                                extractedImages[name] = outFile
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }

            if (jsonText.isNullOrBlank()) return null

            val root = JSONObject(jsonText)
            val arr = root.getJSONArray("medicines")
            val result = mutableListOf<Medicine>()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val imageFile = obj.optString("imageFile", "")
                val imageUrlFromJson = obj.optString("imageUrl", "")

                val finalImagePath = when {
                    // Локальная картинка из архива
                    imageFile.isNotBlank() && extractedImages.containsKey(imageFile) -> {
                        val src = extractedImages[imageFile]!!
                        val dest = File(imagesOutDir, "${System.currentTimeMillis()}_$i.${src.extension}")
                        src.copyTo(dest, overwrite = true)
                        dest.absolutePath
                    }
                    // URL из интернета
                    imageUrlFromJson.startsWith("http") -> imageUrlFromJson
                    else -> ""
                }

                result.add(
                    Medicine(
                        id = 0, // Room сгенерирует новый
                        name = obj.getString("name"),
                        expirationDate = LocalDate.parse(obj.getString("expirationDate")),
                        quantity = obj.optString("quantity", ""),
                        notes = obj.optString("notes", ""),
                        imageUrl = finalImagePath,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            // Чистим временные файлы
            tempDir.deleteRecursively()

            ImportResult(result, result.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
