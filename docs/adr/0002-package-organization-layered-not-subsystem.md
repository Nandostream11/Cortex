# ADR-0002: Package organization is layer-first, subsystem-second

- Status: Accepted
- Date: Phase 2 (formalizing a choice already made silently in Phase 1)

## Context

Architecture.md contains two organizing principles that don't fully agree with each
other:

- Section 2 ("Major layers") describes a classic layered architecture: UI layer, Domain
  layer, Data layer, Intelligence layer, AI layer, Connector layer, Security layer.
- Section 5 ("Proposed module packages") proposes top-level packages organized by
  subsystem instead: `app.ui`, `app.capture`, `app.memory`, `app.graph`, `app.guidance`,
  `app.connectors`, `app.security`, `app.ai`, `app.sync`, `app.domain`, `app.data`.

These are two different, both-reasonable ways to organize the same codebase
(organize-by-layer vs. organize-by-feature), and the document proposes both without
reconciling them. Taken literally, section 5 would put `CaptureTextMemoryUseCase` in
`app.capture` while section 2 implies use cases belong in a domain layer alongside
graph/search/guidance use cases — those aren't the same tree.

Phase 1 already picked layer-first (`com.cortex.app.domain`, `.data`, `.ui`,
`.security`) without writing that decision down. Phase 2 forces the question explicitly:
does the graph engine live in `com.cortex.app.graph`, or split across
`com.cortex.app.domain.graph` / `com.cortex.app.data.graph`?

## Decision

Layer-first at the top level, subsystem as the package underneath:

```
com.cortex.app.domain.graph      — pure algorithms: extraction, ranking, dedup
com.cortex.app.domain.search     — pure BM25 scoring
com.cortex.app.domain.usecase    — pipelines that tie subsystems together
com.cortex.app.data.graph        — Room-backed GraphRepository, GraphBuilder, GraphQueryEngine
com.cortex.app.data.search       — SearchEngine (repositories + domain.search)
com.cortex.app.ui.screens.graph  — the graph visualization screen
```

This keeps CodingStandards' "each subsystem independently testable" as a hard property
of the folder structure: everything under `domain/` has zero Android dependency and
compiles/tests as plain Kotlin, which is exactly the boundary that made it possible to
verify graph-engine logic in this sandbox without an Android build (see
`docs/PHASE2_STATUS.md`). A pure subsystem-package scheme (`app.graph` containing both
the algorithm and the Room DAO in one folder) would blur that boundary.

Subsystem grouping is preserved one level down, which keeps section 5's actual intent —
"everything about the graph lives near everything else about the graph" — mostly intact.

## Consequence

Architecture.md section 5 is updated to describe this explicitly instead of listing flat
subsystem packages, so the next session isn't left to resolve the same ambiguity again.
