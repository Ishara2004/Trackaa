# Contributing to Trackaa

Trackaa prioritizes **data correctness and recoverability** over feature count. A visually attractive feature is not acceptable if it can silently inflate, lose, duplicate, or misattribute focus history.

## Development principles

1. Verified focus segments are the authoritative source of focus time.
2. Pause and break intervals must never be included in focus totals.
3. Unknown downtime after crashes/reboots must never be silently counted.
4. Planning engines must expose infeasibility rather than invent capacity.
5. Derived analytics must be reproducible from persisted source data.
6. Permission denial must degrade gracefully.
7. Public documentation must not claim an unverified feature is production-ready.

## Local setup

Requirements:

- JDK 17
- Android SDK 36
- Gradle 9.6 or a regenerated Gradle 9.6 wrapper
- current Android Studio recommended

Run verification:

```bash
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
```

## Branches

Use focused branch names such as:

```text
fix/focus-recovery
feat/target-editor
test/room-migrations
docs/analytics-model
```

## Commit messages

Prefer conventional, descriptive commits:

```text
fix: prevent pause intervals from entering daily focus totals
feat: add target revision editor
test: cover midnight focus segment attribution
docs: document backup restore invariants
```

## Pull requests

A PR should explain:

- problem / requirement
- implementation
- data-model or migration impact
- edge cases
- tests added/updated
- Android permission/background implications
- screenshots for user-facing UI changes

Do not merge a change that modifies time calculations, persistence, restore, DND or scheduling without appropriate tests.

## Testing priorities

High-risk paths:

- Start / Pause / Resume / Break / Stop
- sub-minute intervals
- midnight boundaries
- process death and reboot recovery
- task switching
- target feasibility and recovery allocation
- session edits and recalculation
- backup encryption / corruption / wrong passphrase
- restore transactions and migrations
- denied notification / DND / exact-alarm permissions

See [docs/TESTING.md](docs/TESTING.md).

## Code style

- Keep calculation engines deterministic where possible.
- Prefer explicit domain types over magic constants.
- Avoid hard-coded target/capacity/report values in ViewModels.
- Keep Android framework concerns outside pure calculation code.
- Document non-obvious invariants.
- Avoid adding cloud/network dependencies to V1 without an explicit product decision.
