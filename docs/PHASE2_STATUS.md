# Phase 2 Status

Read this before starting Phase 3 — it's the handoff between sessions, in the same
spirit as `docs/PHASE1_STATUS.md`.

## Step 1: Phase 1 verification — what was actually done

The instructions asked for a full Gradle sync, debug build, and test run before writing
any Phase 2 code. Honest account of what that meant in this environment: **this sandbox
has no access to Google's Maven repository** (confirmed again this session — Maven
Central, `dl.google.com`, and friends are not reachable; only a small fixed allowlist
is, none of it a general package repo), so a literal `./gradlew build` could not be run
here, same limitation noted at the end of Phase 1.

What was done instead, and found:

1. **A real bug, not just stale versions.** `app/build.gradle.kts` turned on
   `buildFeatures.compose = true` but never applied a Compose Compiler Gradle plugin.
   Since Kotlin 2.0, that plugin is no longer bundled with `org.jetbrains.kotlin.android`
   — it's a separate plugin (`org.jetbrains.kotlin.plugin.compose`) that must be applied
   explicitly. Without this fix, the project would fail to build the moment any
   `@Composable` got compiled. Phase 1 was written against a pre-2.0 mental model where
   this wasn't necessary. **Fixed** — see ADR-0003.
2. **Stale dependency versions**, corrected via live web search against
   developer.android.com / github.com/Kotlin / mvnrepository.com (this session does have
   web search, unlike bash network access). Bumped AGP, Kotlin, Compose BOM, coroutines
   to current stable. Deliberately did **not** jump to AGP 9.x or Room 3.0 — both are
   major breaking rewrites released after this assistant's training cutoff, and
   authoring against unfamiliar API surface with no way to compiler-check it here is a
   worse bet than staying one generation back on well-understood tooling. Full reasoning
   in ADR-0003.
3. **A real, missing Room migration**, added as part of Phase 2's schema change (see
   below) rather than deferred — `fallbackToDestructiveMigration()` was never used.
4. Every Phase 1 unit test was re-read for correctness; none needed changes.

No architecture changes were needed to Phase 1's foundations beyond the above — the
layering, the security split (Keystore vs. password-derived backup crypto), and the
manual DI approach all held up.

## Verification method this session (same discipline as Phase 1, extended)

Same approach as Phase 1: since a full Android/Gradle build isn't possible here, every
piece of **pure, dependency-free Kotlin** was compiled and executed against a real
Kotlin compiler (downloaded into the sandbox from GitHub releases) with actual
assertions, not just read for plausibility. This went further than Phase 1's spot-check
— essentially the entire `domain.graph` and `domain.search` package was exercised this
way, because that's where nearly all of Phase 2's actual logic lives.

**Compiled and run with real assertions, all passing:**
- `EntityExtractor` — a realistic multi-paragraph sample exercising every entity kind
  (bug ids, commands, branch names, commits, people, dates including relative-date
  resolution, research paper citations in two formats, file paths, a GitHub URL correctly
  classified as REPOSITORY, and structural TASK/GOAL/ERROR_MESSAGE spans correctly
  surviving alongside point entities nested inside them)
- `WordTrie` — multi-word dictionary term matching, longest-match preference,
  case-insensitivity
- `GraphRanking` — PageRank sums to ~1.0 and correctly ranks a hub above its spokes and
  above an isolated pair; degree centrality; recency decay at exactly one half-life;
  composite importance bounds
- `NodeSimilarity` / `NodeDeduplicator` — alias-table resolution, type-gated matching,
  and (after the bug described below) the fixed token-wise similarity function
- `RelationInferer` — category-gated rules, confidence bounds, dedup of repeated
  mentions
- `Bm25Index` — relevant-above-irrelevant ranking, term-frequency sensitivity, stopword
  filtering, empty-corpus/empty-query edge cases
- `GraphLayout` — bounds, non-overlap, connected-nodes-end-up-closer-than-unrelated,
  determinism, empty/single-node edge cases
- The exact input strings used in this session's `GraphBuilderTest` JUnit file (e.g.
  `"Task: fix CORTEX-99 in the Cortex project using Kotlin"`) were run through the real
  extractor to confirm the JUnit assertions match actual behavior, not assumed behavior

**Not compiled here — genuinely can't be, not a corner cut:** anything touching
`kotlinx.coroutines.Flow`, Room annotations, or Jetpack Compose. That's
`GraphRepositoryImpl`, `GraphBuilder`'s Room-backed path, `SearchEngine`,
`MemoryLinkingPipeline`, `GraphViewModel`/`SearchViewModel`, and both new screens
(`GraphScreen`, `SearchScreen`). These were written carefully — cross-checked line by
line against the already-verified pure logic they call, and every JUnit test written
against them uses a hand-built `FakeGraphRepository`/`FakeMemoryRepository` rather than
Room, so at least the *orchestration logic* (not the Room wiring itself) is exercised by
tests that will actually run once Gradle can. `GraphScreen`'s gesture handling
(`detectTransformGestures`/`detectTapGestures`, coordinate transforms) is the single
least-verified file in this codebase — it follows standard, well-established Compose
patterns, but there was no way to compile-check it here at all. **If Phase 3 starts with
one task, make it opening this project in Android Studio and fixing whatever that first
real build surfaces** — expect it to be small, but expect something.

## A real bug found and fixed during this session

`NodeDeduplicator`'s similarity check originally used `max(tokenJaccard,
wholeStringLevenshteinRatio)`. Testing it against a deliberately adversarial case —
two short, clearly-different names — found that **"Project A" and "Project B" would
have silently merged into one node** (raw Levenshtein ratio scores a 1-character edit in
a 9-character string at 0.89, above the 0.82 merge threshold, regardless of whether that
character is semantically meaningful). This would have been a real, silent data-quality
bug: two different projects merging into one graph node with no error, no log, nothing
for a user to notice until their graph quietly stopped distinguishing between two things
they'd named similarly.

Fixed by replacing whole-string Levenshtein with token-wise matching (`NodeSimilarity.
tokenwiseSimilarity`): each token in the shorter name is greedily matched to its
best-scoring counterpart in the longer name, scores are averaged over the *longer*
name's token count so a length mismatch costs something. This correctly separates "one
word is a typo of another" (`"Raspberry Pi"` vs `"Rasberry Pi"` → 0.94, still merges) from
"one whole word is different" (`"Project A"` vs `"Project B"` → 0.5, correctly doesn't).
Verified via the compiler that this fix doesn't regress any previously-passing scenario
(ROS2/PCL alias resolution, TensorFlow correctly not matching anything, type-gating).
Both the original failure and the fix are captured as regression tests in
`NodeSimilarityTest`.

This is the kind of thing "read the code and it looks right" would not have caught —
the bug only showed up by actually running numbers through the similarity function.
Worth keeping in mind for any future subsystem that does fuzzy matching.

## What Phase 2 built

**`domain.graph`** (pure, no Android dependency): `EntityKind` (24-value extraction
taxonomy) + `EntityToNodeMapping` (folds it onto `NodeType`'s 10 values + subtype,
documented reasoning for what's excluded and what's kept despite being low-reuse);
`WordTrie` + `EntityDictionaries` (seed robotics/software term lists); `EntityExtractor`
(regex/trie/rule-based, zero AI calls, two-layer overlap resolution — point entities and
structural spans resolved independently so a GOAL span survives containing a DATE);
`RelationInferer` (co-occurrence rules mapped onto the existing `RelationType` enum,
each mapping documented inline); `NodeSimilarity` + `AliasTable` + `NodeDeduplicator`
(dedup, see the bug above); `GraphRanking` (degree centrality, PageRank, recency decay,
composite importance); `GraphLayout` (deterministic force-directed layout).

**`domain.search`**: `Bm25Index` (Okapi BM25, no semantic/AI search per the brief).

**`data.graph`**: `GraphRepository`/`GraphRepositoryImpl` (Room-backed, dedup-aware
`findOrCreateNode`, reinforce-on-repeat `createOrReinforceEdge`); `GraphBuilder` (the
per-memory extract→dedupe→link pipeline); `GraphUpdater` (whole-graph ranking refresh);
`GraphQueryEngine` (neighbors, BFS related-memories traversal); `GraphStatistics`.

**`data.search`**: `SearchEngine` (BM25 + tag/date/node-name filters + graph-traversal
related-memories, all wired to real repositories).

**Schema**: `NodeEntity` gained `subtype` and `importanceScore` columns via a real
`Migration(1, 2)` — additive, no data loss, not `fallbackToDestructiveMigration()`.

**Pipeline**: `MemoryLinkingPipeline` replaces bare `CaptureTextMemoryUseCase` as what
the UI actually calls — capture now automatically extracts, links, and re-ranks, per
MemoryEngine.md Step 7's "this entire pipeline must happen automatically."

**UI**: `SearchScreen` (real BM25 search, debounced) and `GraphScreen` (Canvas-based
pan/zoom/tap visualization, node color by type, tap-to-highlight-neighbors, search bar,
tap-through hook to related memories) replace their Phase 1 placeholders.
Guidance/Connectors/Settings remain honest placeholders — out of scope per the brief.

## Architecture decisions recorded as ADRs this session

- **ADR-0001** (retroactive): why backup encryption is password-derived, not
  Keystore-bound — documents a Phase 1 decision that was made but never written down.
- **ADR-0002**: resolves a real tension in `Architecture.md` between its layered-model
  section and its flat-subsystem-package section, which Phase 2's graph engine forced a
  decision on. Both docs updated to match.
- **ADR-0003**: the toolchain version decisions from Step 1 (fix the Compose Compiler
  gap; bump what's safe; deliberately stay behind on AGP 9/Room 3).

## Known gaps to prioritize next, in order

1. **Open in Android Studio, get a clean sync and build.** Unchanged from Phase 1's
   #1 — still the actual first checkpoint, now with more code riding on it.
2. **Room database encryption at rest.** Still open from Phase 1. Now more pressing:
   the graph makes *inferred relationships between memories* newly queryable in plain
   SQLite, not just the memories themselves.
3. **The three documented performance tradeoffs** (whole-graph ranking recompute,
   BM25 rebuilt per query, N+1 BFS traversal) — all fine today, all flagged in their own
   doc comments and in `GraphEngine.md`'s addendum as the first things to revisit.
4. **GraphScreen is the least-verified file in the codebase** — see above. Budget real
   device/emulator testing time for pan/zoom/tap feel, not just a compile check.
5. **Node aliasing is a fixed seed list** (`AliasTable`). GraphEngine.md's Step 5 doesn't
   ask for user-editable aliases, but it's the natural next step once someone hits a
   dedup miss the alias table doesn't cover.
6. **Voice capture** — still just a disabled button, unchanged from Phase 1.
7. **Guidance engine, connectors, OpenRouter integration** — explicitly out of scope for
   this phase per the brief, not started.
