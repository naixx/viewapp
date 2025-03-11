package github.naixx.network

import de.jensklingenberg.ktorfit.http.*
import github.naixx.network.internal.AddressResponseSerializer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*

@Serializable
data class Auth(val session: String)

@Serializable(with = AddressResponseSerializer::class)
sealed class AddressResponse() : AddressResponse.Url {

    interface Url {

        var fromUrl: String?
    }

    @Serializable
    data class Address(val address: String, override var fromUrl: String? = null) : AddressResponse()

    @Serializable
    data class LoginRequired(val message: String, override var fromUrl: String? = null) : AddressResponse()
}

@Serializable
data class Clip(
    val index: Int, val id: Int, val frames: Int, val name: String,
    @SerialName("image") val imageBase64: String
)

interface ViewApi {

    @POST("api/login")
    suspend fun auth(@Body username: Any): Auth

    @GET("socket/address")
    suspend fun socketUrl(): AddressResponse

    @GET("clips")
    suspend fun clips(): List<Clip>

    @GET("download")
    suspend fun download(@Query file: String): ByteArray

    @GET
    suspend fun socketUrl(@Url url: String): AddressResponse
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun HttpClient.scanHosts(hosts: List<String>): AddressResponse? = withContext(Dispatchers.IO) {
    val requestTimeout = 3000L
    withTimeoutOrNull(requestTimeout * 255) {
        hosts.asFlow()
            .flatMapMerge(concurrency = 30) { url -> // Process 10 requests concurrently
                flow {
                    runCatching {
                        val response = withTimeoutOrNull(requestTimeout) {
                            get(url + "socket/address").body<AddressResponse?>()
                        }
                        when (response) {
                            is AddressResponse -> emit(response.apply { fromUrl = url })
                            null               -> emit(null)
                        }
                    }
                }
            }
            .firstOrNull { it != null }
    }
}
