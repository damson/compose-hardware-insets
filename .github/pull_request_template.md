<!--
Work into `develop`, which is almost all of it. A promotion to `main` or a hotfix
has its own template, and CONTRIBUTING.md says how to reach them.

Delete any heading below that would be empty rather than writing "n/a" under it.
-->

## Summary

What was wrong, and what is true now. Two or three sentences, in plain words, no
identifiers: the detail goes under *What changed*.

## What changed

The shape of the change, not a file list, which the diff already gives. Worth a line
each: a decision and the alternative it rejected, anything a reader would expect to
be here and will not find, and anything that moved beyond the obvious blast radius.

If a fact you changed is written in more than one place, say where the other copies
are. The same command lives in `CONTRIBUTING.md`, `AGENTS.md` and this template, and
a change that updates two of the three leaves a contributor following the stale one.

## Test plan

- [ ] `./gradlew check apiCheck koverVerify :hardware-insets:koverVerify` green, on JDK 21
- [ ] Any new test seen failing once, against the mistake it exists to catch, and the
      test that died was the one meant to
- [ ] `./gradlew apiDump` run and `api/*.api` committed, if the public API moved

For a public declaration: KDoc that says what the signature cannot, which is units,
coordinate space, what a caller has to do first, and what the name misleads about.

For anything in `domain`: it is gated at 100% of lines and branches, so a new branch
there needs a test that reaches it. A test written to move the counter rather than to
pin behaviour is worse than the uncovered branch.

## Evidence

Numbers and claims in this description, and where each came from. A figure read off a
different scope than the one being discussed, or a guard whose failure was never
seen, is the thing this section exists to catch: say which command produced it.

## Review

Anything you are unsure about, or a call a reader might make differently. Naming the
weakest part is worth more than defending all of it.
