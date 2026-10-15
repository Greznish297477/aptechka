package com.medexpiry.app.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Автообновление из GitHub Releases.
 * Скачивает APK → устанавливает → удаляет файл (при следующем запуске / после установки).
 */
object AppUpdater {

    /** Репозиторий с релизами — при смене имени репо поменяй здесь */
    const val GITHUB_OWNER = "Greznish297477"
    const val GITHUB_REPO = "aptechka"

    private const val USER_AGENT = "Aptechka-Android-Updater"

    data class UpdateInfo(
        val tagName: String,
        val versionName: String,
        val apkUrl: String,
        val releaseNotes: String,
        val fileName: String
    )

    sealed class CheckResult {
        data class Available(val info: UpdateInfo) : CheckResult()
        object UpToDate : CheckResult()
        data class Error(val message: String) : CheckResult()
    }

    fun currentVersionName(context: Context): String {
        return try {
            val p = context.packageManager.getPackageInfo(context.packageName, 0)
            p.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    fun currentVersionCode(context: Context): Long {
        return try {
            val p = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) p.longVersionCode
            else @Suppress("DEPRECATION") p.versionCode.toLong()
        } catch (e: Exception) {
            1L
        }
    }

    /** Папка для временных APK */
    private fun updatesDir(context: Context): File {
        val dir = File(context.cacheDir, "updates")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Удаляет все скачанные APK, чтобы не засорять память */
    fun cleanupDownloadedApks(context: Context) {
        try {
            updatesDir(context).listFiles()?.forEach { f ->
                if (f.name.endsWith(".apk", ignoreCase = true)) {
                    f.delete()
                }
            }
        } catch (_: Exception) { }
    }

    suspend fun checkForUpdate(context: Context): CheckResult = withContext(Dispatchers.IO) {
        try {
            val api = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
            val json = httpGet(api) ?: return@withContext CheckResult.Error("Нет ответа от GitHub")
            val root = JSONObject(json)

            if (root.has("message") && !root.has("tag_name")) {
                return@withContext CheckResult.Error("Релизов пока нет на GitHub")
            }

            val tag = root.optString("tag_name", "").trim()
            if (tag.isEmpty()) return@withContext CheckResult.Error("Пустой тег релиза")

            val notes = root.optString("body", "")
            val assets = root.optJSONArray("assets")
                ?: return@withContext CheckResult.Error("В релизе нет файлов")

            var apkUrl = ""
            var fileName = "Aptechka-update.apk"
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name", "")
                val url = a.optString("browser_download_url", "")
                if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
                    // Предпочитаем release, иначе debug
                    if (name.contains("release", ignoreCase = true) || apkUrl.isEmpty()) {
                        apkUrl = url
                        fileName = name
                    }
                    if (name.contains("release", ignoreCase = true)) break
                }
            }

            if (apkUrl.isEmpty()) {
                return@withContext CheckResult.Error("APK не найден в релизе $tag")
            }

            val remoteVersion = tag.removePrefix("v").removePrefix("V").trim()
            val localVersion = currentVersionName(context).trim()

            if (!isNewer(remoteVersion, localVersion)) {
                return@withContext CheckResult.UpToDate
            }

            CheckResult.Available(
                UpdateInfo(
                    tagName = tag,
                    versionName = remoteVersion,
                    apkUrl = apkUrl,
                    releaseNotes = notes,
                    fileName = fileName
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            CheckResult.Error(e.message ?: "Ошибка проверки обновлений")
        }
    }

    /**
     * Скачивает APK во временную папку.
     * @return файл или null при ошибке
     */
    suspend fun downloadApk(
        context: Context,
        info: UpdateInfo,
        onProgress: (Float) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        try {
            cleanupDownloadedApks(context)
            val out = File(updatesDir(context), info.fileName.ifBlank { "update.apk" })

            val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                instanceFollowRedirects = true
                connectTimeout = 30000
                readTimeout = 120000
            }
            if (conn.responseCode !in 200..299) return@withContext null

            val total = conn.contentLengthLong.coerceAtLeast(0)
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(8192)
                    var read: Int
                    var done = 0L
                    while (input.read(buf).also { read = it } != -1) {
                        output.write(buf, 0, read)
                        done += read
                        if (total > 0) onProgress(done.toFloat() / total)
                    }
                }
            }
            out
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Можно ли устанавливать неизвестные приложения */
    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    /**
     * Запускает установку APK через системный установщик.
     * Файл удалится при следующем запуске приложения (cleanupDownloadedApks).
     */
    fun installApk(context: Context, apkFile: File): Boolean {
        return try {
            if (!apkFile.exists()) return false
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Сравнить semver-подобные версии: 1.0.1 > 1.0.0 */
    fun isNewer(remote: String, local: String): Boolean {
        val r = parseVersion(remote)
        val l = parseVersion(local)
        val max = maxOf(r.size, l.size)
        for (i in 0 until max) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        return false
    }

    private fun parseVersion(v: String): List<Int> {
        return v.split(Regex("[^0-9]+"))
            .filter { it.isNotBlank() }
            .mapNotNull { it.toIntOrNull() }
    }

    private fun httpGet(url: String): String? {
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 15000
                readTimeout = 15000
            }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
