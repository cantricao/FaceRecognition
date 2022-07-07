package com.example.detectionexample.repository

import android.graphics.Bitmap
import android.os.Build
import android.util.Base64
import android.util.Log
import android.util.Pair
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.detectionexample.models.Person
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

@Singleton
class RecognizedPersonRepository @Inject constructor(private val dataStore: DataStore<Preferences>){

    private object PreferencesKeys{
        val REGISTER = stringPreferencesKey("registered_person")
    }
    private val registeredPerson = mutableListOf<Person>()

    fun registerPerson(person: Person) {
        registeredPerson.add(person)
    }

    fun clear(){
        registeredPerson.clear()
    }

    fun findNearest(emb: FloatArray, threshold: Float): Pair<String, Float>? {
        return registeredPerson.map {
            val knownEmb = it.embeddings
            val distance = sqrt(emb.zip(knownEmb).sumOf { (i , j) -> (i - j).pow( 2).toDouble() }).toFloat()
            Pair(it.name, distance)
        }
            .filter { it.second <= threshold }
            .minByOrNull { it.second }

    }



    fun isEmpty(): Boolean {
        return registeredPerson.isEmpty()
    }

    fun getRegisteredPerson() = registeredPerson.toList()
    suspend fun clearRegisteredPerson() {
        registeredPerson.clear()
        dataStore.edit { it.clear() }
    }
    fun removeRegisteredPerson(person: Person) {
        registeredPerson.remove(person)
    }
    suspend fun saveAllToDatastore() {
        val jsonString = Json.encodeToString(registeredPerson)
        dataStore.edit { registered ->
            registered[PreferencesKeys.REGISTER] = jsonString
        }
    }

    suspend fun loadAllFromDatastore() {
        dataStore.data.map { registered ->
            val json = registered[PreferencesKeys.REGISTER]
            json?.let { Json.decodeFromString<List<Person>>(it) } ?: listOf()
        }.collect {
            registeredPerson.clear()
            registeredPerson.addAll(it)
        }
    }
}
