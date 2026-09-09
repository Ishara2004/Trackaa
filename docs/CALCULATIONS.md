# Trackaa Calculation Rules

This document defines the mathematical invariants that code and tests should share.

## Authoritative time

Persist exact interval timestamps and seconds. Rounded display minutes are not authoritative.

```text
Verified Focus Seconds = Σ valid focus-segment seconds
Pause Seconds          = Σ valid pause-segment seconds
Break Seconds          = Σ valid break-segment seconds
```

A time interval must never contribute to verified focus more than once.

### Display rounding

Human display may use minutes/hours, but display rounding must never change stored totals.

A 37-second focus interval is **37 seconds**, not 1 minute.

## Calendar attribution

Split each verified focus segment at local-midnight boundaries.

Example:

```text
23:30 → 01:30
Day 1 = 30m
Day 2 = 90m
```

Never split the entire session wall interval if it contains pauses/breaks.

## Countdown overtime

```text
OvertimeSeconds = max(VerifiedFocusSeconds - PlannedFocusSeconds, 0)
```

Reaching zero does not automatically end a countdown session.

## Target progress

For positive target `T` and actual verified work `A`:

```text
ProgressPercent = A / T × 100
Remaining       = max(T - A, 0)
```

Progress can exceed 100%.

## Target tiers

Given Minimum `M`, Goal `G`, Stretch `S`:

```text
A >= S → STRETCH
A >= G → GOAL
A >= M → MINIMUM
else   → NONE
```

## Focus Debt / Credit

```text
Debt   = max(ExpectedToDate - Actual, 0)
Credit = max(Actual - ExpectedToDate, 0)
```

The UI should not show a negative debt.

## Capacity feasibility

For eligible future days with capacities `Ci`:

```text
RemainingCapacity = Σ Ci
Feasible = RemainingWork <= RemainingCapacity
Deficit = max(RemainingWork - RemainingCapacity, 0)
```

No planned allocation may exceed its day's capacity.

## Recovery plan

For each eligible day:

```text
Headroom_i = max(Capacity_i - ExistingPlan_i, 0)
```

Recovery allocation must satisfy:

```text
0 <= Recovery_i <= Headroom_i
Σ Recovery_i <= FocusDebt
```

If total headroom is less than debt, expose the unrecoverable deficit instead of silently overloading days.

## Rolling pace

A rolling pace includes eligible zero-output days.

```text
RollingPace = VerifiedFocusWithinWindow / EligibleDaysInWindow
```

The default product window should be explainable (for example 14 eligible days), with shorter/longer comparisons as secondary analytics.

## Projected completion

If rolling pace is positive:

```text
EligibleDaysRequired = ceil(RemainingWork / RollingPace)
```

Then walk future eligible working days to obtain the real projected calendar date. Do not simply add calendar days when some days are excluded.

## Required pace to deadline

```text
RequiredPerEligibleDay = ceil(RemainingWork / RemainingEligibleDays)
```

When per-day capacity varies, the capacity engine is more authoritative than a simple uniform average.

## Detailed progress

Default conceptual weighting:

```text
Time  50%
Tasks 30%
Topics 20%
```

If a dimension is unavailable, re-normalize the remaining dimensions transparently.

For academic aggregation with credits:

```text
Aggregate = Σ(ModuleProgress_i × Credits_i) / Σ Credits_i
```

## Momentum score

Momentum is a product indicator, not a clinical/scientific psychological measure.

Current components:

```text
Target adherence
Consistency
Recent trend
```

Every component must be bounded, explainable and based on actual configured eligibility/targets rather than magic constants.

## Editing invariants

Historical edits must eventually obey:

- no negative duration
- no overlapping verified intervals
- original value retained in audit history
- affected task/session aggregates recalculated
- affected target/forecast/analytics views recomputed
- XP/achievements must not duplicate because of an edit
