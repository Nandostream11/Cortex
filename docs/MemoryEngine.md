# Cortex Memory Engine

## Purpose
Convert raw input into structured, durable, searchable memory.

## Pipeline
1. Capture input
2. Normalize text
3. Detect language and command-like segments
4. Extract entities and tasks
5. Extract relations
6. Score importance
7. Store locally
8. Update graph and search indexes
9. Optionally send to AI for synthesis or cleanup

## Memory categories
- Idea
- Note
- Task
- Project update
- Bug report
- Paper note
- Decision
- Reflection
- Reminder
- Connector import

## Importance score factors
- Recency
- Frequency
- Repetition
- Project relevance
- Novelty
- User pinning
- Connector source weight
- Number of graph links
- Actionability
- Unfinished state

## Normalization rules
- Remove obvious transcription noise.
- Split long text into smaller units.
- Preserve technical terms, code, paths, filenames, and commands.
- Detect dates, deadlines, and instructions.
- Store raw and normalized text.

## Deduplication
- Merge near-identical notes.
- Preserve source history.
- Keep a confidence score for merges.
- Never silently delete user intent.

## Recall strategy
- Keyword retrieval first
- Project-aware retrieval second
- Graph-aware ranking third
- Semantic retrieval later if available

## Long-term behavior
- Frequently referenced memories become stronger.
- Unused memories can decay in rank, not disappear.
- Pinned memories always remain easy to access.
