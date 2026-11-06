package com.example.detectionexample.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.detectionexample.models.PaginationState
import com.example.detectionexample.models.Person
import com.example.detectionexample.models.TrackedRecognition
import com.example.detectionexample.repository.ExtractorRepository
import com.example.detectionexample.repository.RecognizedPersonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DatastoreViewModel @Inject constructor(
    private val recognizedPersonRepository: RecognizedPersonRepository,
    private val extractorRepository: ExtractorRepository
) : ViewModel() {

    private val _paginationState = MutableStateFlow(PaginationState<Person>())
    val paginationState: StateFlow<PaginationState<Person>> = _paginationState.asStateFlow()

    suspend fun addPerson(trackedRecognition: TrackedRecognition, face: Bitmap) = withContext(Dispatchers.Default) {
        val person = Person(
            id = UUID.randomUUID().toString(),
            name = trackedRecognition.title,
            score = (trackedRecognition.detectionConfidence * 100).toInt(),
            embeddings = extractorRepository.extractImage(face),
            face = face
        )
        recognizedPersonRepository.registerPerson(person)
    }

    fun isRegisteredPersonEmpty() = recognizedPersonRepository.isEmpty()
    fun getRegisteredPerson() = recognizedPersonRepository.getRegisteredPerson()

    fun loadNextPage() {
        val current = _paginationState.value
        if (current.isLoading || (!current.hasMore && current.items.isNotEmpty())) return

        viewModelScope.launch(Dispatchers.IO) {
            _paginationState.value = current.copy(isLoading = true)
            val total = recognizedPersonRepository.getPersonCount()
            val nextPage = current.page + 1
            val offset = (nextPage - 1) * current.pageSize
            val newItems = recognizedPersonRepository.getPersonsPaged(limit = current.pageSize, offset = offset)
            val allItems = current.items + newItems

            _paginationState.value = PaginationState(
                items = allItems,
                page = nextPage,
                pageSize = current.pageSize,
                totalCount = total,
                isLoading = false,
                hasMore = allItems.size < total
            )
        }
    }

    fun refreshPagination() {
        viewModelScope.launch(Dispatchers.IO) {
            val pageSize = _paginationState.value.pageSize
            val total = recognizedPersonRepository.getPersonCount()
            val items = recognizedPersonRepository.getPersonsPaged(limit = pageSize, offset = 0)
            _paginationState.value = PaginationState(
                items = items,
                page = 1,
                pageSize = pageSize,
                totalCount = total,
                isLoading = false,
                hasMore = items.size < total
            )
        }
    }

    fun clearRegisteredPerson() {
        viewModelScope.launch(Dispatchers.IO) {
            recognizedPersonRepository.clearRegisteredPerson()
        }
    }
    fun removeRegisteredPerson(person: Person) {
        viewModelScope.launch(Dispatchers.IO) {
            recognizedPersonRepository.removeRegisteredPerson(person)
        }
    }
    fun saveAllToDatastore() {
        viewModelScope.launch(Dispatchers.IO) {
            recognizedPersonRepository.saveAllToDatastore()
        }
    }

    fun loadAllToDatastore() {
        viewModelScope.launch(Dispatchers.IO) {
            recognizedPersonRepository.loadAllFromDatastore()
        }
    }
}
