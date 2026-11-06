package com.example.detectionexample.tv

import com.example.detectionexample.tv.model.TvMediaCatalog
import com.example.detectionexample.tv.model.TvVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for TV components, catalog data grouping, seeking calculations, and navigation routes.
 */
class TvScreenTest {

    @Test
    fun catalog_groupsVideosByCategoryCorrectly() {
        val categorized = TvMediaCatalog.getCategorizedVideos()

        assertTrue("Expected at least one category", categorized.isNotEmpty())
        assertTrue("Expected Surveillance Feeds category", categorized.containsKey("Surveillance Feeds"))
        assertTrue("Expected Access Logs category", categorized.containsKey("Access Logs"))
        assertTrue("Expected Perimeter Alerts category", categorized.containsKey("Perimeter Alerts"))

        val totalCategorized = categorized.values.sumOf { it.size }
        assertEquals("Total videos across categories should match catalog size", TvMediaCatalog.sampleVideos.size, totalCategorized)
    }

    @Test
    fun catalog_allVideosHaveValidAttributes() {
        for (video in TvMediaCatalog.sampleVideos) {
            assertTrue("Video id should not be blank", video.id.isNotBlank())
            assertTrue("Video title should not be blank", video.title.isNotBlank())
            assertTrue("Video description should not be blank", video.description.isNotBlank())
            assertTrue("Video URL should start with http", video.videoUrl.startsWith("http"))
            assertTrue("Card image URL should start with http", video.cardImageUrl.startsWith("http"))
            assertTrue("Background image URL should start with http", video.bgImageUrl.startsWith("http"))
            assertTrue("Duration should follow mm:ss format", video.duration.matches(Regex("\\d{2}:\\d{2}")))
        }
    }

    @Test
    fun playbackSeeking_computesCorrectBounds() {
        val duration = 90_000L // 90 seconds
        val step = 10_000L // 10 seconds

        // Rewind near start clamps to 0
        val posAtStart = 5_000L
        val rewindClamped = (posAtStart - step).coerceAtLeast(0L)
        assertEquals(0L, rewindClamped)

        // Normal rewind
        val posMid = 40_000L
        val rewindNormal = (posMid - step).coerceAtLeast(0L)
        assertEquals(30_000L, rewindNormal)

        // Fast forward near end clamps to duration
        val posNearEnd = 85_000L
        val forwardClamped = (posNearEnd + step).coerceAtMost(duration)
        assertEquals(duration, forwardClamped)

        // Normal fast forward
        val forwardNormal = (posMid + step).coerceAtMost(duration)
        assertEquals(50_000L, forwardNormal)
    }

    @Test
    fun routes_supportsAllTvDestinations() {
        val sampleVideo = TvMediaCatalog.sampleVideos.first()

        val browseRoute: TvRoute = TvRoute.Browse
        val playbackRoute: TvRoute = TvRoute.Playback(sampleVideo)
        val settingsRoute: TvRoute = TvRoute.Settings
        val guidedRoute: TvRoute = TvRoute.GuidedStep
        val searchRoute: TvRoute = TvRoute.Search

        assertEquals(TvRoute.Browse, browseRoute)
        assertTrue(playbackRoute is TvRoute.Playback)
        assertEquals(sampleVideo, (playbackRoute as TvRoute.Playback).video)
        assertEquals(TvRoute.Settings, settingsRoute)
        assertEquals(TvRoute.GuidedStep, guidedRoute)
        assertEquals(TvRoute.Search, searchRoute)
    }

    @Test
    fun sampleVideos_containUniqueIds() {
        val ids = TvMediaCatalog.sampleVideos.map { it.id }
        val uniqueIds = ids.toSet()
        assertEquals("All sample video IDs must be distinct", ids.size, uniqueIds.size)
    }
}
