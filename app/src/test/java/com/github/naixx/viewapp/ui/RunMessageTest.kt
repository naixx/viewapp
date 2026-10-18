package com.github.naixx.viewapp.ui

import github.naixx.network.Run
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class RunMessageTest {

    private fun runProgram(program: JsonObject): JsonObject {
        val vm = ProgramViewModel().apply { onDeviceProgram(program) }
        return (vm.runMessage() as Run).program
    }

    @Test
    fun aProgramWithoutDelayStartsAfterOneSecond() {
        // 2026-10-18: after a reboot the VIEW's program had no delay; tl-446..448 never took a frame
        val program = runProgram(JsonObject(mapOf("rampMode" to JsonPrimitive("auto"))))
        assertEquals(1, program["delay"]?.jsonPrimitive?.int)
        assertEquals("auto", program["rampMode"]?.jsonPrimitive?.content)
    }

    @Test
    fun keepsAPickedDelay() {
        val program = runProgram(JsonObject(mapOf("delay" to JsonPrimitive(600))))
        assertEquals(600, program["delay"]?.jsonPrimitive?.int)
    }
}
