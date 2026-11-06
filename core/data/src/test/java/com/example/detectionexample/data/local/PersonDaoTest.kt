package com.example.detectionexample.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PersonDaoTest {

    private lateinit var database: FaceRecognitionDatabase
    private lateinit var personDao: PersonDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FaceRecognitionDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        personDao = database.personDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetPerson() = runTest {
        val entity = PersonEntity(
            id = "test-id-1",
            name = "Alice",
            score = 95,
            timeStamp = 1000L,
            embeddingsJson = "[0.1, 0.2, 0.3]",
            faceBase64 = "AAAA"
        )

        personDao.insertPerson(entity)

        val retrieved = personDao.getPersonById("test-id-1")
        assertNotNull(retrieved)
        assertEquals("Alice", retrieved?.name)
        assertEquals(95, retrieved?.score)
    }

    @Test
    fun getAllPersons_observesFlow() = runTest {
        val person1 = PersonEntity("id-1", "Bob", 88, 1000L, "[]", "AAAA")
        val person2 = PersonEntity("id-2", "Charlie", 92, 2000L, "[]", "BBBB")

        personDao.getAllPersons().test {
            assertEquals(0, awaitItem().size)

            personDao.insertPerson(person1)
            val listWithOne = awaitItem()
            assertEquals(1, listWithOne.size)
            assertEquals("Bob", listWithOne[0].name)

            personDao.insertPerson(person2)
            val listWithTwo = awaitItem()
            assertEquals(2, listWithTwo.size)
            assertEquals("Charlie", listWithTwo[0].name) // Ordered by timeStamp DESC

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deletePerson_removesFromDb() = runTest {
        val entity = PersonEntity("id-del", "David", 75, 500L, "[]", "CCCC")
        personDao.insertPerson(entity)

        personDao.deleteById("id-del")
        val result = personDao.getPersonById("id-del")
        assertNull(result)
    }

    @Test
    fun clearAll_emptiesTable() = runTest {
        val person1 = PersonEntity("id-1", "Eve", 90, 100L, "[]", "DDDD")
        val person2 = PersonEntity("id-2", "Frank", 85, 200L, "[]", "EEEE")
        personDao.insertPersons(listOf(person1, person2))

        val snapshotBefore = personDao.getAllPersonsSnapshot()
        assertEquals(2, snapshotBefore.size)

        personDao.clearAll()
        val snapshotAfter = personDao.getAllPersonsSnapshot()
        assertTrue(snapshotAfter.isEmpty())
    }

    @Test
    fun getPersonsPaged_limitsAndOffsets() = runTest {
        val people = (1..10).map { index ->
            PersonEntity("id-$index", "Person-$index", 80, index * 1000L, "[]", "AAAA")
        }
        personDao.insertPersons(people)

        val totalCount = personDao.getPersonCount()
        assertEquals(10, totalCount)

        val page1 = personDao.getPersonsPaged(limit = 4, offset = 0)
        assertEquals(4, page1.size)
        assertEquals("Person-10", page1[0].name) // Ordered by timeStamp DESC

        val page2 = personDao.getPersonsPaged(limit = 4, offset = 4)
        assertEquals(4, page2.size)
        assertEquals("Person-6", page2[0].name)

        val page3 = personDao.getPersonsPaged(limit = 4, offset = 8)
        assertEquals(2, page3.size)
        assertEquals("Person-2", page3[0].name)
    }

    @Test
    fun searchPersonsPaged_filtersByName() = runTest {
        val people = listOf(
            PersonEntity("id-1", "John Doe", 90, 1000L, "[]", "AAAA"),
            PersonEntity("id-2", "Jane Smith", 85, 2000L, "[]", "BBBB"),
            PersonEntity("id-3", "Johnny Bravo", 80, 3000L, "[]", "CCCC")
        )
        personDao.insertPersons(people)

        val searchResult = personDao.searchPersonsPaged(query = "John", limit = 10, offset = 0)
        assertEquals(2, searchResult.size)
        assertEquals("Johnny Bravo", searchResult[0].name)
        assertEquals("John Doe", searchResult[1].name)
    }
}
