package com.example.detectionexample.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TraceUtilsTest {

    @Test
    fun traceSection_executesBlockAndReturnsResult() {
        val result = traceSection("TestSection") {
            42
        }
        assertEquals(42, result)
    }

    @Test
    fun traceSection_preservesExceptions() {
        var finallyRun = false
        try {
            traceSection("FailingSection") {
                finallyRun = true
                error("Expected test exception")
            }
        } catch (e: IllegalStateException) {
            assertEquals("Expected test exception", e.message)
        }
        assert(finallyRun)
    }
}
