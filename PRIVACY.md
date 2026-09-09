# Trackaa Privacy Principles

Trackaa V1 is designed as a local-first productivity application.

## V1 data flow

The intended V1 product does not require:

- an account
- cloud synchronization
- an advertising SDK
- a generative-AI API
- behavioral analytics telemetry

Primary user data is stored locally in Room and preferences in DataStore.

## Data stored on device

Depending on the features the user enables, Trackaa may store:

- goals, work items/modules/projects and topics
- tasks, priorities, estimates and completion status
- focus-session timestamps
- exact focus/pause/break segments
- session quality and energy ratings
- optional session intent/outcome/reflection
- interruptions and reasons
- targets and target revision history
- weekly availability/capacity
- scheduled focus reminders
- XP and achievement state
- audit/history records
- locally generated backup/report metadata

## Permissions

Trackaa should request only capabilities needed by an enabled feature.

Potential Android permissions/special access include:

- notifications
- notification-policy / Do Not Disturb access
- exact alarms when the user wants precise scheduled-focus reminders
- foreground-service capability for an active user-started focus session
- Storage Access Framework access when the user explicitly exports/imports a file

Denying an optional permission should not prevent basic local focus tracking.

## Backups

Portable backups are encrypted and authenticated before export. The user's passphrase is not stored by Trackaa.

Automatic private backups use a key held by Android Keystore and are intended for same-device recovery. They are not a substitute for a user-controlled portable backup when moving devices.

Decrypted productivity data can reveal routines, schedules, study/work areas and performance history. Users should treat exported reports and decrypted backups as sensitive personal files.

## Future changes

If a future release adds accounts, cloud sync, remote APIs, crash reporting, telemetry or AI features, the user-facing privacy disclosures and this document must be updated before release.
