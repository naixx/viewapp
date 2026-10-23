import kotlin.time.Clock
import kotlin.time.Instant

/** A clock the test moves by hand. */
class FakeClock(nowMs: Long = 0) : Clock {
    var nowMs = nowMs
    override fun now(): Instant = Instant.fromEpochMilliseconds(nowMs)
}
