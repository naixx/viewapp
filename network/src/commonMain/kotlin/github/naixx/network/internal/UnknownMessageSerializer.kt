package github.naixx.network.internal

import github.naixx.network.BaseMessage
import github.naixx.network.UnknownMessage
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder

internal class UnknownMessageSerializer : KSerializer<BaseMessage> {

    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor(
        "AnyMessage",
        StructureKind.OBJECT
    )

    override fun serialize(encoder: Encoder, value: BaseMessage) {
        TODO("not supported")
    }

    override fun deserialize(decoder: Decoder): BaseMessage {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This serializer can only be used with JSON decoding")
        val jsonElement = jsonDecoder.decodeJsonElement()
        return UnknownMessage(jsonElement.toString())
    }
}
