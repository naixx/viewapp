import github.naixx.network.*
import github.naixx.network.AddressResponse.Address
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.websocket.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.*
import org.koin.core.context.*
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.test.*
import kotlin.test.*
import kotlin.test.Test
import kotlin.time.measureTime

val testModule = module {
    single<StorageProvider> {
        mockk<StorageProvider>(relaxed = true).apply {
            every { session() } returns System.getProperty("session")
        }
    }
}

//val URL = "https://app.view.tl/"
val URL = "http://192.168.31.21/"

//val URL = "http://10.0.0.1/"
class KtorTest : KoinTest {

    lateinit var api: ViewApi
    lateinit var json: Json

    @Before
    fun setup() {
        startKoin {
            modules(listOf(networkModule, testModule))
        }
        api = get<ViewApi>(parameters = { parametersOf(REMOTE_URL) })
        json = get()
    }

    @After
    fun close() {
        stopKoin()
    }

    @Test
    fun `test login`() = runTest {
        //secrets.properties
        val email = System.getProperty("email")
        val password = System.getProperty("password")

        val r = api.auth(mapOf("email" to email, "password" to password))

        assert(r.session.isNotEmpty())
    }

    @Test
    fun `get socket url`() = runTest {
        val r: AddressResponse

        println(measureTime {
            r = api.socketUrl()
        })
        assert(r is AddressResponse.Address)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `find first available server`() = runTest {
        val base = "http://192.168.31."
        val requestTimeout = 500L  // 1.5x average response time
        val result: Address?
        val dur = measureTime {
            result = withContext(Dispatchers.IO) {
                withTimeoutOrNull(30000) {
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
        }
        println(dur)
        assertNotNull(result, "No responsive server found in network")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `find first available server with api`() = runTest {
        val base = "http://192.168.31."
        val result: AddressResponse?
        val client = get<HttpClient>()
        val dur = measureTime {
            result = withContext(Dispatchers.IO) {
                withTimeoutOrNull(60000) {
                    val urls = (1..254).map { "$base$it/" }.toMutableList()
                    urls.add(128, REMOTE_URL)
                    client.scanHosts(urls)
                }
            }
        }
        println(dur)
        assertNotNull(result, "No responsive server found in network")
    }

    inline fun <reified T> typeTest(type: String): BaseMessage {
        val message = json.decodeFromString<BaseMessage>("{\"type\":\"$type\"}")
        println(message)
        assertIs<T>(message)
        return message
    }

    @Test
    fun `json decode`() {
        typeTest<NoDevice>("nodevice")
        typeTest<Pong>("pong")
        typeTest<UnknownMessage>("anystringtype")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `connect ws`() = runTest {
        val g = get<StorageProvider>()
        val webSocketClient: WebSocketClient = get()

        val messageReceived = CompletableDeferred<BaseMessage>()

        val webSocketJob = launch(Dispatchers.Default.limitedParallelism(1)) {
            webSocketClient.startWebSocket(listOf(ACCESS_POINT_URL + "socket/address"), { ses, state ->
                ses.sendSerialized(Session(g.session() ?: ""))
                ses.sendSerialized(Get("camera"))
                ses.sendSerialized(Ping())
            }) { message ->
                // Complete the deferred when any message is received
                if (!messageReceived.isCompleted) {
                    println("---->$message")
                    messageReceived.complete(message)
                }
            }
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

