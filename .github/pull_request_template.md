# Pull Request

<!--
Per ADR-010 (TBD): one PR per story; squash-merge to main; tag releases.
Per ADR-011: every PR closes an issue. Per ADR-009: every gate must be green.
-->

## Summary

<!-- 1-3 sentences. What changed and why. -->

## Story

Closes #<NN>

<!-- The PR closes the linked story. Issue's Acceptance Criteria must
all be checked before this PR is merged. -->

## Traceability

- **Epic**: EPIC-N / S-N.M
- **PRD**: FR-X.Y, NFR-Z
- **ADR(s)**: ADR-NNN

## Changes

<!-- Bullet the user-visible behavior changes and the structural changes. -->

## Test Plan

- [ ] Unit tests added/updated
- [ ] Integration tests added/updated (if applicable)
- [ ] Coverage thresholds met for the affected module
- [ ] `./mvnw verify` passes locally
- [ ] Manual verification (describe, if applicable)

## Quality Gates (per ADR-009)

- [ ] Spotless clean
- [ ] Checkstyle clean
- [ ] PMD clean
- [ ] SpotBugs clean
- [ ] ArchUnit rules pass
- [ ] Modulith verification passes
- [ ] JaCoCo coverage thresholds met
- [ ] No new suppressions OR each new suppression has a justification
      comment AND is named below

## New Suppressions (if any)

<!--
For each new @SuppressWarnings or filter-file entry, list:
  - File and rule suppressed
  - Why (one sentence — false positive? trade-off?)
Empty if no suppressions added.
-->

_None._

## Definition of Done

This PR satisfies the universal DoD in
[`CONTRIBUTING.md`](../CONTRIBUTING.md#universal-definition-of-done).

## Notes for Reviewer

<!-- Anything subtle, judgment-calls, or things to focus on. -->
