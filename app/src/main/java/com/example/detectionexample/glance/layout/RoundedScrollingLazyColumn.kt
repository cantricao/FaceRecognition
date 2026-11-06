package com.example.detectionexample.glance.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.LazyListScope
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height

/**
 * A variant of [LazyColumn] that clips its scrolling content to a rounded rectangle.
 */
@Composable
fun RoundedScrollingLazyColumn(
    modifier: GlanceModifier = GlanceModifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyListScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .cornerRadius(16.dp)
    ) {
        LazyColumn(
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}

/**
 * A variant of [LazyColumn] that clips its scrolling content to a rounded rectangle, spaces
 * out each item in the list with a default spacing, and assigns stable item IDs.
 */
@Composable
fun <T> RoundedScrollingLazyColumn(
    items: List<T>,
    itemContentProvider: @Composable (item: T) -> Unit,
    modifier: GlanceModifier = GlanceModifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalItemsSpacing: Dp = 4.dp,
    itemId: ((T) -> Long)? = null,
) {
    val lastIndex = items.size - 1

    RoundedScrollingLazyColumn(modifier, horizontalAlignment) {
        if (itemId != null) {
            items(items = items, itemId = itemId) { item ->
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    itemContentProvider(item)
                    if (item != items.lastOrNull()) {
                        Spacer(modifier = GlanceModifier.height(verticalItemsSpacing))
                    }
                }
            }
        } else {
            itemsIndexed(items) { index, item ->
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    itemContentProvider(item)
                    if (index != lastIndex) {
                        Spacer(modifier = GlanceModifier.height(verticalItemsSpacing))
                    }
                }
            }
        }
    }
}
