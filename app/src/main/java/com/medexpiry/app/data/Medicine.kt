package com.medexpiry.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val expirationDate: LocalDate,
    val quantity: String = "",
    val notes: String = "",
    /** URL картинки из интернета (опционально) */
    val imageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun daysUntilExpiration(): Long {
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expirationDate)
    }

    fun isExpired(): Boolean = daysUntilExpiration() < 0

    fun statusColor(): MedicineStatus {
        val days = daysUntilExpiration()
        return when {
            days < 0 -> MedicineStatus.EXPIRED
            days <= 7 -> MedicineStatus.CRITICAL
            days <= 30 -> MedicineStatus.WARNING
            else -> MedicineStatus.SAFE
        }
    }

    fun hasImage(): Boolean = imageUrl.isNotBlank()
}

enum class MedicineStatus {
    SAFE, WARNING, CRITICAL, EXPIRED
}
