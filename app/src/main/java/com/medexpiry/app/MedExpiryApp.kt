package com.medexpiry.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.medexpiry.app.data.AppDatabase
import com.medexpiry.app.data.MedicineRepository
import com.medexpiry.app.worker.NotificationScheduler
import okhttp3.OkHttpClient

class MedExpiryApp : Application(), ImageLoaderFactory {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { MedicineRepository(database.medicineDao()) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        NotificationScheduler.scheduleAll(this)
    }

    /** Coil с User-Agent — Wikimedia и Wikipedia лучше отдают картинки */
    override fun newImageLoader(): ImageLoader {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "MedExpiry/2.0 (Android; medicine app)")
                    .build()
                chain.proceed(request)
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .crossfade(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_desc)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "med_expiry_channel"
    }
}
