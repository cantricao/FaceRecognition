package com.example.detectionexample.appfunctions

import androidx.appfunctions.AppFunctionSerializable

/** A summary of a person enrolled in the face recognition database. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class PersonSummary(
    /** The unique identifier of the registered person. */
    val id: String,
    /** The name or label assigned to the registered person. */
    val name: String,
    /** The detection confidence score or registration quality percentage. */
    val score: Int,
    /** The Unix epoch timestamp in milliseconds when the person was registered. */
    val timeStamp: Long,
)
