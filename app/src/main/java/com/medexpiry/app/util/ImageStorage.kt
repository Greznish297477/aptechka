package com.medexpiry.app.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Сохраняет выбранное из галереи изображение во внутреннее хранилище приложения.
 * Возвращает абсолютный путь к файлу (Coil умеет его загружать).
 */
object ImageStorage {

    private const val DIR_NAME = "medicine_images"

    fun saveFromUri(context: Context, uri: Uri): String? {
        return try {
            val dir = File(context.filesDir, DIR_NAME).apply {
                if (!exists()) mkdirs()
            }
            val ext = guessExtension(context, uri)
            val file = File(dir, "${UUID.randomUUID()}.$ext")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteIfLocal(path: String?) {
        if (path.isNullOrBlank()) return
        if (path.startsWith("http://") || path.startsWith("https://")) return
        try {
            val file = File(path)
            if (file.exists()) file.delete()
        } catch (_: Exception) { }
    }

    private fun guessExtension(context: Context, uri: Uri): String {
        val type = context.contentResolver.getType(uri) ?: return "jpg"
        return when {
            type.contains("png") -> "png"
            type.contains("webp") -> "webp"
            type.contains("gif") -> "gif"
            else -> "jpg"
        }
    }
}
