# Kotlin style

ktlint and detekt run inside `check`, so a style finding fails the build like a
test does.

## Fixing a finding

```
./gradlew formatKotlin
```

This fixes most of what ktlint reports, across the whole project. A rule with no
auto-correction, such as a line over 100 columns inside a string, still wants a
hand edit. detekt findings are never auto-fixable and are meant to be read.

## Where the rules live

Two files, and neither plugin block:

- `.editorconfig` for formatting, which Android Studio and IntelliJ read too.
- `config/detekt/detekt.yml` for static analysis, stating only where this
  project differs from detekt's defaults.

Both give the reason for each deviation on the line that sets it. The brace
rules for `if` and `when` are enforced by review, not by either tool; the
comment above `ktlint_standard_multiline-if-else` states them and says why no
tool can.

## The pre-commit hook

```
./gradlew installGitHooks
```

Optional, once per clone. It points git at `.githooks`, whose `pre-commit` runs
both tools over the Kotlin files you staged, so a formatting fix never becomes a
second commit on a pull request.

- **It reads the files as they are on disk.** A file staged in part is judged
  whole, and the hook says when that happens.
- **It checks only staged `.kt` files inside a module.** A file you are still
  editing elsewhere does not block an unrelated commit. `.kts` build scripts
  and files outside any module are skipped, as they are by `check`.
- **`SKIP_HOOKS=1 git commit` goes past it.**

Before you install it:

- The setting lives in the clone's config, so every worktree shares it, and a
  worktree on a branch without `.githooks` runs nothing and says nothing.
- It takes precedence over a global `core.hooksPath` and over `.git/hooks`, so
  it replaces hooks of your own without a word.
- Hooks are not cloned. Without this command nothing runs locally, and `check`
  is still the gate.
