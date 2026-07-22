# Cortex Security

## Security goals
- Protect user data on-device
- Encrypt secrets and backups
- Minimize connector risk
- Keep local data private by default

## Required controls
### Secrets
- Store API keys using Android Keystore-backed encryption or equivalent secure storage.
- Never hardcode secrets in source.
- Never log secrets.

### Data at rest
- Encrypt sensitive local files.
- Encrypt export archives.
- Protect connector tokens.

### Permissions
- Ask for only needed permissions.
- Make microphone use explicit.
- Make connector permissions explicit.

### Network
- Use HTTPS only.
- Do not call external services unless the user has configured them.
- Keep connector traffic isolated.

### Backup
- Backup files should be encrypted by default.
- Restoring should require user action and password or secure key.

## Security UX
- Show exactly what data leaves the device.
- Make connector scopes readable.
- Explain any external model call in plain language.

## Threat model priorities
- Lost phone
- Leaked backup
- Misconfigured connector
- Accidental secret exposure
- Unwanted cloud sync
