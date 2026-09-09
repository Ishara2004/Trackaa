# Trackaa Roadmap

Trackaa is being hardened in correctness-first order. Status below is deliberately conservative.

## P0 — Data truth and reliability

- [x] Exact-second focus persistence
- [x] Remove minimum-one-minute inflation
- [x] Freeze Stop timestamp before review
- [x] Verified-segment analytics source of truth
- [x] Recovery-safe checkpoint behavior
- [x] Pomodoro break-complete state
- [x] Capacity-capped recovery allocation
- [x] Eligible-working-day forecast math
- [x] Real foreground-service notification path
- [x] DND state persistence and notification-action consistency
- [x] Full-workspace encrypted backup snapshot
- [x] Transactional restore + safety snapshot
- [x] Automatic Keystore-backed rotating backup
- [x] Reboot restoration for scheduled focus reminders
- [ ] Green Android CI on production-readiness branch
- [ ] Real-device matrix for Android 13–16 and representative OEMs

## P1 — Core product completeness

- [ ] First-run onboarding and contextual permission education
- [ ] Full target CRUD for Daily / Weekly / Monthly / Deadline targets
- [ ] Minimum / Goal / Stretch target editor
- [ ] Goal / Work-Item scoped target editor
- [ ] Target revision-history UI
- [ ] Working-day / daily-capacity editor
- [ ] Scheduled Focus CRUD UI and exact-alarm settings UX
- [ ] Topic creation/editing and task-topic assignment UI
- [ ] Task dependency editor and blocked-state explanation
- [ ] Work-item deadline/exam date, icon and color editing
- [ ] User-defined Work Item Types
- [ ] User-defined interruption reasons
- [ ] Streak threshold and progress-weight settings
- [ ] Quiet hours and report/reminder settings
- [ ] Session history editor with original snapshot + audit event
- [ ] Derived-data recalculation after historical edits/deletes/restores

## P2 — Analytics depth

- [ ] Semester reporting period view
- [ ] Year view
- [ ] All-Time view
- [ ] 365-day contribution heatmap
- [ ] Calendar explorer
- [ ] 24-hour verified-focus timeline
- [ ] Legacy AM/PM interval log inspired by the original handwritten workflow
- [ ] exact-hour productivity analysis
- [ ] day-of-week analysis
- [ ] planned-vs-actual trends
- [ ] burn-down chart
- [ ] personal records
- [ ] interruption pattern analysis
- [ ] task/module drill-down from every aggregate

## P3 — Reports, backup UX and gamification

- [ ] Daily / Weekly / Monthly / Goal / Module PDF report choices
- [ ] multi-page PDF session log
- [ ] report charts and recovery/forecast sections
- [ ] Storage Access Framework save/open flow for portable backups
- [ ] automatic-backup browser and restore UI
- [ ] all seeded achievements evaluated deterministically
- [ ] target/streak XP rules protected against duplicate reward events
- [ ] isolated Demo Workspace that cannot contaminate real analytics

## P4 — Stable public release

- [ ] final package/namespace cleanup
- [ ] accessibility audit
- [ ] battery/background behavior audit
- [ ] migration tests from every released DB schema
- [ ] backup restore compatibility tests
- [ ] release signing in CI/release process
- [ ] Play Store privacy/Data Safety review
- [ ] screenshots, feature graphic and store copy
- [ ] tagged release + signed AAB

## Future candidates

These are not V1 commitments:

- optional multi-device/cloud sync
- iOS client
- cross-platform architecture
- optional AI coach grounded strictly in Trackaa data
- social/leaderboard features
