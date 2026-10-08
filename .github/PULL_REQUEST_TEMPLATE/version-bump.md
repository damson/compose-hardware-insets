<!--
The pull request that precedes a tag. There is no promotion: `main` is the only
long-lived branch, and a release is a tag on it.

This template is optional. A version bump is ordinary work and the default
template fits it; this one exists because the checks below are the ones a bump
is uniquely able to get wrong.
-->

## Summary

What this version carries, in two or three sentences, for someone deciding
whether to upgrade. Not a list of pull requests: that is what the changelog is.

<!--
Example:

  0.2.0 is the toolchain release. Nothing about consuming the library changed
  except Kotlin, which has to be 2.3.0 or later because the library is built
  with 2.4.20 and a 2.2 compiler cannot read its metadata.
-->

## What changed

Only the release mechanics belong here. Anything else in the diff is a separate
pull request.

- [ ] `VERSION_NAME` in `gradle.properties`, which is the only place a version
      is declared
- [ ] `CHANGELOG.md` heading dated for this version
- [ ] `README.md` install line, if it names the version

## What a consumer has to change

The floors, and which of them moved. State "nothing" if nothing did, rather
than leaving the reader to infer it.

| | Previous release | This one |
|---|---|---|
| `minSdk` | | |
| `compileSdk` | | |
| AGP | | |
| Kotlin | | |

## Before the tag

- [ ] The tag to be cut matches `VERSION_NAME` exactly, with no `v` prefix.
      `release.yml` refuses a mismatch before it publishes anything, so this is
      a courtesy rather than the guard
- [ ] `CHANGELOG.md` has a section for this version. The release notes are built
      from it and the workflow stops if it is empty
- [ ] The gate green on this branch

## After merging

Push the tag on the merge commit. Nothing else is manual: the tag push builds
the artifacts, creates the release from the changelog section, attaches five
files and publishes the API reference.

```sh
git tag <version> && git push origin refs/tags/<version>
```
