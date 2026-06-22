package com.github.naixx.viewapp.ui.components

import kotlin.math.abs

/** A selectable option: [label] is shown to the user, [value] is written to the program. */
data class Opt<T>(val label: String, val value: T)

private fun stops(v: Double): String {
    val n = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
    val sign = if (v > 0) "+" else ""
    val unit = if (abs(v) == 1.0) "stop" else "stops"
    return "$sign$n $unit"
}

/**
 * Option lists ported verbatim from the original web app's `IntervalometerPage.lists`
 * (see docs/ORIGINAL_APP.md and frontend/www/build/main.js).
 */
object ProgramLists {

    val rampModes = listOf(
        Opt("Auto Ramping", "auto"),
        Opt("Basic Fixed", "fixed"),
        Opt("Eclipse", "eclipse"),
    )

    val intervalModes = listOf(
        Opt("Fixed", "fixed"),
        Opt("Auto Day/Night", "auto"),
        Opt("External AUX2", "aux"),
    )

    val exposureMode = listOf(
        Opt("Auto Ramping", "auto"),
        Opt("Preset Value", "preset"),
        Opt("Lock Exposure", "locked"),
    )

    val rampParameters = listOf(
        Opt("Shutter, ISO", "S+I"),
        Opt("Shutter, ISO (balanced)", "S=I"),
        Opt("Shutter, ISO, Aperture", "S+I+A"),
        Opt("Shutter, ISO, Aperture (balanced)", "S=I=A"),
    )

    val dayLuminance = listOf(-1.5, -1.0, -0.5, 0.0, 0.5, 1.0, 1.5).map { Opt(stops(it), it) }

    val nightLuminance =
        listOf(-3.0, -2.5, -2.0, -1.5, -1.0, -0.5, 0.0, 0.5, 1.0, 1.5, 2.0).map { Opt(stops(it), it) }

    val bracketing = listOf(
        Opt("Single frames", 1),
        Opt("Multi-exposure Sets", 3),
    )

    val bracketingFrames = (2..10).map { Opt("sets of $it", it) }

    val bracketingStops = (1..12).map { val s = it / 3.0; Opt(stops(s), s) }

    /** Delay-start presets, in seconds (Program.delay). */
    val delays = listOf(
        Opt("1 minute", 60),
        Opt("5 minutes", 300),
        Opt("10 minutes", 600),
        Opt("20 minutes", 1200),
        Opt("30 minutes", 1800),
        Opt("45 minutes", 2700),
        Opt("1 hour", 3600),
        Opt("1.5 hours", 5400),
        Opt("2 hours", 7200),
        Opt("3 hours", 10800),
        Opt("4 hours", 14400),
        Opt("5 hours", 18000),
    )

    /** 00:00 .. 23:30 in 30-minute steps (HH:mm), matching the original schedule pickers. */
    val timeOfDay: List<Opt<String>> = buildList {
        for (h in 0..23) for (m in listOf(0, 30)) {
            val t = "%02d:%02d".format(h, m)
            add(Opt(t, t))
        }
    }

    /** Capitalised weekday names, matching the `sched<Day>` program keys. */
    val weekDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    /** "Apply over N frames" options for live dynamic changes (0 = immediately). */
    val rampFrames = listOf(
        Opt("Immediately", 0),
        Opt("over 10 frames", 10),
        Opt("over 20 frames", 20),
        Opt("over 30 frames", 30),
        Opt("over 40 frames", 40),
        Opt("over 50 frames", 50),
        Opt("over 60 frames", 60),
        Opt("over 90 frames", 90),
        Opt("over 120 frames", 120),
        Opt("over 180 frames", 180),
        Opt("over 240 frames", 240),
    )
}
