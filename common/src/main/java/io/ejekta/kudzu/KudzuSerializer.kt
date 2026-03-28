package io.ejekta.kudzu

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject

object KudzuSerializer : KSerializer<KudzuVine> {

    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun serialize(encoder: Encoder, value: KudzuVine) {
        encoder.encodeSerializableValue(JsonObject.serializer(), value.toJsonObject())
    }

    override fun deserialize(decoder: Decoder): KudzuVine {
        return decoder.decodeSerializableValue(JsonObject.serializer()).toKudzu()
    }

}
