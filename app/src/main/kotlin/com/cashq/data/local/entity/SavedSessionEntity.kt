package com.cashq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONObject

/**
 * Room entity representing a saved cash counting session.
 */
@Entity(tableName = "saved_sessions")
data class SavedSessionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val totalPaise: Long,
    val totalUnits: Long,
    val coinsVisible: Boolean,
    val coinMode: String,
    val quantitiesJson: String,
    val isNotesOnly: Boolean
) {
    /**
     * Helper to deserialize the quantities map from JSON.
     */
    fun getQuantitiesMap(): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        try {
            val json = JSONObject(quantitiesJson)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.optInt(key, 0)
            }
        } catch (_: Exception) {
        }
        return map
    }

    companion object {
        /**
         * Helper to serialize a quantities map into a JSON string.
         */
        fun serializeQuantities(quantities: Map<String, Int>): String {
            val json = JSONObject()
            quantities.forEach { (key, value) ->
                json.put(key, value)
            }
            return json.toString()
        }
    }
}
