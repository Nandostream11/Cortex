# Cortex Connector SDK

## Purpose
Provide an opt-in framework for importing data from external services into the local Cortex graph and memory store.

## Connector principles
- User-controlled
- Explicitly enabled
- Per-connector scopes
- Secure secrets storage
- Local copy remains the source of truth
- Sync is reversible and inspectable

## Connector interface
Each connector should define:
- id
- displayName
- description
- auth mode
- supported entities
- sync() function
- import mapping rules
- error reporting
- last sync metadata

## Supported auth modes
- API key
- OAuth
- PAT
- custom token
- manual import

## Import mapping
Connectors should map external objects to Cortex entities:
- issue -> task or bug
- document -> note or paper
- calendar event -> task or memory
- message -> memory
- repo issue/PR -> task, bug, or project update

## Configuration UI
For each connector show:
- enabled state
- connection health
- scopes
- sync frequency
- last sync time
- imported items count
- error logs
- delete connection

## Connector safety rules
- Never auto-enable.
- Never over-scope permissions.
- Never store secrets in plain text.
- Never overwrite local data without confirmation.
- Always show provenance for imported content.

## Future plugin direction
The connector SDK should be easy to extend with community connectors later.
