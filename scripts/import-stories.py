#!/usr/bin/env python3
"""
Bulk-import stories from .sdlc/artifacts/epics/EPIC-N-*.md into GitHub Issues.

Idempotent: re-running skips issues whose title already exists. Adds them
to the GitHub Project v2 and sets all custom fields (Status, Points, Epic,
Wave, Priority, ADRs).

Usage:
    scripts/import-stories.py --dry-run        # preview only
    scripts/import-stories.py                  # create issues
    scripts/import-stories.py --epic 1         # only EPIC-1
    scripts/import-stories.py --story S-1.3    # only that story

Per ADR-011. Run once at project bootstrap; not a recurring sync job.
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path

REPO = "xmljim/retirement-planner"
PROJECT_NUMBER = 4
PROJECT_OWNER = "xmljim"

# IDs captured from `gh project field-list` (see ADR-011 setup commit).
PROJECT_ID = "PVT_kwHOAKhals4BaKTo"
FIELD_STATUS = "PVTSSF_lAHOAKhals4BaKTozhVD2BY"
FIELD_POINTS = "PVTF_lAHOAKhals4BaKTozhVD2RM"
FIELD_EPIC = "PVTSSF_lAHOAKhals4BaKTozhVD2RQ"
FIELD_WAVE = "PVTSSF_lAHOAKhals4BaKTozhVD2SI"
FIELD_PRIORITY = "PVTSSF_lAHOAKhals4BaKTozhVD2SM"
FIELD_ADRS = "PVTF_lAHOAKhals4BaKTozhVD2SQ"

STATUS_BACKLOG = "54af224d"
STATUS_READY = "f7e14dfe"

EPIC_OPTIONS = {
    1: "29a7be04", 2: "2fa6bdab", 3: "f5c94bbe", 4: "f76883fe",
    5: "528665d7", 6: "f8ff362d", 7: "326789e8", 8: "d45fba12",
}

WAVE_OPTIONS = {
    1: "1689fab7", 2: "68bb7d45", 3: "c27435e0", 4: "97e16175",
}

PRIORITY_OPTIONS = {"P0": "d7681d5f", "P1": "54c40002", "P2": "a35a44d9"}

# Wave assignment per ADR-011.
EPIC_TO_WAVE = {1: 1, 2: 1, 3: 1, 4: 2, 5: 2, 6: 3, 7: 3, 8: 4}

# Wave milestones (created via gh api before this script runs).
# `gh issue create --milestone` takes the title, not the number.
WAVE_TO_MILESTONE_TITLE = {
    1: "Wave-1: Engine Core (v0.1.0)",
    2: "Wave-2: Goals & Uncertainty (v0.2.0)",
    3: "Wave-3: Scenarios & Surface (v0.3.0)",
    4: "Wave-4: Hardening & Release (v1.0.0)",
}

# Epic → primary area label.
EPIC_TO_AREA = {
    1: "area:infra",
    2: "area:contribution",
    3: "area:tax",
    4: "area:bucket",
    5: "area:simulation",
    6: "area:scenario",
    7: "area:api",
    8: "area:infra",
}

# Stories that are foundational / blocking get P0; everything else P1
# until prioritized differently. The user can re-prioritize after import.
P0_STORIES = {"S-1.1", "S-1.2", "S-1.3"}


@dataclass
class Story:
    epic_number: int
    epic_file: str
    code: str           # e.g. "S-1.3"
    title: str          # e.g. "Implement Money value type"
    narrative: str      # the As-a/I-want/So-that paragraph
    acceptance: list[str] = field(default_factory=list)
    points: int | None = None
    traces_to: list[str] = field(default_factory=list)

    @property
    def issue_title(self) -> str:
        return f"EPIC-{self.epic_number} / {self.code}: {self.title}"

    @property
    def priority(self) -> str:
        return "P0" if self.code in P0_STORIES else "P1"

    @property
    def adrs(self) -> list[str]:
        return [t for t in self.traces_to if t.startswith("ADR-")]

    @property
    def frs_nfrs(self) -> list[str]:
        return [t for t in self.traces_to if t.startswith(("FR-", "NFR-"))]


def run(*args: str, capture: bool = True, check: bool = True) -> str:
    """Run a shell command, return stdout. Raises on non-zero unless check=False."""
    result = subprocess.run(
        list(args),
        capture_output=capture,
        text=True,
        check=False,
    )
    if check and result.returncode != 0:
        sys.stderr.write(f"FAIL: {' '.join(args)}\nstderr: {result.stderr}\n")
        raise RuntimeError(result.stderr or "command failed")
    return result.stdout


def parse_epic(path: Path) -> list[Story]:
    text = path.read_text(encoding="utf-8")
    epic_match = re.search(r"# EPIC-(\d+)", text)
    if not epic_match:
        raise ValueError(f"No EPIC heading in {path}")
    epic_num = int(epic_match.group(1))

    stories: list[Story] = []

    # Split on story headings.
    parts = re.split(r"\n### (S-\d+\.\d+) — (.+?)\n", text)
    # parts = [pre, code1, title1, body1, code2, title2, body2, ...]
    for i in range(1, len(parts), 3):
        code = parts[i].strip()
        title = parts[i + 1].strip()
        body = parts[i + 2]

        # Narrative: first **As a** ... line block.
        narrative_match = re.search(
            r"\*\*As an?\*\*\s*(.+?)\*\*so that\*\*\s*(.+?)\.",
            body,
            re.IGNORECASE | re.DOTALL,
        )
        if narrative_match:
            who_what = re.search(
                r"\*\*As an?\*\*\s*(.+?)\*\*I want\*\*\s*(.+?)\*\*so that\*\*\s*(.+?)\.",
                body,
                re.IGNORECASE | re.DOTALL,
            )
            if who_what:
                actor = who_what.group(1).strip().rstrip("*").strip()
                want = who_what.group(2).strip().rstrip("*").strip()
                outcome = who_what.group(3).strip()
                narrative = f"**As a** {actor}\n**I want** {want}\n**So that** {outcome}."
            else:
                narrative = ""
        else:
            narrative = ""

        # Acceptance criteria bullets.
        ac_match = re.search(
            r"\*\*Acceptance criteria\*\*\n((?:[\-*] .+\n)+)",
            body,
        )
        ac: list[str] = []
        if ac_match:
            for line in ac_match.group(1).splitlines():
                line = line.strip()
                if line.startswith(("-", "*")):
                    ac.append(line.lstrip("-* ").strip())

        # Points.
        points_match = re.search(r"\*\*Points\*\*:\s*(\d+)", body)
        points = int(points_match.group(1)) if points_match else None

        # Traces to.
        traces: list[str] = []
        traces_match = re.search(r"\*\*Traces to\*\*:\s*(.+)", body)
        if traces_match:
            traces = [t.strip() for t in traces_match.group(1).split(",") if t.strip()]

        stories.append(
            Story(
                epic_number=epic_num,
                epic_file=path.name,
                code=code,
                title=title,
                narrative=narrative,
                acceptance=ac,
                points=points,
                traces_to=traces,
            )
        )

    return stories


def load_all_stories(repo_root: Path) -> list[Story]:
    epics_dir = repo_root / ".sdlc" / "artifacts" / "epics"
    files = sorted(epics_dir.glob("EPIC-*.md"))
    stories: list[Story] = []
    for f in files:
        if "EPIC-" not in f.name or f.name == "README.md":
            continue
        stories.extend(parse_epic(f))
    return stories


def existing_issue_titles() -> set[str]:
    """Fetch all existing issue titles in the repo to support idempotent import."""
    out = run(
        "gh", "issue", "list",
        "--repo", REPO,
        "--state", "all",
        "--limit", "500",
        "--json", "title",
    )
    return {item["title"] for item in json.loads(out)}


def render_issue_body(story: Story) -> str:
    epic_anchor = f"s-{story.code.lower().replace('s-', '')}"  # "S-1.3" → "s-1.3"
    epic_anchor = epic_anchor.replace(".", "")  # GitHub anchors strip dots

    adr_lines = []
    for adr in story.adrs:
        # Look up the ADR file by number
        adr_lines.append(f"  - {adr} — see [`.sdlc/artifacts/adr/{adr}-*.md`](../tree/main/.sdlc/artifacts/adr)")
    if not adr_lines:
        adr_lines = ["  - _none referenced in the epic decomposition_"]

    fr_nfr_str = ", ".join(story.frs_nfrs) if story.frs_nfrs else "_none cited in epic decomposition_"

    ac_lines = "\n".join(f"- [ ] {item}" for item in story.acceptance) if story.acceptance else "- [ ] _to be defined_"

    return f"""<!-- Imported from .sdlc/artifacts/epics/{story.epic_file} -->

## Story

{story.narrative or '_(narrative not parsed; see epic markdown)_'}

## Traceability

- **Epic**: [EPIC-{story.epic_number} / {story.code}](../tree/main/.sdlc/artifacts/epics/{story.epic_file})
- **PRD**: {fr_nfr_str}
- **ADRs**:
{chr(10).join(adr_lines)}

## Acceptance Criteria

{ac_lines}

## Definition of Done

This story also satisfies the universal Definition of Done in [`CONTRIBUTING.md`](../tree/main/CONTRIBUTING.md#universal-definition-of-done). Confirm before closing:

- [ ] All Acceptance Criteria above are checked
- [ ] `./mvnw verify` passes locally
- [ ] CI green on the feature branch (all 9 status checks)
- [ ] Coverage thresholds met for the affected module
- [ ] `/quality fix`, `/java-review`, `/retirement-style` all clean
- [ ] No new suppressions OR each new suppression has a justified comment
- [ ] Public API has Javadoc; new endpoints in OpenAPI spec
- [ ] Relevant ADR / PRD references in the PR description

## Points

**Points**: {story.points if story.points is not None else '_(not specified in epic)_'}

## Suppression Notes

_None expected._

## Notes / Out of Scope

_None._
"""


def create_issue(story: Story, milestone_title: str, area_label: str) -> int:
    body = render_issue_body(story)
    body_path = Path(f"/tmp/issue-body-{story.code.replace('.', '-')}.md")
    body_path.write_text(body, encoding="utf-8")

    labels = [
        f"epic:{story.epic_number}",
        "type:story",
        f"priority:{story.priority.lower()}",
        area_label,
    ]
    label_args: list[str] = []
    for label in labels:
        label_args += ["--label", label]

    out = run(
        "gh", "issue", "create",
        "--repo", REPO,
        "--title", story.issue_title,
        "--body-file", str(body_path),
        "--milestone", milestone_title,
        *label_args,
    )
    # gh outputs the URL; extract the number.
    match = re.search(r"/issues/(\d+)", out)
    if not match:
        raise RuntimeError(f"Couldn't parse issue number from: {out}")
    return int(match.group(1))


def add_to_project(issue_number: int) -> str:
    """Add issue to project; return the project item ID."""
    issue_url = f"https://github.com/{REPO}/issues/{issue_number}"
    out = run(
        "gh", "project", "item-add", str(PROJECT_NUMBER),
        "--owner", PROJECT_OWNER,
        "--url", issue_url,
        "--format", "json",
    )
    return json.loads(out)["id"]


def set_project_field(item_id: str, field_id: str, *, value: dict | None = None) -> None:
    """Set a project field. value dict like {'singleSelectOptionId': '...'} or {'text': '...'} or {'number': N}."""
    if value is None:
        return
    args = [
        "gh", "project", "item-edit",
        "--id", item_id,
        "--project-id", PROJECT_ID,
        "--field-id", field_id,
    ]
    if "singleSelectOptionId" in value:
        args += ["--single-select-option-id", value["singleSelectOptionId"]]
    elif "text" in value:
        args += ["--text", value["text"]]
    elif "number" in value:
        args += ["--number", str(value["number"])]
    else:
        raise ValueError(f"Unknown value shape: {value}")
    run(*args)


def populate_fields(item_id: str, story: Story) -> None:
    set_project_field(item_id, FIELD_STATUS, value={"singleSelectOptionId": STATUS_BACKLOG})
    set_project_field(item_id, FIELD_EPIC, value={"singleSelectOptionId": EPIC_OPTIONS[story.epic_number]})
    set_project_field(item_id, FIELD_WAVE, value={"singleSelectOptionId": WAVE_OPTIONS[EPIC_TO_WAVE[story.epic_number]]})
    set_project_field(item_id, FIELD_PRIORITY, value={"singleSelectOptionId": PRIORITY_OPTIONS[story.priority]})
    if story.points is not None:
        set_project_field(item_id, FIELD_POINTS, value={"number": story.points})
    if story.adrs:
        set_project_field(item_id, FIELD_ADRS, value={"text": ", ".join(story.adrs)})


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true", help="Print plan without creating issues")
    parser.add_argument("--epic", type=int, help="Only process this epic number")
    parser.add_argument("--story", help="Only process this story code (e.g. S-1.3)")
    args = parser.parse_args()

    repo_root = Path(__file__).resolve().parent.parent
    stories = load_all_stories(repo_root)

    if args.epic:
        stories = [s for s in stories if s.epic_number == args.epic]
    if args.story:
        stories = [s for s in stories if s.code == args.story]

    if not stories:
        print("No stories matched the filter.")
        return 1

    print(f"Found {len(stories)} stories from {len({s.epic_number for s in stories})} epics.")

    if args.dry_run:
        for s in stories:
            wave = EPIC_TO_WAVE[s.epic_number]
            print(f"  {s.issue_title}")
            print(f"    epic={s.epic_number}  wave={wave}  priority={s.priority}  points={s.points}")
            print(f"    AC items: {len(s.acceptance)}  ADRs: {s.adrs or '(none)'}")
        return 0

    existing = existing_issue_titles()
    print(f"Found {len(existing)} existing issues; will skip duplicates by title.")

    created = 0
    skipped = 0
    failed = 0

    for s in stories:
        if s.issue_title in existing:
            print(f"  [skip] {s.issue_title}")
            skipped += 1
            continue

        wave = EPIC_TO_WAVE[s.epic_number]
        milestone_title = WAVE_TO_MILESTONE_TITLE[wave]
        area_label = EPIC_TO_AREA[s.epic_number]

        try:
            issue_num = create_issue(s, milestone_title, area_label)
            print(f"  [created] #{issue_num}: {s.issue_title}")
            item_id = add_to_project(issue_num)
            populate_fields(item_id, s)
            created += 1
        except Exception as exc:
            print(f"  [FAIL] {s.issue_title}: {exc}")
            failed += 1

    print(f"\nDone. Created: {created}  Skipped: {skipped}  Failed: {failed}")
    return 0 if failed == 0 else 2


if __name__ == "__main__":
    sys.exit(main())
