package com.cortex.app.data.db

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Entity columns that hold a list of ids (e.g. MemoryItemEntity.tagIdsJson) store plain
 * JSON strings rather than using a Room @TypeConverter, so the raw SQLite content stays
 * human-inspectable — that matters for a local-first, explainable-memory product.
 * Mappers call these helpers explicitly when converting entity <-> domain model.
 */
object StringListJson {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(String.serializer())

    fun encode(value: List<String>): String = json.encodeToString(serializer, value)

    fun decode(value: String): List<String> =
        if (value.isBlank()) emptyList() else json.decodeFromString(serializer, value)
}
