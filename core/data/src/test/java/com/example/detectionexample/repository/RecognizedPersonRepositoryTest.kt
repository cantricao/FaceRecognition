package com.example.detectionexample.repository

import android.graphics.Bitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.detectionexample.models.Person
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecognizedPersonRepositoryTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var personDao: com.example.detectionexample.data.local.PersonDao
    private lateinit var repository: RecognizedPersonRepository
    private val dummyBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

    @Before
    fun setUp() {
        dataStore = mockk(relaxed = true)
        personDao = mockk(relaxed = true)
        repository = RecognizedPersonRepository(dataStore, personDao)
    }

    @Test
    fun initially_repositoryIsEmpty() {
        assertTrue(repository.isEmpty())
        assertTrue(repository.getRegisteredPerson().isEmpty())
    }

    @Test
    fun registerPerson_addsPersonSuccessfully() {
        val person = Person(
            id = "1",
            name = "Alice",
            score = 95,
            embeddings = floatArrayOf(0.1f, 0.2f, 0.3f),
            face = dummyBitmap
        )

        repository.registerPerson(person)

        assertFalse(repository.isEmpty())
        assertEquals(1, repository.getRegisteredPerson().size)
        assertEquals("Alice", repository.getRegisteredPerson().first().name)
    }

    @Test
    fun findNearest_returnsClosestMatchWithinThreshold() {
        val alice = Person(
            id = "1",
            name = "Alice",
            score = 95,
            embeddings = floatArrayOf(0.1f, 0.2f, 0.3f),
            face = dummyBitmap
        )
        val bob = Person(
            id = "2",
            name = "Bob",
            score = 90,
            embeddings = floatArrayOf(0.9f, 0.8f, 0.7f),
            face = dummyBitmap
        )
        repository.registerPerson(alice)
        repository.registerPerson(bob)

        // Query close to Alice
        val query = floatArrayOf(0.12f, 0.21f, 0.31f)
        val match = repository.findNearest(query, threshold = 0.5f)

        assertNotNull(match)
        assertEquals("Alice", match?.first)
    }

    @Test
    fun findNearest_returnsNullWhenDistanceExceedsThreshold() {
        val alice = Person(
            id = "1",
            name = "Alice",
            score = 95,
            embeddings = floatArrayOf(0.1f, 0.2f, 0.3f),
            face = dummyBitmap
        )
        repository.registerPerson(alice)

        // Query far away with tight threshold
        val query = floatArrayOf(0.9f, 0.9f, 0.9f)
        val match = repository.findNearest(query, threshold = 0.1f)

        assertNull(match)
    }

    @Test
    fun removeRegisteredPerson_removesSpecifiedPerson() {
        val person = Person(
            id = "1",
            name = "Alice",
            score = 95,
            embeddings = floatArrayOf(0.1f, 0.2f, 0.3f),
            face = dummyBitmap
        )
        repository.registerPerson(person)
        assertEquals(1, repository.getRegisteredPerson().size)

        repository.removeRegisteredPerson(person)
        assertTrue(repository.isEmpty())
    }

    @Test
    fun clear_removesAllPersons() {
        repository.registerPerson(
            Person(id = "1", name = "Alice", score = 95, embeddings = floatArrayOf(0.1f), face = dummyBitmap)
        )
        repository.registerPerson(
            Person(id = "2", name = "Bob", score = 90, embeddings = floatArrayOf(0.2f), face = dummyBitmap)
        )
        assertEquals(2, repository.getRegisteredPerson().size)

        repository.clear()
        assertTrue(repository.isEmpty())
    }

    @Test
    fun findNearestMatch_withCosineMetric() {
        val alice = Person(
            id = "1",
            name = "Alice",
            score = 95,
            embeddings = floatArrayOf(1.0f, 0.0f, 0.0f),
            face = dummyBitmap
        )
        val bob = Person(
            id = "2",
            name = "Bob",
            score = 90,
            embeddings = floatArrayOf(0.0f, 1.0f, 0.0f),
            face = dummyBitmap
        )
        repository.registerPerson(alice)
        repository.registerPerson(bob)

        val query = floatArrayOf(0.95f, 0.05f, 0.0f)
        val match = repository.findNearestMatch(
            emb = query,
            threshold = 0.3f,
            metric = com.example.detectionexample.core.common.vector.DistanceMetric.COSINE
        )

        assertNotNull(match)
        assertEquals("Alice", match?.item?.name)
        assertTrue(match!!.similarity > 0.9f)
    }

    @Test
    fun findTopK_returnsTopMatches() {
        val p1 = Person(id = "1", name = "P1", score = 80, embeddings = floatArrayOf(0.1f), face = dummyBitmap)
        val p2 = Person(id = "2", name = "P2", score = 80, embeddings = floatArrayOf(0.5f), face = dummyBitmap)
        val p3 = Person(id = "3", name = "P3", score = 80, embeddings = floatArrayOf(0.2f), face = dummyBitmap)
        repository.registerPerson(p1)
        repository.registerPerson(p2)
        repository.registerPerson(p3)

        val query = floatArrayOf(0.0f)
        val top2 = repository.findTopK(query, k = 2)

        assertEquals(2, top2.size)
        assertEquals("P1", top2[0].item.name)
        assertEquals("P3", top2[1].item.name)
    }

    @Test
    fun savePerson_persistsToRoomAndUpdatesCache() = kotlinx.coroutines.test.runTest {
        val person = Person(id = "1", name = "Charlie", score = 90, embeddings = floatArrayOf(0.3f), face = dummyBitmap)
        repository.savePerson(person)

        assertEquals(1, repository.getRegisteredPerson().size)
        assertEquals("Charlie", repository.getRegisteredPerson().first().name)
        io.mockk.coVerify { personDao.insertPerson(any()) }
    }

    @Test
    fun deletePerson_removesFromRoomAndCache() = kotlinx.coroutines.test.runTest {
        val person = Person(id = "1", name = "Charlie", score = 90, embeddings = floatArrayOf(0.3f), face = dummyBitmap)
        repository.registerPerson(person)
        assertEquals(1, repository.getRegisteredPerson().size)

        repository.deletePerson(person)
        assertTrue(repository.isEmpty())
        io.mockk.coVerify { personDao.deleteById("1") }
    }

    @Test
    fun clearRegisteredPerson_clearsRoomAndCache() = kotlinx.coroutines.test.runTest {
        val person = Person(id = "1", name = "Charlie", score = 90, embeddings = floatArrayOf(0.3f), face = dummyBitmap)
        repository.registerPerson(person)

        repository.clearRegisteredPerson()
        assertTrue(repository.isEmpty())
        io.mockk.coVerify { personDao.clearAll() }
    }
}
