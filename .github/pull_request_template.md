<!--
Work into `develop`, which is most of it. A promotion or a hotfix has its own
template, named at the end of CONTRIBUTING.md.
-->

## Summary

What was wrong, and what is true now. Two or three sentences, no identifiers.

## What changed

## Test plan

- [ ] `./gradlew check apiCheck koverVerify :hardware-insets:koverVerify` green, on JDK 21
- [ ] Any new test seen failing once, against the mistake it exists to catch
- [ ] `./gradlew apiDump` run and `api/*.api` committed, if the public API moved

## Review

Anything you are unsure about, or a call a reader might make differently.
