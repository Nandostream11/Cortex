# ADR-0001: Backup encryption uses a password-derived key, not Android Keystore

- Status: Accepted
- Date: Phase 1

## Context

Security.md requires two things that are easy to conflate: (1) API keys and connector
tokens should be encrypted at rest via Android Keystore, and (2) exported backups should
be "restored... with a password or secure key" — i.e. portable to a new device or a
fresh install.

Android Keystore keys are device- and app-install-bound. They do not survive
uninstall/reinstall and cannot be exported. A backup encrypted with a Keystore key would
only ever be restorable on the exact same app installation that created it, which
defeats the purpose of a backup.

## Decision

Two separate encryption mechanisms, for two separate purposes:

- `AndroidSecretStore` (Keystore-backed `EncryptedSharedPreferences`) for API keys and
  connector tokens. These should never leave the device, so device-binding is a feature,
  not a bug.
- `BackupCrypto` (password-derived AES-256-GCM via PBKDF2, 210k iterations) for
  export/import. Portable by construction — anyone with the file and the password can
  restore it, on any device.

## Consequence

A restored `ConnectorAccount` comes back without its credential (the export format never
includes secrets — see `ExportFormat.json`'s `connectorAccounts: array_without_secrets`)
and shows as needing re-authentication. This is intentional, not a gap.

## Note

An earlier draft during the same Phase 1 session used Keystore-bound `EncryptedFile` for
export. That was wrong for the reason above and was replaced with `BackupCrypto` before
anything shipped — noted here so the reasoning is on record, not because the mistake
reached `main`.
