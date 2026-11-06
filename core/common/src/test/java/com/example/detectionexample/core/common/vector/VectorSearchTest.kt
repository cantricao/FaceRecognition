package com.example.detectionexample.core.common.vector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VectorSearchTest {

    private data class Candidate(val id: String, val vector: FloatArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Candidate) return false
            return id == other.id && vector.contentEquals(other.vector)
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + vector.contentHashCode()
            return result
        }
    }

    @Test
    fun testEuclideanDistanceIdenticalVectors() {
        val a = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f)
        val b = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f)
        val dist = VectorSearch.euclideanDistance(a, b)
        assertEquals(0.0f, dist, 0.0001f)
    }

    @Test
    fun testEuclideanDistanceCalculatesCorrectly() {
        val a = floatArrayOf(0.0f, 0.0f, 0.0f)
        val b = floatArrayOf(3.0f, 4.0f, 0.0f)
        val dist = VectorSearch.euclideanDistance(a, b)
        assertEquals(5.0f, dist, 0.0001f)
    }

    @Test
    fun testEuclideanDistanceThresholdPruning() {
        val a = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
        val b = floatArrayOf(10.0f, 10.0f, 10.0f, 10.0f)
        val dist = VectorSearch.euclideanDistance(a, b, maxDistance = 2.0f)
        assertEquals(Float.MAX_VALUE, dist)
    }

    @Test
    fun testCosineDistanceIdenticalVectors() {
        val a = floatArrayOf(1.0f, 2.0f, 3.0f)
        val b = floatArrayOf(2.0f, 4.0f, 6.0f) // collinear in same direction
        val dist = VectorSearch.cosineDistance(a, b)
        assertEquals(0.0f, dist, 0.0001f)
    }

    @Test
    fun testCosineDistanceOppositeVectors() {
        val a = floatArrayOf(1.0f, 0.0f)
        val b = floatArrayOf(-1.0f, 0.0f)
        val dist = VectorSearch.cosineDistance(a, b)
        assertEquals(2.0f, dist, 0.0001f)
    }

    @Test
    fun testFindNearestMatchesClosestCandidate() {
        val query = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
        val candidates = listOf(
            Candidate("c1", floatArrayOf(5.0f, 5.0f, 5.0f, 5.0f)),
            Candidate("c2", floatArrayOf(1.1f, 0.9f, 1.0f, 1.0f)),
            Candidate("c3", floatArrayOf(2.0f, 2.0f, 2.0f, 2.0f))
        )

        val match = VectorSearch.findNearest(
            query = query,
            candidates = candidates,
            threshold = 1.0f,
            metric = DistanceMetric.EUCLIDEAN,
            vectorSelector = { it.vector }
        )

        assertNotNull(match)
        assertEquals("c2", match?.item?.id)
        assertTrue(match!!.distance < 0.2f)
    }

    @Test
    fun testFindNearestReturnsNullWhenAboveThreshold() {
        val query = floatArrayOf(1.0f, 1.0f)
        val candidates = listOf(
            Candidate("c1", floatArrayOf(10.0f, 10.0f))
        )

        val match = VectorSearch.findNearest(
            query = query,
            candidates = candidates,
            threshold = 1.0f,
            metric = DistanceMetric.EUCLIDEAN,
            vectorSelector = { it.vector }
        )

        assertNull(match)
    }

    @Test
    fun testFindTopKReturnsSortedMatches() {
        val query = floatArrayOf(0.0f, 0.0f)
        val candidates = listOf(
            Candidate("c3", floatArrayOf(3.0f, 0.0f)),
            Candidate("c1", floatArrayOf(1.0f, 0.0f)),
            Candidate("c4", floatArrayOf(4.0f, 0.0f)),
            Candidate("c2", floatArrayOf(2.0f, 0.0f))
        )

        val top2 = VectorSearch.findTopK(
            query = query,
            candidates = candidates,
            k = 2,
            metric = DistanceMetric.EUCLIDEAN,
            vectorSelector = { it.vector }
        )

        assertEquals(2, top2.size)
        assertEquals("c1", top2[0].item.id)
        assertEquals("c2", top2[1].item.id)
    }
}
