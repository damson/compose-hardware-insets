# Changelog

All notable changes to this project are documented here, in the format of
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). This project follows
[semantic versioning](https://semver.org/spec/v2.0.0.html) from 0.1.0, which means the public API
may change in any 0.x release.

## [Unreleased]

### Changed

- **Consumers now need `compileSdk 37` or later, and AGP 9.1 or later.** The aar declares
  `minCompileSdk=37`, so a project on 36 resolves the dependency and then fails its
  `check<Variant>AarMetadata` task, with a message naming this library alongside every Compose and
  `androidx.core` artifact that requires the same, and stating the `compileSdk` the module is on.
  Both floors follow the Compose version this builds against: 1.12 requires `compileSdk 37` and AGP
  9.1, and the toolchain moves with it, to AGP 9.4.1, Gradle 9.8.0 and Kotlin 2.4.20. `minSdk` is
  unchanged at 23.

## [0.1.0] - 2026-10-01

First release. Extracted from a production app and generalised, so every decision that was that
app's rather than the platform's is now a parameter.

### Added

- `CutoutShape` and `View.cutoutShape()`, publishing the display cutout's rectangles and per-edge
  insets as Compose state refreshed on every inset dispatch.
- `View.stopReportingCutoutShape()`, to remove that listener.
- `hardwareInsets()` and `Modifier.clearOfTheHardware()`, taking a `HardwarePolicy` for which insets
  count and an `isFarEdgeIgnored` flag for hosts that must not grow on the far edge.
- `cornerClearance()` and the pure `cornerClearanceFor()`, which move a corner control clear of only
  the cutout rectangles it actually overlaps.
- `ScreenEdge` and `ScreenEdge.onScreenAt()`, mapping a chosen device edge onto the screen edge it
  has become under a given rotation.
- `drawBehindTheHardware()` taking a `CutoutMode` and both `SystemBarStyle`s, and
  `hideTheSystemBars()` taking a type mask and a behaviour.

### Known gaps

- `cornerClearance` returns a zero offset for `ScreenEdge.LEFT` and `ScreenEdge.RIGHT`, documented
  rather than implemented.
- `HardwarePolicy` has no rounded-corner term; the platform reports corners as a radius, not an
  inset.

[Unreleased]: https://github.com/damson/compose-hardware-insets/compare/0.1.0...HEAD
[0.1.0]: https://github.com/damson/compose-hardware-insets/releases/tag/0.1.0
