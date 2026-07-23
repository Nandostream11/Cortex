# ADR-0004: Room database encrypted at rest via SQLCipher

- Status: Accepted
- Date: Post-Phase-2 hardening (top item on Phase 2's "known gaps" list)

## Context

Security.md requires "encrypt sensitive local files," and both `docs/PHASE1_STATUS.md`
and `docs/PHASE2_STATUS.md` flagged the same open gap: `AndroidSecretStore` protects API
keys and connector tokens, `BackupCrypto` protects exports, but the Room SQLite file
itself (`cortex.db`) was plaintext. Phase 2 made this more pressing, not less — the
graph engine persists *inferred relationships between memories* (a bug is `RESOLVES`-ed
by a specific commit, a person is `RELATED_TO` a specific project) in that same file,
not just raw captured text.

## Decision

Encrypt the database with SQLCipher via `net.zetetic:sqlcipher-android`'s
`SupportOpenHelperFactory`, which Room's `openHelperFactory()` accepts as a drop-in
replacement for its default SQLite implementation — no DAO, query, or entity code
changes needed.

**Library choice**: `net.zetetic:sqlcipher-android`, not the older
`net.zetetic:android-database-sqlcipher`. Confirmed via web search this session (not
training-data memory, since this rename appears to have happened close to or after this
assistant's cutoff) that the older artifact is officially deprecated — its own GitHub
repo states so directly, and cites Google's 16KB memory page size requirement for Play
Store submissions as a concrete reason the deprecated version can't be brought current.
The new library's integration API is different (`SupportOpenHelperFactory` instead of
the old `SupportFactory`), which is why this isn't a trivial version bump of the same
class.

**Passphrase storage**: a random 256-bit passphrase, generated once via `SecureRandom`
and stored through the existing Keystore-backed `SecretStore` (`DatabasePassphraseProvider`).
Not a Keystore-native key used directly — SQLCipher's API takes a raw passphrase byte
array, and Keystore has no mechanism to hand out the private material of a hardware-backed
key for that purpose. This is the standard, documented pattern for this exact situation
(confirmed against multiple current guides during this session's research): generate a
strong random secret, then protect *that* secret with Keystore-backed storage, same
protection `SecretStore` already gives the OpenRouter API key.

## Consequence — the honest verification gap

Every other pure-logic piece of this codebase (`EntityExtractor`, `GraphRanking`,
`NodeSimilarity`, `Bm25Index`, etc.) was compiled and executed against a real Kotlin
compiler in the sandbox this was built in, per the verification discipline established
in `docs/PHASE1_STATUS.md` and extended in `docs/PHASE2_STATUS.md`. **That method does
not apply here.** SQLCipher requires native (JNI) binaries that only run on a real
Android runtime — there is no way to load and exercise `libsqlcipher.so` on a plain JVM.
This integration was written carefully against `sqlcipher-android`'s own current
documentation (fetched via web search, not recalled from training), but it is
**unverified beyond that**. Treat getting this specific integration working — the
Gradle dependency resolving, the native library loading, a real device actually opening
an encrypted database — as its own explicit checkpoint the first time this project
reaches a real Android build, not a detail safe to assume works alongside everything
else that has been more thoroughly checked.

## What this does not cover

- **Migrating an existing plaintext database.** Not implemented, and deliberately not
  designed around yet: this project has never had a successful build, so there is no
  real installed base with an existing plaintext `cortex.db` to migrate from. If that
  changes before this ships, SQLCipher's own `SQLiteDatabase.loadLibs()` +
  encrypt-in-place tooling is the documented path — noted here so it isn't forgotten,
  not implemented speculatively against a scenario that doesn't exist yet.
- **Key rotation.** Still listed as pending in `docs/CURRENT_STATE.md`'s Security
  Status section, unchanged by this ADR.
