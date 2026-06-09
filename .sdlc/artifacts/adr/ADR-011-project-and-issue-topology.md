# ADR-011: Project Board, Milestone, and Issue Topology

- **Status**: Accepted
- **Date**: 2026-06-09
- **Deciders**: Owner
- **Related**: ADR-009 (quality gates), ADR-010 (TBD branching), PRD-001 §8 (milestones), all EPIC-* docs

## Context

The repository now has a planning-complete backlog: 8 epics decomposed into
~70 user stories totaling ~250 points. Implementation begins with EPIC-1.
Before opening the first story, the question is how that backlog projects
into GitHub's data model so it's navigable for solo work and scales if
contributors join.

GitHub's data model has fixed shape:
- **Issue** belongs to ≤ 1 **Milestone**
- **Issue** can have many **Labels**
- **Issue** can be added to many **Projects** (v2)
- **Project v2** can have arbitrary custom fields per issue
- **Milestones** don't nest

So whatever we map to "milestone" is the *primary* GitHub-UI grouping;
every other dimension lives as labels and project fields.

## Decision

### Hybrid model

- **Milestone = Wave (release-aligned bundle of epics)**
- **Label `epic:N` = Epic** (1..8)
- **Issue = Story** (one per S-N.M from the epic decomposition)
- **GitHub Project v2** holds cross-cutting fields and views

### Wave → Epic → Tag mapping

| Wave | Tag at end | Epics | Capability shipped |
|---|---|---|---|
| **Wave-1** | v0.1.0 | EPIC-1, EPIC-2, EPIC-3 | Deterministic end-to-end projection across accumulation, bridge, and drawdown phases. Tax engine, RMDs, conversions wired. Reproduces Sheet2 within 1%. No buckets, no MC, no scenarios. |
| **Wave-2** | v0.2.0 | EPIC-4, EPIC-5 | Goal-based buckets with adaptive spending policies. Monte Carlo with glide-path and historical bootstrap. Bucket-sacrifice statistics in output. |
| **Wave-3** | v0.3.0 | EPIC-6, EPIC-7 | Scenario management (save, clone, compare). API contract solid; frontend repo can integrate. |
| **Wave-4** | v1.0.0 | EPIC-8 | Hardening, observability, additional state tax tables, accessibility, release readiness. |

Each wave end produces a tag on `main` per ADR-010 release flow. A wave
is "done" when all milestones it contains are closed AND the tag is cut.

Note: PRD-001 §8 originally called the bundles M1..M8 ("milestones" in
plain-English usage). They remain the *engineering milestones* in the
PRD — but in GitHub-speak they're now epics, and the GitHub Milestone
field maps to the Wave grouping.

### Project v2 fields

| Field | Type | Values |
|---|---|---|
| Title | (issue title) | imperative form, e.g. "Implement Money value type" |
| Status | single-select | Backlog, Ready, In Progress, In Review, Done |
| Points | number | Fibonacci: 1, 2, 3, 5 (≥8 must be split) |
| Epic | single-select | EPIC-1..EPIC-8 (mirrors `epic:N` label) |
| Wave | single-select | Wave-1..Wave-4 (mirrors milestone) |
| Priority | single-select | P0, P1, P2 |
| ADRs | text | comma-separated, e.g. "ADR-002, ADR-007" |
| Blocked by | text | issue link, e.g. "#42, #51" |

The `Epic` field and `epic:N` label are deliberately redundant — the
label drives GitHub-native filtering (URL parameters, search), the
field drives Project board grouping.

### Project v2 views

1. **Board "By Status"** — kanban (Backlog / Ready / In Progress / In Review / Done) — daily working view
2. **Table "Current Wave"** — filtered by milestone; grouped by Epic; sorted by Priority then Points
3. **Table "By Epic"** — grouped by Epic field — backlog visibility per epic
4. **Table "All Open"** — flat table for ad-hoc sorting/filtering

**Manual setup required**: GitHub's GraphQL API does not expose view
creation as of this writing — `createProjectV2View` and
`updateProjectV2View` mutations do not exist. Fields, custom options,
and items can be managed programmatically; views cannot. The four views
above must be created via the web UI at the project URL. Configuration
steps are documented in `scripts/setup-project-views.md` for repeatable
setup if the project is ever rebuilt.

### Status automation

Status transitions on the project board are driven by GitHub Actions:

- **Backlog** — issue created, no branch
- **Ready** — issue manually marked Ready (small set per wave at any time)
- **In Progress** — feature branch pushed referencing the issue, OR issue assigned + manually moved
- **In Review** — PR opened with `Closes #N` in body
- **Done** — PR merged

If automation proves flaky, manual status updates remain valid.

### Issue content (mandatory blocks)

Every story issue has these blocks. The issue template enforces them.

1. **Story** — As-a / I-want / So-that
2. **Traceability**
   - Epic: link to `.sdlc/artifacts/epics/EPIC-N-*.md` and the specific S-N.M anchor
   - PRD: FR / NFR numbers (`FR-2.5`, `NFR-1`) with brief context
   - ADRs: which ADRs govern this story (link to file, optionally a section)
3. **Acceptance Criteria** — checkbox list, each item testable. Must be
   comprehensive: the story isn't done until every box is checked.
4. **Definition of Done** — checkbox reference to the universal DoD in
   `CONTRIBUTING.md`. Universal items (CI green, quality gates, docs
   updated) live there, not in every issue.
5. **Points** — Fibonacci (1, 2, 3, 5). 8+ must be split.
6. **Suppression notes** — if the story will require a justified
   PMD/Checkstyle/SpotBugs suppression to ship per ADR-009 §"Suppressions
   Policy", capture *why* here before the PR is opened. Empty if no
   suppressions expected.
7. **Notes / Out of scope** — anything that's specifically NOT this story
   to prevent scope drift in PR review.

### Universal Definition of Done

Captured in `CONTRIBUTING.md` and referenced from each issue. A story
cannot be closed until **all** of these are true:

- All Acceptance Criteria checkboxes met
- `./mvnw verify` passes locally
- CI green on the feature branch (all 9 status checks per ADR-009)
- Coverage thresholds met for the affected module (engine 85/75,
  domain 75/65, infra 75/65, total 70/60 per ADR-009)
- `/quality fix`, `/java-review`, `/retirement-style` all clean
- `/test` runs and passes
- Any suppression added has a justified comment per ADR-009 policy
- Architecture rules pass (`ProjectArchitectureTest`, Modulith verify)
- New public API documented in Javadoc; new endpoints in OpenAPI spec
- Relevant ADR / PRD references added to the PR description
- No new TODOs without an issue link

### Bulk import vs lazy creation

Stories are imported in bulk **once** from the existing epic markdown
files. After bulk import, edits go to GitHub directly (the markdown is
no longer authoritative for story content — issues are). The bulk-import
script lives at `scripts/import-stories.py` and is documented as a
one-time-bootstrap utility, not a recurring sync job.

### Labels

In addition to `epic:N`:

- **Type**: `type:story`, `type:bug`, `type:chore`, `type:tech-debt`,
  `type:docs`
- **Priority**: `priority:p0`, `priority:p1`, `priority:p2`
- **Area**: `area:contribution`, `area:tax`, `area:bucket`,
  `area:simulation`, `area:scenario`, `area:api`, `area:frontend`,
  `area:infra`
- **Status helpers**: `blocked`, `needs-discussion`, `good-first-issue`

Labels are flat — no hierarchy. The Project board fields handle
grouping.

## Rationale

- **Milestone=Wave** matches how releases will actually happen per
  ADR-010. A closing milestone = a tag cut.
- **Label=Epic** preserves epic-level filtering in the GitHub UI while
  freeing the milestone slot for the higher-altitude grouping.
- **Bulk import** preserves the planning investment. Lazy creation
  loses the strategic visibility the PRD + epic decomposition built up.
- **Comprehensive AC + universal DoD** matches the owner's quality bar
  per ADR-009 and prevents scope creep in PR review.
- **Suppressions surfaced at issue time** keeps suppressions intentional
  rather than emergent.

## Consequences

**Positive**
- One source of truth per dimension: milestone for waves, label for
  epics, issue for stories
- Releases tag cleanly at wave boundaries
- Project board shows multiple meaningful views without recreating data
- Issue template forces traceability — every story can be traced to PRD
  and ADRs

**Negative**
- Setup cost: 4 milestones, 8+ labels, 1 Project v2, ~70 issues. ~1
  hour of work via the bulk-import script.
- Two places to update Epic (label + Project field) — automation closes
  the gap; if it lags, prefer the label as authoritative.
- The epic-decomposition markdown becomes a historical record after
  bulk import, not a living source. New stories are added directly as
  issues. Mitigation: the markdown stays in `.sdlc/artifacts/epics/` as
  a snapshot of the v0 plan; we don't pretend it's living docs.
- Stories within a milestone can't be sub-grouped natively; the Project
  board's "group by Epic" view handles this.

## Alternatives Considered

- **Milestone=Epic** (the owner's default heuristic) — rejected; loses
  the wave/release alignment, and tag cuts have no natural milestone
  trigger. Useful if releases were planned per-epic, but they aren't
  here.
- **Skip milestones, labels only** — rejected; loses the GitHub-UI
  release-scope visibility that milestones provide for free.
- **Sub-issues / parent-child in Issues** — rejected; experimental
  GitHub feature, model still evolving, and adds a hierarchy where
  flat labels work fine for this scale.
- **Lazy issue creation (per-epic or per-story)** — rejected; loses
  end-to-end backlog visibility that justifies the planning effort.
- **Per-wave separate Project boards** — rejected; switching boards
  is friction with no payoff beyond what fields/views already provide.

## Notes

- The Project board belongs to the **`xmljim` user** (not an org).
  GitHub user-level Project v2 is supported.
- A future ADR may revisit if/when the project goes multi-team — at
  that point an org-level Project board with shared field templates
  makes sense.
- The `/issue` skill from PlaceFinder uses this same pattern; we'll
  customize the skill or fall back to direct `gh issue create` calls
  with the template populated by our import script.
- A small GitHub Action handles status automation — if it proves
  unreliable, manual status updates work fine for solo dev.
