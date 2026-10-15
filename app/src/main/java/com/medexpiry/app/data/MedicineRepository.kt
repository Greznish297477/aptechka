package com.medexpiry.app.data

import kotlinx.coroutines.flow.Flow

class MedicineRepository(private val dao: MedicineDao) {
    val allMedicines: Flow<List<Medicine>> = dao.getAllMedicines()

    suspend fun getById(id: Long): Medicine? = dao.getMedicineById(id)

    suspend fun insert(medicine: Medicine): Long = dao.insert(medicine)

    suspend fun update(medicine: Medicine) = dao.update(medicine)

    suspend fun delete(medicine: Medicine) = dao.delete(medicine)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun getAllOnce(): List<Medicine> = dao.getAllOnce()
}
