package com.example.detectionexample.glance.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.lazy.LazyVerticalGridScope
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import kotlin.math.ceil

/**
 * A variant of [LazyVerticalGrid] that clips its scrolling content to a rounded rectangle.
 */
@Composable
fun RoundedScrollingLazyVerticalGrid(
    gridCells: GridCells,
    modifier: GlanceModifier = GlanceModifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyVerticalGridScope.() -> Unit,
) {
    Box(
        modifier = GlanceModifier
            .cornerRadius(16.dp)
            .then(modifier)
    ) {
        LazyVerticalGrid(
            gridCells = gridCells,
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}

/**
 * A variant of [LazyVerticalGrid] that clips its scrolling content to a rounded rectangle and
 * spaces out each item in the grid with spacing.
 */
@Composable
fun <T> RoundedScrollingLazyVerticalGrid(
    gridCells: Int,
    items: List<T>,
    itemContentProvider: @Composable (item: T) -> Unit,
    modifier: GlanceModifier = GlanceModifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    cellSpacing: Dp = 12.dp,
) {
    val numRows = ceil(items.size.toDouble() / gridCells).toInt()

    val perCellHorizontalPadding = (cellSpacing * (gridCells - 1)) / gridCells
    val perCellVerticalPadding = if (numRows > 0) (cellSpacing * (numRows - 1)) / numRows else 0.dp

    RoundedScrollingLazyVerticalGrid(
        gridCells = GridCells.Fixed(gridCells),
        horizontalAlignment = horizontalAlignment,
        modifier = modifier
    ) {
        itemsIndexed(items) { index, item ->
            val row = index / gridCells
            val column = index % gridCells

            val cellTopPadding = when (row) {
                0 -> 0.dp
                numRows - 1 -> perCellVerticalPadding
                else -> perCellVerticalPadding / 2
            }

            val cellBottomPadding = when (row) {
                0 -> perCellVerticalPadding
                numRows - 1 -> 0.dp
                else -> perCellVerticalPadding / 2
            }

            val cellStartPadding = when (column) {
                0 -> 0.dp
                gridCells - 1 -> perCellHorizontalPadding
                else -> perCellHorizontalPadding / 2
            }

            val cellEndPadding = when (column) {
                0 -> perCellHorizontalPadding
                gridCells - 1 -> 0.dp
                else -> perCellHorizontalPadding / 2
            }

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(
                        start = cellStartPadding,
                        end = cellEndPadding,
                        top = cellTopPadding,
                        bottom = cellBottomPadding
                    )
            ) {
                itemContentProvider(item)
            }
        }
    }
}
