package com.medexpiry.app.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
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
 * Скачивает APK → ставит поверх текущей версии (тот же applicationId) → удаляет APK.
 */
object AppUpdater {

    const val GITHUB_OWNER = "Greznish297477"
    const val GITHUB_REPO = "aptechka"

    private const val USER_AGENT = "Aptechka-Android/1.0"
    private const val BUFFER_SIZE = 256 * 1024 // 256 KB — быстрее загрузка

    const val ACTION_INSTALL_STATUS = "com.medexpiry.app.INSTALL_STATUS"

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

    private fun updatesDir(context: Context): File {
        val dir = File(context.cacheDir, "updates")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun cleanupDownloadedApks(context: Context) {
        try {
            updatesDir(context).listFiles()?.forEach { f ->
                if (f.name.endsWith(".apk", ignoreCase = true) || f.name.endsWith(".tmp")) {
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

            // Приоритет: release APK (меньше и «правильная» сборка) → любой .apk
            var bestUrl = ""
            var bestName = "Aptechka-update.apk"
            var bestScore = -1

            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name", "")
                val url = a.optString("browser_download_url", "")
                if (!name.endsWith(".apk", true) || url.isBlank()) continue

                val score = when {
                    name.contains("release", true) && !name.contains("unsigned", true) -> 100
                    name.contains("Aptechka-release", true) -> 100
                    name.contains("app-release", true) -> 90
                    name.contains("release", true) -> 80
                    name.contains("debug", true) -> 10
                    else -> 50
                }
                if (score > bestScore) {
                    bestScore = score
                    bestUrl = url
                    bestName = name
                }
            }

            if (bestUrl.isEmpty()) {
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
                    apkUrl = bestUrl,
                    releaseNotes = notes,
                    fileName = bestName
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            CheckResult.Error(e.message ?: "Ошибка проверки обновлений")
        }
    }

    /**
     * Быстрое скачивание: большой буфер, без лишних пауз.
     */
    suspend fun downloadApk(
        context: Context,
        info: UpdateInfo,
        onProgress: (Float) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        try {
            cleanupDownloadedApks(context)
            val out = File(updatesDir(context), "update.apk")
            val tmp = File(updatesDir(context), "update.apk.tmp")

            val conn = openDownload(info.apkUrl)
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return@withContext null
            }

            val total = conn.contentLengthLong.coerceAtLeast(0)
            conn.inputStream.use { input ->
                tmp.outputStream().buffered(BUFFER_SIZE).use { output ->
                    val buf = ByteArray(BUFFER_SIZE)
                    var read: Int
                    var done = 0L
                    var lastReport = 0L
                    while (input.read(buf).also { read = it } != -1) {
                        output.write(buf, 0, read)
                        done += read
                        // Обновляем прогресс не чаще чем раз на ~2% или 256KB
                        if (total > 0 && (done - lastReport >= BUFFER_SIZE || done == total)) {
                            lastReport = done
                            onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                    output.flush()
                }
            }
            conn.disconnect()

            if (out.exists()) out.delete()
            if (!tmp.renameTo(out)) {
                tmp.copyTo(out, overwrite = true)
                tmp.delete()
            }
            onProgress(1f)
            out
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

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
     * Установка **поверх** текущего приложения (обновление).
     * Использует PackageInstaller — система заменяет старую версию, данные сохраняются.
     */
    fun installApk(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists()) return false
        return try {
            installWithPackageInstaller(context, apkFile)
        } catch (e: Exception) {
            e.printStackTrace()
            // запасной путь — Intent (тоже обновляет при том же applicationId + подписи)
            installWithIntent(context, apkFile)
        }
    }

    private fun installWithPackageInstaller(context: Context, apkFile: File): Boolean {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        )
        // Явно указываем пакет — система обновит существующее приложение
        params.setAppPackageName(context.packageName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }

        val sessionId = installer.createSession(params)
        val session = installer.openSession(sessionId)

        apkFile.inputStream().use { input ->
            session.openWrite("base.apk", 0, apkFile.length()).use { out ->
                val buf = ByteArray(BUFFER_SIZE)
                var n: Int
                while (input.read(buf).also { n = it } != -1) {
                    out.write(buf, 0, n)
                }
                session.fsync(out)
            }
        }

        val callback = Intent(context, InstallResultReceiver::class.java).apply {
            action = ACTION_INSTALL_STATUS
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        val pi = PendingIntent.getBroadcast(context, sessionId, callback, flags)
        session.commit(pi.intentSender)
        session.close()
        return true
    }

    private fun installWithIntent(context: Context, apkFile: File): Boolean {
        return try {
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

    private fun parseVersion(v: String): List<Int> =
        v.split(Regex("[^0-9]+")).filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }

    private fun openDownload(urlStr: String): HttpURLConnection {
        var current = urlStr
        // Следуем редиректам вручную (GitHub иногда редиректит)
        for (i in 0 until 5) {
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/octet-stream")
                connectTimeout = 20000
                readTimeout = 120000
            }
            val code = conn.responseCode
            if (code in 300..399) {
                val loc = conn.getHeaderField("Location")
                conn.disconnect()
                if (loc.isNullOrBlank()) break
                current = if (loc.startsWith("http")) loc else URL(URL(current), loc).toString()
                continue
            }
            return conn
        }
        return (URL(urlStr).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/octet-stream")
            connectTimeout = 20000
            readTimeout = 120000
        }
    }

    private fun httpGet(url: String): String? {
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 12000
                readTimeout = 12000
            }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * Результат установки через PackageInstaller.
 * Удаляет APK после успеха / неудачи, чтобы не засорять память.
 */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // Нужно подтверждение пользователя
                val confirm = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (confirm != null) context.startActivity(confirm)
            }
            PackageInstaller.STATUS_SUCCESS -> {
                AppUpdater.cleanupDownloadedApks(context)
            }
            else -> {
                // Ошибка — всё равно чистим APK
                AppUpdater.cleanupDownloadedApks(context)
            }
        }
    }
}
