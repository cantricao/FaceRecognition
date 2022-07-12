package com.example.detectionexample.models

import android.graphics.RectF
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object RectFSerializer : KSerializer<RectF> {
    private val delegateSerializer = FloatArraySerializer()

    override fun deserialize(decoder: Decoder): RectF {
        val coordinates = decoder.decodeSerializableValue(delegateSerializer)
        return RectF(coordinates[0], coordinates[1], coordinates[2], coordinates[3])
    }

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor
        get() = SerialDescriptor("RectF", delegateSerializer.descriptor)

    override fun serialize(encoder: Encoder, value: RectF) {
        val coordinates = floatArrayOf(
            value.left,
            value.top,
            value.right,
            value.bottom
        )
        encoder.encodeSerializableValue(delegateSerializer, coordinates)
    }
}