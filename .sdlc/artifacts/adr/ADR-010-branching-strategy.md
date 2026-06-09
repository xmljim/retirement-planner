# ADR-010: Branching Strategy — Trunk-Based Development

- **Status**: Accepted
- **Date**: 2026-06-08
- **Deciders**: Owner
- **Related**: ADR-001 (platform), ADR-008 (Modulith), ADR-009 (quality gates)

## Context

The owner has a strong working knowledge of Gitflow (matches the
`places-maps-spring` workflow) and a healthy skepticism of trunk-based
development at scale. The question for this project is whether to
default to Gitflow's structure or commit to TBD given this project's
specific profile.

Project profile relevant to the decision:

- **Solo developer** for the foreseeable v1 (DISC-001, ADR-001)
- **No external testers / QA** — the owner is the only consumer
- **Pure backend Java + a separate frontend repo** — no big-bang
  multi-repo releases coordinated through a shared develop branch
- **Strong static safety nets**: Modulith verification (ADR-008),
  Money-type Checkstyle rule (ADR-007), full quality-gate suite
  (ADR-009)
- **Fast CI** — backend Java with module-level test boots; targeting
  < 3 min for the full quality + test pipeline
- **Releases are tags, not events** — solo dev, no release ceremonies

## Decision

**Trunk-based development.** A single long-lived branch, `main`. Short-
lived `feature/*` branches off `main` for any non-trivial work; merged
back via PR. Releases are tags on `main`, not separate branches.

### Branch Topology

```
main (protected, always shippable)
├── feature/issue-NN-short-description    (created from main, merged back)
└── tags vX.Y.Z                           (releases)
```

That's the entire topology. No `develop`. No `release/*`. No
`hotfix/*` (a hotfix is just a feature branch with a fast PR + patch
release tag).

### Branch Lifecycle Rules

- **`main`**: always shippable, protected (see "Branch Protection"
  below). Direct push prohibited.
- **`feature/*`**: short-lived (target ≤ 3 days). Branch name carries
  the issue number for traceability: `feature/issue-42-bridge-bucket`.
- **PR size**: target one story per PR (per epic decomposition,
  stories are ≤ 5 points). A feature branch that grows past 5 points
  of scope should be split.
- **Rebase before merge**: feature branches rebase onto `main` to
  resolve conflicts; merge commits are squashed into `main` (one PR =
  one commit on `main`) so the history is linear and readable.
- **Merge requirement**: green CI on the feature branch (all quality
  gates per ADR-009 + tests + coverage thresholds met) before merge.

### Releases

A release is a **tag on `main`**:

```bash
mvn versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false
git commit -am "chore(release): vX.Y.Z"
git push
git tag -a vX.Y.Z -m "Release X.Y.Z"
git push origin vX.Y.Z
mvn versions:set -DnewVersion=X.Y+1.0-SNAPSHOT -DgenerateBackupPoms=false
git commit -am "chore: bump to X.Y+1.0-SNAPSHOT"
git push
```

The `/release` skill automates this. There is no `release/*` branch —
the tag *is* the release.

If a defect is found in vX.Y.Z that needs patching:
1. Branch `feature/issue-NN-fix-something` from the tag (not `main` —
   `main` may have moved past).
2. Apply the fix, run quality gates.
3. PR back to `main` (so future versions also have the fix).
4. Cherry-pick the merged commit onto a new patch branch from the tag,
   bump version, tag `vX.Y.Z+1`.

This is rare enough that automating it isn't worth it.

### Feature Flags Replace Long-Lived Branches

When a feature is large enough that it can't ship in one short-lived
branch, **gate it behind a feature flag** rather than parking it on a
long-lived branch:

```yaml
# application.yml
app.features:
  monte-carlo-v2: false        # WIP; default off
  guyton-klinger-spending: true
```

Code merges incrementally to `main` behind the flag; flip the flag
when ready. This is the foundational TBD discipline — and it costs us
almost nothing because we're already going to need flags for SaaS-vs-solo
configuration anyway (per ADR-001).

## Branch Protection (configured via `gh api` on initial push)

`main` requires:

- **Pull request review**: 1 approval (or `bypass` for solo, see Notes)
- **Required status checks** (must pass before merge):
  - `quality / spotless`
  - `quality / checkstyle`
  - `quality / pmd`
  - `quality / spotbugs`
  - `quality / archunit`
  - `quality / coverage`
  - `tests / unit`
  - `tests / integration`
  - `modulith / verify`
- **Up-to-date branch required** before merge (rebase if behind)
- **Linear history required** (squash-merges only)
- **No force pushes**
- **No deletions**

Direct push to `main` is forbidden, full stop.

## Rationale

- **Solo + fast CI + strong static gates** is the canonical TBD profile.
  Gitflow's structure exists primarily to absorb integration risk that
  Modulith verification + ADR-009 quality gates already absorb.
- **No external consumers** means there's no QA-stable target that
  needs a long-lived branch.
- **The `release/*` branch is friction without payoff** for a solo dev
  who's both authoring and releasing. Tags + scripted release flow give
  the same outcome.
- **Feature flags > long-lived feature branches**. Already a SaaS-vs-solo
  necessity per ADR-001; reusing for TBD discipline is free.
- **Linear history (squash-merge)** keeps the log readable. Each story
  is one commit on `main`, with the issue number in the message.
- **Patch releases via cherry-pick from tag** is a manual but rare
  exercise. Gitflow's `hotfix/*` branch is more ceremony than this is
  worth.

## Consequences

**Positive**
- One branch to think about
- Releases are mechanical (tag + scripted version bump)
- No "is this in develop yet?" coordination
- CI runs against the same target every time
- New contributors (if/when) onboard with one rule: "branch off main, PR back to main"

**Negative**
- **Discipline-dependent**: every commit to `main` must be shippable.
  Without quality gates this would be terrifying; with them it's a
  forcing function.
- **Patch releases are manual** (cherry-pick from a tag). Acceptable
  given expected frequency (low).
- **Feature flag rot**: WIP flags can stick around. Mitigation: a
  flag-cleanup story added at the end of each milestone.
- **Re-evaluation if conditions change**: see Triggers below.

## Triggers for Re-evaluation

This decision should be revisited if **any** of these become true:

1. **CI duration > 5 minutes** for the full quality + test pipeline.
   TBD's economics depend on fast CI.
2. **A second active contributor** joins the project. TBD with
   multiple contributors requires very small PRs and short branch life
   — workable, but Gitflow's structure begins to pay for itself.
3. **External testers / a stable QA target** emerges. A `develop`
   branch becomes a sensible staging surface.
4. **A regulatory or compliance need** requires a documented
   stabilization window separate from active development.

If any of those land, opening this ADR back up and proposing a
superseding ADR-XXX is the right move. Migration TBD → Gitflow is
mechanical (create `develop`, branch policy update, retrain workflow).

## Alternatives Considered

- **Gitflow** (the owner's default) — viable, but the integration risk
  it manages is largely already managed by Modulith verification +
  quality gates + the solo-dev coordination cost of zero. Net cost
  exceeds net benefit for this project's profile.
- **GitHub Flow** (TBD with topic branches, no release branches) —
  effectively what we've decided. Calling it "TBD" rather than
  "GitHub Flow" because the latter conflates with deploy-on-merge,
  which we're not doing.
- **Release Flow / OneFlow** (variants with one release branch but no
  develop) — reasonable middle ground, but still adds a branch type
  whose payoff is small for solo dev with tag-based releases.

## Notes

- Branch protection's "1 approval" requirement: GitHub allows
  bypassing for repository admins. For solo work, the owner can merge
  their own PR after running `/code-review` skill against the diff.
  The bypass is **not** a license to skip the *status checks* — those
  must still pass.
- The PR template (in `.github/pull_request_template.md`) will include
  a checklist tying back to the relevant ADRs and the FR/NFR being
  satisfied.
- The `/commit-push-pr` skill handles the branch → PR flow; configured
  to target `main` (not `develop`).
