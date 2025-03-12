package github.naixx.network

import github.naixx.network.internal.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
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
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

enum class Qualifiers {
    DYNAMIC,
    STATIC
}

interface StorageProvider {

    fun session(): String?

    fun session(session: String)

    fun lastSuccessfulAddress(string: String)

    fun email(): String?

    fun password(): String?
}

val networkModule = module {
    singleOf(::KtorfitClientProvider)
    factory { (baseUrl: String) ->
        val ktorfit = get<KtorfitClientProvider>().getClient(baseUrl)
        ktorfit.create<ViewApi>()
    }
    single {
        Json {
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
        }
    }
    singleOf(::WebSocketClient)
    single {
        val auth = get<StorageProvider>()
        HttpClient(OkHttp) {
            engine {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            }
            //install(HttpCache)
            val json: Json = get()
            install(ContentNegotiation) {
                json(json)
            }
//            install(HttpTimeout) {
//                requestTimeoutMillis = get(qualifier(Net.TimeoutMillis))
//            }
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
                        println(e)
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
                    auth.session()?.let {
                        appendIfNameAbsent("x-view-session", it)
                    }
                }
            }
        }
    }
}
