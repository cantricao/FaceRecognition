package com.example.detectionexample.models

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.ByteArrayOutputStream


object BitmapSerializer: KSerializer<Bitmap> {
    override fun deserialize(decoder: Decoder): Bitmap {
        val encodeByte: ByteArray = Base64.decode(decoder.decodeString(), Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(
            encodeByte, 0,
            encodeByte.size
        )
    }

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Bitmap", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Bitmap) {
        val bitmapByteArray = ByteArrayOutputStream()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            value.compress(Bitmap.CompressFormat.WEBP_LOSSY, 0, bitmapByteArray)
        } else {
            @Suppress("DEPRECATION")
            value.compress(Bitmap.CompressFormat.WEBP, 0, bitmapByteArray)
        }
        val bitmapString = Base64.encodeToString(bitmapByteArray.toByteArray(), Base64.DEFAULT)
        encoder.encodeString(bitmapString)
    }
}
