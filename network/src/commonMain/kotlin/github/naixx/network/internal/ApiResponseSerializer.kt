package github.naixx.network.internal

import github.naixx.network.AddressResponse
import io.ktor.http.cio.Response
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*

internal object AddressResponseSerializer : JsonContentPolymorphicSerializer<AddressResponse>(AddressResponse::class) {

    override fun selectDeserializer(element: JsonElement): KSerializer<out AddressResponse> {
        val json = element.jsonObject
        return when {
            json.containsKey("address") -> AddressResponse.Address.serializer()
            json.containsKey("action") -> AddressResponse.LoginRequired.serializer()
            else                        -> throw IllegalArgumentException("Unknown response format: $json")
        }
    }
}
