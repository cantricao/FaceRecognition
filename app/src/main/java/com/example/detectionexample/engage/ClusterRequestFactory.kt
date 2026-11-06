package com.example.detectionexample.engage

import android.content.Context
import android.net.Uri
import com.google.android.engage.common.datamodel.AccountProfile
import com.google.android.engage.common.datamodel.ContinuationCluster
import com.google.android.engage.common.datamodel.FeaturedCluster
import com.google.android.engage.common.datamodel.Image
import com.google.android.engage.common.datamodel.RecommendationCluster
import com.google.android.engage.common.datamodel.RecommendationClusterType
import com.google.android.engage.common.datamodel.SignInCardEntity
import com.google.android.engage.service.PublishContinuationClusterRequest
import com.google.android.engage.service.PublishFeaturedClusterRequest
import com.google.android.engage.service.PublishRecommendationClustersRequest
import com.google.android.engage.service.PublishUserAccountManagementRequest
import java.util.Locale

class ClusterRequestFactory(private val context: Context) {

    val accountProfile: AccountProfile
        get() = AccountProfile.Builder()
            .setAccountId("user_default")
            .setProfileId("profile_default")
            .setLocale(Locale.getDefault().toLanguageTag())
            .build()

    private val signInCard: SignInCardEntity
        get() = SignInCardEntity.Builder()
            .addPosterImage(
                Image.Builder()
                    .setImageUri(Uri.parse("android.resource://${context.packageName}/drawable/ic_launcher_foreground"))
                    .setImageHeightInPixel(500)
                    .setImageWidthInPixel(500)
                    .setAccessibilityText("Sign in to sync face detections")
                    .build()
            )
            .setActionText("Sign in")
            .setActionUri(Uri.parse("facerecognition://signin"))
            .build()

    fun constructRecommendationClustersRequest(items: List<EngageMediaItem>): PublishRecommendationClustersRequest {
        val clusterBuilder = RecommendationCluster.Builder()
            .setTitle("Recommended Face Analysis Videos")
            .setRecommendationClusterType(RecommendationClusterType.TYPE_TOP_PICKS_FOR_YOU)

        for (item in items) {
            clusterBuilder.addEntity(ItemToEntityConverter.convert(item, isContinuation = false))
        }

        return PublishRecommendationClustersRequest.Builder()
            .addRecommendationCluster(clusterBuilder.build())
            .setAccountProfile(accountProfile)
            .build()
    }

    fun constructContinuationClusterRequest(items: List<EngageMediaItem>): PublishContinuationClusterRequest {
        val clusterBuilder = ContinuationCluster.Builder()
            .setAccountProfile(accountProfile)

        for (item in items) {
            clusterBuilder.addEntity(ItemToEntityConverter.convert(item, isContinuation = true))
        }

        return PublishContinuationClusterRequest.Builder()
            .setContinuationCluster(clusterBuilder.build())
            .build()
    }

    fun constructFeaturedClusterRequest(featuredItem: EngageMediaItem): PublishFeaturedClusterRequest {
        val cluster = FeaturedCluster.Builder()
            .addEntity(ItemToEntityConverter.convert(featuredItem, isContinuation = false))
            .build()

        return PublishFeaturedClusterRequest.Builder()
            .setFeaturedCluster(cluster)
            .build()
    }

    fun constructUserAccountManagementClusterRequest(): PublishUserAccountManagementRequest {
        return PublishUserAccountManagementRequest.Builder()
            .setSignInCardEntity(signInCard)
            .build()
    }
}
