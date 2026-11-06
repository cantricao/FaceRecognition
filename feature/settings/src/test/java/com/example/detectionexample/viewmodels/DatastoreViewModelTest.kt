package com.example.detectionexample.viewmodels

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.detectionexample.models.Person
import com.example.detectionexample.models.TrackedRecognition
import com.example.detectionexample.repository.ExtractorRepository
import com.example.detectionexample.repository.RecognizedPersonRepository
import com.example.detectionexample.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DatastoreViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var recognizedPersonRepository: RecognizedPersonRepository
    private lateinit var extractorRepository: ExtractorRepository
    private lateinit var viewModel: DatastoreViewModel
    private val dummyBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

    @Before
    fun setUp() {
        dataStore = mockk(relaxed = true)
        val personDao = mockk<com.example.detectionexample.data.local.PersonDao>(relaxed = true)
        recognizedPersonRepository = RecognizedPersonRepository(dataStore, personDao)
        extractorRepository = mockk {
            every { extractImage(any()) } returns floatArrayOf(0.25f, 0.5f, 0.75f)
        }
        viewModel = DatastoreViewModel(recognizedPersonRepository, extractorRepository)
    }

    @Test
    fun isRegisteredPersonEmpty_initiallyReturnsTrue() {
        assertTrue(viewModel.isRegisteredPersonEmpty())
        assertTrue(viewModel.getRegisteredPerson().isEmpty())
    }

    @Test
    fun addPerson_extractsEmbeddingsAndRegistersPerson() = runTest {
        val tracked = TrackedRecognition(
            title = "Jane Doe",
            location = RectF(0f, 0f, 100f, 100f),
            detectionConfidence = 0.98f,
            color = 0
        )

        viewModel.addPerson(tracked, dummyBitmap)

        assertFalse(viewModel.isRegisteredPersonEmpty())
        val registered = viewModel.getRegisteredPerson()
        assertEquals(1, registered.size)
        assertEquals("Jane Doe", registered.first().name)
        assertEquals(98, registered.first().score)
    }

    @Test
    fun removeRegisteredPerson_removesTargetPerson() = runTest {
        val person = Person(id = "id1", name = "Alice", score = 90, embeddings = floatArrayOf(0.1f), face = dummyBitmap)
        recognizedPersonRepository.registerPerson(person)

        assertEquals(1, viewModel.getRegisteredPerson().size)
        viewModel.removeRegisteredPerson(person)

        // Await completion of background IO job
        var isEmpty = viewModel.isRegisteredPersonEmpty()
        var retries = 0
        while (!isEmpty && retries < 20) {
            Thread.sleep(50)
            isEmpty = viewModel.isRegisteredPersonEmpty()
            retries++
        }
        assertTrue(isEmpty)
    }

    @Test
    fun paginationState_initialDefaultValues() {
        val state = viewModel.paginationState.value
        assertEquals(0, state.page)
        assertTrue(state.items.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.hasMore)
    }

    @Test
    fun refreshPagination_loadsFirstPageFromRepository() = runTest {
        viewModel.refreshPagination()
        var page = viewModel.paginationState.value.page
        var retries = 0
        while (page == 0 && retries < 20) {
            Thread.sleep(50)
            page = viewModel.paginationState.value.page
            retries++
        }
        assertEquals(1, page)
        assertFalse(viewModel.paginationState.value.isLoading)
    }
}
