import github.naixx.network.buildScanUrls
import kotlin.test.*

/**
 * Covers the phase-2 scan order produced by [buildScanUrls]. The known-good ("priority")
 * addresses themselves are probed first in phase 1 of `scanHosts`, so they are deliberately
 * absent from the head of this list — they only reappear as periodic re-probes.
 */
class ScanUrlsTest {

    @Test
    fun `priority addresses are filtered out of the candidate scan`() {
        val result = buildScanUrls(
            priority = listOf("http://a/"),
            candidates = listOf("http://a/", "http://b/", "http://c/"),
        )
        assertEquals(listOf("http://b/", "http://c/"), result)
    }

    @Test
    fun `small priority list is re-inserted every N candidates`() {
        val candidates = (1..150).map { "http://c$it/" }
        val result = buildScanUrls(
            priority = listOf("http://p/"),
            candidates = candidates,
            repeatEvery = 70,
        )
        assertEquals("http://c1/", result[0])    // scan starts with the subnet, not the known-good
        assertEquals("http://p/", result[70])    // re-probed after the first 70 candidates
        assertEquals("http://p/", result[141])   // and again after the next 70
        assertEquals(2, result.count { it == "http://p/" })
    }

    @Test
    fun `priority is not re-inserted when there are fewer than N candidates`() {
        val result = buildScanUrls(
            priority = listOf("http://p/"),
            candidates = listOf("http://c1/", "http://c2/"),
            repeatEvery = 70,
        )
        assertEquals(listOf("http://c1/", "http://c2/"), result)
    }

    @Test
    fun `large priority list is never re-inserted`() {
        val priority = (1..10).map { "http://p$it/" }
        val candidates = (1..150).map { "http://c$it/" }
        val result = buildScanUrls(priority, candidates, repeatEvery = 70, maxRepeatable = 5)

        assertEquals(candidates, result)
        priority.forEach { p -> assertEquals(0, result.count { it == p }) }
    }

    @Test
    fun `empty priority returns candidates unchanged`() {
        val result = buildScanUrls(emptyList(), listOf("http://c/", "http://d/"))
        assertEquals(listOf("http://c/", "http://d/"), result)
    }

    @Test
    fun `duplicate priority entries are collapsed in re-insertions`() {
        val candidates = (1..140).map { "http://c$it/" }
        val result = buildScanUrls(
            priority = listOf("http://a/", "http://a/"),
            candidates = candidates,
            repeatEvery = 70,
        )
        // 'a' is re-inserted after candidate 70 and after candidate 140 — once each, not twice.
        assertEquals(2, result.count { it == "http://a/" })
    }
}
