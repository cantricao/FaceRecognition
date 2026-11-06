package com.example.detectionexample.data.local

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.detectionexample.models.Person
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val score: Int,
    val timeStamp: Long,
    val embeddingsJson: String,
    val faceBase64: String
) {
    fun toDomain(): Person {
        val embeddings = runCatching {
            Json.decodeFromString<List<Float>>(embeddingsJson).toFloatArray()
        }.getOrDefault(FloatArray(0))

        val face = runCatching {
            val bytes = Base64.decode(faceBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull() ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        return Person(
            id = id,
            name = name,
            score = score,
            timeStamp = timeStamp,
            embeddings = embeddings,
            face = face
        )
    }

    companion object {
        private const val WEBP_COMPRESSION_QUALITY = 80

        fun fromDomain(person: Person): PersonEntity {
            val embeddingsList = person.embeddings.toList()
            val embeddingsJson = Json.encodeToString(embeddingsList)

            val outputStream = ByteArrayOutputStream()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                person.face.compress(Bitmap.CompressFormat.WEBP_LOSSY, WEBP_COMPRESSION_QUALITY, outputStream)
            } else {
                @Suppress("DEPRECATION")
                person.face.compress(Bitmap.CompressFormat.WEBP, WEBP_COMPRESSION_QUALITY, outputStream)
            }
            val faceBase64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            return PersonEntity(
                id = person.id,
                name = person.name,
                score = person.score,
                timeStamp = person.timeStamp,
                embeddingsJson = embeddingsJson,
                faceBase64 = faceBase64
            )
        }
    }
}
