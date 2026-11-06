package com.example.detectionexample.core.common.vector

import kotlin.math.sqrt

enum class DistanceMetric {
    EUCLIDEAN,
    COSINE,
    SQUARED_EUCLIDEAN
}

data class VectorMatch<T>(
    val item: T,
    val distance: Float,
    val similarity: Float
)

object VectorSearch {
    private const val UNROLL_BLOCK_SIZE = 4
    private const val MIN_COSINE_SIMILARITY = -1.0f
    private const val MAX_COSINE_SIMILARITY = 1.0f
    private const val BASE_SIMILARITY_DIVISOR = 1.0f

    /**
     * Computes the Euclidean distance between two vectors with optional early-exit threshold pruning.
     * Aborts as soon as the running sum of squared differences exceeds maxDistance^2.
     */
    fun euclideanDistance(
        a: FloatArray,
        b: FloatArray,
        maxDistance: Float = Float.MAX_VALUE
    ): Float {
        val len = minOf(a.size, b.size)
        if (len == 0) return Float.MAX_VALUE

        val maxDistSq = if (maxDistance < Float.MAX_VALUE) maxDistance * maxDistance else Float.MAX_VALUE
        var sumSq = 0f

        var i = 0
        val unrollLimit = len - (UNROLL_BLOCK_SIZE - 1)
        while (i < unrollLimit) {
            val d0 = a[i] - b[i]
            val d1 = a[i + 1] - b[i + 1]
            val d2 = a[i + 2] - b[i + 2]
            val d3 = a[i + 3] - b[i + 3]
            sumSq += d0 * d0 + d1 * d1 + d2 * d2 + d3 * d3
            if (sumSq > maxDistSq) return Float.MAX_VALUE
            i += UNROLL_BLOCK_SIZE
        }
        while (i < len) {
            val diff = a[i] - b[i]
            sumSq += diff * diff
            if (sumSq > maxDistSq) return Float.MAX_VALUE
            i++
        }
        return sqrt(sumSq)
    }

    /**
     * Computes Cosine distance: 1.0 - cosine_similarity.
     * Normalized Range: [0.0 (identical), 2.0 (opposite directions)].
     */
    fun cosineDistance(a: FloatArray, b: FloatArray): Float {
        val len = minOf(a.size, b.size)
        if (len == 0) return Float.MAX_VALUE

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        var i = 0
        val unrollLimit = len - (UNROLL_BLOCK_SIZE - 1)
        while (i < unrollLimit) {
            val a0 = a[i]
            val b0 = b[i]
            val a1 = a[i + 1]
            val b1 = b[i + 1]
            val a2 = a[i + 2]
            val b2 = b[i + 2]
            val a3 = a[i + 3]
            val b3 = b[i + 3]

            dotProduct += a0 * b0 + a1 * b1 + a2 * b2 + a3 * b3
            normA += a0 * a0 + a1 * a1 + a2 * a2 + a3 * a3
            normB += b0 * b0 + b1 * b1 + b2 * b2 + b3 * b3
            i += UNROLL_BLOCK_SIZE
        }
        while (i < len) {
            val ai = a[i]
            val bi = b[i]
            dotProduct += ai * bi
            normA += ai * ai
            normB += bi * bi
            i++
        }

        if (normA <= 0f || normB <= 0f) return Float.MAX_VALUE
        val similarity = (dotProduct / (sqrt(normA) * sqrt(normB))).coerceIn(
            MIN_COSINE_SIMILARITY,
            MAX_COSINE_SIMILARITY
        )
        return BASE_SIMILARITY_DIVISOR - similarity
    }

    /**
     * Computes distance based on selected metric.
     */
    fun distance(
        a: FloatArray,
        b: FloatArray,
        metric: DistanceMetric,
        maxDistance: Float = Float.MAX_VALUE
    ): Float = when (metric) {
        DistanceMetric.EUCLIDEAN -> euclideanDistance(a, b, maxDistance)
        DistanceMetric.COSINE -> cosineDistance(a, b)
        DistanceMetric.SQUARED_EUCLIDEAN -> {
            val d = euclideanDistance(a, b, maxDistance)
            if (d == Float.MAX_VALUE) Float.MAX_VALUE else d * d
        }
    }

    /**
     * Zero-allocation nearest neighbor vector search across candidate collection.
     */
    fun <T> findNearest(
        query: FloatArray,
        candidates: List<T>,
        threshold: Float,
        metric: DistanceMetric = DistanceMetric.EUCLIDEAN,
        vectorSelector: (T) -> FloatArray
    ): VectorMatch<T>? {
        if (candidates.isEmpty() || query.isEmpty()) return null

        var bestItem: T? = null
        var minDistance = threshold

        for (i in candidates.indices) {
            val candidate = candidates[i]
            val candidateVec = vectorSelector(candidate)
            val dist = distance(query, candidateVec, metric, maxDistance = minDistance)
            if (dist <= minDistance) {
                minDistance = dist
                bestItem = candidate
            }
        }

        return bestItem?.let {
            val similarity = if (metric == DistanceMetric.COSINE) {
                BASE_SIMILARITY_DIVISOR - minDistance
            } else {
                BASE_SIMILARITY_DIVISOR / (BASE_SIMILARITY_DIVISOR + minDistance)
            }
            VectorMatch(it, minDistance, similarity)
        }
    }

    /**
     * Top-K nearest neighbors search.
     */
    fun <T> findTopK(
        query: FloatArray,
        candidates: List<T>,
        k: Int,
        threshold: Float = Float.MAX_VALUE,
        metric: DistanceMetric = DistanceMetric.EUCLIDEAN,
        vectorSelector: (T) -> FloatArray
    ): List<VectorMatch<T>> {
        if (candidates.isEmpty() || query.isEmpty() || k <= 0) return emptyList()

        val results = mutableListOf<VectorMatch<T>>()
        for (i in candidates.indices) {
            val candidate = candidates[i]
            val candidateVec = vectorSelector(candidate)
            val dist = distance(query, candidateVec, metric, maxDistance = threshold)
            if (dist <= threshold) {
                val similarity = if (metric == DistanceMetric.COSINE) {
                    BASE_SIMILARITY_DIVISOR - dist
                } else {
                    BASE_SIMILARITY_DIVISOR / (BASE_SIMILARITY_DIVISOR + dist)
                }
                results.add(VectorMatch(candidate, dist, similarity))
            }
        }
        results.sortBy { it.distance }
        return if (results.size > k) results.subList(0, k) else results
    }
}
