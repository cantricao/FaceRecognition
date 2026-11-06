package com.example.detectionexample.glance.layout

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.detectionexample.R
import com.example.detectionexample.glance.layout.Dimensions.NUM_GRID_CELLS
import com.example.detectionexample.glance.layout.Dimensions.fillItemItemPadding
import com.example.detectionexample.glance.layout.Dimensions.filledItemCornerRadius
import com.example.detectionexample.glance.layout.Dimensions.imageCornerRadius
import com.example.detectionexample.glance.layout.Dimensions.verticalSpacing
import com.example.detectionexample.glance.layout.Dimensions.widgetPadding
import com.example.detectionexample.glance.layout.ImageTextListLayoutSize.Companion.shouldDisplayTrailingIconButton
import com.example.detectionexample.glance.layout.ImageTextListLayoutSize.Companion.showTitleBar
import com.example.detectionexample.glance.layout.ImageTextListLayoutSize.Large
import com.example.detectionexample.glance.layout.ImageTextListLayoutSize.Medium
import com.example.detectionexample.glance.layout.ImageTextListLayoutSize.Small
import com.example.detectionexample.glance.utils.ActionUtils
import com.example.detectionexample.glance.utils.LargeWidgetPreview
import com.example.detectionexample.glance.utils.MediumWidgetPreview
import com.example.detectionexample.glance.utils.SmallWidgetPreview

/**
 * A layout focused on presenting a list of text with an image, and an optional icon button.
 * The list is displayed in a [Scaffold] below an app-specific title bar.
 *
 * @param title the text to be displayed as title of the widget.
 * @param titleIconRes a tintable icon that represents the app.
 * @param titleBarActionIconRes resource id of a tintable icon for title bar action.
 * @param titleBarActionIconContentDescription description of the action button.
 * @param titleBarAction action to be performed on click of the title bar button.
 * @param items list of items to be displayed in the list.
 */
@Composable
fun ImageTextListLayout(
    title: String,
    @DrawableRes titleIconRes: Int,
    @DrawableRes titleBarActionIconRes: Int,
    titleBarActionIconContentDescription: String,
    titleBarAction: (() -> Unit),
    items: List<ImageTextListItemData>,
) {
    val imageTextListLayoutSize = ImageTextListLayoutSize.fromLocalSize()

    fun titleBar(): @Composable (() -> Unit) = {
        TitleBar(
            startIcon = ImageProvider(titleIconRes),
            title = title.takeIf { imageTextListLayoutSize != Small } ?: "",
            iconColor = GlanceTheme.colors.primary,
            textColor = GlanceTheme.colors.onSurface,
            actions = {
                CircleIconButton(
                    imageProvider = ImageProvider(titleBarActionIconRes),
                    contentDescription = titleBarActionIconContentDescription,
                    contentColor = GlanceTheme.colors.secondary,
                    backgroundColor = null,
                    onClick = titleBarAction
                )
            }
        )
    }

    val scaffoldTopPadding = if (showTitleBar()) {
        0.dp
    } else {
        widgetPadding
    }

    Scaffold(
        backgroundColor = GlanceTheme.colors.widgetBackground,
        modifier = GlanceModifier.padding(
            top = scaffoldTopPadding,
            bottom = widgetPadding
        ),
        titleBar = if (showTitleBar()) {
            titleBar()
        } else {
            null
        }
    ) {
        Content(items)
    }
}

@Composable
private fun Content(items: List<ImageTextListItemData>) {
    val displayTrailingIconIfPresent = shouldDisplayTrailingIconButton()

    if (items.isEmpty()) {
        EmptyListContent()
    } else {
        when (ImageTextListLayoutSize.fromLocalSize()) {
            Small -> {
                ListView(
                    items = items,
                    displayImage = false,
                    displayTrailingIconIfPresent = displayTrailingIconIfPresent,
                )
            }

            Medium -> {
                ListView(
                    items = items,
                    displayImage = true,
                    displayTrailingIconIfPresent = displayTrailingIconIfPresent,
                )
            }

            Large -> {
                GridView(
                    items = items,
                    displayImage = true,
                    displayTrailingIconIfPresent = displayTrailingIconIfPresent
                )
            }
        }
    }
}

@Composable
private fun ListView(
    items: List<ImageTextListItemData>,
    displayImage: Boolean,
    displayTrailingIconIfPresent: Boolean,
) {
    val clickAction = ActionUtils.actionStartMainActivity()
    RoundedScrollingLazyColumn(
        modifier = GlanceModifier.fillMaxSize(),
        items = items,
        verticalItemsSpacing = verticalSpacing,
        itemId = { it.key.hashCode().toLong() },
        itemContentProvider = { item ->
            FilledHorizontalListItem(
                item = item,
                displayImage = displayImage,
                displayTrailingIcon = displayTrailingIconIfPresent,
                onClick = clickAction,
                modifier = GlanceModifier.fillMaxSize(),
            )
        },
    )
}

@Composable
private fun GridView(
    items: List<ImageTextListItemData>,
    displayImage: Boolean,
    displayTrailingIconIfPresent: Boolean,
) {
    val clickAction = ActionUtils.actionStartMainActivity()
    RoundedScrollingLazyVerticalGrid(
        gridCells = NUM_GRID_CELLS,
        items = items,
        cellSpacing = verticalSpacing,
        itemContentProvider = { item ->
            FilledHorizontalListItem(
                item = item,
                displayImage = displayImage,
                displayTrailingIcon = displayTrailingIconIfPresent,
                onClick = clickAction,
                modifier = GlanceModifier.fillMaxSize()
            )
        },
        modifier = GlanceModifier.fillMaxSize()
    )
}

@Composable
private fun FilledHorizontalListItem(
    item: ImageTextListItemData,
    displayImage: Boolean,
    displayTrailingIcon: Boolean,
    onClick: Action,
    modifier: GlanceModifier = GlanceModifier,
) {
    @Composable
    fun TitleText() {
        Text(
            text = item.title,
            maxLines = 2,
            style = TextStyles.titleText,
        )
    }

    @Composable
    fun SupportingText() {
        Text(
            text = item.supportingText,
            maxLines = 2,
            style = TextStyles.supportingText
        )
    }

    @Composable
    fun SupportingImage() {
        val imageProvider = if (item.supportingImageBitmap != null) {
            ImageProvider(item.supportingImageBitmap)
        } else {
            ImageProvider(R.drawable.sample_placeholder_image)
        }

        Image(
            provider = imageProvider,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.cornerRadius(imageCornerRadius).size(Dimensions.imageSize)
        )
    }

    @Composable
    fun IconButton() {
        if (item.trailingIconButton != null) {
            CircleIconButton(
                imageProvider = ImageProvider(item.trailingIconButton),
                backgroundColor = null,
                contentDescription = item.trailingIconButtonContentDescription,
                onClick = onClick
            )
        }
    }

    ListItem(
        modifier = modifier
            .padding(fillItemItemPadding)
            .cornerRadius(filledItemCornerRadius)
            .background(GlanceTheme.colors.secondaryContainer),
        headlineContent = { TitleText() },
        supportingContent = { SupportingText() },
        onClick = onClick,
        leadingContent = if (displayImage) {
            { SupportingImage() }
        } else {
            null
        },
        trailingContent = if (displayTrailingIcon) {
            { IconButton() }
        } else null,
    )
}

/**
 * Holds data fields for [ImageTextListLayout].
 */
data class ImageTextListItemData(
    val key: String,
    val title: String,
    val supportingText: String,
    val supportingImageBitmap: Bitmap? = null,
    @DrawableRes val trailingIconButton: Int? = null,
    val trailingIconButtonContentDescription: String? = null,
)

private enum class ImageTextListLayoutSize(val maxWidth: Dp) {
    Small(maxWidth = 260.dp),
    Medium(maxWidth = 479.dp),
    Large(maxWidth = 644.dp);

    companion object {
        @Composable
        fun fromLocalSize(): ImageTextListLayoutSize {
            val width = LocalSize.current.width
            return if (width >= Medium.maxWidth) {
                Large
            } else if (width >= Small.maxWidth) {
                Medium
            } else {
                Small
            }
        }

        @Composable
        fun showTitleBar(): Boolean {
            return LocalSize.current.height >= 180.dp
        }

        @Composable
        fun shouldDisplayTrailingIconButton(): Boolean {
            val widgetWidth = LocalSize.current.width
            return (widgetWidth in 340.dp..479.dp || widgetWidth > 620.dp)
        }
    }
}

private object TextStyles {
    val titleText: TextStyle
        @Composable get() = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = if (ImageTextListLayoutSize.fromLocalSize() == ImageTextListLayoutSize.Small) {
                14.sp
            } else {
                16.sp
            },
            color = GlanceTheme.colors.onSurface
        )

    val supportingText: TextStyle
        @Composable get() = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = GlanceTheme.colors.secondary
        )
}

private object Dimensions {
    const val NUM_GRID_CELLS = 2
    val widgetPadding = 12.dp
    val filledItemCornerRadius = 16.dp
    val fillItemItemPadding = 12.dp
    val verticalSpacing = 4.dp
    val imageSize: Dp = 68.dp
    val imageCornerRadius = 12.dp
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 259, heightDp = 200)
@Preview(widthDp = 261, heightDp = 200)
@Preview(widthDp = 480, heightDp = 200)
@Preview(widthDp = 644, heightDp = 200)
private annotation class ImageTextListBreakpointPreviews

@ImageTextListBreakpointPreviews
@SmallWidgetPreview
@MediumWidgetPreview
@LargeWidgetPreview
@Composable
private fun ImageTextListLayoutPreview() {
    val context = LocalContext.current

    ImageTextListLayout(
        items = listOf(
            ImageTextListItemData(
                key = "1",
                title = "Alice Smith",
                supportingText = "Confidence: 98.4% • Enrolled",
                supportingImageBitmap = null
            ),
            ImageTextListItemData(
                key = "2",
                title = "Bob Jones",
                supportingText = "Confidence: 95.1% • Enrolled",
                supportingImageBitmap = null
            ),
            ImageTextListItemData(
                key = "3",
                title = "Charlie Brown",
                supportingText = "Confidence: 91.8% • Enrolled",
                supportingImageBitmap = null
            ),
            ImageTextListItemData(
                key = "4",
                title = "Diana Prince",
                supportingText = "Confidence: 89.2% • Enrolled",
                supportingImageBitmap = null
            )
        ),
        title = context.getString(R.string.sample_text_and_image_list_app_widget_name),
        titleIconRes = R.drawable.sample_image_icon,
        titleBarActionIconRes = R.drawable.sample_refresh_icon,
        titleBarActionIconContentDescription = context.getString(R.string.sample_refresh_icon_button_label),
        titleBarAction = {},
    )
}
