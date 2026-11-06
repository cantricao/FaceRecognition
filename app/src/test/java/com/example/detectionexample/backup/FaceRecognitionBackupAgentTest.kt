package com.example.detectionexample.backup

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FaceRecognitionBackupAgentTest {

    private class TestBackupAgent : FaceRecognitionBackupAgent() {
        init {
            attachBaseContext(ApplicationProvider.getApplicationContext())
        }
    }

    @Test
    fun onRestoreFinished_runsSynchronouslyWithoutCrashing() {
        val agent = TestBackupAgent()
        assertNotNull(agent)

        // Exercise onRestoreFinished synchronous restore flow
        agent.onRestoreFinished()
    }
}
