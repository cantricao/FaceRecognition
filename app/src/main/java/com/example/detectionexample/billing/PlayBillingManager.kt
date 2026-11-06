package com.example.detectionexample.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Modern Google Play Billing Library (v9.1.0) Manager.
 *
 * Implements PBL 9.x architectural patterns:
 * - Builder pattern with enableAutoServiceReconnection() (PBL 8+ connection management).
 * - Mandatory PendingPurchaseParams with enableOneTimeProducts().
 * - Modern ProductDetails data models (replacing legacy SkuDetails).
 * - QueryProductDetailsResult callback signature.
 * - Async purchase querying via queryPurchasesAsync(QueryPurchasesParams).
 * - Mandatory purchase acknowledgment within 3 days.
 * - PBL 9 updated error handling: catching BILLING_UNAVAILABLE for blocked Play Store environments.
 */
@Singleton
class PlayBillingManager(
    @param:ApplicationContext private val context: Context,
    billingClientProvider: ((PlayBillingManager) -> BillingClient)? = null
) : PurchasesUpdatedListener, BillingClientStateListener {

    @Inject
    constructor(@ApplicationContext context: Context) : this(context, null)

    companion object {
        private const val TAG = "PlayBillingManager"
        const val PRODUCT_PRO_DETECTION = "pro_face_detection_monthly"
        const val PRODUCT_UNLOCK_PREMIUM = "unlock_premium_features"
    }

    private val externalScope = CoroutineScope(Dispatchers.Default)

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap: StateFlow<Map<String, ProductDetails>> = _productDetailsMap.asStateFlow()

    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    val purchases: StateFlow<List<Purchase>> = _purchases.asStateFlow()

    private val _billingEvents = MutableSharedFlow<String>()
    val billingEvents: SharedFlow<String> = _billingEvents.asSharedFlow()

    val billingClient: BillingClient by lazy {
        billingClientProvider?.invoke(this) ?: BillingClient.newBuilder(context)
            .setListener(this)
            // PBL 8+: Native automatic reconnection; replaces manual retry loops in onServiceDisconnected
            .enableAutoServiceReconnection()
            // PBL 7+ & 9: Mandatory PendingPurchasesParams configuration
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
    }

    fun startBillingConnection() {
        if (!billingClient.isReady) {
            billingClient.startConnection(this)
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage

        if (responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Billing client successfully set up and ready.")
            _isConnected.value = true
            externalScope.launch {
                queryAvailableProducts()
                refreshPurchases()
            }
        } else {
            // PBL 9: Handle BILLING_UNAVAILABLE specifically for blocked Play Store (e.g. Kids mode)
            if (responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                Log.w(TAG, "Play Store or Billing is unavailable/blocked: $debugMessage")
            } else {
                Log.e(TAG, "Billing setup failed with code $responseCode: $debugMessage")
            }
            _isConnected.value = false
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.w(TAG, "Billing service disconnected. AutoServiceReconnection will handle retries.")
        _isConnected.value = false
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchasesList: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchasesList.isNullOrEmpty()) {
                    externalScope.launch {
                        for (purchase in purchasesList) {
                            handlePurchase(purchase)
                        }
                        refreshPurchases()
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled purchase flow.")
                externalScope.launch { _billingEvents.emit("Purchase canceled by user") }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned.")
                externalScope.launch {
                    refreshPurchases()
                    _billingEvents.emit("Item is already owned")
                }
            }
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                Log.w(TAG, "Billing unavailable: ${billingResult.debugMessage}")
                externalScope.launch { _billingEvents.emit("Billing service unavailable") }
            }
            else -> {
                Log.e(TAG, "Purchases updated error ${billingResult.responseCode}: ${billingResult.debugMessage}")
                externalScope.launch { _billingEvents.emit("Purchase error: ${billingResult.debugMessage}") }
            }
        }
    }

    /**
     * Query product details using the PBL 8+/9 QueryProductDetailsParams pattern.
     */
    suspend fun queryAvailableProducts() {
        if (!billingClient.isReady) return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PRO_DETECTION)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_UNLOCK_PREMIUM)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        try {
            val result = billingClient.queryProductDetails(params)
            if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val detailsList = result.productDetailsList ?: emptyList()
                _productDetailsMap.value = detailsList.associateBy { it.productId }
                Log.d(TAG, "Loaded ${detailsList.size} product details via PBL 9.")
            } else {
                Log.e(TAG, "Failed to query products: ${result.billingResult.debugMessage}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying products", e)
        }
    }

    /**
     * Query active one-time products and subscriptions asynchronously.
     */
    suspend fun refreshPurchases() {
        if (!billingClient.isReady) return

        val allPurchases = mutableListOf<Purchase>()

        // Query INAPP purchases
        val inappParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val inappResult = billingClient.queryPurchasesAsync(inappParams)
        if (inappResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            allPurchases.addAll(inappResult.purchasesList)
        }

        // Query SUBS purchases
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val subsResult = billingClient.queryPurchasesAsync(subsParams)
        if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            allPurchases.addAll(subsResult.purchasesList)
        }

        _purchases.value = allPurchases
    }

    /**
     * Mandatory Purchase Acknowledgment (PBL 2.0+ & retained in PBL 9).
     * Purchases not acknowledged within 3 days will be refunded.
     */
    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged successfully: ${purchase.orderId}")
                    } else {
                        Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
                    }
                }
            }
        }
    }

    /**
     * Launch purchase flow for a product.
     */
    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        selectedOfferToken: String? = null
    ): BillingResult {
        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        // For subscriptions, offerToken is mandatory in modern PBL
        if (productDetails.productType == BillingClient.ProductType.SUBS && selectedOfferToken != null) {
            productDetailsParamsBuilder.setOfferToken(selectedOfferToken)
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))
            .build()

        return billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    fun destroy() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
