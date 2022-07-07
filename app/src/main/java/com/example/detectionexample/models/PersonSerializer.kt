@file:Suppress("BlockingMethodInNonBlockingContext")

package com.example.detectionexample.models

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object PersonSerializer: Serializer<Person> {

    override suspend fun readFrom(input: InputStream): Person {
        try {
            val bytes = input.readBytes()
            val string = bytes.decodeToString()
            return Json.decodeFromString(string)
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot read stored data", e)
        }
    }

    override val defaultValue: Person = Person()


    override suspend fun writeTo(t: Person, output: OutputStream) {
        val string = Json.encodeToString(t)
        val bytes = string.encodeToByteArray()
        output.write(bytes)
    }

}