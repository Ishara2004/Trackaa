# Trackaa Testing Strategy

Trackaa's highest-risk failures are silent data errors, so calculation and persistence tests take precedence over screenshot-only coverage.

## Automated CI gates

Every production-readiness PR should pass:

```text
testDebugUnitTest
lintDebug
assembleDebug
```

The workflow also uploads a debug APK after a successful build.

## Calculation tests

### Duration

Cover:

- sub-minute focus
- exact minute boundaries
- multi-hour sessions
- midnight split
- month/year boundaries
- timezone-sensitive local dates
- invalid/reversed intervals

### Capacity

Cover:

- sufficient capacity
- exact capacity
- insufficient capacity
- unavailable days
- unequal daily capacity
- allocation rounding remainder
- invariant: allocation <= daily capacity

### Recovery

Cover:

- no debt
- recoverable debt
- partially recoverable debt
- zero headroom
- existing planned usage

### Forecast

Cover:

- insufficient history
- zero pace
- eligible zero-output days
- ceiling semantics
- excluded weekdays
- ahead/on-time/late deadline states

### Progress / Momentum

Cover missing dimensions, custom weights, >100% target progress, zero denominators and bounded score outputs.

## Focus state-machine tests

Required scenarios:

```text
IDLE → FOCUSING → REVIEW_PENDING → IDLE
IDLE → FOCUSING → PAUSED → FOCUSING → REVIEW_PENDING
IDLE → FOCUSING → ON_BREAK → FOCUSING
Pomodoro FOCUS → ON_BREAK → BREAK_COMPLETE → FOCUSING
```

Verify:

- exact focus seconds
- pause/break exclusion
- task switching closes the old focus segment
- Stop freezes the end timestamp
- review duration is excluded
- mandatory ratings reject invalid values
- no duplicate session save

## Recovery tests

Simulate a persisted checkpoint during:

- focus
- pause
- break
- review pending

On recovery, unknown time after the checkpoint must never become verified focus automatically.

## Room tests

Before stable release add migration tests for every published schema version.

Test:

- schema migration
- target revisions
- soft delete / restore / purge
- overlapping-session validation
- relationship integrity
- transaction rollback

## Backup / restore tests

Test:

- portable crypto round-trip
- wrong passphrase
- corrupt ciphertext
- incomplete snapshot
- unsupported version
- every database table included
- pre-restore safety snapshot
- transaction rollback on insert failure
- automatic backup rotation
- Android Keystore same-device decrypt

## Android behavior tests

Representative devices/OS versions should verify:

- notification permission denied/granted
- DND access denied/granted/revoked
- active session with screen locked
- process removal/recreation
- device reboot
- reminder after reboot
- exact-alarm access unavailable
- foreground service controls
- OEM battery restrictions

## UI acceptance flows

1. Create Goal → Work Item → Topic → Task.
2. Start Quick Focus.
3. Pause/resume and verify excluded pause.
4. Switch tasks mid-session.
5. Log interruption.
6. Complete mandatory review.
7. Verify Home and Analytics totals.
8. Create/edit targets and capacities once UI is implemented.
9. Generate report and compare every metric with on-screen source data.
10. Backup → modify workspace → restore → compare all tables.

## Release rule

A build must not be presented as production-ready only because it opens successfully. CI, data-integrity tests and real-device behavior must all be reviewed before a stable tag.
