package com.medexpiry.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Поиск картинок лекарств из нескольких бесплатных источников:
 * 1. Wikipedia (RU + EN) — самые точные фото препаратов
 * 2. Wikimedia Commons — дополнительные изображения
 */
object ImageSearchService {

    data class ImageResult(
        val title: String,
        val url: String,
        val thumbUrl: String
    )

    private const val USER_AGENT =
        "MedExpiry/2.0 (Android; medicine expiry tracker; contact@example.com)"

    suspend fun searchImages(query: String, limit: Int = 15): List<ImageResult> {
        if (query.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            coroutineScope {
                val q = query.trim()

                val wikiRu = async { searchWikipedia(q, "ru") }
                val wikiEn = async { searchWikipedia(q, "en") }
                val commons = async { searchCommons(q, limit) }

                val all = mutableListOf<ImageResult>()
                all.addAll(wikiRu.await())
                all.addAll(wikiEn.await())
                all.addAll(commons.await())

                // Убираем дубликаты по URL
                all.distinctBy { it.url }.take(limit)
            }
        }
    }

    // ——— Wikipedia REST Summary (быстрый и надёжный) ———
    private fun searchWikipedia(query: String, lang: String): List<ImageResult> {
        val results = mutableListOf<ImageResult>()
        try {
            // 1. Ищем страницы
            val searchUrl = "https://$lang.wikipedia.org/w/api.php" +
                    "?action=query" +
                    "&list=search" +
                    "&srsearch=${URLEncoder.encode(query, "UTF-8")}" +
                    "&srlimit=5" +
                    "&format=json"

            val searchJson = httpGet(searchUrl) ?: return emptyList()
            val searchRoot = JSONObject(searchJson)
            val searchArray = searchRoot
                .optJSONObject("query")
                ?.optJSONArray("search") ?: return emptyList()

            for (i in 0 until searchArray.length()) {
                val title = searchArray.getJSONObject(i).optString("title", "")
                if (title.isBlank()) continue

                // 2. Получаем картинку страницы
                val summaryUrl = "https://$lang.wikipedia.org/api/rest_v1/page/summary/" +
                        URLEncoder.encode(title, "UTF-8").replace("+", "%20")

                val summaryJson = httpGet(summaryUrl) ?: continue
                val summary = JSONObject(summaryJson)

                val thumbnail = summary.optJSONObject("thumbnail")
                val original = summary.optJSONObject("originalimage")

                val thumbSrc = thumbnail?.optString("source", "") ?: ""
                val origSrc = original?.optString("source", "") ?: thumbSrc

                if (origSrc.isNotBlank() && isLikelyImage(origSrc)) {
                    results.add(
                        ImageResult(
                            title = summary.optString("title", title),
                            url = origSrc,
                            thumbUrl = if (thumbSrc.isNotBlank()) thumbSrc else origSrc
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    // ——— Wikimedia Commons ———
    private fun searchCommons(query: String, limit: Int): List<ImageResult> {
        try {
            // Простой и более эффективный запрос
            val encoded = URLEncoder.encode(query, "UTF-8")
            val apiUrl = "https://commons.wikimedia.org/w/api.php" +
                    "?action=query" +
                    "&generator=search" +
                    "&gsrsearch=$encoded" +
                    "&gsrnamespace=6" +
                    "&gsrlimit=$limit" +
                    "&prop=imageinfo" +
                    "&iiprop=url|size|mime" +
                    "&iiurlwidth=400" +
                    "&format=json"

            val json = httpGet(apiUrl) ?: return emptyList()
            return parseCommons(json)
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    private fun parseCommons(json: String): List<ImageResult> {
        val results = mutableListOf<ImageResult>()
        try {
            val pages = JSONObject(json)
                .optJSONObject("query")
                ?.optJSONObject("pages") ?: return emptyList()

            val keys = pages.keys()
            while (keys.hasNext()) {
                val page = pages.getJSONObject(keys.next())
                val title = page.optString("title", "").removePrefix("File:")
                val imageInfo = page.optJSONArray("imageinfo") ?: continue
                if (imageInfo.length() == 0) continue

                val info = imageInfo.getJSONObject(0)
                val mime = info.optString("mime", "")
                // Только реальные изображения
                if (!mime.startsWith("image/")) continue

                val url = info.optString("url", "")
                val thumb = info.optString("thumburl", url)

                if (url.isNotBlank()) {
                    results.add(
                        ImageResult(
                            title = title,
                            url = url,
                            thumbUrl = if (thumb.isNotBlank()) thumb else url
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    private fun httpGet(urlString: String): String? {
        return try {
            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
                connectTimeout = 12000
                readTimeout = 12000
                instanceFollowRedirects = true
            }
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isLikelyImage(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".jpg") || lower.contains(".jpeg") ||
                lower.contains(".png") || lower.contains(".webp") ||
                lower.contains(".gif") || lower.contains("upload.wikimedia") ||
                lower.contains("wikipedia")
    }
}
