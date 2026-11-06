package com.example.detectionexample.agent

import com.google.mlkit.genai.agent.AgentTool
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class HitlConfirmationToolTest {

    @Test
    fun execute_whenUserConfirms_delegatesExecution() = runTest {
        val mockDelegate = mockk<AgentTool>(relaxed = true)
        every { mockDelegate.name } returns "deletePersonById"
        every { mockDelegate.description } returns "Deletes person"
        coEvery { mockDelegate.execute(any()) } returns mapOf("deleted" to true)

        var confirmationPrompted = false
        val hitlTool = HitlConfirmationTool(mockDelegate) { toolName, args ->
            confirmationPrompted = true
            assertEquals("deletePersonById", toolName)
            assertEquals("person-42", args["personId"])
            true
        }

        val result = hitlTool.execute(mapOf("personId" to "person-42"))

        assertEquals(true, confirmationPrompted)
        assertEquals(true, result["deleted"])
        coVerify(exactly = 1) { mockDelegate.execute(mapOf("personId" to "person-42")) }
    }

    @Test
    fun execute_whenUserDeclines_abortsExecutionAndReturnsCancellation() = runTest {
        val mockDelegate = mockk<AgentTool>(relaxed = true)
        every { mockDelegate.name } returns "deletePersonById"

        val hitlTool = HitlConfirmationTool(mockDelegate) { _, _ -> false }

        val result = hitlTool.execute(mapOf("personId" to "person-99"))

        assertEquals("CANCELLED_BY_USER", result["status"])
        assertNotNull(result["message"])
        coVerify(exactly = 0) { mockDelegate.execute(any()) }
    }
}
