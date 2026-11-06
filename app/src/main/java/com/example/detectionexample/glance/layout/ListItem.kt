package com.example.detectionexample.glance.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics

/**
 * Component to build an item within a list. Lists are continuous, vertical indexes of text or images.
 */
@Composable
fun ListItem(
    headlineContent: @Composable (() -> Unit),
    modifier: GlanceModifier = GlanceModifier,
    contentSpacing: Dp = 16.dp,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: Action? = null,
    itemContentDescription: String? = null,
) {
    val listItemModifier = if (itemContentDescription != null) {
        modifier.semantics { contentDescription = itemContentDescription }
    } else {
        modifier
    }

    Row(
        modifier = listItemModifier.maybeClickable(onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading
        leadingContent?.let {
            it()
            Spacer(modifier = GlanceModifier.width(contentSpacing))
        }
        // Center
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            headlineContent()
            supportingContent?.let { it() }
        }
        // Trailing
        trailingContent?.let {
            Spacer(modifier = GlanceModifier.width(contentSpacing))
            it()
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
