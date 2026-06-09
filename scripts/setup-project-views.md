# Project v2 View Setup (Manual)

GitHub's GraphQL API for ProjectV2 does **not** expose view creation
or modification as of June 2026. Fields, custom field options, items,
and item-field values are scriptable; views are not. The
`createProjectV2View` and `updateProjectV2View` mutations do not exist
in the public schema.

Per ADR-011, this project uses four views. They must be created via
the web UI at:

> https://github.com/users/xmljim/projects/4

If the project is ever rebuilt (e.g. cloned to a new owner), follow
the steps below in order. This file documents the canonical view
configuration so the setup is repeatable.

## Prerequisites

- Project v2 created with the 7 custom fields per ADR-011 (this is
  scripted; see the field-creation block in repo history).
- All issues imported via `scripts/import-stories.py`.

## View 1 — All Open (default view, rename)

Replaces the auto-generated "View 1".

1. Click the dropdown caret next to **View 1** in the tab bar
2. Click **Rename view** → type `All Open`
3. Click the filter bar (top of the view) and add: `is:open`
4. Press Enter to apply
5. Click the floppy-disk save icon next to the view name to persist

**Purpose**: flat table; useful for ad-hoc filtering/sorting/searching.

## View 2 — By Status (kanban board)

1. Click the **+** at the right of the view tab strip → **New view**
2. Name: `By Status`
3. Layout: **Board**
4. Click the layout's "Group by" dropdown → **Status**
5. Confirm the columns appear: Backlog, Ready, In Progress, In Review, Done
6. Save

**Purpose**: daily working view. Drag cards as work progresses.

## View 3 — Current Wave (filtered table)

1. **+** → **New view**
2. Name: `Current Wave`
3. Layout: **Table**
4. Filter: `milestone:"Wave-1: Engine Core (v0.1.0)"`
5. Group by: **Epic**
6. Sort: **Priority** ascending, then **Points** descending
7. Save

**Purpose**: focus on the current ship. Update the milestone filter
as waves progress (Wave-1 → Wave-2 at v0.1.0 release, etc.).

## View 4 — By Epic (grouped table)

1. **+** → **New view**
2. Name: `By Epic`
3. Layout: **Table**
4. Group by: **Epic**
5. Sort: **Wave** ascending, then **Priority** ascending
6. Save

**Purpose**: backlog visibility per epic. Useful for spotting
gaps within an epic or tracking epic completion.

## Recommended visible columns (all table views)

- Title
- Status
- Points
- Wave
- Priority
- Labels
- Milestone
- ADRs

Hide: Reviewers, Linked pull requests (until PR phase), Created,
Updated, Closed, Parent issue, Sub-issues progress.

## Optional: Status field column colors

The Status field's options were configured at field creation:

- Backlog: GRAY
- Ready: BLUE
- In Progress: YELLOW
- In Review: PURPLE
- Done: GREEN

If colors don't appear correctly on the kanban board, edit the Status
field in **Project settings → Fields → Status** and reapply colors.

## Verification

After all four views exist:

1. **By Status** — confirm board shows columns Backlog (70 cards) /
   Ready (0) / In Progress (0) / In Review (0) / Done (0)
2. **Current Wave** — confirm 29 cards visible (Wave-1) grouped under
   EPIC-1 (8), EPIC-2 (10), EPIC-3 (11)
3. **By Epic** — confirm 8 epic groups, story counts match
   `import-stories.py --dry-run` output
4. **All Open** — confirm 70 cards (one per imported story)

## Why this is manual

GitHub's roadmap acknowledges Project v2 view-management API as a
known gap. Tracking issues exist on the GitHub Community Discussions.
This ADR-011 will be revised when the API becomes available; until
then, this document is the substitute.
