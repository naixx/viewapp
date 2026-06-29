# Changelog

All notable changes to ViewApp are documented here. This project loosely follows
[Keep a Changelog](https://keepachangelog.com/) and [Semantic Versioning](https://semver.org/).

## [2.0] — 2026-06-29

The headline of this release: **you can now create and launch time-lapses directly from the app** —
the feature that was previously "in development". The app is no longer just a monitor; it's a full
controller for the VIEW.

> Requires custom VIEW firmware **1.8.54+** (the [`naixx/VIEW`](https://github.com/naixx/VIEW/releases/latest) fork).

### Added

- **Create & launch time-lapses** from a full program editor with three modes:
  - **Auto Ramping** — automatic exposure ramping with day/night interval, bracketing/HDR, and a
    ramping-options dialog (ramp parameters, day/night luminance, highlight protection, ISO/aperture/
    shutter EV limits).
  - **Basic Fixed** — fixed interval and frame count.
  - **Eclipse** — eclipse-aware planning with pre-eclipse / partial (C1–C4) / post-eclipse phases and
    automatic recalculation for your location.
  - Plus delay-start, optional scheduling, and **Save program** to store a configuration on the device.
- **Live editing of a running time-lapse** — adjust frames, ramp mode, brightness/EV reference
  offsets, interval, and day/night interval mid-capture, ramped smoothly over a chosen number of
  frames.
- **New YouTube-style seek bar** for the time-lapse player: an overlay scrubber with tap-anywhere-to-
  seek and a "go live" pill while a capture is still running. Works for both finished clips and the
  live status screen.
- A **notification bell** surfacing device messages/warnings.

### Changed

- More reliable per-frame download flow for clips (batched downloads, better cancellation/handling).
- Reworked status and clip-info screens around the new player and seek bar.
- Upgraded the build to **AGP 9.2.1 / Kotlin 2.4.0**.

### Fixed

- Live `intervalometerStatus` frames from the device now deserialize correctly — a `Status` field
  typed `@Contextual Any?` had no registered serializer, so every running-status frame failed with
  "Serializer for class 'Any' is not found" and was silently dropped.
- MP4 export color conversion (YUV) — corrected exported video colors and encoding robustness.
- Various frame-download edge cases during live captures and reconnects.

## [1.0] — 2025-04-22

Initial release.

- Persisted, self-restoring WebSocket connection to the VIEW over the local network (hotspot, access
  point, or shared LAN) via a foreground service — keeps running and saving thumbnails while the app
  is backgrounded.
- Live status monitoring: camera, settings, battery, and the currently running time-lapse.
- Clip browser with offline persistence (Room); thumbnails and frames cached on the phone for fast
  access.
- Playback of the current and past time-lapses.
- MP4 export of clips via `MediaCodec`.
