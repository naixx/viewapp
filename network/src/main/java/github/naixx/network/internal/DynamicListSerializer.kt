package github.naixx.network.internal

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

open class DynamicListSerializer<T>(val baseSer: KSerializer<T>) : KSerializer<List<T>> {

    override val descriptor = ListSerializer(baseSer).descriptor

    override fun deserialize(decoder: Decoder): List<T> {
        return when (val element = decoder.decodeSerializableValue(JsonElement.Companion.serializer())) {
            is JsonArray -> Json.Default.decodeFromJsonElement(ListSerializer(baseSer), element)
            else         -> emptyList()
        }
    }

    override fun serialize(encoder: Encoder, value: List<T>) {
        encoder.encodeSerializableValue(ListSerializer(baseSer), value)
    }
}
