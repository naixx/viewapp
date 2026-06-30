import github.naixx.network.ACCESS_POINT_URL
import github.naixx.network.buildSubnetUrls
import kotlin.test.*

class SubnetUrlsTest {

    @Test
    fun `expands the 24 of a single address and includes the access point`() {
        val result = buildSubnetUrls(listOf("192.168.31.5"))

        assertTrue(ACCESS_POINT_URL in result)
        assertTrue("http://192.168.31.1/" in result)
        assertTrue("http://192.168.31.254/" in result)
        // access point + 254 hosts
        assertEquals(255, result.size)
    }

    @Test
    fun `scans every distinct subnet the device is on`() {
        val result = buildSubnetUrls(listOf("192.168.1.10", "10.0.2.15"))

        assertTrue("http://192.168.1.1/" in result)
        assertTrue("http://10.0.2.1/" in result)
        // access point + two /24s
        assertEquals(1 + 254 * 2, result.size)
    }

    @Test
    fun `collapses addresses that share a subnet`() {
        val result = buildSubnetUrls(listOf("192.168.1.10", "192.168.1.20"))

        assertEquals(255, result.size)
    }

    @Test
    fun `ignores malformed addresses`() {
        val result = buildSubnetUrls(listOf("not-an-ip", "192.168.1.5"))

        assertTrue("http://192.168.1.1/" in result)
        assertEquals(255, result.size)
    }

    @Test
    fun `no addresses still yields the access point`() {
        assertEquals(listOf(ACCESS_POINT_URL), buildSubnetUrls(emptyList()))
    }
}
