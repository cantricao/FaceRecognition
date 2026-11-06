package com.example.detectionexample.wear

import com.example.detectionexample.wear.navigation.WearNavKey
import com.example.detectionexample.wear.screens.WearIdentityItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Wear OS companion navigation, key serialization, and state logic.
 */
class WearNavigationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun homeKey_serializesAndDeserializesCorrectly() {
        val original: WearNavKey = WearNavKey.Home
        val serialized = json.encodeToString<WearNavKey>(original)
        val deserialized = json.decodeFromString<WearNavKey>(serialized)

        assertEquals(WearNavKey.Home, deserialized)
    }

    @Test
    fun identitiesKey_serializesAndDeserializesCorrectly() {
        val original: WearNavKey = WearNavKey.Identities
        val serialized = json.encodeToString<WearNavKey>(original)
        val deserialized = json.decodeFromString<WearNavKey>(serialized)

        assertEquals(WearNavKey.Identities, deserialized)
    }

    @Test
    fun detailKey_serializesAndDeserializesCorrectly() {
        val original: WearNavKey = WearNavKey.Detail(
            id = "face_42",
            name = "Jane Doe",
            confidence = 97.8f,
        )
        val serialized = json.encodeToString<WearNavKey>(original)
        val deserialized = json.decodeFromString<WearNavKey>(serialized)

        assertTrue(deserialized is WearNavKey.Detail)
        val detail = deserialized as WearNavKey.Detail
        assertEquals("face_42", detail.id)
        assertEquals("Jane Doe", detail.name)
        assertEquals(97.8f, detail.confidence, 0.001f)
    }

    @Test
    fun identityItem_propertiesPreserved() {
        val item = WearIdentityItem(id = "101", name = "Test Person", confidence = 93.5f)
        assertEquals("101", item.id)
        assertEquals("Test Person", item.name)
        assertEquals(93.5f, item.confidence, 0.001f)
    }

    @Test
    fun navigationFlow_simulatesBackStackHierarchy() {
        val backStack = mutableListOf<WearNavKey>(WearNavKey.Home)
        assertEquals(1, backStack.size)
        assertEquals(WearNavKey.Home, backStack.last())

        // Navigate to Identities
        backStack.add(WearNavKey.Identities)
        assertEquals(2, backStack.size)
        assertEquals(WearNavKey.Identities, backStack.last())

        // Navigate to Detail
        backStack.add(WearNavKey.Detail("1", "Alice", 99.0f))
        assertEquals(3, backStack.size)
        assertTrue(backStack.last() is WearNavKey.Detail)

        // Dismiss detail back to Identities
        backStack.removeAt(backStack.lastIndex)
        assertEquals(2, backStack.size)
        assertEquals(WearNavKey.Identities, backStack.last())

        // Dismiss identities back to Home
        backStack.removeAt(backStack.lastIndex)
        assertEquals(1, backStack.size)
        assertEquals(WearNavKey.Home, backStack.last())
    }
}
