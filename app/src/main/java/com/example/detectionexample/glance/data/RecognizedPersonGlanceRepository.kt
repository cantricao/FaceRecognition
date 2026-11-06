package com.example.detectionexample.glance.data

import android.content.Context
import android.graphics.Bitmap
import androidx.glance.GlanceId
import com.example.detectionexample.R
import com.example.detectionexample.glance.layout.ImageTextListItemData
import com.example.detectionexample.glance.utils.computeIfAbsent
import com.example.detectionexample.glance.utils.decodeDownsampledWidgetBitmap
import com.example.detectionexample.models.Person
import com.example.detectionexample.repository.RecognizedPersonRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GlancePersonRepositoryEntryPoint {
    fun recognizedPersonRepository(): RecognizedPersonRepository
}

/**
 * Repository producing [ImageTextListItemData] from recognized/enrolled face identities.
 */
class RecognizedPersonGlanceRepository(
    private val injectedRepository: RecognizedPersonRepository? = null
) {

    private val _data = MutableStateFlow<List<ImageTextListItemData>>(emptyList())
    fun data(): Flow<List<ImageTextListItemData>> = _data

    suspend fun refresh(context: Context) {
        load(context)
    }

    suspend fun load(context: Context): List<ImageTextListItemData> = withContext(Dispatchers.IO) {
        val repo = injectedRepository ?: runCatching {
            val appContext = context.applicationContext
            EntryPointAccessors.fromApplication(
                appContext,
                GlancePersonRepositoryEntryPoint::class.java
            ).recognizedPersonRepository()
        }.getOrNull()

        val loadedPersons: List<Person>? = repo?.let {
            runCatching {
                it.getPersonsPaged(limit = MAX_DISPLAY_ITEMS, offset = 0)
            }.getOrNull()
        }

        val items = if (!loadedPersons.isNullOrEmpty()) {
            val distinctCount = maxOf(1, loadedPersons.size)

            loadedPersons.take(MAX_DISPLAY_ITEMS).map { person: Person ->
                val scaledFace = person.face.takeIf { it.width > 1 && it.height > 1 }?.let {
                    decodeDownsampledWidgetBitmap(
                        context = context,
                        bitmap = it,
                        distinctImageCount = distinctCount,
                        maxWidthDp = 68,
                        maxHeightDp = 68,
                        config = Bitmap.Config.RGB_565,
                    )
                }
                val confidenceText = if (person.score > 0) {
                    "Confidence: ${person.score}% • Enrolled"
                } else {
                    "Enrolled Face Identity"
                }

                val keyStr = if (person.id.isNotEmpty()) person.id else person.name
                val titleStr = if (person.name.isNotEmpty()) {
                    person.name
                } else if (person.id.isNotEmpty()) {
                    "Identity #${person.id.take(4)}"
                } else {
                    "Enrolled Identity"
                }

                ImageTextListItemData(
                    key = keyStr,
                    title = titleStr,
                    supportingText = confidenceText,
                    supportingImageBitmap = scaledFace,
                    trailingIconButton = R.drawable.sample_heart_icon,
                    trailingIconButtonContentDescription = "Verified Identity"
                )
            }
        } else {
            defaultDemoItems
        }

        _data.value = items
        items
    }

    companion object {
        private const val MAX_DISPLAY_ITEMS = 10

        val defaultDemoItems = listOf(
            ImageTextListItemData(
                key = "demo_1",
                title = "Alice Smith",
                supportingText = "Confidence: 98.4% • Enrolled",
                supportingImageBitmap = null,
                trailingIconButton = R.drawable.sample_heart_icon,
                trailingIconButtonContentDescription = "Verified"
            ),
            ImageTextListItemData(
                key = "demo_2",
                title = "Bob Jones",
                supportingText = "Confidence: 95.1% • Enrolled",
                supportingImageBitmap = null,
                trailingIconButton = R.drawable.sample_heart_icon,
                trailingIconButtonContentDescription = "Verified"
            ),
            ImageTextListItemData(
                key = "demo_3",
                title = "Charlie Brown",
                supportingText = "Confidence: 91.8% • Enrolled",
                supportingImageBitmap = null,
                trailingIconButton = R.drawable.sample_heart_icon,
                trailingIconButtonContentDescription = "Verified"
            ),
            ImageTextListItemData(
                key = "demo_4",
                title = "Diana Prince",
                supportingText = "Confidence: 89.2% • Enrolled",
                supportingImageBitmap = null,
                trailingIconButton = R.drawable.sample_heart_icon,
                trailingIconButtonContentDescription = "Verified"
            )
        )

        private val repositories = mutableMapOf<GlanceId, RecognizedPersonGlanceRepository>()

        fun getRepo(glanceId: GlanceId): RecognizedPersonGlanceRepository {
            return synchronized(repositories) {
                repositories.computeIfAbsent(glanceId) { RecognizedPersonGlanceRepository() }
            }
        }

        fun cleanUp(glanceId: GlanceId) {
            synchronized(repositories) {
                repositories.remove(glanceId)
            }
        }
    }
}
