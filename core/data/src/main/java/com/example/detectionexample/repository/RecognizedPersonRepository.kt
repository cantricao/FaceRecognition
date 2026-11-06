package com.example.detectionexample.repository

import android.util.Pair
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.detectionexample.core.common.vector.DistanceMetric
import com.example.detectionexample.core.common.vector.VectorMatch
import com.example.detectionexample.core.common.vector.VectorSearch
import com.example.detectionexample.data.local.PersonDao
import com.example.detectionexample.data.local.PersonEntity
import com.example.detectionexample.models.Person
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecognizedPersonRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val personDao: PersonDao
) {

    private object PreferencesKeys {
        val REGISTER = stringPreferencesKey("registered_person")
    }

    private val registeredPerson = mutableListOf<Person>()

    val personsFlow: Flow<List<Person>> = personDao.getAllPersons().map { entities ->
        entities.map { it.toDomain() }
    }

    fun registerPerson(person: Person) {
        registeredPerson.add(person)
    }

    suspend fun savePerson(person: Person) {
        registeredPerson.add(person)
        personDao.insertPerson(PersonEntity.fromDomain(person))
    }

    fun clear() {
        registeredPerson.clear()
    }

    fun findNearest(emb: FloatArray, threshold: Float): Pair<String, Float>? {
        val match = VectorSearch.findNearest(
            query = emb,
            candidates = registeredPerson,
            threshold = threshold,
            metric = DistanceMetric.EUCLIDEAN,
            vectorSelector = { it.embeddings }
        )
        return match?.let { Pair(it.item.name, it.distance) }
    }

    fun findNearestMatch(
        emb: FloatArray,
        threshold: Float,
        metric: DistanceMetric = DistanceMetric.EUCLIDEAN
    ): VectorMatch<Person>? {
        return VectorSearch.findNearest(
            query = emb,
            candidates = registeredPerson,
            threshold = threshold,
            metric = metric,
            vectorSelector = { it.embeddings }
        )
    }

    fun findTopK(
        emb: FloatArray,
        k: Int,
        threshold: Float = Float.MAX_VALUE,
        metric: DistanceMetric = DistanceMetric.EUCLIDEAN
    ): List<VectorMatch<Person>> {
        return VectorSearch.findTopK(
            query = emb,
            candidates = registeredPerson,
            k = k,
            threshold = threshold,
            metric = metric,
            vectorSelector = { it.embeddings }
        )
    }

    suspend fun getPersonsPaged(limit: Int, offset: Int): List<Person> {
        return personDao.getPersonsPaged(limit, offset).map { it.toDomain() }
    }

    fun getPersonsPagedFlow(limit: Int, offset: Int): Flow<List<Person>> {
        return personDao.getPersonsPagedFlow(limit, offset).map { list -> list.map { it.toDomain() } }
    }

    suspend fun getPersonCount(): Int {
        return personDao.getPersonCount()
    }

    suspend fun searchPersonsPaged(query: String, limit: Int, offset: Int): List<Person> {
        return personDao.searchPersonsPaged(query, limit, offset).map { it.toDomain() }
    }

    fun isEmpty(): Boolean {
        return registeredPerson.isEmpty()
    }

    fun getRegisteredPerson() = registeredPerson.toList()

    suspend fun clearRegisteredPerson() {
        registeredPerson.clear()
        personDao.clearAll()
        dataStore.edit { it.clear() }
    }

    fun removeRegisteredPerson(person: Person) {
        registeredPerson.remove(person)
    }

    suspend fun getPersonById(id: String): Person? {
        return registeredPerson.find { it.id == id } ?: personDao.getPersonById(id)?.toDomain()
    }

    suspend fun deletePerson(person: Person) {
        registeredPerson.remove(person)
        personDao.deleteById(person.id)
    }

    suspend fun saveAllToDatastore() {
        val jsonString = Json.encodeToString(registeredPerson)
        dataStore.edit { registered ->
            registered[PreferencesKeys.REGISTER] = jsonString
        }
        personDao.insertPersons(registeredPerson.map { PersonEntity.fromDomain(it) })
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

    suspend fun loadAllFromRoom() {
        val entities = personDao.getAllPersonsSnapshot()
        registeredPerson.clear()
        registeredPerson.addAll(entities.map { it.toDomain() })
    }
}
