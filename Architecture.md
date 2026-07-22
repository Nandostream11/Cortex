# Cortex Architecture

## 1. System overview
Cortex is a modular Android app with a local intelligence core and an optional AI/connector layer.

## 2. Major layers
### UI layer
- Jetpack Compose
- Screens: Home, Capture, Search, Graph, Guidance, Connectors, Settings

### Domain layer
- Use cases for capture, indexing, search, guidance, graph traversal, and export

### Data layer
- Room database for memories, nodes, edges, tasks, projects, connectors, and events
- DataStore for settings
- Encrypted file storage for backups and exports

### Intelligence layer
- Text normalization
- Entity extraction
- Relation extraction
- Importance scoring
- Similarity ranking
- Pattern discovery
- Guidance generation

### AI layer
- OpenRouter client
- Structured prompt engine
- JSON output parsing
- AI used only when local algorithms are not enough

### Connector layer
- Connector interface
- OAuth/API-key based auth support
- Scoped sync jobs
- Import pipelines into local memory

### Security layer
- Android Keystore
- Encrypted preferences/secure storage
- Backup encryption
- Permission boundaries

## 3. Data flow
Input -> normalize -> extract entities/relations -> persist local memory -> update graph/index -> rank/search -> guidance -> optional AI synthesis

## 4. Design principles
- Local-first
- Deterministic first
- Modular by subsystem
- Test each use case independently
- Keep the UI thin
- Keep the memory model explicit

## 5. Proposed module packages
- `app.ui`
- `app.capture`
- `app.memory`
- `app.graph`
- `app.guidance`
- `app.connectors`
- `app.security`
- `app.ai`
- `app.sync`
- `app.domain`
- `app.data`

## 6. Recommended implementation strategy
1. Build a clean Android project shell.
2. Implement local persistence and security.
3. Add capture and memory creation.
4. Add graph storage and search.
5. Add guidance and ranking.
6. Add OpenRouter.
7. Add connector framework.
8. Improve graph visualization and pattern discovery.

## 7. Long-term extensibility
The architecture should support future plugin-like modules for:
- connectors
- specialized parsers
- research assistants
- task planners
- external data importers
- visualization add-ons

---

## Phase 2 addendum: package organization

Section 5's proposed flat subsystem packages (`app.graph`, `app.capture`, etc.) turned
out to be in tension with section 2's layered model once the graph engine needed both a
pure-algorithm half and a Room-backed half. Resolved in ADR-0002
(`docs/adr/0002-package-organization-layered-not-subsystem.md`): layer-first at the top
level (`domain`, `data`, `ui`, `security`), subsystem grouping one level down
(`domain.graph`, `data.graph`, `domain.search`, `data.search`, `ui.screens.graph`, ...).
This is now the standing convention for every subsystem added after Phase 2, not just
the graph engine.
