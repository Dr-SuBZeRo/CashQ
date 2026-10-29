package com.cashq.data.local

import com.cashq.data.local.entity.SavedSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedSessionEntityTest {

    @Test
    fun serializeAndDeserialize_quantitiesMap_preservesAllValues() {
        val originalMap = mapOf(
            "note_500" to 74,
            "note_200" to 14,
            "coin_20" to 5,
            "coin_1" to 50
        )

        val jsonString = SavedSessionEntity.serializeQuantities(originalMap)
        val entity = SavedSessionEntity(
            id = "test-123",
            title = "Test Session",
            createdAt = 1730180000000L,
            updatedAt = 1730180000000L,
            totalPaise = 3700000L,
            totalUnits = 74L,
            coinsVisible = true,
            coinMode = "SEPARATE",
            quantitiesJson = jsonString,
            isNotesOnly = false
        )

        val restoredMap = entity.getQuantitiesMap()

        assertEquals(4, restoredMap.size)
        assertEquals(74, restoredMap["note_500"])
        assertEquals(14, restoredMap["note_200"])
        assertEquals(5, restoredMap["coin_20"])
        assertEquals(50, restoredMap["coin_1"])
    }

    @Test
    fun emptyMap_serializesAndDeserializesCleanly() {
        val jsonString = SavedSessionEntity.serializeQuantities(emptyMap())
        val entity = SavedSessionEntity(
            id = "test-empty",
            title = "Empty",
            createdAt = 0L,
            updatedAt = 0L,
            totalPaise = 0L,
            totalUnits = 0L,
            coinsVisible = false,
            coinMode = "SEPARATE",
            quantitiesJson = jsonString,
            isNotesOnly = true
        )

        val restored = entity.getQuantitiesMap()
        assertTrue(restored.isEmpty())
    }
}
