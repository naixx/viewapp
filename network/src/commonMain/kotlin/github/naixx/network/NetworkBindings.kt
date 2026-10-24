package github.naixx.network

import com.github.naixx.logger.LL
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import github.naixx.network.internal.*
import io.ktor.client.HttpClient
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.appendIfNameAbsent
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.*

/** What the shells persist for the protocol layer: the relay session and credentials, last-known addresses. */
interface StorageProvider {

    fun session(): String?

    fun session(session: String)

    fun lastSuccessfulAddress(string: String)

    fun email(): String?

    fun password(): String?
}

/** The protocol's JSON: polymorphic on `type` with unknown types kept, lenient about what the device sends. */
fun createJson(): Json = Json {
    serializersModule = SerializersModule {
        polymorphic(BaseMessage::class) {
            defaultDeserializer {
                UnknownMessageSerializer()
            }
        }
    }
    isLenient = true
//            prettyPrint = true
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}

/** The one HTTP/WebSocket client: platform engine, JSON, timeouts, the `x-view-session` header from [storage]. */
fun createHttpClient(json: Json, storage: StorageProvider): HttpClient = platformHttpClient {
    //install(HttpCache)
    install(ContentNegotiation) {
        json(json)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 10000
        connectTimeoutMillis = 10000
        socketTimeoutMillis = 10000
    }
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(json)
        pingIntervalMillis = 1000
    }
    HttpResponseValidator {
        validateResponse { response ->
            val statusCode = response.status.value
            if (statusCode < 300) {
                return@validateResponse
            }
            val exceptionResponseText = try {
                response.bodyAsText()
            } catch (e: Throwable) {
                LL.e(e)
                "<failed to parse body>"
            }
//                    if (!response.status.isSuccess()) {
//                        val httpException = HttpException(
//                            response.status,
//                            try {
//                                (json.parseToJsonElement(exceptionResponseText) as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull
//                            } catch (e: Exception) {
//                                println(e)
//                                null
//                            }, response, exceptionResponseText
//                        )
//                        exceptionHandler(httpException)
//                        throw httpException
//                    }
        }
    }
//            install(Logging) {
//                logger = Logger.ANDROID
//                level = LogLevel.ALL
//            }
//                install(ResponseLoggingPlugin)
    //wtf why do we need to add content type if we installed negotiation??
    defaultRequest {
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)
        headers {
            storage.session()?.let {
                appendIfNameAbsent("x-view-session", it)
            }
        }
    }
}

/**
 * The protocol layer's bindings, merged into every shell's `@DependencyGraph(AppScope::class)`. The shells bind
 * [StorageProvider]; [ConnectionViewApi] and [WebSocketClient] contribute themselves.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object NetworkBindings {

    @Provides
    @SingleIn(AppScope::class)
    fun json(): Json = createJson()

    @Provides
    @SingleIn(AppScope::class)
    fun httpClient(json: Json, storage: StorageProvider): HttpClient = createHttpClient(json, storage)
}
