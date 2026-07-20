# ENGINEERING_PRINCIPLES.md

# Cortex Engineering Principles

Version: 1.0

This document defines the timeless engineering philosophy of Cortex. Unlike the implementation roadmap, these principles should remain stable even if the technology stack changes.

---

# Purpose

Engineering decisions should optimize for the long-term evolution of Cortex rather than the fastest short-term implementation.

The codebase should remain understandable, modular, testable, and extensible for years.

---

# Fundamental Principles

## 1. Simplicity

Choose the simplest architecture that satisfies the requirements.

Simple does **not** mean minimal.

Simple means:

- understandable
- maintainable
- extensible
- predictable

Avoid clever code.

Avoid unnecessary abstractions.

Avoid premature optimization.

---

## 2. Modularity

Every subsystem should be independently understandable.

Examples:

- Memory Engine
- Graph Engine
- Search Engine
- Guidance Engine
- Connector Framework
- AI Layer
- Security Layer

Modules communicate through interfaces.

No module should depend directly on another module's implementation.

---

## 3. Separation of Concerns

Presentation

↓

Application

↓

Domain

↓

Infrastructure

No layer should violate this boundary.

---

## 4. Local First

The local database is the source of truth.

Everything else is secondary.

Connectors import data.

AI interprets data.

Neither owns the data.

---

## 5. Deterministic First

Whenever possible:

Use algorithms.

Not AI.

Examples:

Search

Ranking

Deduplication

Parsing

Graph construction

Scheduling

Relationship inference

All should initially use deterministic approaches.

---

## 6. Explainability

Every recommendation should be explainable.

If Cortex suggests something, it should be possible to answer:

Why?

Which memories contributed?

Which graph relationships were followed?

Which rules fired?

---

## 7. Progressive Intelligence

The application should become smarter over time without requiring increasingly larger AI models.

Priority:

Better algorithms

Better graph

Better memory

Better ranking

Better context

Only then consider larger models.

---

## 8. Incremental Development

Build systems in layers.

Never build everything simultaneously.

Preferred order:

Capture

↓

Storage

↓

Graph

↓

Search

↓

Guidance

↓

AI

↓

Connectors

↓

Optimization

---

## 9. Loose Coupling

Prefer:

Interfaces

Dependency Injection

Repositories

Events

Avoid:

Singletons

Global mutable state

Tight dependencies

---

## 10. Strong Domain Models

Domain models should represent real concepts.

Avoid "utility" models that mix unrelated concerns.

A MemoryItem should represent memory.

A Task should represent work.

A Project should represent a project.

Not arbitrary JSON blobs.

---

## 11. Security by Design

Security is not a feature.

It is a property of every feature.

Assume:

Devices can be lost.

Backups can leak.

Logs can be inspected.

Never expose secrets.

---

## 12. Testing Philosophy

Tests are specifications.

Good tests describe behavior.

Do not write tests only for coverage.

Write tests that describe intent.

---

## 13. Documentation

Every important decision should outlive its author.

Documentation should explain:

Why

Not just

What

---

## 14. Performance

Optimize only after correctness.

Then optimize using measurements.

Never guess.

Profile first.

---

## 15. Extensibility

Design every subsystem assuming it will gain additional capabilities.

Examples:

More connectors

More AI providers

Desktop version

Plugin ecosystem

Synchronization

Avoid designs that require rewriting the core.

---

## 16. Failure Handling

Failures should be:

Predictable

Recoverable

Logged

Explainable

Silent failures are unacceptable.

---

## 17. Data Integrity

Never destroy user data silently.

When unsure:

Preserve.

Warn.

Ask.

Never assume.

---

## 18. Architecture Evolution

Architecture is allowed to improve.

Breaking architectural changes require:

- ADR
- Migration strategy
- Updated documentation

---

## 19. User Trust

The most valuable asset of Cortex is user trust.

Never compromise it for convenience.

---

## 20. Long-Term Goal

Build software that remains understandable five years from now.

Every commit should move Cortex toward becoming a long-lived, reliable cognitive operating system.
