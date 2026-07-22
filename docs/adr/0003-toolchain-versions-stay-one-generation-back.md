# ADR-0003: Stay one generation back on AGP and Room; fix the missing Compose Compiler plugin

- Status: Accepted
- Date: Phase 2, Step 1 (Phase 1 verification)

## Context

Verifying Phase 1 before starting Phase 2 (per this session's instructions) surfaced one
real bug and several stale version pins:

**Real bug**: `app/build.gradle.kts` had `buildFeatures.compose = true` but never applied
a Compose Compiler plugin. Since Kotlin 2.0, the Compose compiler is no longer bundled
with `org.jetbrains.kotlin.android` — it ships as a separate Gradle plugin
(`org.jetbrains.kotlin.plugin.compose`) that must be applied explicitly. Phase 1 was
written against pre-2.0 mental model where this wasn't necessary. Without the fix, the
project would fail to build the moment any `@Composable` function got compiled.

**Stale versions**: Phase 1's version catalog was pinned from training-era knowledge.
Live search during this session found the real current state (mid-2026): AGP is at
9.1/9.2, Room has released 3.0 (new `androidx.room3` package, KSP-only, coroutine-first,
breaking API), Compose BOM is at 2026.06.00, Kotlin at 2.2.20.

## Decision

- **Fix the Compose Compiler plugin gap.** Not optional — this is a correctness bug, not
  a version choice.
- **Bump Kotlin, Compose BOM, coroutines, DataStore-adjacent libraries to current
  stable.** These are additive/compatible updates within a generation this assistant
  understands well.
- **Do NOT jump to AGP 9.x or Room 3.0.** Both are major breaking rewrites released after
  this assistant's training cutoff, and this sandbox has no Maven access to
  compiler-verify unfamiliar API surface against them. AGP 9's new DSL specifically
  breaks compatibility with the traditional `org.jetbrains.kotlin.android` plugin
  arrangement used here; Room 3.0 moves to a different Maven coordinate
  (`androidx.room3`) and Java-codegen-free, suspend-only API. Authoring against either
  from search summaries alone, with no way to catch a subtly wrong DSL call, is a worse
  bet than staying one generation back on tooling with a well-established, thoroughly
  documented API this assistant has high confidence in.

## Consequence

The project stays on AGP 8.11.0 / Room 2.8.1 for now. Android Studio's own AGP 9
upgrade tooling ("agent skill" per developer.android.com) is the right way to do that
migration when someone is ready to drive it interactively and check the result — not
this session, blind.

This is a deliberate, temporary gap, not an oversight: it's called out here and in
`docs/PHASE2_STATUS.md` so it isn't mistaken for either.
