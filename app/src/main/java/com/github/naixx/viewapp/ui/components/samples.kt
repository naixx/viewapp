package com.github.naixx.viewapp.ui.components

import github.naixx.network.Aperture
import github.naixx.network.AutoSettings
import github.naixx.network.Axes
import github.naixx.network.CameraDetails
import github.naixx.network.CameraSettings
import github.naixx.network.Exposure
import github.naixx.network.ExposureConfig
import github.naixx.network.ExposureStatus
import github.naixx.network.Focus
import github.naixx.network.ISO
import github.naixx.network.Program
import github.naixx.network.Shutter
import github.naixx.network.Status
import github.naixx.network.Sunrise
import github.naixx.network.Sunset
import github.naixx.network.TimelapseClipInfo

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
    isoMin = 0,
    rampParameters = "S=A=I",
    apertureMax = -1,
    apertureMin = -7,
    manualAperture = -6,
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
        shutter = Shutter("1/640", 3.333, code = 66176, duration_ms = 100, cameraName = "1/640"),
        aperture = Aperture("8", -2.0),
        iso = ISO("100", 0.0)
    ),
    battery = 0.0,
    focusPos = 0.0
)
val status = Status(
    running = true,
    frames = 0,
    rampRate = 0.0,
    intervalMs = 5000,
    message = "starting",
    autoSettings = AutoSettings(paddingTimeMs = 2000.0),
    exposure = Exposure(
        status = ExposureStatus(rampEv = null, highlights = null, rate = null, direction = null, highlightProtection = 0),
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
    trackingTilt = 0,
    trackingPan = 0
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
