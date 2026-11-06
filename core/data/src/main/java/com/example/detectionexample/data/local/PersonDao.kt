package com.example.detectionexample.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Query("SELECT * FROM persons ORDER BY timeStamp DESC")
    fun getAllPersons(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons ORDER BY timeStamp DESC")
    suspend fun getAllPersonsSnapshot(): List<PersonEntity>

    @Query("SELECT * FROM persons ORDER BY timeStamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getPersonsPaged(limit: Int, offset: Int): List<PersonEntity>

    @Query("SELECT * FROM persons ORDER BY timeStamp DESC LIMIT :limit OFFSET :offset")
    fun getPersonsPagedFlow(limit: Int, offset: Int): Flow<List<PersonEntity>>

    @Query("SELECT COUNT(*) FROM persons")
    suspend fun getPersonCount(): Int

    @Query(
        "SELECT * FROM persons WHERE name LIKE '%' || :query || '%' ORDER BY timeStamp DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun searchPersonsPaged(query: String, limit: Int, offset: Int): List<PersonEntity>

    @Query("SELECT * FROM persons WHERE id = :id LIMIT 1")
    suspend fun getPersonById(id: String): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersons(persons: List<PersonEntity>)

    @Delete
    suspend fun deletePerson(person: PersonEntity)

    @Query("DELETE FROM persons WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM persons")
    suspend fun clearAll()
}
