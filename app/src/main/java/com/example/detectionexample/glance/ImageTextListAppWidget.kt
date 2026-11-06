package com.example.detectionexample.glance

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.detectionexample.R
import com.example.detectionexample.glance.data.RecognizedPersonGlanceRepository
import com.example.detectionexample.glance.data.pushWidgetRevisionUpdate
import com.example.detectionexample.glance.layout.ImageTextListItemData
import com.example.detectionexample.glance.layout.ImageTextListLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Jetpack Glance Canonical Widget displaying recognized face identities in an Image Text List layout,
 * compliant with Android 17 bitmap memory limits, DataStore revision state, and composition error recovery.
 */
class ImageTextListAppWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = RecognizedPersonGlanceRepository.getRepo(id)

        // Hoist shared bitmap and data loading before provideContent to deduplicate across sizes
        val initialItems = withContext(Dispatchers.Default) {
            repo.load(context)
        }

        provideContent {
            // Observe revision from Preferences to recompose upon DataStore updates
            val revision = currentState<Preferences>()[KEY_REVISION] ?: 0
            GlanceTheme {
                val items by repo.data().collectAsState(initial = initialItems)
                val coroutineScope = rememberCoroutineScope()

                key(LocalSize.current, revision) {
                    WidgetContent(
                        items = items,
                        refreshAction = {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    repo.refresh(context)
                                    pushWidgetRevisionUpdate(context)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) {
        val fallback = RemoteViews(context.packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, "Can't show recognized faces widget")
        }
        runCatching {
            AppWidgetManager.getInstance(context)?.updateAppWidget(appWidgetId, fallback)
        }
    }

    @Composable
    fun WidgetContent(
        items: List<ImageTextListItemData>,
        refreshAction: () -> Unit,
    ) {
        val context = LocalContext.current

        ImageTextListLayout(
            items = items,
            title = context.getString(
                R.string.sample_text_and_image_list_app_widget_name
            ),
            titleIconRes = R.drawable.sample_image_icon,
            titleBarActionIconRes = R.drawable.sample_refresh_icon,
            titleBarActionIconContentDescription = context.getString(
                R.string.sample_refresh_icon_button_label
            ),
            titleBarAction = refreshAction,
        )
    }

    companion object {
        val KEY_REVISION = intPreferencesKey("widget_revision")
    }
}

class ImageTextListAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ImageTextListAppWidget()

    @SuppressLint("RestrictedApi")
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach {
            RecognizedPersonGlanceRepository.cleanUp(AppWidgetId(appWidgetId = it))
        }
        super.onDeleted(context, appWidgetIds)
    }
}
