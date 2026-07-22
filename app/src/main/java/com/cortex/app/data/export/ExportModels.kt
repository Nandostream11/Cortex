package com.cortex.app.data.export

import kotlinx.serialization.Serializable

/**
 * Mirrors ExportFormat.json field-for-field. Kept separate from the domain models and
 * Room entities on purpose — the export format needs to stay stable across internal
 * refactors, and having its own DTOs is what makes that possible.
 */
@Serializable
data class ExportEnvelope(
    val formatVersion: Int = 1,
    val exportedAt: String,
    val app: String = "Cortex",
    val encrypted: Boolean = true,
    val contents: ExportContents
)

@Serializable
data class ExportContents(
    val settings: ExportSettings,
    val memories: List<MemoryItemDto>,
    val nodes: List<NodeDto>,
    val edges: List<EdgeDto>,
    val tasks: List<TaskDto>,
    val projects: List<ProjectDto>,
    /** Never includes the secret itself — see ConnectorAccountDto. */
    val connectorAccounts: List<ConnectorAccountDto>,
    val guidanceEvents: List<GuidanceEventDto>
)

@Serializable
data class ExportSettings(
    val apiProvider: String,
    val autonomyMode: String,
    val connectorStates: List<String>
)

@Serializable
data class MemoryItemDto(
    val id: String,
    val createdAt: String,
    val updatedAt: String,
    val rawText: String,
    val normalizedText: String,
    val sourceType: String,
    val sourceRef: String?,
    val category: String,
    val importanceScore: Double,
    val confidenceScore: Double,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val tagIds: List<String>,
    val linkedNodeIds: List<String>
)

@Serializable
data class NodeDto(
    val id: String,
    val label: String,
    val type: String,
    val subtype: String? = null,
    val canonicalName: String,
    val description: String?,
    val importanceScore: Double = 0.0,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class EdgeDto(
    val id: String,
    val fromNodeId: String,
    val toNodeId: String,
    val relationType: String,
    val weight: Double,
    val confidence: Double,
    val createdAt: String
)

@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val details: String?,
    val status: String,
    val priority: String,
    val dueAt: String?,
    val projectId: String?,
    val linkedMemoryId: String?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ProjectDto(
    val id: String,
    val name: String,
    val description: String?,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

/** Deliberately has no secret/token field. Secrets never leave [com.cortex.app.security.SecretStore]. */
@Serializable
data class ConnectorAccountDto(
    val id: String,
    val name: String,
    val type: String,
    val enabled: Boolean,
    val authMode: String,
    val scopes: List<String>,
    val lastSyncAt: String?,
    val lastError: String?
)

@Serializable
data class GuidanceEventDto(
    val id: String,
    val type: String,
    val content: String,
    val createdAt: String,
    val sourceMemoryIds: List<String>
)
