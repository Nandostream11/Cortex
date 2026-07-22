# Cortex Product Requirements Document

## 1. Summary
Cortex is an Android application that acts as a personal intelligence assistant. It accepts voice and text input, turns thoughts into structured local memory, stores and links knowledge in a graph, and provides proactive guidance with minimal AI usage.

## 2. Goals
- Capture thoughts quickly by text or push-to-talk voice.
- Store everything locally in encrypted form.
- Build a durable knowledge graph of memories, entities, projects, tasks, and relations.
- Surface relevant memories and patterns over time.
- Provide actionable guidance, summaries, and follow-up questions.
- Support optional connectors for external services.
- Use OpenRouter only when deterministic methods are insufficient.

## 3. Non-goals
- No always-on cloud dependency.
- No generic social/chat product behavior.
- No hard dependency on embeddings or LLMs for core storage.
- No opaque memory system that the user cannot inspect or export.

## 4. Core user stories
- As a user, I can speak a thought and have Cortex turn it into memory.
- As a user, I can type a note and have Cortex organize it automatically.
- As a user, I can search by concept, project, tool, bug, or person.
- As a user, I can see linked memories and graph relationships.
- As a user, I can receive a daily brief of unfinished work and important thoughts.
- As a user, I can connect external services only when I choose.
- As a user, I can export all data in encrypted JSON.

## 5. Functional requirements
### Capture
- Text input
- Push-to-talk voice input
- Transcription pipeline
- Raw note storage and normalized memory storage

### Memory
- Entity extraction
- Relation extraction
- Deduplication
- Importance scoring
- Search ranking
- Time-based resurfacing

### Graph
- Nodes and edges persisted locally
- Graph traversal
- Topic clustering
- Project-centric views
- Related-memory surfacing

### Guidance
- Summary generation
- Next-step suggestions
- Follow-up questions
- Pattern detection
- Daily brief

### Connectors
- Configurable connector framework
- Per-connector auth and scope settings
- Manual and scheduled sync
- Local import into Cortex graph and memory store

### Security
- Encrypted local storage
- Encrypted API key storage
- Encrypted JSON export/import
- No secrets in plaintext

## 6. Quality requirements
- Fast start-up
- Smooth offline operation
- Readable codebase
- Test coverage for core logic
- Modular architecture
- Clear migration path for future plugins

## 7. Success criteria
- A user can capture, organize, search, and revisit memories reliably.
- Cortex can explain why a memory is important.
- Cortex can suggest useful next steps without overwhelming the user.
- The app remains useful even if AI is unavailable.
