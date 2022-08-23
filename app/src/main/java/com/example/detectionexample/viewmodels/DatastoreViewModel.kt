package com.example.detectionexample.viewmodels

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.detectionexample.models.Person
import com.example.detectionexample.models.TrackedRecognition
import com.example.detectionexample.repository.ExtractorRepository
import com.example.detectionexample.repository.RecognizedPersonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DatastoreViewModel @Inject constructor (private val recognizedPersonRepository: RecognizedPersonRepository,
                                              private val extractorRepository: ExtractorRepository
) : ViewModel(){

    fun addPerson(trackedRecognition: TrackedRecognition, face: Bitmap) {
        val person = Person(
            id = UUID.randomUUID().toString(),
            name = trackedRecognition.title,
            score = (trackedRecognition.detectionConfidence * 100).toInt(),
            embeddings = extractorRepository.extractImage(face),
            face = face
        )
        recognizedPersonRepository.registerPerson(person)
//        Log.d(AnalysisViewModel.TAG, "Register: ${person.name}")
    }

    fun isRegisteredPersonEmpty() = recognizedPersonRepository.isEmpty()
    fun getRegisteredPerson() = recognizedPersonRepository.getRegisteredPerson()
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