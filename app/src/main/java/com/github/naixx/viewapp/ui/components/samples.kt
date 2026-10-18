package com.github.naixx.viewapp.ui.components

import github.naixx.network.Aperture
import github.naixx.network.ApertureEntry
import github.naixx.network.AutoSettings
import github.naixx.network.Axes
import github.naixx.network.CameraDetails
import github.naixx.network.CameraSettings
import github.naixx.network.Exposure
import github.naixx.network.ExposureConfig
import github.naixx.network.ExposureStatus
import github.naixx.network.Focus
import github.naixx.network.Histogram
import github.naixx.network.ISO
import github.naixx.network.ISOEntry
import github.naixx.network.Program
import github.naixx.network.Shutter
import github.naixx.network.ShutterEntry
import github.naixx.network.ProgramDraft
import github.naixx.network.Status
import github.naixx.network.Sunrise
import github.naixx.network.Sunset
import github.naixx.network.TimelapseClipInfo
import github.naixx.network.programJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

// Create a sample TimelapseClipInfo for preview
val sampleProgram = Program(
    rampMode = "auto",
    lrtDirection = "auto",
    intervalMode = "auto",
    rampAlgorithm = "lum",
    highlightProtection = true,
    interval = "5",
    dayInterval = 5.0,
    nightInterval = 30.0,
    frames = 300,
    destination = "camera",
    nightLuminance = -1.3,
    dayLuminance = 0.0,
    isoMax = -3.0,
    isoMin = 0.0,
    rampParameters = "S=A=I",
    apertureMax = -1.0,
    apertureMin = -7.0,
    manualAperture = -6.0,
    hdrCount = 0,
    hdrStops = 1,
    trackingTarget = "moon",
    autoRestart = false,
    tracking = "none",
    delay = 1,
    axes = Axes(focus = Focus("disabled")),
    focusPos = 0
)
val cameraSettings = CameraSettings(
    shutter = "1/640",
    aperture = "8",
    iso = "100",
    details = CameraDetails(
        shutter = Shutter(
            "1/640", 3.333, code = 66176, duration_ms = 100, cameraName = "1/640",
            // EVs as the VIEW's Sony tables have them (1/64 s, ISO 100 and f/16 are 0; more light is negative),
            // covering the sample program's limits.
            list = listOf(
                ShutterEntry("1/2000", 5.0, 1, 0, "1/2000"),
                ShutterEntry("1/640", 3.33, 2, 1, "1/640"),
                ShutterEntry("1/100", 0.67, 3, 10, "1/100"),
                ShutterEntry("1/30", -1.0, 4, 33, "1/30"),
                ShutterEntry("1\"", -6.0, 5, 1000, "1\""),
                ShutterEntry("20\"", -10.33, 6, 20000, "20\""),
            )
        ),
        aperture = Aperture(
            "8", -2.0,
            list = listOf(
                ApertureEntry("1.4", -7.0, 1, "1.4"),
                ApertureEntry("2.8", -5.0, 2, "2.8"),
                ApertureEntry("5.6", -3.0, 3, "5.6"),
                ApertureEntry("8", -2.0, 4, "8"),
                ApertureEntry("11", -1.0, 5, "11"),
                ApertureEntry("16", 0.0, 6, "16"),
            )
        ),
        iso = ISO(
            "100", 0.0,
            list = listOf(
                ISOEntry("100", 0.0, 1, "100"),
                ISOEntry("400", -2.0, 2, "400"),
                ISOEntry("800", -3.0, 3, "800"),
                ISOEntry("1600", -4.0, 4, "1600"),
                ISOEntry("6400", -6.0, 5, "6400"),
            )
        )
    ),
    battery = 0.0,
    focusPos = 0.0
)
val status = Status(
    running = true,
    frames = 0,
    rampRate = 0.0,
    intervalMs = 5000.0,
    message = "starting",
    autoSettings = AutoSettings(paddingTimeMs = 2000.0),
    exposure = Exposure(
        status = ExposureStatus(rampEv = null, highlights = null, rate = null, direction = 1.0, highlightProtection = 0.0),
        config = ExposureConfig(
            sunrise = Sunrise(
                p = 0.97,
                i = 0.5,
                d = 0.6,
                targetTimeSeconds = 360,
                evIntegrationSeconds = 360,
                historyIntegrationSeconds = 480,
                highlightIntegrationFrames = 3
            ),
            sunset = Sunset(
                p = 1.1,
                i = 0.6,
                d = 0.4,
                targetTimeSeconds = 480,
                evIntegrationSeconds = 480,
                historyIntegrationSeconds = 480,
                highlightIntegrationFrames = 3
            ),
            maxEv = 20.0,
            minEv = -3.33,
            maxRate = 30,
            hysteresis = 0.4,
            nightCompensationDayEv = 10,
            nightCompensationNightEv = -1,
            nightCompensation = "auto",
            nightLuminance = "-1.3",
            dayLuminance = 0.0,
            highlightProtection = true,
            highlightProtectionLimit = 1
        )
    ),
    tlName = "tl-329",
    timelapseFolder = "/root/time-lapse/tl-329",
    startTime = 1732260769.096,
    cameraSettings = cameraSettings,
    hdrIndex = 0,
    hdrCount = 0,
    currentPlanIndex = null,
    panDiffNew = 0,
    tiltDiffNew = 0,
    focusDiffNew = 0,
    panDiff = 0,
    tiltDiff = 0,
    trackingPanEnabled = false,
    trackingTiltEnabled = false,
    trackingTilt = 0.0,
    trackingPan = 0.0
)

val sampleClipInfo = TimelapseClipInfo(
    id = 295,
    name = "tl-329",
    date = "2024-11-22T07:32:50.143Z",
    program = sampleProgram,
    status = status,
    logfile = "/var/log/view-core-20241122-072900.txt",
    cameras = 1,
    primary_camera = 1,
    thumbnail = "/root/time-lapse/tl-329/cam-1-00001.jpg",
    frames = 3786
)

// Raw device program JSON (the inner `program` object of a timelapseProgram frame), for Program tab
// previews. Driven through ProgramDraft exactly like a live program.
val sampleProgramRaw: JsonObject =
    Json.Default.parseToJsonElement(programJson).jsonObject["program"]!!.jsonObject

val sampleFixedRaw: JsonObject =
    ProgramDraft(sampleProgramRaw).set("rampMode", "fixed").set("intervalMode", "fixed").json

val sampleEclipseRaw: JsonObject =
    ProgramDraft(sampleProgramRaw)
        .set("rampMode", "eclipse")
        .set("exposurePlans", sampleProgramRaw["savedExposurePlans"] ?: JsonArray(emptyList()))
        .json

// Sample histogram for previews
val sampleHistogram = Histogram(List(256) {
    when {
        it > 20 && it < 50 -> 80
        it in 100..200     -> 40
        else               -> 10
    }
}, "histogram")
