# Contributing

Pull requests are welcome. You do not need to ask first.

## The one command

```
JAVA_HOME=<a JDK 21> ./gradlew check apiCheck koverVerify :hardware-insets:koverVerify
```

That is everything CI runs. If it passes locally it passes there.

The two `koverVerify` tasks are two different gates, which is why both are named: the root one holds
the pure geometry in `domain` at 100% of lines and branches, and the module one holds the library
overall at 90% of lines and 85% of branches.

**JDK 21 is required**, not optional. Robolectric loads the Android jar for the emulated SDK, and
the API 36 jar refuses to load under anything earlier. On JDK 17 every test fails in setup with
`Failed to create a Robolectric sandbox`, which reads like broken tests rather than a wrong JDK.

## Branches and pull requests

**One long-lived branch, `main`.** Cut a short-lived branch from it, open a pull
request back into it, and delete the branch on merge. `feature/<what-it-does>` and
`fix/<what-it-fixes>` are the prefixes in use; nothing enforces the naming.

**A release is a tag on `main`, not a branch.** There is no integration branch to
promote from, which is deliberate: a release run reads the workflow present at the
ref the event carries, so a tag cut on a branch work never lands on can publish a
release that attaches nothing with no check going red. This repository shipped
`0.1.0` that way. One branch makes that impossible rather than guarded against.

**A backport is cut on demand.** If a released version ever needs a patch after
`main` has moved on, branch `release/<x.y>` from that version's tag, fix it there,
and tag from it. Nothing maintains such a branch between releases.

Three templates, and the two uncommon ones have to be named in the URL because
GitHub applies the default without offering a choice:

| Flow | Base | Template |
|---|---|---|
| Ordinary work | `main` | the default, applied automatically |
| The version bump before a tag | `main` | add `?template=version-bump.md` to the compare URL |
| Backport on a `release/<x.y>` branch | that branch | add `?template=hotfix.md` |

## What a change needs

- **A test that has been seen failing.** Break what your new test guards, watch it go red, put it
  back. A check that has never failed is not known to check anything, and the bugs this library
  exists to catch are all silent: an inset on the wrong edge looks like a layout choice.
- **A public declaration documented where a caller could not work it out.** Units, coordinate space,
  which API level starts reporting the thing, what a name misleads about. Not a restatement of the
  signature.
- **`./gradlew apiDump` run and the result committed** if you changed the public API. `apiCheck`
  fails the build otherwise, which is the point: an accidental break should cost us a red build
  rather than costing a consumer a broken one.

## Testing hardware you do not have

There is no resource qualifier for a display cutout and none at all for a curved edge, so the tests
build a `WindowInsetsCompat` and dispatch it through a real layout. Add to that rather than reaching
for a device: a test that needs particular hardware only ever runs on one desk.

`cornerClearanceFor` is pure and public. Geometry belongs there, where it can be tested without a
window at all.

## Scope

This library is about hardware that is physically in the way of your UI: cutouts, waterfall curves,
and later folds and rounded corners. Insets that are software, such as the IME or the system bars,
belong to Compose and androidx, and are offered here only as a policy flag rather than reimplemented.

## Cutting a release

**One pull request, then one tag.** Nothing else is manual.

1. Open an ordinary pull request against `main` that sets `VERSION_NAME` in
   `gradle.properties` and dates the `CHANGELOG.md` heading for that version.
   Both modules read `VERSION_NAME`, so it is the only place a version is
   declared.
2. Merge it, then push the tag on the resulting commit:
   `git tag 0.3.0 && git push origin refs/tags/0.3.0`.

That is the whole procedure. The tag push runs two workflows, and both create
what they publish rather than reacting to something a person made.

**The tag carries no `v`.** JitPack's coordinate *is* the tag, so `v0.3.0` is
served as version `v0.3.0` while every file says `0.3.0`.

**The tag cannot be moved or deleted.** A ruleset blocks both on anything
matching `[0-9]*`, because a moved tag serves different bytes under one
coordinate to anyone whose build service caches per tag, and JitPack does.
`non_fast_forward` alone does not stop a move: the rule that does is `update`.
The admin role bypasses, so a genuinely mis-cut tag can still be corrected
deliberately.

**`release.yml` refuses a tag that disagrees with `VERSION_NAME`, before
publishing anything.** It also stops if `CHANGELOG.md` has no section for the
version, because the release notes are built from it. Both happen before the
release object exists, so the only thing to undo is the tag.

**Publishing the API reference rests on two repository settings** that no
workflow can set for itself. Both are set; they are recorded here because
nothing in the build fails if one is undone, and a fork starts with neither.
Pages is enabled with "GitHub Actions" as its source, and the `github-pages`
environment carries a `tag: *` deployment rule beside the default-branch one,
because the run's ref is a tag and GitHub creates that environment limited to
the default branch. Without the rule the deploy is refused with a message
naming a branch for a tag. A manual run of `docs.yml` from a branch proves the
setup without cutting a release.

Two of the five attached files come with caveats worth knowing before anyone
relies on them.

- **The aar is for reading and archiving. JitPack is how you depend on this.** The aar's own
  coordinates are `com.devddagnet:hardware-insets`, which resolves nowhere, and JitPack serves
  `com.github.damson:compose-hardware-insets`. The pom and the Gradle metadata are attached beside it,
  so a consumer who does drop it into `libs/` can at least see the Compose and activity dependencies
  it needs.
- **The sample apk is debug-signed, and every release's is signed by a different key.** The runner
  generates one when none exists, so consecutive samples cannot upgrade over each other: uninstall the
  previous one first. Its `versionCode` is 1 and stays there, which would block an upgrade on its own
  even if the keys matched. It is also debuggable and unminified, which is fine for reading and is not a
  release build of anything.
