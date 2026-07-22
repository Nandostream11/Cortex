# Cortex Graph Engine

## Purpose
Transform raw notes into a knowledge graph that makes memory useful, navigable, and explainable.

## Node types
- Memory
- Concept
- Project
- Task
- Person
- Tool
- Paper
- Bug
- Goal
- ConnectorSource

## Edge types
- MENTIONS
- RELATED_TO
- DERIVED_FROM
- DEPENDS_ON
- BLOCKS
- RESOLVES
- FOLLOW_UP_ON
- SUPPORTS
- CONTRADICTS
- IMPORTED_FROM

## Graph behaviors
### 1. Node canonicalization
Merge equivalent labels into one canonical node.

### 2. Relation extraction
Infer edges from phrases like:
- "because"
- "for"
- "to fix"
- "depends on"
- "related to"
- "next step"

### 3. Weighted edges
Edges have a weight and confidence score.
Weights may increase when:
- a relation repeats
- the node is referenced often
- the relation is central to a project

### 4. Duplicate handling
- Detect similar nodes
- Propose merges
- Keep provenance for explainability

### 5. Search ranking
Rank results using:
- keyword relevance
- recency
- importance
- graph centrality
- project relevance
- source confidence

### 6. Topic clustering
Cluster nodes into topic groups for:
- robotics
- research
- writing
- debugging
- planning
- study

### 7. Explainability
Every graph result should explain why it surfaced:
- shared tag
- direct relation
- recent mention
- project membership
- similar wording

## Recommended algorithms
- BM25 or similar lexical ranking
- Jaccard similarity for tag overlap
- PageRank or degree-based centrality
- Union-find for dedup workflows
- Community detection when graph size grows
- Heuristic relation inference before LLM assistance

---

## Phase 2 implementation notes (appended, not a rewrite of the design above)

The design above is what was targeted; this section records what actually shipped and
where it differs, so the two don't drift silently out of sync.

**Package layout**: per ADR-0002, split layer-first then subsystem — pure algorithms in
`domain.graph` (EntityExtractor, RelationInferer, NodeDeduplicator, NodeSimilarity,
GraphRanking, GraphLayout, WordTrie, AliasTable, EntityDictionaries,
EntityToNodeMapping), Room-backed orchestration in `data.graph` (GraphRepository,
GraphBuilder, GraphUpdater, GraphQueryEngine, GraphStatistics).

**EntityKind vs NodeType**: extraction uses a 24-value `EntityKind` taxonomy, finer than
the 10-value `NodeType` this doc originally specified. `EntityToNodeMapping` folds one
onto the other and keeps the detail as `Node.subtype`. DATE/TIME/NUMBER/VERSION/
ERROR_MESSAGE are extracted as classification signal but deliberately never become
nodes — see that file's doc comment for the full reasoning, including why
GIT_COMMIT/FILE_PATH/URL/BRANCH_NAME/COMMAND *are* promoted despite usually being
mentioned only once.

**Relation types**: this doc's example relations (`belongs_to`, `resolved_by`,
`used_in`, `executed_for`) aren't all literal `RelationType` values — the schema has 10
edge types, not one per English verb used in an example. `RelationInferer` maps each
co-occurrence rule onto the closest existing type and documents the mapping inline per
rule (e.g. "task belongs_to project" → `DEPENDS_ON`, not a new `BELONGS_TO` type).

**Ranking**: implemented per this doc's intent — degree centrality, weighted PageRank
(power iteration with dangling-mass redistribution), exponential recency decay,
log-normalized reference count. One honest simplification: MemoryEngine.md's composite
score distinguishes "reference count" from "retrieval frequency," but Cortex doesn't
track read/retrieval events yet, so `GraphUpdater` currently feeds the same
inbound-MENTIONS count into both terms rather than inventing a second number. Flagged in
code, not silently merged into one term.

**Deduplication**: alias table (exact abbreviation lookup) first, then string similarity
gated to the same `NodeType`. The similarity function is *not* raw whole-string
Levenshtein — an early version was, and it wrongly merged "Project A" with "Project B"
(a one-character edit in a nine-character string scores 0.89 regardless of whether that
character is semantically load-bearing). Fixed to token-wise matching; see
`NodeSimilarity`'s doc comment and `docs/PHASE2_STATUS.md` for how this was caught.

**Performance tradeoffs taken knowingly, not silently**: `GraphUpdater` recomputes
importance for every node on every capture (whole-graph PageRank is not incremental);
`SearchEngine.search` rebuilds the BM25 index from scratch per query;
`GraphQueryEngine.relatedMemories` issues one DAO call per node visited during BFS
rather than a batched query. All three are the right complexity tradeoff at the node/
memory counts a personal graph reaches early on, and all three are called out in their
own doc comments as the first thing to revisit if that stops being true — not left as
an undocumented surprise for whoever hits the slowdown first.
