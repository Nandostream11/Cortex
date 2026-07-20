AI_DEVELOPER_CONTRACT.md

«Project: Cortex
Version: 1.0
Purpose: This document defines the engineering principles, architecture philosophy, quality standards, and development workflow that every AI coding agent must follow when contributing to Cortex.»

---

Mission

Cortex is not a note-taking application.

Cortex is a local-first cognitive operating system designed to help users capture thoughts, organize knowledge, discover connections, reduce cognitive load, and receive trustworthy guidance while maintaining complete ownership of their data.

Every architectural and implementation decision must support this mission.

---

Core Philosophy

Privacy Before Convenience

The user's data belongs to the user.

Default assumptions:

- Local storage
- Local processing
- Local indexing
- Local search
- Local reasoning

Cloud services must always be optional.

---

Algorithms Before AI

Every feature must first attempt to solve the problem deterministically.

Preferred order:

1. Rule engine
2. Data structures
3. Search algorithms
4. Graph algorithms
5. Statistical models
6. Local ML models (optional)
7. LLM reasoning (only if the above are insufficient)

Do not call an LLM because it is easier to implement.

---

Knowledge Before Notes

The application stores knowledge, not text.

Every captured memory should gradually become:

- entities
- relationships
- projects
- goals
- tasks
- concepts
- evidence
- decisions

Raw text is only the starting point.

---

Guidance Before Automation

The purpose of Cortex is to improve human thinking.

Do not automate decisions that belong to the user.

Prefer:

- asking useful questions
- identifying missing information
- surfacing relevant memories
- suggesting alternatives

over acting autonomously.

---

Transparency Before Magic

Every recommendation should be explainable.

If Cortex recommends something, it should be able to identify:

- what memories contributed
- what graph relationships were used
- what rules were triggered
- what assumptions were made

Avoid opaque behavior.

---

Product Principles

Cortex should feel:

- calm
- intelligent
- trustworthy
- predictable
- private
- research-oriented
- engineering-oriented

It should never feel like a chatbot pretending to know everything.

---

Engineering Principles

Architecture

Follow Clean Architecture.

Presentation

↓

Application

↓

Domain

↓

Infrastructure

Dependencies always point inward.

No UI layer should know implementation details of storage or networking.

---

SOLID

Apply SOLID principles whenever practical.

Favor interfaces over concrete implementations.

Favor composition over inheritance.

Avoid god classes.

---

Modularity

Large systems must be isolated.

Expected modules include:

- capture
- memory
- graph
- search
- guidance
- connectors
- ai
- export
- security
- settings

Modules communicate through interfaces.

---

Repository Pattern

Persistent data must be accessed through repositories.

UI must never query Room directly.

---

Dependency Injection

Constructor injection is preferred.

Avoid service locators.

If manual DI becomes difficult to maintain, migrate to Hilt or another DI framework with a documented ADR.

---

Knowledge Graph Rules

The knowledge graph is the primary memory structure.

It is not a visualization feature.

Everything eventually maps to nodes and edges.

Examples:

Task

↓

Project

↓

Technology

↓

Research Paper

↓

Person

↓

Idea

↓

Decision

↓

Evidence

↓

Experiment

Edges must include:

- type
- confidence
- source
- timestamp
- provenance where applicable

Support graph evolution without breaking existing data.

---

AI Usage Rules

LLMs are assistants.

They are not databases.

They are not memory.

They are not the source of truth.

Use OpenRouter only for:

- summarization
- synthesis
- brainstorming
- clarification
- long-form explanation
- guidance

Never use an LLM for:

- storage
- indexing
- graph maintenance
- ranking
- deterministic extraction
- security
- permissions

---

Connector Rules

Connectors are plugins.

They are optional.

Every connector must:

- declare permissions
- be independently enabled
- be independently disabled
- store credentials securely
- synchronize explicitly or on a configurable schedule

The local database remains the source of truth.

---

Security Requirements

API keys:

- encrypted
- never logged
- never exported

Connector tokens:

- encrypted
- revocable
- never embedded in backups

Backups:

- password encrypted
- portable across devices
- versioned

Memory database:

- encrypted at rest where practical
- protected by migrations
- validated on restore

---

Performance Targets

App launch:

< 1.5 seconds

Memory capture:

< 200 ms

Search:

< 100 ms for common queries

Graph update:

< 300 ms for typical note sizes

UI interactions:

Maintain approximately 60 FPS during common operations.

Avoid unnecessary background work.

---

Coding Standards

Maximum class size:

~400 lines preferred.

Maximum function size:

~60 lines preferred.

Prefer descriptive names.

Avoid abbreviations.

Document public APIs.

Use immutable data whenever practical.

Favor sealed classes and enums for domain concepts.

---

Documentation Rules

Every significant feature requires:

- architecture notes
- implementation notes
- tests
- changelog entry if user-visible

Major decisions require an Architecture Decision Record (ADR).

---

Testing Policy

Every public algorithm requires tests.

Required test categories:

- unit
- integration
- persistence
- export/import
- graph correctness
- search
- security
- regression

Aim for meaningful coverage rather than a numeric target alone.

---

Architecture Decision Records

Whenever a major technical decision changes direction:

Create:

docs/adr/ADR-XXX.md

Include:

- context
- options considered
- decision
- rationale
- consequences

Never silently change architectural direction.

---

Version Control

Use small, focused commits.

Suggested style:

feat:

fix:

refactor:

docs:

test:

perf:

build:

chore:

Avoid mixing unrelated work in a single commit.

---

User Experience Rules

Avoid unnecessary dialogs.

Avoid interrupting the user.

Default to subtle guidance.

The UI should reduce cognitive load.

Consistency is more important than visual novelty.

---

"Done" Definition

A feature is complete only when:

- implemented
- documented
- tested
- integrated
- reviewed
- accessible
- maintainable

Placeholder implementations are not considered complete.

---

Decision Framework

When multiple implementations are possible, prioritize:

1. Correctness
2. Privacy
3. Maintainability
4. Extensibility
5. Performance
6. Simplicity
7. Developer convenience

---

When Requirements Conflict

Resolve conflicts in this order:

1. User privacy
2. Data integrity
3. Correctness
4. Explainability
5. Local-first operation
6. Performance
7. Convenience

---

Expected Development Workflow

For every development session:

1. Read this contract.
2. Read the latest handoff document.
3. Build and run tests.
4. Understand the existing architecture.
5. Propose improvements if necessary.
6. Implement one coherent milestone.
7. Update documentation.
8. Add or update tests.
9. Record ADRs for architectural changes.
10. Produce a detailed handoff document for the next session.

Never skip verification to add features faster.

---

Long-Term Vision

Cortex should become a platform rather than a single application.

The architecture should support:

- plugin ecosystems
- additional connectors
- local AI models
- future desktop companion
- future web companion
- synchronization (optional)
- collaborative research workflows (optional)

without requiring major rewrites.

---

Final Principle

Every contribution should make Cortex more capable of helping users think, not merely store information.

If a proposed feature increases complexity without improving understanding, memory, guidance, or user trust, reconsider the design before implementing it.
