package com.example.detectionexample.models

import android.graphics.PointF
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = PointF::class)
object PointFSerializer : KSerializer<PointF> {
    override fun deserialize(decoder: Decoder): PointF {
        val stringFloatArray = decoder.decodeString().split(',')
        return PointF(stringFloatArray[0].toFloat(), stringFloatArray[1].toFloat())
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("PointF", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: PointF) {
        val rectFString = "${value.x},${value.y}"
        encoder.encodeString(rectFString)
    }
}

