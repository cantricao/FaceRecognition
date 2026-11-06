package com.example.detectionexample.glance.layout

import androidx.compose.runtime.Composable
import androidx.glance.LocalContext
import com.example.detectionexample.R
import com.example.detectionexample.glance.utils.ActionUtils

/**
 * Content to be displayed when there are no items in the list.
 */
@Composable
internal fun EmptyListContent() {
    val context = LocalContext.current

    NoDataContent(
        noDataText = context.getString(R.string.sample_no_data_text),
        noDataIconRes = R.drawable.sample_no_data_icon,
        actionButtonText = context.getString(R.string.sample_add_button_text),
        actionButtonIcon = R.drawable.sample_add_icon,
        actionButtonOnClick = ActionUtils.actionStartMainActivity()
    )
}
