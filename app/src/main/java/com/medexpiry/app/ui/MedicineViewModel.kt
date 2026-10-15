package com.medexpiry.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.medexpiry.app.data.Medicine
import com.medexpiry.app.data.MedicineRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MedicineViewModel(private val repository: MedicineRepository) : ViewModel() {

    val medicines = repository.allMedicines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMedicine(medicine: Medicine) {
        viewModelScope.launch {
            repository.insert(medicine)
        }
    }

    fun updateMedicine(medicine: Medicine) {
        viewModelScope.launch {
            repository.update(medicine)
        }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            repository.delete(medicine)
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    /** Получить все лекарства один раз (для экспорта) */
    suspend fun getAllOnce(): List<Medicine> = repository.getAllOnce()

    /** Импорт: добавить пачку лекарств (с новыми id) */
    fun importMedicines(list: List<Medicine>, replaceExisting: Boolean, onDone: (Int) -> Unit) {
        viewModelScope.launch {
            if (replaceExisting) {
                repository.getAllOnce().forEach { repository.delete(it) }
            }
            list.forEach { repository.insert(it.copy(id = 0)) }
            onDone(list.size)
        }
    }
}

class MedicineViewModelFactory(private val repository: MedicineRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicineViewModel::class.java)) {
            return MedicineViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
