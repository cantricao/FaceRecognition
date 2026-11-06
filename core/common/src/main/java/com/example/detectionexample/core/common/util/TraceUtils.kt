package com.example.detectionexample.core.common.util

import android.os.Trace

/**
 * Executes [block] inside an Android systrace / Perfetto section identified by [sectionName].
 * Inlined with zero object allocations, making it suitable for high-frequency camera loops.
 * Safely handles unmocked android.os.Trace invocations in host JVM unit test environments.
 */
inline fun <T> traceSection(sectionName: String, block: () -> T): T {
    try {
        Trace.beginSection(sectionName)
    } catch (_: RuntimeException) {
        // android.os.Trace is not mocked on plain host JVM tests; ignore gracefully
    }
    return try {
        block()
    } finally {
        try {
            Trace.endSection()
        } catch (_: RuntimeException) {
            // android.os.Trace is not mocked on plain host JVM tests; ignore gracefully
        }
    }
}
