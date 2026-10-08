<!--
A fix for an already released version, on a `release/<x.y>` branch cut from that
version's tag. If the fix belongs in the next release and nothing is urgent, it
is ordinary work into `main` and uses the default template.

Open this with ?template=hotfix.md on the compare URL, with the base set to the
`release/<x.y>` branch rather than to `main`.
-->

## Summary

What a consumer cannot do until this ships, and why it cannot wait for the next
ordinary release. Two or three sentences.

<!--
Example:

  0.2.0's aar resolves on a consumer at compileSdk 36 and then fails the AAR
  metadata check, because the published metadata demands 37. Nobody on 36 can
  build against the release at all, and that is every consumer who has not
  already moved.
-->

## Why it is a backport and not the next release

- [ ] `main` has moved on in ways a consumer on this version should not be made
      to take
- [ ] The fix applies to the released tree without carrying anything else

If neither is true, close this and open the fix against `main`.

## The branch

- [ ] Cut from the tag, not from `main`:
      `git switch -c release/<x.y> <x.y.z>`
- [ ] Contains this fix and nothing else

## It has to reach `main` too

A backport branch is not merged into `main`; it is tagged and left to go stale.
So the fix lands on `main` separately, or the next release silently ships without
it and nothing fails.

- [ ] The same fix is on `main`, or a follow-up is open and named here

<!--
Example:

  Landed on `main` as #44 before this was opened, so `main` already carries it and
  this branch exists only to get it to consumers on 0.2.x.
-->

## Verification

- [ ] The gate green on this branch
- [ ] The failure reproduced against the released version first, so the fix is
      known to address what consumers are actually hitting
- [ ] A test that fails without the fix

## After merging

Bump `VERSION_NAME` on this branch and tag the patch version from it. The tag
push publishes it exactly as it would from `main`.

```sh
git tag <x.y.z> && git push origin refs/tags/<x.y.z>
```
