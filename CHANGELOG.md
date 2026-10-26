# Changelog

All notable changes to ViewApp are documented here. This project loosely follows
[Keep a Changelog](https://keepachangelog.com/) and [Semantic Versioning](https://semver.org/).

## [2.3] — 2026-10-26

Layouts for tablets and wide windows, a connection that follows the phone to another network, and the
same screens in a browser, served by the VIEW itself.

> Works with VIEW firmware **1.8.54+**; get **1.8.58** ([`naixx/VIEW`](https://github.com/naixx/VIEW/releases/latest))
> for the `view.local` name and ViewApp in a browser at `http://view.local/`.

### Added

- **Layouts for tablets, foldables and landscape.** From 600 dp wide the tabs move to a rail at the side.
  From 840 dp the Clips tab shows the clip grid and the open clip side by side, and the Status screen puts
  the picture (the running time-lapse, or the camera controls when idle) beside the program and the status.
  The clip list becomes a grid of two or more columns from 480 dp.
- **`view.local`**: the app tries `http://view.local/` first, next to the last known addresses (VIEW firmware
  1.8.58 answers this name); the subnet scan still runs beside it.
- **ViewApp in a browser.** The Status and Clips screens are also built for the browser and served by the VIEW
  (firmware 1.8.58) at `http://view.local/`, for an iPhone, a computer, or anyone without the app. The browser
  has no MP4 export and no background connection: the page catches up when it is opened again.

### Changed

- **Android 9 or newer** is required (was Android 7).

### Fixed

- **Long time-lapses on Android 15+**: the connection service is now a "connected device" foreground service.
  Android 15 lets the previous type (data sync) run only 6 hours a day in the background and then crashes an app
  whose service keeps running, so a night time-lapse could lose its connection and its saved thumbnails partway.
- **After the phone moves to another network** the next reconnect attempt scans that network; the app kept
  scanning the old one until Disconnect and Connect.
- **Clip list**: an older clip that the VIEW lists without a database id no longer stops the whole list from
  refreshing.
- The notification menu closes when you tap a notification or Clear all.

## [2.2] — 2026-10-22

Keeps the ramping limits working with VIEW firmware 1.8.56.

> Works with VIEW firmware **1.8.54+**; get **1.8.56** ([`naixx/VIEW`](https://github.com/naixx/VIEW/releases/latest))
> for much lower CPU load and battery drain while shooting, and the new Cable Release mode on the VIEW.

### Fixed

- **Ramping options of a running time-lapse** with VIEW firmware 1.8.56: the ISO, aperture and shutter limits
  take the camera's value lists from the settings message. Firmware 1.8.56 no longer repeats those lists in the
  status it sends on every frame (1.8 KB instead of 17 KB), so with ViewApp 2.1 the limits had nothing to pick
  from. Older firmware keeps working as before.

## [2.1] — 2026-10-18

Camera controls on the status screen, a closer look at a running time-lapse, and a round of fixes from
real night runs.

> Works with VIEW firmware **1.8.54+**; get **1.8.55** ([`naixx/VIEW`](https://github.com/naixx/VIEW/releases/latest))
> for the drive mode line, a notification when a running time-lapse stops producing frames, and a much
> faster VIEW boot.

### Added

- **Camera controls** — shutter, aperture and ISO pickers with the camera's own names, EV −/+, a test
  shot and live view, right on the status screen.
- **Histogram of the frame under the player** — follows scrubbing and playback on the running-status
  and clip screens; computed in the app with the VIEW's own formula.
- **Frame progress bar** on the running status screen, like the VIEW's OLED: exposure, download and
  processing, and the time since the capture started, so a late or stuck frame is visible from the
  phone.
- **Ramping options of a running time-lapse** — the exposure limits are shown by the camera's names
  (e.g. "Limits: ISO 400-6400, aperture f/1.4-f/8, shutter up to 20""), and the Ramping Options dialog
  can change the ramp parameters and the ISO, aperture and shutter limits mid-run; they apply from the
  next frame.
- **Drive mode** of the camera under the exposure, e.g. "Drive mode: Continuous Shooting Lo (Single
  from the next shot)" — the VIEW switches to Single before it shoots (firmware 1.8.55+).

### Changed

- In access-point mode the app keeps the phone on the VIEW hotspot: Android used to leave it for mobile
  data (the hotspot has no internet) and not rejoin it after the hotspot restarted.

### Fixed

- Connecting when the phone and the VIEW are on different subnets.
- Status and settings parsing for fractional and null values (no camera connected, highlight
  protection stepping by 1/3).
- "camera disconnected" instead of "unknown connected" when the camera drops off mid-run.
- The clip length on the running status screen (it divided the capture time by 30).
- "Download failed" right after starting a time-lapse: frames are no longer fetched before the first
  one.
- A time-lapse started from the app after a VIEW reboot could wait forever without a frame: the start
  delay is now always sent.

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
