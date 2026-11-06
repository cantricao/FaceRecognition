package com.example.detectionexample.appfunctions

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.os.CancellationSignal
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.metadata.AppFunctionMetadata
import com.example.detectionexample.models.Person
import com.example.detectionexample.repository.RecognizedPersonRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.function.Consumer

class FaceRecognitionAppFunctionServiceTest {

    private lateinit var mockRepository: RecognizedPersonRepository
    private lateinit var service: TestFaceRecognitionAppFunctionService
    private lateinit var mockBitmap: Bitmap

    private class TestFaceRecognitionAppFunctionService : BaseFaceRecognitionAppFunctionService() {
        fun initContext(context: Context) {
            attachBaseContext(context)
        }

        override fun onExecuteFunction(
            request: ExecuteAppFunctionRequest,
            metadata: AppFunctionMetadata,
            cancellationSignal: CancellationSignal,
            callback: Consumer<ExecuteAppFunctionResponse>
        ) {
            // no-op for direct unit testing of declared AppFunctions
        }
    }

    @Before
    fun setUp() {
        mockkStatic(Bitmap::class)
        mockkStatic(PendingIntent::class)

        mockBitmap = mockk(relaxed = true)
        every { Bitmap.createBitmap(any<Int>(), any<Int>(), any<Bitmap.Config>()) } returns mockBitmap

        val mockPendingIntent = mockk<PendingIntent>()
        every { PendingIntent.getActivity(any(), any(), any(), any()) } returns mockPendingIntent

        val mockContext = mockk<Context>(relaxed = true)
        mockRepository = mockk(relaxed = true)
        service = TestFaceRecognitionAppFunctionService().apply {
            initContext(mockContext)
            recognizedPersonRepository = mockRepository
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun getRegisteredPersonCount_returnsCorrectCount() = runTest {
        coEvery { mockRepository.getPersonCount() } returns 5

        val count = service.getRegisteredPersonCount()

        assertEquals(5, count)
        coVerify(exactly = 1) { mockRepository.getPersonCount() }
    }

    @Test
    fun searchPersons_withQuery_returnsMatchingPersons() = runTest {
        val testPerson = Person(
            id = "test-123",
            name = "Alice",
            score = 98,
            timeStamp = 1700000000000L,
            face = mockBitmap
        )
        coEvery { mockRepository.searchPersonsPaged("Alice", 10, 0) } returns listOf(testPerson)

        val results = service.searchPersons(query = "Alice", limit = 10)

        assertEquals(1, results.size)
        assertEquals("test-123", results[0].id)
        assertEquals("Alice", results[0].name)
        assertEquals(98, results[0].score)
        assertEquals(1700000000000L, results[0].timeStamp)
    }

    @Test
    fun searchPersons_withNullQuery_returnsRecentPersons() = runTest {
        val testPerson = Person(
            id = "test-456",
            name = "Bob",
            score = 85,
            timeStamp = 1700000050000L,
            face = mockBitmap
        )
        coEvery { mockRepository.getPersonsPaged(10, 0) } returns listOf(testPerson)

        val results = service.searchPersons(query = null, limit = 0)

        assertEquals(1, results.size)
        assertEquals("Bob", results[0].name)
        coVerify(exactly = 1) { mockRepository.getPersonsPaged(10, 0) }
    }

    @Test
    fun deletePersonById_whenPersonExists_deletesAndReturnsTrue() = runTest {
        val testPerson = Person(id = "target-id", name = "Charlie", face = mockBitmap)
        coEvery { mockRepository.getPersonById("target-id") } returns testPerson
        coEvery { mockRepository.deletePerson(testPerson) } returns Unit

        val result = service.deletePersonById("target-id")

        assertTrue(result)
        coVerify(exactly = 1) { mockRepository.deletePerson(testPerson) }
    }

    @Test
    fun deletePersonById_whenBlank_throwsInvalidArgumentException() = runTest {
        var thrown = false
        try {
            service.deletePersonById("   ")
        } catch (e: AppFunctionInvalidArgumentException) {
            thrown = true
        }
        assertTrue("Expected AppFunctionInvalidArgumentException", thrown)
    }

    @Test
    fun deletePersonById_whenNotFound_throwsElementNotFoundException() = runTest {
        coEvery { mockRepository.getPersonById("non-existent") } returns null
        var thrown = false
        try {
            service.deletePersonById("non-existent")
        } catch (e: AppFunctionElementNotFoundException) {
            thrown = true
        }
        assertTrue("Expected AppFunctionElementNotFoundException", thrown)
    }

    @Test
    fun openLiveDetection_returnsValidPendingIntent() = runTest {
        val pendingIntent = service.openLiveDetection()
        assertNotNull(pendingIntent)
    }
}
