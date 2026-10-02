<!--
A promotion of `develop` to `main`, which is what `main` ever receives.

Open it with ?template=release.md on the compare URL, or this is the wrong
template: ordinary work into `develop` uses the default one.
-->

## Summary

What `main` is missing and what it will carry. Two or three sentences. A promotion
has no "what was wrong": the work was already reviewed on its way into `develop`.

## What changed

Nothing. List the commits being promoted, by their pull request numbers, and say
which merge method this lands under.

## Proof it carries the reviewed content

A promotion is proved by the tree, not by the commits: a squash merge means the
commits on `develop` are not the commits that were reviewed, so ancestry answers the
wrong question.

- [ ] `git rev-parse origin/main^{tree} origin/develop^{tree}` printed the same hash
      after merging, or the difference is named below and justified
- [ ] No commit was added to this branch to answer review. Findings become follow-ups
      against `develop`, so the tree that was reviewed is the tree that gets tagged

## Before the tag

- [ ] The version in `hardware-insets/build.gradle.kts` and `sample/build.gradle.kts`
      matches the tag that will be cut, with no `v` prefix. A workflow checks this
      after the release is published, where it can only report
- [ ] `CHANGELOG.md` has a dated heading for this version
- [ ] Every workflow that runs on a release is already on `main`. A release run uses
      the workflow present at the tag, so one that has not been promoted yet does
      nothing and reports no failure

## Review

A release pull request is answered with follow-ups rather than commits. Anything
found here that is not a reason to stop goes in an issue.
