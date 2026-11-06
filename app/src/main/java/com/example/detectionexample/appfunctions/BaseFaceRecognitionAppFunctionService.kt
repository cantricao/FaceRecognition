package com.example.detectionexample.appfunctions

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import com.example.detectionexample.MainActivity
import com.example.detectionexample.repository.RecognizedPersonRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Base entry point service exposing face recognition features to Android system AI agents.
 */
@RequiresApi(36)
@AndroidEntryPoint
@AppFunctionServiceEntryPoint(
    serviceName = "FaceRecognitionAppFunctionService",
    appFunctionXmlFileName = "face_recognition_app_function_service",
)
abstract class BaseFaceRecognitionAppFunctionService : AppFunctionService() {

    @Inject
    internal lateinit var recognizedPersonRepository: RecognizedPersonRepository

    /**
     * Searches registered persons by name or lists recent enrollments.
     *
     * If [query] is null or blank, returns the most recently enrolled persons up to [limit].
     *
     * @param query The search query string matching person names, or null to list recent enrollments.
     * @param limit The maximum number of persons to return. If zero or negative, defaults to 10. Maximum allowed is 50.
     * @return A list of [PersonSummary] objects matching the search criteria.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun searchPersons(
        query: String? = null,
        limit: Int = 0,
    ): List<PersonSummary> = withContext(Dispatchers.IO) {
        val resolvedLimit = if (limit > 0) limit.coerceAtMost(50) else 10
        try {
            val persons = if (query.isNullOrBlank()) {
                recognizedPersonRepository.getPersonsPaged(limit = resolvedLimit, offset = 0)
            } else {
                recognizedPersonRepository.searchPersonsPaged(
                    query = query.trim(),
                    limit = resolvedLimit,
                    offset = 0
                )
            }
            persons.map { person ->
                PersonSummary(
                    id = person.id,
                    name = person.name,
                    score = person.score,
                    timeStamp = person.timeStamp,
                )
            }
        } catch (e: Exception) {
            throw AppFunctionInvalidArgumentException("Failed to query registered persons: ${e.message}")
        }
    }

    /**
     * Retrieves the total count of registered persons in the face recognition database.
     *
     * @return The total count of registered individuals.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getRegisteredPersonCount(): Int = withContext(Dispatchers.IO) {
        try {
            recognizedPersonRepository.getPersonCount()
        } catch (e: Exception) {
            throw AppFunctionInvalidArgumentException("Failed to retrieve registered person count: ${e.message}")
        }
    }

    /**
     * Deletes an enrolled person from the face recognition database by unique ID.
     *
     * @param personId The unique identifier of the registered person to delete. Must not be blank.
     * @return True if the person was successfully found and removed.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun deletePersonById(
        personId: String,
    ): Boolean = withContext(Dispatchers.IO) {
        if (personId.isBlank()) {
            throw AppFunctionExceptionFactory.createInvalidArgument("personId must not be blank")
        }
        val person = recognizedPersonRepository.getPersonById(personId)
            ?: throw AppFunctionExceptionFactory.createElementNotFound("No registered person found with ID '$personId'")

        try {
            recognizedPersonRepository.deletePerson(person)
            true
        } catch (e: Exception) {
            throw AppFunctionExceptionFactory.createInvalidArgument(
                "Failed to delete person with ID '$personId': ${e.message}"
            )
        }
    }

    /**
     * Launches the live camera face detection and recognition screen.
     *
     * @return A [PendingIntent] to open the live camera face recognition screen.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun openLiveDetection(): PendingIntent = withContext(Dispatchers.IO) {
        val launchIntent = Intent(this@BaseFaceRecognitionAppFunctionService, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        PendingIntent.getActivity(
            this@BaseFaceRecognitionAppFunctionService,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
