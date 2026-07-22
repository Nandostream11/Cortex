package com.cortex.app.data.export

import com.cortex.app.data.db.CortexDatabase
import com.cortex.app.data.db.StringListJson
import com.cortex.app.data.db.entity.ConnectorAccountEntity
import com.cortex.app.data.db.entity.EdgeEntity
import com.cortex.app.data.db.entity.GuidanceEventEntity
import com.cortex.app.data.db.entity.MemoryItemEntity
import com.cortex.app.data.db.entity.NodeEntity
import com.cortex.app.data.db.entity.ProjectEntity
import com.cortex.app.data.db.entity.TaskEntity
import com.cortex.app.data.settings.SettingsRepository
import com.cortex.app.security.BackupCrypto
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Implements Roadmap Phase 1's "Encrypted JSON export/import." Builds the exact shape
 * described in ExportFormat.json, then encrypts the serialized JSON with a
 * user-supplied password (see BackupCrypto for why Keystore alone isn't used here).
 *
 * Import merges by primary key (REPLACE on conflict) rather than wiping existing data
 * first — per ExportFormat.json's "Import should merge duplicates carefully" and
 * CodingStandards' rule against silently destroying user data.
 */
class ExportManager(
    private val database: CortexDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    suspend fun exportEncrypted(password: CharArray): ByteArray {
        val envelope = buildEnvelope()
        val plaintext = json.encodeToString(ExportEnvelope.serializer(), envelope).encodeToByteArray()
        return BackupCrypto.encrypt(plaintext, password)
    }

    /** @throws javax.crypto.AEADBadTagException if [password] is wrong or the file is corrupted. */
    suspend fun importEncrypted(payload: ByteArray, password: CharArray) {
        val plaintext = BackupCrypto.decrypt(payload, password)
        val envelope = json.decodeFromString(ExportEnvelope.serializer(), plaintext.decodeToString())
        applyEnvelope(envelope)
    }

    private suspend fun buildEnvelope(): ExportEnvelope {
        val settings = settingsRepository.settings.first()
        val enabledConnectorIds = database.connectorAccountDao().getEnabled().map { it.id }

        return ExportEnvelope(
            exportedAt = Instant.now().toString(),
            contents = ExportContents(
                settings = ExportSettings(
                    apiProvider = "OpenRouter",
                    autonomyMode = settings.autonomyMode.name,
                    connectorStates = enabledConnectorIds
                ),
                memories = database.memoryDao().getAllForExport().map { it.toDto() },
                nodes = database.nodeDao().observeAll().first().map { it.toDto() },
                edges = database.edgeDao().getAllOrderedByWeight().map { it.toDto() },
                tasks = database.taskDao().getAllForExport().map { it.toDto() },
                projects = database.projectDao().observeAll().first().map { it.toDto() },
                connectorAccounts = database.connectorAccountDao().observeAll().first().map { it.toDto() },
                guidanceEvents = database.guidanceEventDao().getAllForExport().map { it.toDto() }
            )
        )
    }

    private suspend fun applyEnvelope(envelope: ExportEnvelope) {
        val c = envelope.contents
        c.memories.forEach { database.memoryDao().insert(it.toEntity()) }
        c.nodes.forEach { database.nodeDao().insert(it.toEntity()) }
        c.edges.forEach { database.edgeDao().insert(it.toEntity()) }
        c.projects.forEach { database.projectDao().insert(it.toEntity()) }
        c.tasks.forEach { database.taskDao().insert(it.toEntity()) }
        c.connectorAccounts.forEach { database.connectorAccountDao().insert(it.toEntity()) }
        c.guidanceEvents.forEach { database.guidanceEventDao().insert(it.toEntity()) }
        // Deliberately does not touch SecretStore: a restored ConnectorAccount shows up
        // disabled/needing re-auth until the user re-enters its credential, since the
        // export never contained one.
    }
}

// --- entity <-> export DTO mappers (kept local to this file: this mapping is export's
// concern only, and shouldn't leak into the general-purpose EntityMappers used by the
// rest of the app). ---

private fun MemoryItemEntity.toDto() = MemoryItemDto(
    id = id,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    updatedAt = Instant.ofEpochMilli(updatedAt).toString(),
    rawText = rawText,
    normalizedText = normalizedText,
    sourceType = sourceType,
    sourceRef = sourceRef,
    category = category,
    importanceScore = importanceScore,
    confidenceScore = confidenceScore,
    isPinned = isPinned,
    isArchived = isArchived,
    tagIds = StringListJson.decode(tagIdsJson),
    linkedNodeIds = StringListJson.decode(linkedNodeIdsJson)
)

private fun MemoryItemDto.toEntity() = MemoryItemEntity(
    id = id,
    createdAt = Instant.parse(createdAt).toEpochMilli(),
    updatedAt = Instant.parse(updatedAt).toEpochMilli(),
    rawText = rawText,
    normalizedText = normalizedText,
    sourceType = sourceType,
    sourceRef = sourceRef,
    category = category,
    importanceScore = importanceScore,
    confidenceScore = confidenceScore,
    isPinned = isPinned,
    isArchived = isArchived,
    tagIdsJson = StringListJson.encode(tagIds),
    linkedNodeIdsJson = StringListJson.encode(linkedNodeIds)
)

private fun NodeEntity.toDto() = NodeDto(
    id = id, label = label, type = type, subtype = subtype, canonicalName = canonicalName, description = description,
    importanceScore = importanceScore,
    createdAt = Instant.ofEpochMilli(createdAt).toString(), updatedAt = Instant.ofEpochMilli(updatedAt).toString()
)

private fun NodeDto.toEntity() = NodeEntity(
    id = id, label = label, type = type, subtype = subtype, canonicalName = canonicalName, description = description,
    importanceScore = importanceScore,
    createdAt = Instant.parse(createdAt).toEpochMilli(), updatedAt = Instant.parse(updatedAt).toEpochMilli()
)

private fun EdgeEntity.toDto() = EdgeDto(
    id = id, fromNodeId = fromNodeId, toNodeId = toNodeId, relationType = relationType,
    weight = weight, confidence = confidence, createdAt = Instant.ofEpochMilli(createdAt).toString()
)

private fun EdgeDto.toEntity() = EdgeEntity(
    id = id, fromNodeId = fromNodeId, toNodeId = toNodeId, relationType = relationType,
    weight = weight, confidence = confidence, createdAt = Instant.parse(createdAt).toEpochMilli()
)

private fun TaskEntity.toDto() = TaskDto(
    id = id, title = title, details = details, status = status, priority = priority,
    dueAt = dueAt?.let { Instant.ofEpochMilli(it).toString() }, projectId = projectId,
    linkedMemoryId = linkedMemoryId, createdAt = Instant.ofEpochMilli(createdAt).toString(),
    updatedAt = Instant.ofEpochMilli(updatedAt).toString()
)

private fun TaskDto.toEntity() = TaskEntity(
    id = id, title = title, details = details, status = status, priority = priority,
    dueAt = dueAt?.let { Instant.parse(it).toEpochMilli() }, projectId = projectId,
    linkedMemoryId = linkedMemoryId, createdAt = Instant.parse(createdAt).toEpochMilli(),
    updatedAt = Instant.parse(updatedAt).toEpochMilli()
)

private fun ProjectEntity.toDto() = ProjectDto(
    id = id, name = name, description = description, status = status,
    createdAt = Instant.ofEpochMilli(createdAt).toString(), updatedAt = Instant.ofEpochMilli(updatedAt).toString()
)

private fun ProjectDto.toEntity() = ProjectEntity(
    id = id, name = name, description = description, status = status,
    createdAt = Instant.parse(createdAt).toEpochMilli(), updatedAt = Instant.parse(updatedAt).toEpochMilli()
)

private fun ConnectorAccountEntity.toDto() = ConnectorAccountDto(
    id = id, name = name, type = type, enabled = enabled, authMode = authMode,
    scopes = StringListJson.decode(scopeJson),
    lastSyncAt = lastSyncAt?.let { Instant.ofEpochMilli(it).toString() }, lastError = lastError
)

private fun ConnectorAccountDto.toEntity() = ConnectorAccountEntity(
    id = id, name = name, type = type, enabled = enabled, authMode = authMode,
    scopeJson = StringListJson.encode(scopes),
    lastSyncAt = lastSyncAt?.let { Instant.parse(it).toEpochMilli() }, lastError = lastError
)

private fun GuidanceEventEntity.toDto() = GuidanceEventDto(
    id = id, type = type, content = content,
    createdAt = Instant.ofEpochMilli(createdAt).toString(),
    sourceMemoryIds = StringListJson.decode(sourceMemoryIdsJson)
)

private fun GuidanceEventDto.toEntity() = GuidanceEventEntity(
    id = id, type = type, content = content,
    createdAt = Instant.parse(createdAt).toEpochMilli(),
    sourceMemoryIdsJson = StringListJson.encode(sourceMemoryIds)
)
