package com.example.detectionexample.engage

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.engage.common.datamodel.ClusterType
import com.google.android.engage.service.AppEngageErrorCode
import com.google.android.engage.service.AppEngageException
import com.google.android.engage.service.AppEngagePublishClient
import com.google.android.engage.service.AppEngagePublishStatusCode
import com.google.android.engage.service.PublishStatusRequest
import com.google.android.engage.service.ServiceAvailabilityRequest
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.tasks.await

class EngageWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "EngageWorker"
    }

    private val client = AppEngagePublishClient(context)
    private val clusterRequestFactory = ClusterRequestFactory(context)

    override suspend fun doWork(): Result {
        if (runAttemptCount > Constants.MAX_PUBLISHING_ATTEMPTS) {
            updatePublishStatus(AppEngagePublishStatusCode.NOT_PUBLISHED_SERVICE_ERROR)
            return Result.failure()
        }

        val publishType = inputData.getString(Constants.PUBLISH_TYPE_KEY)
        val intendedClusterType = when (publishType) {
            Constants.PUBLISH_TYPE_RECOMMENDATIONS -> ClusterType.TYPE_RECOMMENDATION
            Constants.PUBLISH_TYPE_FEATURED -> ClusterType.TYPE_FEATURED
            Constants.PUBLISH_TYPE_CONTINUATION -> ClusterType.TYPE_CONTINUATION
            Constants.PUBLISH_TYPE_USER_ACCOUNT_MANAGEMENT -> ClusterType.TYPE_ENGAGEMENT
            else -> ClusterType.TYPE_UNKNOWN
        }

        if (intendedClusterType != ClusterType.TYPE_UNKNOWN) {
            val request = ServiceAvailabilityRequest.Builder()
                .addIntendedClusterType(intendedClusterType)
                .build()
            try {
                val availabilityMap = client.isServiceAvailable(request).await()
                if (availabilityMap[intendedClusterType] != true) {
                    Log.w(TAG, "Cluster type $intendedClusterType is not available on this device.")
                    return Result.failure()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking service availability", e)
                return Result.failure()
            }
        }

        return when (publishType) {
            Constants.PUBLISH_TYPE_RECOMMENDATIONS -> publishRecommendations()
            Constants.PUBLISH_TYPE_FEATURED -> publishFeatured()
            Constants.PUBLISH_TYPE_CONTINUATION -> publishContinuation()
            Constants.PUBLISH_TYPE_USER_ACCOUNT_MANAGEMENT -> publishUserAccountManagement()
            else -> Result.failure()
        }
    }

    private suspend fun publishRecommendations(): Result {
        val recommendations = getRecommendationsData()
        if (recommendations.isEmpty()) {
            Log.d(TAG, "No recommendation data available to publish.")
            return Result.success()
        }

        val request = clusterRequestFactory.constructRecommendationClustersRequest(recommendations)
        val publishTask: Task<Void> = client.publishRecommendationClusters(request)
        return publishAndProvideResult(publishTask)
    }

    private suspend fun publishFeatured(): Result {
        val featuredItem = getFeaturedData()
        if (featuredItem == null) {
            Log.d(TAG, "No featured data available to publish.")
            return Result.success()
        }

        val request = clusterRequestFactory.constructFeaturedClusterRequest(featuredItem)
        val publishTask: Task<Void> = client.publishFeaturedCluster(request)
        return publishAndProvideResult(publishTask)
    }

    private suspend fun publishContinuation(): Result {
        val continuationData = getContinuationData()
        // Empty Continuation Guard: If there is no continuation content,
        // we must delete the cluster instead of publishing empty data on the UI.
        if (continuationData.isEmpty()) {
            val deleteTask = client.deleteContinuationCluster()
            return publishAndProvideResult(deleteTask)
        }

        val request = clusterRequestFactory.constructContinuationClusterRequest(continuationData)
        val publishTask: Task<Void> = client.publishContinuationCluster(request)
        return publishAndProvideResult(publishTask)
    }

    private suspend fun publishUserAccountManagement(): Result {
        val publishTask: Task<Void>
        if (isAccountSignedIn()) {
            publishTask = client.deleteUserManagementCluster()
            return publishAndProvideResult(publishTask)
        } else {
            publishTask = client.publishUserAccountManagementRequest(
                clusterRequestFactory.constructUserAccountManagementClusterRequest()
            )
            return try {
                publishTask.await()
                updatePublishStatus(AppEngagePublishStatusCode.NOT_PUBLISHED_REQUIRES_SIGN_IN)
                Result.success()
            } catch (publishException: Exception) {
                handlePublishException(publishException)
            }
        }
    }

    private fun isAccountSignedIn(): Boolean = true

    private fun getRecommendationsData(): List<EngageMediaItem> {
        val placeholderUri = Uri.parse("android.resource://${context.packageName}/drawable/ic_launcher_foreground")
        return listOf(
            EngageMediaItem(
                id = "demo_analysis_walking",
                title = "Pedestrian Demographics Walking Analysis",
                description = "Automated face demographics and landmark tracking session.",
                playBackUri = Uri.parse("facerecognition://video/playback?asset=face-demographics-walking-and-pause.mp4"),
                posterUri = placeholderUri,
                durationMillis = 180_000L
            ),
            EngageMediaItem(
                id = "demo_analysis_crowd",
                title = "Multi-Face Crowd Recognition Stream",
                description = "High-density multi-face tracking and recognition pipeline.",
                playBackUri = Uri.parse("facerecognition://video/playback?asset=crowd_analysis.mp4"),
                posterUri = placeholderUri,
                durationMillis = 240_000L
            )
        )
    }

    private fun getFeaturedData(): EngageMediaItem {
        val placeholderUri = Uri.parse("android.resource://${context.packageName}/drawable/ic_launcher_foreground")
        return EngageMediaItem(
            id = "featured_walking_demographics",
            title = "Featured: Real-Time Face Demographics Video Demo",
            description = "Explore real-time facial feature and identity verification with LiteRT.",
            playBackUri = Uri.parse("facerecognition://video/playback?asset=face-demographics-walking-and-pause.mp4"),
            posterUri = placeholderUri,
            durationMillis = 180_000L
        )
    }

    private fun getContinuationData(): List<EngageMediaItem> {
        val placeholderUri = Uri.parse("android.resource://${context.packageName}/drawable/ic_launcher_foreground")
        return listOf(
            EngageMediaItem(
                id = "continue_demo_walking",
                title = "Resume: Walking Face Analysis Session",
                description = "Continue playback at 00:45",
                playBackUri = Uri.parse("facerecognition://video/playback?asset=face-demographics-walking-and-pause.mp4"),
                posterUri = placeholderUri,
                durationMillis = 180_000L,
                lastPlaybackPositionMillis = 45_000L,
                lastEngagementTimeMillis = System.currentTimeMillis() - 3600_000L
            )
        )
    }

    private suspend fun publishAndProvideResult(publishTask: Task<Void>): Result {
        return try {
            publishTask.await()
            updatePublishStatus(AppEngagePublishStatusCode.PUBLISHED)
            Result.success()
        } catch (publishException: Exception) {
            handlePublishException(publishException)
        }
    }

    private fun handlePublishException(publishException: Exception): Result {
        val appEngageException = publishException as? AppEngageException
        if (appEngageException != null) {
            logPublishing(appEngageException)

            val errorStatusCode = when (appEngageException.errorCode) {
                AppEngageErrorCode.SERVICE_CALL_INVALID_ARGUMENT ->
                    AppEngagePublishStatusCode.NOT_PUBLISHED_CLIENT_ERROR

                AppEngageErrorCode.SERVICE_CALL_PERMISSION_DENIED ->
                    AppEngagePublishStatusCode.NOT_PUBLISHED_CLIENT_ERROR

                else ->
                    AppEngagePublishStatusCode.NOT_PUBLISHED_SERVICE_ERROR
            }
            updatePublishStatus(errorStatusCode)

            return if (isErrorRecoverable(appEngageException)) Result.retry() else Result.failure()
        }
        return Result.failure()
    }

    private fun updatePublishStatus(statusCode: Int) {
        client.updatePublishStatus(
            PublishStatusRequest.Builder().setStatusCode(statusCode).build()
        ).addOnSuccessListener {
            Log.i(TAG, "Successfully updated publish status code to $statusCode")
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to update publish status code to $statusCode: ${exception.message}")
        }
    }

    private fun logPublishing(publishingException: AppEngageException) {
        val message = when (publishingException.errorCode) {
            AppEngageErrorCode.SERVICE_NOT_FOUND -> "Service not found"
            AppEngageErrorCode.SERVICE_CALL_EXECUTION_FAILURE -> "Execution failure"
            AppEngageErrorCode.SERVICE_NOT_AVAILABLE -> "Service not available"
            AppEngageErrorCode.SERVICE_CALL_PERMISSION_DENIED -> "Permission denied"
            AppEngageErrorCode.SERVICE_CALL_INVALID_ARGUMENT -> "Invalid argument"
            AppEngageErrorCode.SERVICE_CALL_INTERNAL -> "Internal error"
            AppEngageErrorCode.SERVICE_CALL_RESOURCE_EXHAUSTED -> "Resource exhausted"
            else -> "Unknown error"
        }
        Log.d(TAG, "Publishing error: $message (code: ${publishingException.errorCode})")
    }

    private fun isErrorRecoverable(publishingException: AppEngageException): Boolean {
        return when (publishingException.errorCode) {
            AppEngageErrorCode.SERVICE_CALL_EXECUTION_FAILURE,
            AppEngageErrorCode.SERVICE_CALL_INTERNAL,
            AppEngageErrorCode.SERVICE_CALL_RESOURCE_EXHAUSTED -> true
            AppEngageErrorCode.SERVICE_NOT_FOUND,
            AppEngageErrorCode.SERVICE_CALL_INVALID_ARGUMENT,
            AppEngageErrorCode.SERVICE_CALL_PERMISSION_DENIED,
            AppEngageErrorCode.SERVICE_NOT_AVAILABLE -> false
            else -> false
        }
    }
}
