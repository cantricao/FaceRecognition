package com.example.detectionexample.engage

import android.net.Uri
import com.google.android.engage.common.datamodel.ContentAvailability
import com.google.android.engage.common.datamodel.Image
import com.google.android.engage.video.datamodel.MovieEntity
import com.google.android.engage.video.datamodel.RatingSystem
import com.google.android.engage.video.datamodel.WatchNextType

/**
 * Local domain representation of a video/clip for Play Engage publishing.
 */
data class EngageMediaItem(
    val id: String,
    val title: String,
    val description: String = "",
    val playBackUri: Uri,
    val posterUri: Uri,
    val durationMillis: Long = 180_000L,
    val lastPlaybackPositionMillis: Long = 0L,
    val lastEngagementTimeMillis: Long = System.currentTimeMillis()
)

/**
 * Converts app's local video models to Play Engage's MovieEntity models for the Watch vertical.
 *
 * Requirements:
 * - Strict image aspect ratio (16:9 for landscape, e.g. 1920x1080 or 1280x720).
 * - Dual content rating fields (addContentRating + addContentRatingsLegacy for backward compatibility).
 * - Mandatory fields for ContinuationCluster (lastEngagementTimeMillis, watchNextType, lastPlayBackPositionTimeMillis).
 */
object ItemToEntityConverter {

    fun convert(item: EngageMediaItem, isContinuation: Boolean = false): MovieEntity {
        // Play Engage strict 16:9 aspect ratio image
        val posterImage = Image.Builder()
            .setImageUri(item.posterUri)
            .setImageWidthInPixel(1920)
            .setImageHeightInPixel(1080)
            .setAccessibilityText(item.title)
            .build()

        // Recommended modern RatingSystem
        val ratingSystem = RatingSystem.Builder()
            .setAgencyName("MPAA")
            .setRating("PG")
            .build()

        val builder = MovieEntity.Builder()
            .setEntityId(item.id)
            .setName(item.title)
            .addPosterImage(posterImage)
            .setPlayBackUri(item.playBackUri)
            .setDurationMillis(item.durationMillis)
            .setAvailability(ContentAvailability.AVAILABILITY_AVAILABLE)
            // Dual content rating fields for Watch vertical compatibility across all Google Play surfaces
            .addContentRating(ratingSystem)
            .addContentRatingsLegacy(listOf("MPAA:PG"))

        if (isContinuation) {
            builder
                .setLastEngagementTimeMillis(item.lastEngagementTimeMillis)
                .setWatchNextType(WatchNextType.TYPE_CONTINUE)
                .setLastPlayBackPositionTimeMillis(item.lastPlaybackPositionMillis)
        }

        return builder.build()
    }
}
