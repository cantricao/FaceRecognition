package com.example.detectionexample.models

import android.graphics.RectF
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private const val INDEX_LEFT = 0
private const val INDEX_TOP = 1
private const val INDEX_RIGHT = 2
private const val INDEX_BOTTOM = 3

object RectFSerializer : KSerializer<RectF> {
    private val delegateSerializer = FloatArraySerializer()

    override fun deserialize(decoder: Decoder): RectF {
        val coordinates = decoder.decodeSerializableValue(delegateSerializer)
        return RectF(
            coordinates[INDEX_LEFT],
            coordinates[INDEX_TOP],
            coordinates[INDEX_RIGHT],
            coordinates[INDEX_BOTTOM]
        )
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
