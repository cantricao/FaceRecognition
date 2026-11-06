package com.example.detectionexample.billing

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlayBillingManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockBillingClient: BillingClient
    private lateinit var billingManager: PlayBillingManager

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockBillingClient = mockk(relaxed = true) {
            every { isReady } returns true
        }
        billingManager = PlayBillingManager(mockContext) { mockBillingClient }
    }

    @Test
    fun onBillingSetupFinished_withOk_setsIsConnectedTrue() {
        val okResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .setDebugMessage("Setup successful")
            .build()

        billingManager.onBillingSetupFinished(okResult)

        assertTrue(billingManager.isConnected.value)
    }

    @Test
    fun onBillingSetupFinished_withBillingUnavailable_setsIsConnectedFalse() {
        // PBL 9: Verify handling of BILLING_UNAVAILABLE (e.g. Play Store blocked in Kids mode)
        val blockedResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE)
            .setDebugMessage("Play Store is blocked")
            .build()

        billingManager.onBillingSetupFinished(blockedResult)

        assertFalse(billingManager.isConnected.value)
    }

    @Test
    fun onBillingServiceDisconnected_setsIsConnectedFalse() {
        val okResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .build()
        billingManager.onBillingSetupFinished(okResult)
        assertTrue(billingManager.isConnected.value)

        billingManager.onBillingServiceDisconnected()
        assertFalse(billingManager.isConnected.value)
    }

    @Test
    fun onPurchasesUpdated_withUserCanceled_doesNotThrow() {
        val cancelResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.USER_CANCELED)
            .setDebugMessage("User dismissed bottom sheet")
            .build()

        billingManager.onPurchasesUpdated(cancelResult, null)
    }

    @Test
    fun onPurchasesUpdated_withItemAlreadyOwned_doesNotThrow() {
        val alreadyOwnedResult = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)
            .setDebugMessage("Already purchased")
            .build()

        billingManager.onPurchasesUpdated(alreadyOwnedResult, null)
    }
}
