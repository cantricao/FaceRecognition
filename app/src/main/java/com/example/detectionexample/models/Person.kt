package com.example.detectionexample.models

import android.graphics.Bitmap
import kotlinx.serialization.Serializable

@Serializable
data class Person(
    val namespace: String = "person",
    val id: String = "id",
    val timeStamp: Long = 0L,
    val score:Int = 0,
    val name: String = "",
    val embeddings: FloatArray = FloatArray(1),
    @Serializable(with = BitmapSerializer::class)
    val face: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Person

        if (namespace != other.namespace) return false
        if (id != other.id) return false
        if (timeStamp != other.timeStamp) return false
        if (score != other.score) return false
        if (name != other.name) return false
        if (!embeddings.contentEquals(other.embeddings)) return false
        if (face != other.face) return false

        return true
    }

    override fun hashCode(): Int {
        var result = namespace.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + timeStamp.hashCode()
        result = 31 * result + score
        result = 31 * result + name.hashCode()
        result = 31 * result + embeddings.contentHashCode()
        result = 31 * result + face.hashCode()
        return result
    }
}