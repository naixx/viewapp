package github.naixx.network

import de.jensklingenberg.ktorfit.http.*
import github.naixx.network.internal.AddressResponseSerializer
import github.naixx.network.internal.ioDispatcher
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable

@Serializable
data class Auth(val session: String)

interface OriginUrl {
    var fromUrl: String?
}

@Serializable(with = AddressResponseSerializer::class)
sealed class AddressResponse() : OriginUrl {

    @Serializable
    data class Address(val address: String, override var fromUrl: String? = null) : AddressResponse()

    @Serializable
    data class LoginRequired(val message: String, override var fromUrl: String? = null) : AddressResponse()
}

interface ViewApi {

    @POST("api/login")
    suspend fun auth(@Body username: Any): Auth

    @GET("socket/address")
    suspend fun socketUrl(): AddressResponse

    @GET("clips")
    suspend fun clips(): List<Clip>

    @GET("clipinfo")
    suspend fun clipInfo(@Query name: String): TimelapseClipInfo

    @GET("download")
    suspend fun download(@Query file: String): ByteArray

    @GET
    suspend fun socketUrl(@Url url: String): AddressResponse
}

/**
 * Builds the subnet-scan order for phase 2 of [scanHosts]: the freshly generated
 * [candidates] (with any [priority] addresses filtered out, since those are tried first
 * on their own in phase 1) and — when the priority set is small — the priority addresses
 * re-inserted every [repeatEvery] candidates, so a device that comes back online mid-scan
 * is retried quickly instead of only at the very end of a 254-host sweep.
 *
 * @param maxRepeatable if there are more priority addresses than this, they are not
 *        re-inserted, to avoid bloating the scan list.
 */
fun buildScanUrls(
    priority: List<String>,
    candidates: List<String>,
    repeatEvery: Int = 70,
    maxRepeatable: Int = 5,
): List<String> {
    val known = priority.distinct()
    val rest = candidates.filter { it !in known }
    if (known.isEmpty() || known.size > maxRepeatable || repeatEvery <= 0) return rest

    return buildList {
        rest.forEachIndexed { index, url ->
            add(url)
            if ((index + 1) % repeatEvery == 0)
                addAll(known)
        }
    }
}

/**
 * Pure helper: turns the device's own IPv4 [ipv4Addresses] into the access point address
 * plus every `.1`–`.254` host of each distinct `/24` they belong to. This is what makes the
 * scan follow whatever network(s) the phone is actually on instead of one hard-coded subnet.
 */
fun buildSubnetUrls(ipv4Addresses: List<String>): List<String> {
    val bases = ipv4Addresses
        .map { it.substringBeforeLast('.') }
        .filter { base -> base.count { it == '.' } == 2 } // keep only well-formed a.b.c
        .distinct()
        .map { "http://$it." }
    val hosts = bases.flatMap { base -> (1..254).map { "$base$it/" } }
    return (listOf(ACCESS_POINT_URL) + hosts).distinct()
}

/**
 * Looks for a reachable VIEW device. The last-known-good [priority] addresses and the subnet
 * scan run **fully in parallel** — the known-good ones are launched on their own concurrency
 * budget so they're probed immediately, but nothing waits on them. This way a device that's
 * still where it was last seen connects almost instantly, while discovery after a Wi-Fi switch
 * (where the stale address only fails after a multi-second connect timeout) isn't held up.
 * Whichever address answers first wins; everything still in flight is cancelled.
 */
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun HttpClient.scanHosts(
    priority: List<String>,
    candidates: List<String>,
    concurrency: Int = 30,
    reprobeEvery: Int = 70,
): AddressResponse? = withContext(ioDispatcher) {
    val known = priority.distinct()

    val knownFlow = known.asFlow()
        .flatMapMerge(concurrency = concurrency) { probe(it) }
    val scanFlow = buildScanUrls(known, candidates, reprobeEvery).asFlow()
        .flatMapMerge(concurrency = concurrency) { probe(it) }

    merge(knownFlow, scanFlow).firstOrNull()
}

/** Single-list scan with no priority phase. */
suspend fun HttpClient.scanHosts(hosts: List<String>, concurrency: Int = 30): AddressResponse? =
    scanHosts(priority = emptyList(), candidates = hosts, concurrency = concurrency)

/** Probes one host; emits an [AddressResponse] only if it answers within [requestTimeout]. */
private fun HttpClient.probe(url: String, requestTimeout: Long = 3000L): Flow<AddressResponse> = flow {
    val response = runCatching {
        withTimeoutOrNull(requestTimeout) {
            get(url + "socket/address").body<AddressResponse?>()
        }
    }.getOrNull()
    if (response != null) emit(response.apply { fromUrl = url })
}
