# Phase 1 Status

Written at the end of the foundation-scaffolding session. Read this before starting the
next piece of work — it's the handoff note between sessions.

## What existed before this session

Just `LICENSE` and a one-line `README.md`. One commit. No source code, no Gradle
project. This was a genuinely greenfield start, not a partial build.

## What this session built

**Project scaffold**: Gradle version catalog, root/app build files, manifest, adaptive
launcher icon (pure vector, no PNG needed since minSdk 26 = the adaptive-icon API
level), local-backup-disabled data extraction rules.

**Domain layer** (`domain/model`, `domain/usecase`): MemoryItem, Node, Edge, Task,
Project, ConnectorAccount, GuidanceEvent, and their enums, matching `GraphSchema.json`
and `Database.md`. `CaptureTextMemoryUseCase` implements the *thin* Phase 1 slice of
MemoryEngine.md's pipeline — normalize, classify, score, persist. Entity/relation
extraction, dedup, and graph updates are explicitly out of scope here and are Phase 2.

**Persistence** (`data/db`): Room database with all seven entities, DAOs with
foreign-key-aware queries (edges cascade with their nodes; tasks SET NULL their project
link rather than cascading, so deleting a project never silently deletes tasks).

**Security** (`security`): Two separate mechanisms, deliberately not merged into one:
- `AndroidSecretStore` — Keystore-backed `EncryptedSharedPreferences` for API keys and
  connector tokens. Never meant to leave the device.
- `BackupCrypto` — password-derived AES-256-GCM (PBKDF2, 210k iterations) for encrypted
  export/import. This one is *not* Keystore-based on purpose: Keystore keys don't
  survive reinstall or a device switch, but `Security.md` requires backups to be
  restorable with a password. An earlier draft of this used Keystore-bound
  `EncryptedFile` and was wrong for that reason — caught and corrected during this same
  session, not shipped.

**Export/import** (`data/export`): Builds the exact envelope shape from
`ExportFormat.json`, encrypts it with `BackupCrypto`. Import merges by primary key
(REPLACE on conflict) rather than wiping local data first. Connector secrets are never
included — a restored connector comes back disabled, needing re-auth.

**UI** (`ui`): Compose Material3 shell, bottom nav across the six top-level
destinations from Architecture.md, capture reached via a FAB (not a tab, since it's the
primary action). Home and Capture are fully wired end-to-end: type text → save → it
persists to Room → the list updates via Flow. Search/Graph/Guidance/Connectors/Settings
are explicit "not built yet" placeholders, not mocked-up fakes.

**DI** (`di`): Manual composition root (`AppContainer`), not Hilt/Dagger. Deliberate for
now — annotation-processing DI adds real risk of subtle misconfiguration that's harder
to catch without a build environment (see Verification below), and the project is small
enough that constructor injection through one container is still clear. Worth
revisiting once wiring gets bigger.

**Tests** (`app/src/test`): JUnit tests for `BackupCrypto` (round-trip, wrong-password
rejection, truncated-payload rejection, salt/IV randomness) and
`CaptureTextMemoryUseCase` (normalization, classification, confidence-by-source,
score bounds) against a fake in-memory repository.

## Verification: what was actually checked vs. what wasn't

This sandbox has no access to Google's Maven repository, so a full Gradle/Android build
could not be run — that's the honest limit here. To reduce risk anyway, two things were
independently compiled and executed with a real Kotlin compiler (downloaded from
GitHub, JDK 21) during this session, outside the Android project:

- `BackupCrypto` — compiled standalone (it has zero Android/library dependencies) and
  run through round-trip, wrong-password, and truncated-payload cases. All passed.
- The classification/scoring heuristic from `CaptureTextMemoryUseCase` — extracted and
  run against 8 sample inputs plus score-bounds checks. All passed.
- All 8 domain model files were compiled together successfully (correct types, correct
  enum references, no circular issues).

Everything else — Room annotation processing, Compose, DataStore, Navigation, the
manual DI wiring, the full `CaptureTextMemoryUseCase`/`MemoryRepository` pair (blocked
specifically by `kotlinx-coroutines-core` not being fetchable without Maven access) —
was written carefully against well-established, standard patterns but **has not been
compiled**. Treat first Gradle sync in Android Studio as the real first checkpoint, and
expect to fix at least small things (a wrong import, a version catalog entry Android
Studio wants bumped).

## Known gaps to prioritize next, in order

1. **Open in Android Studio, get a clean Gradle sync and a successful build.** This is
   the actual next task, before adding any new feature — everything above is unverified
   at the Android-toolchain level until this happens.
2. **Room database encryption at rest.** Right now, `cortex.db` itself is *not*
   encrypted — only secrets (`AndroidSecretStore`) and exports (`BackupCrypto`) are.
   Memory content (potentially sensitive: bug reports, decisions, personal notes) sits
   in a plain SQLite file. `Security.md` implies this should be closed; SQLCipher (via
   Room's `SupportFactory`) is the standard route. Flagging this explicitly rather than
   letting it pass as done — it's the biggest gap between what's built and what
   Security.md asks for.
3. **Room migrations.** `CortexDatabase` is version 1 with no migration strategy yet.
   Fine today; needs a real plan before schema changes ship to anyone.
4. **Voice capture.** The mic button is present and intentionally disabled — no
   transcription pipeline exists. Needs a decision on-device ASR vs. an OpenRouter
   audio-capable model before this is real.
5. **Node/Edge extraction from captured text** (GraphEngine.md) — the seam
   (`CaptureTextMemoryUseCase`) is there; nothing calls into graph creation yet.
6. **Wire Settings/Connectors/Search screens to their now-existing repositories** —
   `ExportManager`, `SettingsRepository`, and the DAOs are ready; the UI for them isn't.

## A note on dependency versions

`gradle/libs.versions.toml` was pinned to versions believed current as of early 2026,
but this sandbox can't reach Maven to confirm the latest patch releases. Android
Studio's dependency update check should be run once the project opens, and its
suggestions accepted rather than left stale.
