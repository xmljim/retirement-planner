---
name: User story
about: Implementation work derived from an epic
title: 'EPIC-N / S-N.M: <imperative title>'
labels: ['type:story']
---

<!--
Per ADR-011, every story issue has these blocks. Don't delete them; if
a section is genuinely N/A, write "N/A" rather than removing it.
-->

## Story

**As a** ...
**I want** ...
**So that** ...

## Traceability

- **Epic**: [EPIC-N / S-N.M](.sdlc/artifacts/epics/EPIC-N-name.md#s-nm-title)
- **PRD**: FR-X.Y, NFR-Z (briefly state what they require)
- **ADRs**:
  - ADR-NNN — [name](.sdlc/artifacts/adr/ADR-NNN-name.md) — why this story touches it

## Acceptance Criteria

<!--
Comprehensive, testable, checkbox list. The story is NOT done until
every box is checked. Each AC should map to at least one test or a
specific observable behavior.
-->

- [ ]
- [ ]
- [ ]

## Definition of Done

This story also satisfies the universal Definition of Done in
[`CONTRIBUTING.md`](../CONTRIBUTING.md#universal-definition-of-done).
Confirm before closing:

- [ ] All Acceptance Criteria above are checked
- [ ] `./mvnw verify` passes locally
- [ ] CI green on the feature branch (all 9 status checks)
- [ ] Coverage thresholds met for the affected module
- [ ] `/quality fix`, `/java-review`, `/retirement-style` all clean
- [ ] No new suppressions OR each new suppression has a justified comment
- [ ] Public API has Javadoc; new endpoints in OpenAPI spec
- [ ] Relevant ADR / PRD references in the PR description

## Points

<!--
Fibonacci: 1, 2, 3, 5. Anything 8+ MUST be split. State the number
and one-line rationale.
-->

**Points**: _

## Suppression Notes

<!--
If you anticipate the story will require a justified PMD / Checkstyle /
SpotBugs / ArchUnit suppression to ship per ADR-009 §"Suppressions
Policy", capture WHY here before opening the PR. Empty if no
suppressions expected.
-->

_None expected._

## Notes / Out of Scope

<!--
Anything specifically NOT included in this story, to prevent scope
drift in PR review. Reference follow-up issues if applicable.
-->

