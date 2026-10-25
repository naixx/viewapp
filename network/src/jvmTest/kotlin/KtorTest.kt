import github.naixx.network.*
import github.naixx.network.AddressResponse.Address
import github.naixx.network.internal.KtorfitClientProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.websocket.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.*
import kotlin.test.*
import kotlin.test.Test
import kotlin.time.measureTime

/**
 * Live tests against a real VIEW ([WIFI_URL] / [ACCESS_POINT_URL]) and the remote relay ([REMOTE_URL]). Skipped
 * unless the build is run with `-Pargs.live=true` (or `live=true` in secrets.properties); the device must be on.
 */
class KtorTest {

    private val storage: StorageProvider = mockk<StorageProvider>(relaxed = true).apply {
        every { session() } returns System.getProperty("session")
    }
    private lateinit var http: HttpClient
    private lateinit var apis: KtorfitClientProvider
    private lateinit var api: ViewApi
    private lateinit var webSocketClient: WebSocketClient

    @Before
    fun setup() {
        Assume.assumeTrue("live tests: run with -Pargs.live=true and a VIEW on the network", System.getProperty("live") == "true")
        // The same objects the shells' graphs build.
        http = createHttpClient(createJson(), storage)
        apis = KtorfitClientProvider(http)
        api = apis.api(REMOTE_URL)
        webSocketClient = WebSocketClient(createJson(), http, storage)
    }

    @After
    fun close() {
        if (::http.isInitialized) http.close()
    }

    // Not runTest: its virtual clock fires Ktor's HttpTimeout at once, before any real response can arrive.
    private fun runLive(block: suspend CoroutineScope.() -> Unit) = runBlocking(Dispatchers.IO) { block() }

    @Test
    fun `test login`() = runLive {
        //secrets.properties
        val email = System.getProperty("email")
        val password = System.getProperty("password")

        val r = api.auth(mapOf("email" to email, "password" to password))

        assert(r.session.isNotEmpty())
    }

    @Test
    fun `get socket url`() = runLive {
        val r: AddressResponse

        println(measureTime {
            r = api.socketUrl()
        })
        assert(r is AddressResponse.Address)
    }

    @Test
    fun `validate that clip info can be deserialized`() = runLive {
        val api = apis.api(WIFI_URL)

        for (i in 101..329) {
            try {
                api.clipInfo("tL-$i")
            } catch (e: NoTransformationFoundException) {
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `find first available server`() = runLive {
        val base = "http://192.168.31."
        val requestTimeout = 500L  // 1.5x average response time
        val result: Address?
        val dur = measureTime {
            result = withTimeoutOrNull(30000) {
                (1..254).asFlow()
                    .flatMapMerge(concurrency = 50) { i ->
                        flow {
                            runCatching {
                                val response = withTimeoutOrNull(requestTimeout) {
                                    api.socketUrl("$base$i/socket/address")
                                } as? AddressResponse.Address?
                                emit(response?.takeIf { it.address.isNotEmpty() })
                            }
                        }
                    }
                    .firstOrNull { it != null }
            }
        }
        println(dur)
        assertNotNull(result, "No responsive server found in network")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `find first available server with api`() = runLive {
        val base = "http://192.168.31."
        val result: AddressResponse?
        val dur = measureTime {
            result = withTimeoutOrNull(60000) {
                val urls = (1..254).map { "$base$it/" }.toMutableList()
                urls.add(128, REMOTE_URL)
                http.scanHosts(urls)
            }
        }
        println(dur)
        assertNotNull(result, "No responsive server found in network")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `connect ws`() = runLive {
        val messageReceived = CompletableDeferred<BaseMessage>()
        val collector = webSocketClient.messages.onEach { messageReceived.complete(it) }.launchIn(this)

        val webSocketJob = launch(Dispatchers.Default.limitedParallelism(1)) {
            webSocketClient.startWebSocket(listOf(WIFI_URL, ACCESS_POINT_URL), emptyList())
        }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            withTimeout(15000) {
                val receivedMessage = messageReceived.await()
                println("Received message type: ${receivedMessage.type}")
                // Verify the received message
                assertNotNull(receivedMessage, "Should receive a non-null message")
            }
        }
        webSocketJob.cancel()
        collector.cancel()
    }
}

private suspend fun WebSocketSession.ping(salt: String) {
    outgoing.send(Frame.Text("text: $salt"))
    incoming.receive()
    outgoing.send(Frame.Text("text: $salt"))
    val frame = incoming.receive()
    check(frame is Frame.Text)

    assertEquals("text: $salt", frame.readText())
    val data = "text: $salt".toByteArray()
    outgoing.send(Frame.Binary(true, data))
    val binaryFrame = incoming.receive()
    check(binaryFrame is Frame.Binary)
    val buffer = binaryFrame.data
    assertEquals(data.contentToString(), buffer.contentToString())
}
