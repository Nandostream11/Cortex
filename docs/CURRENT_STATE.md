# CURRENT_STATE.md

# Cortex Current State

This document always reflects the latest verified implementation state of the project.

It should be updated at the end of every development session.

---

# Repository

Status: Active Development

Current Phase:

Phase 2 — Graph Intelligence (complete; not yet Gradle-verified — see Known Risks)

Next Phase:

Phase 3 — Guidance Engine, Thinking Mode, Daily Brief

---

# Architecture Status

## Completed

- Android project scaffold
- Gradle configuration
- Version catalog (bumped to current stable this phase; see docs/adr/0003)
- Manifest
- Compose application shell (Compose Compiler plugin gap found and fixed this phase)
- Navigation shell
- Room database (schema v2 — see Room migration below)
- DAOs
- Domain models
- Repository interfaces
- Capture pipeline (now routes through MemoryLinkingPipeline, not just persistence)
- Memory persistence
- Encrypted API key storage
- Password-encrypted export/import
- Manual dependency injection
- Unit tests for crypto
- Unit tests for capture pipeline
- **Graph Engine**: entity extraction, relation inference, deduplication, ranking
  (PageRank/degree/recency/composite), force-directed layout
- **Search Engine**: BM25 keyword search, tag/date/node-name filters, graph-traversal
  related-memories
- **Search Screen**, **Graph Screen** (Canvas pan/zoom/tap visualization)
- Room migration 1→2 (adds NodeEntity.subtype, NodeEntity.importanceScore — additive,
  no data loss)

---

## Partially Implemented

- Memory Engine (capture→classify→score→graph-link pipeline exists; deeper
  contradiction/pattern detection is Phase 3)
- Settings (repository exists; no Settings UI yet)
- Export/Import (format supports graph nodes/edges now; no in-app UI trigger yet)
- Home Screen
- Capture Screen

---

## Placeholder Screens

- Guidance
- Connectors
- Settings UI

(Search and Graph are no longer placeholders as of this phase.)

---

# Intelligence Status

## Implemented

Basic text normalization

Memory classification

Importance scoring

Persistence

**Deterministic entity extraction** (24 entity kinds; regex + trie + rule-based, zero AI)

**Relationship extraction** (co-occurrence rules mapped onto RelationType, confidence-scored)

**Knowledge graph** (persistent, Room-backed; nodes deduplicated, edges reinforced on repeat)

**Deduplication** (alias table + token-wise similarity, type-gated)

**Search engine** (BM25, no semantic/AI search)

**Ranking engine** (degree centrality, PageRank, recency decay, composite importance)

---

## Missing

Pattern detection

Thinking mode

Daily brief

Reflection engine

Contradiction detection

Memory evolution

Project intelligence

---

# AI Status

Implemented:

None

Planned:

OpenRouter integration

Prompt engine

Structured output

Guidance synthesis

Reasoning assistance

---

# Connector Status

Implemented:

None

Planned:

GitHub

Google Drive

Calendar

Gmail

Slack

Notion

Markdown folders

Custom connector SDK

---

# Security Status

Implemented

Encrypted API keys

Encrypted connector credentials

Password-encrypted backup

---

Pending

Room database encryption (still open — now higher priority, see Known Risks)

Key rotation

Secure backup verification

Migration strategy — **partially resolved**: a real Migration(1,2) now exists and is
additive-only; the strategy itself (how future migrations get reviewed/tested) is still
informal.

---

# Testing Status

Completed

Crypto tests

Capture tests

**Graph tests** (extraction, relation inference, deduplication — including a regression
test for a real bug found this phase, see Technical Debt — ranking, layout, statistics,
query traversal)

**Search tests** (BM25, tag/date/node filters, related-memories)

**Integration-style tests** for the full capture→graph pipeline (MemoryLinkingPipeline),
using hand-built fakes rather than Room

---

Pending

Room tests (real Room, in-memory database — current graph/repository tests use a fake
`GraphRepository`, not Room itself; the Room-backed implementation is unverified — see
Known Risks)

Connector tests

Guidance tests

Full instrumented integration tests

Performance tests

---

# Technical Debt

High Priority

- Verify full Gradle build — still the top item, now with substantially more code
  riding on it (Phase 2 is unverified at the Android-toolchain level; see Known Risks)
- Encrypt Room database — still open, and more sensitive now that inferred
  relationships between memories are queryable in plain SQLite, not just raw text
- A real deduplication bug was found and fixed this phase (whole-string Levenshtein
  wrongly merged "Project A"/"Project B"); worth an explicit regression-test audit
  before trusting any *other* fuzzy-matching code written the same way in future phases

Medium Priority

- Replace manual DI if needed — not yet needed; Phase 2 added ~15 new classes through
  it without strain
- Improve navigation architecture
- Improve export validation
- Three documented performance tradeoffs (whole-graph ranking recompute on every
  capture, BM25 rebuilt per query, N+1 query pattern in BFS graph traversal) — each is
  fine at current scale and each is flagged in its own doc comment as the first thing
  to revisit if that changes

Low Priority

- UI polish
- Animations
- Accessibility improvements
- GraphScreen is the least-verified file in the codebase (Compose gesture handling
  couldn't be compile-checked in the sandbox this was built in) — budget real
  device/emulator testing time specifically for it

---

# Current Roadmap

## Phase 2 — COMPLETE

Graph Engine

Entity Extraction

Relationship Engine

Deduplication

Ranking

Search

Graph Visualization

---

## Phase 3 (next)

Guidance Engine

Thinking Mode

Daily Brief

Pattern Discovery

Project Intelligence

---

## Phase 4

OpenRouter

Prompt Engine

Structured Outputs

AI Guidance

Reflection

---

## Phase 5

Connector Framework

GitHub

Drive

Calendar

Email

Slack

Custom Plugins

---

## Phase 6

Performance

Offline Optimizations

Widgets

Desktop Companion

Plugin SDK

Public Release

---

# Known Risks

- First Android Studio build may reveal dependency/version issues — unchanged risk,
  now covering roughly 2x the code it did at the end of Phase 1. A real bug (missing
  Compose Compiler plugin) was already found and fixed by manual review this phase;
  there may be others manual review can't catch, which is exactly why this remains
  the top risk rather than something to assume is fine.
- Room schema will evolve; migration strategy must be introduced early — a first real
  migration now exists (v1→v2), but there's no established review process yet for
  what a "safe" migration looks like as the schema keeps changing.
- Graph complexity may grow quickly without careful indexing — three specific spots
  are already flagged (whole-graph ranking recompute, per-query BM25 rebuild, N+1 BFS)
  as the first places to look if this becomes real.
- The Room-backed graph persistence layer (`GraphRepositoryImpl`) is exercised only
  through a hand-built in-memory fake in tests, not real Room — this is a genuine gap,
  not an oversight, and should be closed with real Room instrumented tests before this
  layer is trusted in production.
- Connector framework should remain optional and isolated — not started yet, no risk
  introduced this phase.
- AI integration must not become the primary execution path — not started yet; every
  Phase 2 subsystem (extraction, relations, dedup, ranking, search) is fully
  deterministic, zero LLM calls, consistent with this principle.

---

# Immediate Next Priorities

1. Achieve a clean Android Studio build. Still #1.
2. Verify all existing tests pass, including the ~90 new Phase 2 assertions.
3. Write real Room instrumented tests for GraphRepositoryImpl (currently only
   fake-repository-tested).
4. Encrypt Room database.
5. Begin Phase 3: Guidance Engine, starting with whatever the graph engine's own
   GraphStatistics/ranking output can support without any AI calls (pattern discovery
   from graph structure, before reaching for OpenRouter).
6. Wire a Settings screen so autonomy mode, OpenRouter key entry, and export/import
   are actually reachable from the UI, not just implemented underneath it.

---

# Session Handoff Rules

At the end of every development session:

1. Update this document.
2. Update the roadmap if necessary.
3. Record new technical debt.
4. Record resolved technical debt.
5. Update architecture if it changed.
6. Add ADRs for significant decisions.
7. Produce a detailed handoff document for the next development session.

This document should always represent the current verified state of Cortex.
