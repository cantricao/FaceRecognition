package com.example.detectionexample.glance.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics

/**
 * Component to build an item with content arranged vertically.
 */
@Composable
fun VerticalListItem(
    titleContent: @Composable (() -> Unit),
    modifier: GlanceModifier = GlanceModifier,
    topContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    trailingBottomContent: @Composable (() -> Unit)? = null,
    onClick: Action? = null,
    itemContentDescription: String? = null,
) {
    val listItemModifier = if (itemContentDescription != null) {
        modifier.semantics { contentDescription = itemContentDescription }
    } else {
        modifier
    }

    Column(
        modifier = listItemModifier.maybeClickable(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Top
        topContent?.let {
            it()
            Spacer(modifier = GlanceModifier.height(8.dp))
        }

        // Bottom section
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                titleContent()
                supportingContent?.let { it() }
            }
            trailingBottomContent?.let {
                Spacer(modifier = GlanceModifier.width(8.dp))
                it()
            }
        }
    }
}

private fun GlanceModifier.maybeClickable(action: Action?): GlanceModifier {
    return if (action != null) {
        this.clickable(action)
    } else {
        this
    }
}
