package com.github.naixx.viewapp.ui

import github.naixx.network.Aperture
import github.naixx.network.ApertureEntry
import github.naixx.network.CameraDetails
import github.naixx.network.ISO
import github.naixx.network.ISOEntry
import github.naixx.network.ProgramDraft
import github.naixx.network.Shutter
import github.naixx.network.ShutterEntry
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class RampLimitsLineTest {

    // A6600 entries with the EVs of the VIEW's Sony tables (sony.js)
    private val a6600 = CameraDetails(
        shutter = Shutter("1/200", 1.67, list = listOf(
            ShutterEntry("1/200", 1.67, cameraName = "1/200"),
            ShutterEntry("20\"", -10 - 1 / 3.0, cameraName = "20\""),
            ShutterEntry("30\"", -11 + 1 / 3.0, cameraName = "30\""),
        )),
        aperture = Aperture("5.6", -3.0, list = listOf(
            ApertureEntry("1.4", -7.0, cameraName = "1.4"),
            ApertureEntry("5.6", -3.0, cameraName = "5.6"),
            ApertureEntry("8", -2.0, cameraName = "8"),
        )),
        iso = ISO("125", -1 / 3.0, list = listOf(
            ISOEntry("100", 0.0, code = 100),
            ISOEntry("400", -2.0, code = 400),
            ISOEntry("6400", -6.0, code = 6400),
        )),
    )

    private fun program(vararg limits: Pair<String, Double>) =
        ProgramDraft(JsonObject(limits.associate { (k, v) -> k to JsonPrimitive(v) }))

    @Test
    fun namesTheLimitsOfTheTl443Program() {
        // tl-443 (2026-10-18): isoMax -6, isoMin -2, apertureMax -2, apertureMin -7
        val d = program("isoMax" to -6.0, "isoMin" to -2.0, "apertureMax" to -2.0, "apertureMin" to -7.0, "shutterMax" to -10.333333333333334)
        assertEquals("Limits: ISO 400–6400, aperture f/1.4–f/8, shutter up to 20\"", rampLimitsLine(d, a6600))
    }

    @Test
    fun aOneSidedLimitSaysWhichSide() {
        assertEquals("Limits: ISO up to 6400", rampLimitsLine(program("isoMax" to -6.0), a6600))
        assertEquals("Limits: ISO from 400, aperture from f/1.4", rampLimitsLine(program("isoMin" to -2.0, "apertureMin" to -7.0), a6600))
        assertEquals("Limits: aperture up to f/8", rampLimitsLine(program("apertureMax" to -2.0), a6600))
    }

    @Test
    fun noLimits() {
        assertEquals("Limits: none", rampLimitsLine(program(), a6600))
    }

    @Test
    fun aValueTheCameraDoesNotListShowsItsEv() {
        assertEquals("Limits: ISO up to EV -8", rampLimitsLine(program("isoMax" to -8.0), a6600))
        assertEquals("Limits: ISO up to EV -6", rampLimitsLine(program("isoMax" to -6.0), null))
    }

    @Test
    fun summaryMarksHighlightProtection() {
        val d = ProgramDraft(JsonObject(mapOf("rampParameters" to JsonPrimitive("S=I"), "highlightProtection" to JsonPrimitive(true))))
        assertEquals("S=I, HL", rampingSummary(d))
        assertEquals("S+I", rampingSummary(ProgramDraft(JsonObject(emptyMap()))))
    }
}
