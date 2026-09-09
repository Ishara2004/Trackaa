# Security Policy

## Supported versions

Trackaa is currently pre-release. Security fixes are applied to the latest development branch and the latest tagged release when releases begin.

## Reporting a vulnerability

Please do **not** publish exploitable security details in a public GitHub issue.

Use GitHub's private vulnerability reporting feature for this repository when available, or contact the repository owner privately through an appropriate GitHub profile contact method.

Include:

- affected version / commit
- Android version and device when relevant
- reproduction steps
- expected vs actual behavior
- impact
- proof-of-concept details that are necessary to reproduce the issue

## Security-sensitive areas

Extra scrutiny is required for changes to:

- encrypted backup / restore
- Android Keystore integration
- signing configuration
- Room migrations and restore transactions
- notification-policy / DND behavior
- exported Android components and PendingIntents
- file sharing / FileProvider
- audit/history modification

## Secrets

Never commit:

- `.jks` / `.keystore` files
- signing passwords
- API keys
- backup passphrases
- private device backup keys
- local `.env` secrets

Release signing is expected to use environment-provided credentials.

## Backup security model

Portable backups use authenticated AES-GCM encryption with a passphrase-derived key. Automatic local backups use a device-managed Android Keystore key. A backup can still contain sensitive productivity history after decryption, so exported files should be handled accordingly.
