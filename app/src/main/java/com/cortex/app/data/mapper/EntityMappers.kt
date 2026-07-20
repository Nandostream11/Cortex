package com.cortex.app.data.mapper

import com.cortex.app.data.db.StringListJson
import com.cortex.app.data.db.entity.MemoryItemEntity
import com.cortex.app.data.db.entity.NodeEntity
import com.cortex.app.data.db.entity.EdgeEntity
import com.cortex.app.data.db.entity.TaskEntity
import com.cortex.app.data.db.entity.ProjectEntity
import com.cortex.app.data.db.entity.ConnectorAccountEntity
import com.cortex.app.data.db.entity.GuidanceEventEntity
import com.cortex.app.domain.model.ConnectorAccount
import com.cortex.app.domain.model.ConnectorAuthMode
import com.cortex.app.domain.model.Edge
import com.cortex.app.domain.model.GuidanceEvent
import com.cortex.app.domain.model.GuidanceEventType
import com.cortex.app.domain.model.MemoryCategory
import com.cortex.app.domain.model.MemoryItem
import com.cortex.app.domain.model.MemorySourceType
import com.cortex.app.domain.model.Node
import com.cortex.app.domain.model.NodeType
import com.cortex.app.domain.model.Project
import com.cortex.app.domain.model.ProjectStatus
import com.cortex.app.domain.model.RelationType
import com.cortex.app.domain.model.Task
import com.cortex.app.domain.model.TaskPriority
import com.cortex.app.domain.model.TaskStatus
import java.time.Instant

fun MemoryItemEntity.toDomain() = MemoryItem(
    id = id,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    rawText = rawText,
    normalizedText = normalizedText,
    sourceType = MemorySourceType.valueOf(sourceType),
    sourceRef = sourceRef,
    category = MemoryCategory.valueOf(category),
    importanceScore = importanceScore,
    confidenceScore = confidenceScore,
    isPinned = isPinned,
    isArchived = isArchived,
    tagIds = StringListJson.decode(tagIdsJson),
    linkedNodeIds = StringListJson.decode(linkedNodeIdsJson)
)

fun MemoryItem.toEntity() = MemoryItemEntity(
    id = id,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    rawText = rawText,
    normalizedText = normalizedText,
    sourceType = sourceType.name,
    sourceRef = sourceRef,
    category = category.name,
    importanceScore = importanceScore,
    confidenceScore = confidenceScore,
    isPinned = isPinned,
    isArchived = isArchived,
    tagIdsJson = StringListJson.encode(tagIds),
    linkedNodeIdsJson = StringListJson.encode(linkedNodeIds)
)

fun NodeEntity.toDomain() = Node(
    id = id,
    label = label,
    type = NodeType.valueOf(type),
    canonicalName = canonicalName,
    description = description,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt)
)

fun Node.toEntity() = NodeEntity(
    id = id,
    label = label,
    type = type.name,
    canonicalName = canonicalName,
    description = description,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli()
)

fun EdgeEntity.toDomain() = Edge(
    id = id,
    fromNodeId = fromNodeId,
    toNodeId = toNodeId,
    relationType = RelationType.valueOf(relationType),
    weight = weight,
    confidence = confidence,
    createdAt = Instant.ofEpochMilli(createdAt)
)

fun Edge.toEntity() = EdgeEntity(
    id = id,
    fromNodeId = fromNodeId,
    toNodeId = toNodeId,
    relationType = relationType.name,
    weight = weight,
    confidence = confidence,
    createdAt = createdAt.toEpochMilli()
)

fun TaskEntity.toDomain() = Task(
    id = id,
    title = title,
    details = details,
    status = TaskStatus.valueOf(status),
    priority = TaskPriority.valueOf(priority),
    dueAt = dueAt?.let { Instant.ofEpochMilli(it) },
    projectId = projectId,
    linkedMemoryId = linkedMemoryId,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt)
)

fun Task.toEntity() = TaskEntity(
    id = id,
    title = title,
    details = details,
    status = status.name,
    priority = priority.name,
    dueAt = dueAt?.toEpochMilli(),
    projectId = projectId,
    linkedMemoryId = linkedMemoryId,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli()
)

fun ProjectEntity.toDomain() = Project(
    id = id,
    name = name,
    description = description,
    status = ProjectStatus.valueOf(status),
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt)
)

fun Project.toEntity() = ProjectEntity(
    id = id,
    name = name,
    description = description,
    status = status.name,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli()
)

fun ConnectorAccountEntity.toDomain() = ConnectorAccount(
    id = id,
    name = name,
    type = type,
    enabled = enabled,
    authMode = ConnectorAuthMode.valueOf(authMode),
    scopes = StringListJson.decode(scopeJson),
    lastSyncAt = lastSyncAt?.let { Instant.ofEpochMilli(it) },
    lastError = lastError
)

fun ConnectorAccount.toEntity() = ConnectorAccountEntity(
    id = id,
    name = name,
    type = type,
    enabled = enabled,
    authMode = authMode.name,
    scopeJson = StringListJson.encode(scopes),
    lastSyncAt = lastSyncAt?.toEpochMilli(),
    lastError = lastError
)

fun GuidanceEventEntity.toDomain() = GuidanceEvent(
    id = id,
    type = GuidanceEventType.valueOf(type),
    content = content,
    createdAt = Instant.ofEpochMilli(createdAt),
    sourceMemoryIds = StringListJson.decode(sourceMemoryIdsJson)
)

fun GuidanceEvent.toEntity() = GuidanceEventEntity(
    id = id,
    type = type.name,
    content = content,
    createdAt = createdAt.toEpochMilli(),
    sourceMemoryIdsJson = StringListJson.encode(sourceMemoryIds)
)
