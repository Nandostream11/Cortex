# Cortex Coding Standards

## General
- Prefer clarity over cleverness.
- Keep files small and focused.
- Make public APIs explicit.
- Document non-obvious logic.

## Kotlin style
- Use idiomatic Kotlin.
- Avoid giant classes.
- Prefer small use cases and repositories.
- Keep UI state separate from business logic.

## Architecture
- UI should not hold core business logic.
- Domain rules belong in use cases.
- Data access belongs in repositories/DAOs.
- Each subsystem should be independently testable.

## Error handling
- Fail gracefully.
- Preserve user data on errors.
- Surface clear recovery actions.

## Testing
- Unit test graph logic, ranking logic, parsing, and export/import.
- Add integration tests for persistence and guidance flows.
- Test critical edge cases for connector sync and encryption.

## Documentation
- Update docs when architecture changes.
- Document every public module.
- Keep ADRs for major decisions.

## Commit discipline
- Small, focused commits.
- Meaningful commit messages.
- Never mix unrelated changes.

## AI usage
- Use deterministic logic first.
- Use OpenRouter only when necessary.
- Prefer structured outputs.
- Validate AI output before writing to storage.
