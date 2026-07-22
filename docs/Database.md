# Cortex Database Schema

## 1. Storage choices
- Room for structured persistent data
- Encrypted files for export/import
- DataStore for preferences
- Optional embeddings cache if added later

## 2. Main entities
### MemoryItem
- id
- createdAt
- updatedAt
- rawText
- normalizedText
- sourceType
- sourceRef
- importanceScore
- confidenceScore
- isPinned
- isArchived

### Node
- id
- label
- type
- canonicalName
- description
- createdAt
- updatedAt

### Edge
- id
- fromNodeId
- toNodeId
- relationType
- weight
- confidence
- createdAt

### Task
- id
- title
- details
- status
- priority
- dueAt
- projectId
- linkedMemoryId

### Project
- id
- name
- description
- status
- createdAt
- updatedAt

### ConnectorAccount
- id
- name
- type
- enabled
- authMode
- scopeJson
- lastSyncAt
- lastError

### GuidanceEvent
- id
- type
- content
- createdAt
- sourceMemoryIdsJson

## 3. Relationships
- One MemoryItem can create many Nodes and Edges.
- One Task can link to one Project and one or more MemoryItems.
- ConnectorAccount can import many MemoryItems.
- GuidanceEvent should reference source memories for explainability.

## 4. Indexing and retrieval
- Text index for keyword search
- Rank fields for importance and recency
- Graph lookup tables for traversal
- Optional full-text search if needed

## 5. Export format
The export should preserve:
- all entities
- all relations
- timestamps
- settings metadata
- connector metadata without secrets

## 6. Migration guidance
- Keep schema versioned.
- Add migrations for every structural change.
- Never silently destroy old user data.
