package github.naixx.network

import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.serialization.json.*
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A lossless, editable view over the device's timelapse `program` JsonObject.
 *
 * ViewApp does NOT model every program field (motion `axes`, top-level `keyframes`, per-plan
 * `dayInterval`/`nightInterval`, mixed string/number encodings, …), so editing through the typed
 * [Program] and re-serializing would silently drop or mangle those. Instead we edit the raw
 * JsonObject directly and send it back via [Run]/[SaveProgram]/[ReconfigureProgram] — every key we
 * don't touch survives byte-for-byte.
 *
 * All edit methods are immutable: they return a new [ProgramDraft] (Compose-state friendly).
 */
class ProgramDraft(val json: JsonObject) {

    val rampMode: String get() = str("rampMode") ?: "fixed"
    val intervalMode: String get() = str("intervalMode") ?: "fixed"
    val rampAlgorithm: String get() = str("rampAlgorithm") ?: "lum"
    val lrtDirection: String get() = str("lrtDirection") ?: "auto"

    /** Fixed interval is transmitted as a string on the wire. */
    val interval: String get() = str("interval") ?: "5"
    val dayInterval: Double get() = num("dayInterval") ?: 5.0
    val nightInterval: Double get() = num("nightInterval") ?: 30.0
    val frames: Int get() = int("frames") ?: 0
    val delay: Int get() = int("delay") ?: 1
    val destination: String get() = str("destination") ?: "camera"

    val hdrCount: Int get() = int("hdrCount") ?: 0
    val hdrStops: Double get() = num("hdrStops") ?: 1.0

    val rampParameters: String get() = str("rampParameters") ?: "S+I"
    val dayLuminance: Double get() = num("dayLuminance") ?: 0.0
    val nightLuminance: Double get() = num("nightLuminance") ?: -1.5
    val highlightProtection: Boolean get() = bool("highlightProtection") ?: false
    val isoMax: Double? get() = num("isoMax")
    val isoMin: Double? get() = num("isoMin")
    val apertureMax: Double? get() = num("apertureMax")
    val apertureMin: Double? get() = num("apertureMin")
    val shutterMax: Double? get() = num("shutterMax")
    val manualAperture: Double? get() = num("manualAperture")

    val scheduled: Boolean get() = bool("scheduled") ?: false
    val schedStart: String get() = str("schedStart") ?: "08:00"
    val schedStop: String get() = str("schedStop") ?: "18:00"

    /** @param day capitalised English weekday, e.g. "Monday". */
    fun schedDay(day: String): Boolean = bool("sched$day") ?: false

    // Eclipse — display-only fields are computed by the firmware and echoed via reconfigureProgram.
    val eclipseInfo: String? get() = str("eclipseInfo")
    val eclipseFirstContact: Long? get() = num("eclipseFirstContact")?.toLong()
    val utcOffset: Int? get() = int("utcOffset")
    val plans: List<JsonObject> get() = (json["exposurePlans"] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList()

    // --- edits (return a new draft) ---

    fun set(key: String, value: JsonElement): ProgramDraft = ProgramDraft(JsonObject(json + (key to value)))
    fun set(key: String, value: String): ProgramDraft = set(key, JsonPrimitive(value))
    fun set(key: String, value: Double): ProgramDraft = set(key, JsonPrimitive(value))
    fun set(key: String, value: Int): ProgramDraft = set(key, JsonPrimitive(value))
    fun set(key: String, value: Boolean): ProgramDraft = set(key, JsonPrimitive(value))

    /** Edit one field of the exposure plan at [index] (eclipse mode); no-op if out of range. */
    fun editPlan(index: Int, key: String, value: JsonElement): ProgramDraft {
        val arr = plans.toMutableList()
        if (index !in arr.indices) return this
        arr[index] = JsonObject(arr[index] + (key to value))
        return set("exposurePlans", JsonArray(arr))
    }

    private fun prim(k: String) = json[k] as? JsonPrimitive
    private fun str(k: String) = prim(k)?.let { if (it is JsonNull) null else it.content }
    private fun num(k: String) = prim(k)?.let { it.doubleOrNull ?: it.contentOrNull?.toDoubleOrNull() }
    private fun int(k: String) = num(k)?.toInt()
    private fun bool(k: String) = prim(k)?.let { it.booleanOrNull ?: it.contentOrNull?.toBooleanStrictOrNull() }
}

/**
 * The `date` value the `run` message expects: client local time expressed as epoch-ms in UTC
 * (so the device can do sun/eclipse timing without trusting its own clock). Mirrors the original
 * app's `moment().valueOf() + moment().utcOffset()*60*1000`.
 */
fun runDateMillis(now: Instant = Clock.System.now()): Long =
    now.toEpochMilliseconds() + TimeZone.currentSystemDefault().offsetAt(now).totalSeconds * 1000L
