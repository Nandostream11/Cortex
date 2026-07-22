# Cortex

A local-first Android personal intelligence assistant. Capture stays private on-device;
memory becomes a searchable, connected knowledge graph over time; guidance is surfaced
deterministically wherever possible, with OpenRouter used only where it clearly adds value.

Design docs (architecture, schema, security model, roadmap, issue backlog) live outside
this tree and drive what gets built and in what order — see `docs/PHASE1_STATUS.md` for
where implementation currently stands against them.

## Status: Phase 2 (local knowledge graph) in progress

Not yet buildable end-to-end in a CI environment (no network access to Google's Maven
repo was available while building either phase), so this has been reviewed and, where
possible, compiler-verified in isolation, but not compiled by Android Studio yet. See
`docs/PHASE2_STATUS.md` before continuing work — it also covers what changed since
Phase 1, including a real deduplication bug found and fixed this phase.

## Opening the project

1. Open in Android Studio (Ladybird/Meerkat or newer recommended).
2. Let Android Studio's Gradle sync run — it will offer to generate the Gradle wrapper
   jar (`gradle/wrapper/gradle-wrapper.jar` is intentionally not committed as a binary
   here; `gradle-wrapper.properties` is).
3. If Android Studio flags any dependency version in `gradle/libs.versions.toml` as
   outdated, accept the suggested bump — those versions were pinned without live access
   to Maven and may already be a patch or two behind.
4. Run the `app` configuration on an emulator or device (minSdk 26 / Android 8.0+).

## Module layout

Single `:app` module, packages matching the architecture doc's layering:

- `domain/model`, `domain/usecase` — plain Kotlin, no Android dependency
- `data/db`, `data/repository`, `data/settings`, `data/export`, `data/mapper` — Room, DataStore, export/import
- `security` — Keystore-backed secret storage and password-based backup encryption
- `ui` — Compose screens, navigation, theme
- `di` — a small manual composition root (`AppContainer`); no DI framework yet

## Testing

`app/src/test` has fast JVM unit tests (fake repository, no Android framework needed).
The crypto and classification logic in these tests was independently verified against a
real Kotlin compiler during development — see `docs/PHASE1_STATUS.md`.
