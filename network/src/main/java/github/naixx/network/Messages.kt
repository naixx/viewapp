package github.naixx.network

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

@Serializable
sealed class BaseMessage {

    abstract val type: String
}

@Serializable
abstract class Message(override val type: String) : BaseMessage() {
}

@Serializable
class Ping() : Message("ping")

@Serializable
class RequestClips() : Message("timelapse-clips")

@Serializable
@SerialName("pong")
data class Pong(override val type: String) : BaseMessage()

@Serializable
@SerialName("battery")
data class Battery(val percentage: Float, val charging: Boolean, override val type: String) : BaseMessage()

@Serializable
@SerialName("nodevice")
data class NoDevice(override val type: String) : BaseMessage()

@Serializable
data class Get(val key: String) : Message("get")

@Serializable
data class Session(val session: String) : Message("auth")

@Serializable
data class UnknownMessage(val rawString: String) : Message("unknown")

@Serializable
@SerialName("camera")
data class ConnectedMessage(
    val connected: Boolean,
    val model: String,
    @Serializable(with = CameraFeaturesSerializer::class)
    val supports: CameraFeatures? = null,
    val ack: String? = null,
    override val type: String
) : BaseMessage()

@Serializable
@SerialName("settings")
data class SettingsMessage(
    val settings: CameraSettings,
    override val type: String
) : BaseMessage()

@Serializable
data class CameraFeatures(
    val shutter: Boolean = false,
    val aperture: Boolean = false,
    val iso: Boolean = false,
    val liveview: Boolean = false,
    val destination: Boolean = false,
    val focus: Boolean = false,
    val _bufTime: Int = 0,
    val newISO: Boolean = false
)

object CameraFeaturesSerializer : EmptyNullSerializer<CameraFeatures>(CameraFeatures.serializer())

open class EmptyNullSerializer<T>(val ser: KSerializer<T>) : KSerializer<T?> {

    override fun deserialize(decoder: Decoder): T? {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This class can be loaded only by Json")
        val jsonElement = jsonDecoder.decodeJsonElement()
        return if (jsonElement is JsonObject) {
            if (jsonElement.isEmpty()) {
                null
            } else {
                jsonDecoder.json.decodeFromJsonElement(ser, jsonElement)
            }
        } else {
            null
        }
    }

    override val descriptor: SerialDescriptor = ser.descriptor

    override fun serialize(encoder: Encoder, value: T?) {
        TODO("Not yet implemented")
    }
}

@Serializable
@SerialName("intervalometerStatus")
data class IntervalometerStatus(
    val status: Status,
    val ack: String? = null,
    override val type: String
) : BaseMessage() {

    @Serializable
    data class Status(
        val running: Boolean,
        val frames: Int,
        val framesRemaining: Int,
        val rampRate: Int,
        val intervalMs: Int,
        val message: String,
        val rampEv: Double?,
        val autoSettings: AutoSettings,
        val exposure: Exposure
    )

    @Serializable
    data class AutoSettings(
        val paddingTimeMs: Int
    )

    @Serializable
    data class Exposure(
        val status: Map<String, String>,
        val config: Map<String, String>
    )
}

@Serializable
@SerialName("motion")
data class MotionStatus(
    val nmxConnectedBt: Int,
    val gmConnectedBt: Int,
    val reload: Boolean,
    val motors: List<Motor>,
    override val type: String
) : BaseMessage() {

}

@Serializable
data class Motor(
    val driver: String,
    val motor: Int,
    val connected: Boolean? = null,
    val position: Int,
    val unit: String,
    val orientation: String?,
    val backlash: Int
)

@Serializable
@SerialName("timelapse-clips")
data class TimelapseClips(
    val clips: List<Clip>,
    override val type: String
) : BaseMessage() {
}

val connecJson =
    """{"connected":true,"model":"Sony A6600","supports":{"shutter":true,"aperture":true,"iso":true,"liveview":true,"destination":true,"focus":true,"_bufTime":3000,"newISO":false},"ack":"3sxtjgfdr0","type":"camera"}"""

val settingJson =
    """{"settings":{"shutter":"30s","aperture":"UNKNOWN","iso":"100","battery":72,"focusPos":0,"lists":{"shutter":[{"name":"30s","ev":-11,"code":19660810,"duration_ms":32000,"cameraName":"30s"},{"name":"25s","ev":-10.666666666666666,"code":16384010,"duration_ms":27000,"cameraName":"25s"},{"name":"20s","ev":-10.333333333333334,"code":13107210,"duration_ms":21500,"cameraName":"20s"},{"name":"15s","ev":-10,"code":9830410,"duration_ms":16000,"cameraName":"15s"},{"name":"13s","ev":-9.666666666666666,"code":8519690,"duration_ms":13800,"cameraName":"13s"},{"name":"10s","ev":-9.333333333333334,"code":6553610,"duration_ms":10600,"cameraName":"10s"},{"name":"8s","ev":-9,"code":5242890,"duration_ms":8000,"cameraName":"8s"},{"name":"6s","ev":-8.666666666666666,"code":3932170,"duration_ms":6000,"cameraName":"6s"},{"name":"5s","ev":-8.333333333333334,"code":3276810,"duration_ms":5000,"cameraName":"5s"},{"name":"4s","ev":-8,"code":2621450,"duration_ms":4000,"cameraName":"4s"},{"name":"3s","ev":-7.666666666666667,"code":2097162,"duration_ms":3000,"cameraName":"3s"},{"name":"2.5s","ev":-7.333333333333333,"code":1638410,"duration_ms":2500,"cameraName":"2.5s"},{"name":"2s","ev":-7,"code":1310730,"duration_ms":2000,"cameraName":"2s"},{"name":"1.6s","ev":-6.666666666666667,"code":1048586,"duration_ms":1600,"cameraName":"1.6s"},{"name":"1.3s","ev":-6.333333333333333,"code":851978,"duration_ms":1300,"cameraName":"1.3s"},{"name":"1s","ev":-6,"code":655370,"duration_ms":1000,"cameraName":"1s"},{"name":"0.8s","ev":-5.666666666666667,"code":524298,"duration_ms":800,"cameraName":"0.8s"},{"name":"0.6s","ev":-5.333333333333333,"code":393226,"duration_ms":600,"cameraName":"0.6s"},{"name":"1/2","ev":-5,"code":327690,"duration_ms":500,"cameraName":"1/2"},{"name":"0.4s","ev":-4.666666666666667,"code":262154,"duration_ms":400,"cameraName":"0.4s"},{"name":"1/3","ev":-4.333333333333333,"code":65539,"duration_ms":333,"cameraName":"1/3"},{"name":"1/4","ev":-4,"code":65540,"duration_ms":250,"cameraName":"1/4"},{"name":"1/5","ev":-3.6666666666666665,"code":65541,"duration_ms":200,"cameraName":"1/5"},{"name":"1/6","ev":-3.3333333333333335,"code":65542,"duration_ms":150,"cameraName":"1/6"},{"name":"1/8","ev":-3,"code":65544,"duration_ms":125,"cameraName":"1/8"},{"name":"1/10","ev":-2.6666666666666665,"code":65546,"duration_ms":100,"cameraName":"1/10"},{"name":"1/13","ev":-2.3333333333333335,"code":65549,"duration_ms":100,"cameraName":"1/13"},{"name":"1/15","ev":-2,"code":65551,"duration_ms":100,"cameraName":"1/15"},{"name":"1/20","ev":-1.6666666666666665,"code":65556,"duration_ms":100,"cameraName":"1/20"},{"name":"1/25","ev":-1.3333333333333333,"code":65561,"duration_ms":100,"cameraName":"1/25"},{"name":"1/30","ev":-1,"code":65566,"duration_ms":100,"cameraName":"1/30"},{"name":"1/40","ev":-0.6666666666666666,"code":65576,"duration_ms":100,"cameraName":"1/40"},{"name":"1/50","ev":-0.3333333333333333,"code":65586,"duration_ms":100,"cameraName":"1/50"},{"name":"1/60","ev":0,"code":65596,"duration_ms":100,"cameraName":"1/60"},{"name":"1/80","ev":0.3333333333333333,"code":65616,"duration_ms":100,"cameraName":"1/80"},{"name":"1/100","ev":0.6666666666666666,"code":65636,"duration_ms":100,"cameraName":"1/100"},{"name":"1/125","ev":1,"code":65661,"duration_ms":100,"cameraName":"1/125"},{"name":"1/160","ev":1.3333333333333333,"code":65696,"duration_ms":100,"cameraName":"1/160"},{"name":"1/200","ev":1.6666666666666665,"code":65736,"duration_ms":100,"cameraName":"1/200"},{"name":"1/250","ev":2,"code":65786,"duration_ms":100,"cameraName":"1/250"},{"name":"1/320","ev":2.3333333333333335,"code":65856,"duration_ms":100,"cameraName":"1/320"},{"name":"1/400","ev":2.6666666666666665,"code":65936,"duration_ms":100,"cameraName":"1/400"},{"name":"1/500","ev":3,"code":66036,"duration_ms":100,"cameraName":"1/500"},{"name":"1/640","ev":3.3333333333333335,"code":66176,"duration_ms":100,"cameraName":"1/640"},{"name":"1/800","ev":3.6666666666666665,"code":66336,"duration_ms":100,"cameraName":"1/800"},{"name":"1/1000","ev":4,"code":66536,"duration_ms":100,"cameraName":"1/1000"},{"name":"1/1250","ev":4.333333333333333,"code":66786,"duration_ms":100,"cameraName":"1/1250"},{"name":"1/1600","ev":4.666666666666667,"code":67136,"duration_ms":100,"cameraName":"1/1600"},{"name":"1/2000","ev":5,"code":67536,"duration_ms":100,"cameraName":"1/2000"},{"name":"1/2500","ev":5.333333333333333,"code":68036,"duration_ms":100,"cameraName":"1/2500"},{"name":"1/3200","ev":5.666666666666667,"code":68736,"duration_ms":100,"cameraName":"1/3200"},{"name":"1/4000","ev":6,"code":69536,"duration_ms":100,"cameraName":"1/4000"},{"name":"1/5000","ev":6.333333333333333,"code":70536,"duration_ms":100,"cameraName":"1/5000"},{"name":"1/6400","ev":6.666666666666667,"code":71936,"duration_ms":100,"cameraName":"1/6400"},{"name":"1/8000","ev":7,"code":73536,"duration_ms":100,"cameraName":"1/8000"}],"aperture":[{"name":"1.0","ev":-8,"code":100,"cameraName":"1.0"},{"name":"1.1","ev":-7.666666666666667,"code":110,"cameraName":"1.1"},{"name":"1.2","ev":-7.333333333333333,"code":120,"cameraName":"1.2"},{"name":"1.4","ev":-7,"code":140,"cameraName":"1.4"},{"name":"1.6","ev":-6.666666666666667,"code":160,"cameraName":"1.6"},{"name":"1.8","ev":-6.333333333333333,"code":180,"cameraName":"1.8"},{"name":"2.0","ev":-6,"code":200,"cameraName":"2.0"},{"name":"2.2","ev":-5.666666666666667,"code":220,"cameraName":"2.2"},{"name":"2.5","ev":-5.333333333333333,"code":250,"cameraName":"2.5"},{"name":"2.8","ev":-5,"code":280,"cameraName":"2.8"},{"name":"3.2","ev":-4.666666666666667,"code":320,"cameraName":"3.2"},{"name":"3.5","ev":-4.333333333333333,"code":350,"cameraName":"3.5"},{"name":"4.0","ev":-4,"code":400,"cameraName":"4.0"},{"name":"4.5","ev":-3.6666666666666665,"code":450,"cameraName":"4.5"},{"name":"5.0","ev":-3.3333333333333335,"code":500,"cameraName":"5.0"},{"name":"5.6","ev":-3,"code":560,"cameraName":"5.6"},{"name":"6.3","ev":-2.6666666666666665,"code":630,"cameraName":"6.3"},{"name":"7.1","ev":-2.3333333333333335,"code":710,"cameraName":"7.1"},{"name":"8","ev":-2,"code":800,"cameraName":"8"},{"name":"9","ev":-1.6666666666666665,"code":900,"cameraName":"9"},{"name":"10","ev":-1.3333333333333333,"code":1000,"cameraName":"10"},{"name":"11","ev":-1,"code":1100,"cameraName":"11"},{"name":"13","ev":-0.6666666666666666,"code":1300,"cameraName":"13"},{"name":"14","ev":-0.3333333333333333,"code":1400,"cameraName":"14"},{"name":"16","ev":0,"code":1600,"cameraName":"16"},{"name":"18","ev":0.3333333333333333,"code":1800,"cameraName":"18"},{"name":"20","ev":0.6666666666666666,"code":2000,"cameraName":"20"},{"name":"22","ev":1,"code":2200,"cameraName":"22"},{"name":"25","ev":2.3333333333333335,"code":2500,"cameraName":"25"},{"name":"29","ev":2.6666666666666665,"code":2900,"cameraName":"29"},{"name":"32","ev":3,"code":3200,"cameraName":"32"},{"name":"36","ev":3.3333333333333335,"code":3600,"cameraName":"36"},{"name":"42","ev":3.6666666666666665,"code":4200,"cameraName":"42"},{"name":"45","ev":4,"code":4500,"cameraName":"45"},{"name":"50","ev":4.333333333333333,"code":5000,"cameraName":"50"},{"name":"57","ev":4.666666666666667,"code":5700,"cameraName":"57"},{"name":"64","ev":5,"code":6400,"cameraName":"64"}],"iso":[{"name":"AUTO","ev":null,"code":16777215,"cameraName":"AUTO"},{"name":"UNKNOWN (25)","ev":null,"value":null,"code":25,"cameraName":"UNKNOWN (25)"},{"name":"UNKNOWN (50)","ev":null,"value":null,"code":50,"cameraName":"UNKNOWN (50)"},{"name":"UNKNOWN (64)","ev":null,"value":null,"code":64,"cameraName":"UNKNOWN (64)"},{"name":"UNKNOWN (80)","ev":null,"value":null,"code":80,"cameraName":"UNKNOWN (80)"},{"name":"100","ev":0,"code":100,"cameraName":"100"},{"name":"125","ev":-0.3333333333333333,"code":125,"cameraName":"125"},{"name":"160","ev":-0.6666666666666666,"code":160,"cameraName":"160"},{"name":"200","ev":-1,"code":200,"cameraName":"200"},{"name":"250","ev":-1.3333333333333333,"code":250,"cameraName":"250"},{"name":"320","ev":-1.6666666666666665,"code":320,"cameraName":"320"},{"name":"400","ev":-2,"code":400,"cameraName":"400"},{"name":"500","ev":-2.3333333333333335,"code":500,"cameraName":"500"},{"name":"640","ev":-2.6666666666666665,"code":640,"cameraName":"640"},{"name":"800","ev":-3,"code":800,"cameraName":"800"},{"name":"1000","ev":-3.3333333333333335,"code":1000,"cameraName":"1000"},{"name":"1250","ev":-3.6666666666666665,"code":1250,"cameraName":"1250"},{"name":"1600","ev":-4,"code":1600,"cameraName":"1600"},{"name":"2000","ev":-4.333333333333333,"code":2000,"cameraName":"2000"},{"name":"2500","ev":-4.666666666666667,"code":2500,"cameraName":"2500"},{"name":"3200","ev":-5,"code":3200,"cameraName":"3200"},{"name":"4000","ev":-5.333333333333333,"code":4000,"cameraName":"4000"},{"name":"5000","ev":-5.666666666666667,"code":5000,"cameraName":"5000"},{"name":"6400","ev":-6,"code":6400,"cameraName":"6400"},{"name":"8000","ev":-6.333333333333333,"code":8000,"cameraName":"8000"},{"name":"10000","ev":-6.666666666666667,"code":10000,"cameraName":"10000"},{"name":"12800","ev":-7,"code":12800,"cameraName":"12800"},{"name":"16000","ev":-7.333333333333333,"code":16000,"cameraName":"16000"},{"name":"20000","ev":-7.666666666666667,"code":20000,"cameraName":"20000"},{"name":"25600","ev":-8,"code":25600,"cameraName":"25600"},{"name":"32000","ev":-8.333333333333334,"code":32000,"cameraName":"32000"},{"name":"40000","ev":-8.666666666666666,"code":40000,"cameraName":"40000"},{"name":"51200","ev":-9,"code":51200,"cameraName":"51200"},{"name":"64000","ev":-9.333333333333334,"code":64000,"cameraName":"64000"},{"name":"80000","ev":-9.666666666666666,"code":80000,"cameraName":"80000"},{"name":"102400","ev":-10,"code":102400,"cameraName":"102400"},{"name":"128000","ev":-10.333333333333334,"code":128000,"cameraName":"128000"},{"name":"160000","ev":-10.666666666666666,"code":160000,"cameraName":"160000"},{"name":"204800","ev":-11,"code":204800,"cameraName":"204800"},{"name":"UNKNOWN (256000)","ev":null,"value":null,"code":256000,"cameraName":"UNKNOWN (256000)"},{"name":"UNKNOWN (320000)","ev":null,"value":null,"code":320000,"cameraName":"UNKNOWN (320000)"},{"name":"UNKNOWN (409600)","ev":null,"value":null,"code":409600,"cameraName":"UNKNOWN (409600)"},{"name":"UNKNOWN (33554431)","ev":null,"value":null,"code":33554431,"cameraName":"UNKNOWN (33554431)"},{"name":"UNKNOWN (16777241)","ev":null,"value":null,"code":16777241,"cameraName":"UNKNOWN (16777241)"},{"name":"UNKNOWN (16777266)","ev":null,"value":null,"code":16777266,"cameraName":"UNKNOWN (16777266)"},{"name":"UNKNOWN (16777280)","ev":null,"value":null,"code":16777280,"cameraName":"UNKNOWN (16777280)"},{"name":"UNKNOWN (16777296)","ev":null,"value":null,"code":16777296,"cameraName":"UNKNOWN (16777296)"},{"name":"UNKNOWN (16777316)","ev":null,"value":null,"code":16777316,"cameraName":"UNKNOWN (16777316)"},{"name":"UNKNOWN (16777341)","ev":null,"value":null,"code":16777341,"cameraName":"UNKNOWN (16777341)"},{"name":"UNKNOWN (16777376)","ev":null,"value":null,"code":16777376,"cameraName":"UNKNOWN (16777376)"},{"name":"UNKNOWN (16777416)","ev":null,"value":null,"code":16777416,"cameraName":"UNKNOWN (16777416)"},{"name":"UNKNOWN (16777466)","ev":null,"value":null,"code":16777466,"cameraName":"UNKNOWN (16777466)"},{"name":"UNKNOWN (16777536)","ev":null,"value":null,"code":16777536,"cameraName":"UNKNOWN (16777536)"},{"name":"UNKNOWN (16777616)","ev":null,"value":null,"code":16777616,"cameraName":"UNKNOWN (16777616)"},{"name":"UNKNOWN (16777716)","ev":null,"value":null,"code":16777716,"cameraName":"UNKNOWN (16777716)"},{"name":"UNKNOWN (16777856)","ev":null,"value":null,"code":16777856,"cameraName":"UNKNOWN (16777856)"},{"name":"UNKNOWN (16778016)","ev":null,"value":null,"code":16778016,"cameraName":"UNKNOWN (16778016)"},{"name":"UNKNOWN (16778216)","ev":null,"value":null,"code":16778216,"cameraName":"UNKNOWN (16778216)"},{"name":"UNKNOWN (16778466)","ev":null,"value":null,"code":16778466,"cameraName":"UNKNOWN (16778466)"},{"name":"UNKNOWN (16778816)","ev":null,"value":null,"code":16778816,"cameraName":"UNKNOWN (16778816)"},{"name":"UNKNOWN (16779216)","ev":null,"value":null,"code":16779216,"cameraName":"UNKNOWN (16779216)"},{"name":"UNKNOWN (16779716)","ev":null,"value":null,"code":16779716,"cameraName":"UNKNOWN (16779716)"},{"name":"UNKNOWN (16780416)","ev":null,"value":null,"code":16780416,"cameraName":"UNKNOWN (16780416)"},{"name":"UNKNOWN (16781216)","ev":null,"value":null,"code":16781216,"cameraName":"UNKNOWN (16781216)"},{"name":"UNKNOWN (16782216)","ev":null,"value":null,"code":16782216,"cameraName":"UNKNOWN (16782216)"},{"name":"UNKNOWN (16783616)","ev":null,"value":null,"code":16783616,"cameraName":"UNKNOWN (16783616)"},{"name":"UNKNOWN (16785216)","ev":null,"value":null,"code":16785216,"cameraName":"UNKNOWN (16785216)"},{"name":"UNKNOWN (16787216)","ev":null,"value":null,"code":16787216,"cameraName":"UNKNOWN (16787216)"},{"name":"UNKNOWN (16790016)","ev":null,"value":null,"code":16790016,"cameraName":"UNKNOWN (16790016)"},{"name":"UNKNOWN (16793216)","ev":null,"value":null,"code":16793216,"cameraName":"UNKNOWN (16793216)"},{"name":"UNKNOWN (16802816)","ev":null,"value":null,"code":16802816,"cameraName":"UNKNOWN (16802816)"},{"name":"UNKNOWN (16828416)","ev":null,"value":null,"code":16828416,"cameraName":"UNKNOWN (16828416)"},{"name":"UNKNOWN (16879616)","ev":null,"value":null,"code":16879616,"cameraName":"UNKNOWN (16879616)"},{"name":"UNKNOWN (16982016)","ev":null,"value":null,"code":16982016,"cameraName":"UNKNOWN (16982016)"},{"name":"UNKNOWN (17186816)","ev":null,"value":null,"code":17186816,"cameraName":"UNKNOWN (17186816)"},{"name":"UNKNOWN (50331647)","ev":null,"value":null,"code":50331647,"cameraName":"UNKNOWN (50331647)"},{"name":"UNKNOWN (33554532)","ev":null,"value":null,"code":33554532,"cameraName":"UNKNOWN (33554532)"},{"name":"UNKNOWN (33554632)","ev":null,"value":null,"code":33554632,"cameraName":"UNKNOWN (33554632)"},{"name":"UNKNOWN (33554832)","ev":null,"value":null,"code":33554832,"cameraName":"UNKNOWN (33554832)"},{"name":"UNKNOWN (33555232)","ev":null,"value":null,"code":33555232,"cameraName":"UNKNOWN (33555232)"},{"name":"UNKNOWN (33556032)","ev":null,"value":null,"code":33556032,"cameraName":"UNKNOWN (33556032)"},{"name":"UNKNOWN (33557632)","ev":null,"value":null,"code":33557632,"cameraName":"UNKNOWN (33557632)"},{"name":"UNKNOWN (33560832)","ev":null,"value":null,"code":33560832,"cameraName":"UNKNOWN (33560832)"},{"name":"UNKNOWN (33567232)","ev":null,"value":null,"code":33567232,"cameraName":"UNKNOWN (33567232)"},{"name":"UNKNOWN (33580032)","ev":null,"value":null,"code":33580032,"cameraName":"UNKNOWN (33580032)"},{"name":"UNKNOWN (33605632)","ev":null,"value":null,"code":33605632,"cameraName":"UNKNOWN (33605632)"},{"name":"UNKNOWN (33656832)","ev":null,"value":null,"code":33656832,"cameraName":"UNKNOWN (33656832)"},{"name":"UNKNOWN (33759232)","ev":null,"value":null,"code":33759232,"cameraName":"UNKNOWN (33759232)"},{"name":"UNKNOWN (33964032)","ev":null,"value":null,"code":33964032,"cameraName":"UNKNOWN (33964032)"}]},"details":{"shutter":{"name":"30s","ev":-11,"code":19660810,"duration_ms":32000,"cameraName":"30s","list":[{"name":"30s","ev":-11,"code":19660810,"duration_ms":32000,"cameraName":"30s"},{"name":"25s","ev":-10.666666666666666,"code":16384010,"duration_ms":27000,"cameraName":"25s"},{"name":"20s","ev":-10.333333333333334,"code":13107210,"duration_ms":21500,"cameraName":"20s"},{"name":"15s","ev":-10,"code":9830410,"duration_ms":16000,"cameraName":"15s"},{"name":"13s","ev":-9.666666666666666,"code":8519690,"duration_ms":13800,"cameraName":"13s"},{"name":"10s","ev":-9.333333333333334,"code":6553610,"duration_ms":10600,"cameraName":"10s"},{"name":"8s","ev":-9,"code":5242890,"duration_ms":8000,"cameraName":"8s"},{"name":"6s","ev":-8.666666666666666,"code":3932170,"duration_ms":6000,"cameraName":"6s"},{"name":"5s","ev":-8.333333333333334,"code":3276810,"duration_ms":5000,"cameraName":"5s"},{"name":"4s","ev":-8,"code":2621450,"duration_ms":4000,"cameraName":"4s"},{"name":"3s","ev":-7.666666666666667,"code":2097162,"duration_ms":3000,"cameraName":"3s"},{"name":"2.5s","ev":-7.333333333333333,"code":1638410,"duration_ms":2500,"cameraName":"2.5s"},{"name":"2s","ev":-7,"code":1310730,"duration_ms":2000,"cameraName":"2s"},{"name":"1.6s","ev":-6.666666666666667,"code":1048586,"duration_ms":1600,"cameraName":"1.6s"},{"name":"1.3s","ev":-6.333333333333333,"code":851978,"duration_ms":1300,"cameraName":"1.3s"},{"name":"1s","ev":-6,"code":655370,"duration_ms":1000,"cameraName":"1s"},{"name":"0.8s","ev":-5.666666666666667,"code":524298,"duration_ms":800,"cameraName":"0.8s"},{"name":"0.6s","ev":-5.333333333333333,"code":393226,"duration_ms":600,"cameraName":"0.6s"},{"name":"1/2","ev":-5,"code":327690,"duration_ms":500,"cameraName":"1/2"},{"name":"0.4s","ev":-4.666666666666667,"code":262154,"duration_ms":400,"cameraName":"0.4s"},{"name":"1/3","ev":-4.333333333333333,"code":65539,"duration_ms":333,"cameraName":"1/3"},{"name":"1/4","ev":-4,"code":65540,"duration_ms":250,"cameraName":"1/4"},{"name":"1/5","ev":-3.6666666666666665,"code":65541,"duration_ms":200,"cameraName":"1/5"},{"name":"1/6","ev":-3.3333333333333335,"code":65542,"duration_ms":150,"cameraName":"1/6"},{"name":"1/8","ev":-3,"code":65544,"duration_ms":125,"cameraName":"1/8"},{"name":"1/10","ev":-2.6666666666666665,"code":65546,"duration_ms":100,"cameraName":"1/10"},{"name":"1/13","ev":-2.3333333333333335,"code":65549,"duration_ms":100,"cameraName":"1/13"},{"name":"1/15","ev":-2,"code":65551,"duration_ms":100,"cameraName":"1/15"},{"name":"1/20","ev":-1.6666666666666665,"code":65556,"duration_ms":100,"cameraName":"1/20"},{"name":"1/25","ev":-1.3333333333333333,"code":65561,"duration_ms":100,"cameraName":"1/25"},{"name":"1/30","ev":-1,"code":65566,"duration_ms":100,"cameraName":"1/30"},{"name":"1/40","ev":-0.6666666666666666,"code":65576,"duration_ms":100,"cameraName":"1/40"},{"name":"1/50","ev":-0.3333333333333333,"code":65586,"duration_ms":100,"cameraName":"1/50"},{"name":"1/60","ev":0,"code":65596,"duration_ms":100,"cameraName":"1/60"},{"name":"1/80","ev":0.3333333333333333,"code":65616,"duration_ms":100,"cameraName":"1/80"},{"name":"1/100","ev":0.6666666666666666,"code":65636,"duration_ms":100,"cameraName":"1/100"},{"name":"1/125","ev":1,"code":65661,"duration_ms":100,"cameraName":"1/125"},{"name":"1/160","ev":1.3333333333333333,"code":65696,"duration_ms":100,"cameraName":"1/160"},{"name":"1/200","ev":1.6666666666666665,"code":65736,"duration_ms":100,"cameraName":"1/200"},{"name":"1/250","ev":2,"code":65786,"duration_ms":100,"cameraName":"1/250"},{"name":"1/320","ev":2.3333333333333335,"code":65856,"duration_ms":100,"cameraName":"1/320"},{"name":"1/400","ev":2.6666666666666665,"code":65936,"duration_ms":100,"cameraName":"1/400"},{"name":"1/500","ev":3,"code":66036,"duration_ms":100,"cameraName":"1/500"},{"name":"1/640","ev":3.3333333333333335,"code":66176,"duration_ms":100,"cameraName":"1/640"},{"name":"1/800","ev":3.6666666666666665,"code":66336,"duration_ms":100,"cameraName":"1/800"},{"name":"1/1000","ev":4,"code":66536,"duration_ms":100,"cameraName":"1/1000"},{"name":"1/1250","ev":4.333333333333333,"code":66786,"duration_ms":100,"cameraName":"1/1250"},{"name":"1/1600","ev":4.666666666666667,"code":67136,"duration_ms":100,"cameraName":"1/1600"},{"name":"1/2000","ev":5,"code":67536,"duration_ms":100,"cameraName":"1/2000"},{"name":"1/2500","ev":5.333333333333333,"code":68036,"duration_ms":100,"cameraName":"1/2500"},{"name":"1/3200","ev":5.666666666666667,"code":68736,"duration_ms":100,"cameraName":"1/3200"},{"name":"1/4000","ev":6,"code":69536,"duration_ms":100,"cameraName":"1/4000"},{"name":"1/5000","ev":6.333333333333333,"code":70536,"duration_ms":100,"cameraName":"1/5000"},{"name":"1/6400","ev":6.666666666666667,"code":71936,"duration_ms":100,"cameraName":"1/6400"},{"name":"1/8000","ev":7,"code":73536,"duration_ms":100,"cameraName":"1/8000"}]},"aperture":{"name":"UNKNOWN","ev":null,"value":null,"code":0,"list":[{"name":"1.0","ev":-8,"code":100,"cameraName":"1.0"},{"name":"1.1","ev":-7.666666666666667,"code":110,"cameraName":"1.1"},{"name":"1.2","ev":-7.333333333333333,"code":120,"cameraName":"1.2"},{"name":"1.4","ev":-7,"code":140,"cameraName":"1.4"},{"name":"1.6","ev":-6.666666666666667,"code":160,"cameraName":"1.6"},{"name":"1.8","ev":-6.333333333333333,"code":180,"cameraName":"1.8"},{"name":"2.0","ev":-6,"code":200,"cameraName":"2.0"},{"name":"2.2","ev":-5.666666666666667,"code":220,"cameraName":"2.2"},{"name":"2.5","ev":-5.333333333333333,"code":250,"cameraName":"2.5"},{"name":"2.8","ev":-5,"code":280,"cameraName":"2.8"},{"name":"3.2","ev":-4.666666666666667,"code":320,"cameraName":"3.2"},{"name":"3.5","ev":-4.333333333333333,"code":350,"cameraName":"3.5"},{"name":"4.0","ev":-4,"code":400,"cameraName":"4.0"},{"name":"4.5","ev":-3.6666666666666665,"code":450,"cameraName":"4.5"},{"name":"5.0","ev":-3.3333333333333335,"code":500,"cameraName":"5.0"},{"name":"5.6","ev":-3,"code":560,"cameraName":"5.6"},{"name":"6.3","ev":-2.6666666666666665,"code":630,"cameraName":"6.3"},{"name":"7.1","ev":-2.3333333333333335,"code":710,"cameraName":"7.1"},{"name":"8","ev":-2,"code":800,"cameraName":"8"},{"name":"9","ev":-1.6666666666666665,"code":900,"cameraName":"9"},{"name":"10","ev":-1.3333333333333333,"code":1000,"cameraName":"10"},{"name":"11","ev":-1,"code":1100,"cameraName":"11"},{"name":"13","ev":-0.6666666666666666,"code":1300,"cameraName":"13"},{"name":"14","ev":-0.3333333333333333,"code":1400,"cameraName":"14"},{"name":"16","ev":0,"code":1600,"cameraName":"16"},{"name":"18","ev":0.3333333333333333,"code":1800,"cameraName":"18"},{"name":"20","ev":0.6666666666666666,"code":2000,"cameraName":"20"},{"name":"22","ev":1,"code":2200,"cameraName":"22"},{"name":"25","ev":2.3333333333333335,"code":2500,"cameraName":"25"},{"name":"29","ev":2.6666666666666665,"code":2900,"cameraName":"29"},{"name":"32","ev":3,"code":3200,"cameraName":"32"},{"name":"36","ev":3.3333333333333335,"code":3600,"cameraName":"36"},{"name":"42","ev":3.6666666666666665,"code":4200,"cameraName":"42"},{"name":"45","ev":4,"code":4500,"cameraName":"45"},{"name":"50","ev":4.333333333333333,"code":5000,"cameraName":"50"},{"name":"57","ev":4.666666666666667,"code":5700,"cameraName":"57"},{"name":"64","ev":5,"code":6400,"cameraName":"64"}]},"iso":{"name":"100","ev":0,"code":100,"cameraName":"100","list":[{"name":"AUTO","ev":null,"code":16777215,"cameraName":"AUTO"},{"name":"UNKNOWN (25)","ev":null,"value":null,"code":25,"cameraName":"UNKNOWN (25)"},{"name":"UNKNOWN (50)","ev":null,"value":null,"code":50,"cameraName":"UNKNOWN (50)"},{"name":"UNKNOWN (64)","ev":null,"value":null,"code":64,"cameraName":"UNKNOWN (64)"},{"name":"UNKNOWN (80)","ev":null,"value":null,"code":80,"cameraName":"UNKNOWN (80)"},{"name":"100","ev":0,"code":100,"cameraName":"100"},{"name":"125","ev":-0.3333333333333333,"code":125,"cameraName":"125"},{"name":"160","ev":-0.6666666666666666,"code":160,"cameraName":"160"},{"name":"200","ev":-1,"code":200,"cameraName":"200"},{"name":"250","ev":-1.3333333333333333,"code":250,"cameraName":"250"},{"name":"320","ev":-1.6666666666666665,"code":320,"cameraName":"320"},{"name":"400","ev":-2,"code":400,"cameraName":"400"},{"name":"500","ev":-2.3333333333333335,"code":500,"cameraName":"500"},{"name":"640","ev":-2.6666666666666665,"code":640,"cameraName":"640"},{"name":"800","ev":-3,"code":800,"cameraName":"800"},{"name":"1000","ev":-3.3333333333333335,"code":1000,"cameraName":"1000"},{"name":"1250","ev":-3.6666666666666665,"code":1250,"cameraName":"1250"},{"name":"1600","ev":-4,"code":1600,"cameraName":"1600"},{"name":"2000","ev":-4.333333333333333,"code":2000,"cameraName":"2000"},{"name":"2500","ev":-4.666666666666667,"code":2500,"cameraName":"2500"},{"name":"3200","ev":-5,"code":3200,"cameraName":"3200"},{"name":"4000","ev":-5.333333333333333,"code":4000,"cameraName":"4000"},{"name":"5000","ev":-5.666666666666667,"code":5000,"cameraName":"5000"},{"name":"6400","ev":-6,"code":6400,"cameraName":"6400"},{"name":"8000","ev":-6.333333333333333,"code":8000,"cameraName":"8000"},{"name":"10000","ev":-6.666666666666667,"code":10000,"cameraName":"10000"},{"name":"12800","ev":-7,"code":12800,"cameraName":"12800"},{"name":"16000","ev":-7.333333333333333,"code":16000,"cameraName":"16000"},{"name":"20000","ev":-7.666666666666667,"code":20000,"cameraName":"20000"},{"name":"25600","ev":-8,"code":25600,"cameraName":"25600"},{"name":"32000","ev":-8.333333333333334,"code":32000,"cameraName":"32000"},{"name":"40000","ev":-8.666666666666666,"code":40000,"cameraName":"40000"},{"name":"51200","ev":-9,"code":51200,"cameraName":"51200"},{"name":"64000","ev":-9.333333333333334,"code":64000,"cameraName":"64000"},{"name":"80000","ev":-9.666666666666666,"code":80000,"cameraName":"80000"},{"name":"102400","ev":-10,"code":102400,"cameraName":"102400"},{"name":"128000","ev":-10.333333333333334,"code":128000,"cameraName":"128000"},{"name":"160000","ev":-10.666666666666666,"code":160000,"cameraName":"160000"},{"name":"204800","ev":-11,"code":204800,"cameraName":"204800"},{"name":"UNKNOWN (256000)","ev":null,"value":null,"code":256000,"cameraName":"UNKNOWN (256000)"},{"name":"UNKNOWN (320000)","ev":null,"value":null,"code":320000,"cameraName":"UNKNOWN (320000)"},{"name":"UNKNOWN (409600)","ev":null,"value":null,"code":409600,"cameraName":"UNKNOWN (409600)"},{"name":"UNKNOWN (33554431)","ev":null,"value":null,"code":33554431,"cameraName":"UNKNOWN (33554431)"},{"name":"UNKNOWN (16777241)","ev":null,"value":null,"code":16777241,"cameraName":"UNKNOWN (16777241)"},{"name":"UNKNOWN (16777266)","ev":null,"value":null,"code":16777266,"cameraName":"UNKNOWN (16777266)"},{"name":"UNKNOWN (16777280)","ev":null,"value":null,"code":16777280,"cameraName":"UNKNOWN (16777280)"},{"name":"UNKNOWN (16777296)","ev":null,"value":null,"code":16777296,"cameraName":"UNKNOWN (16777296)"},{"name":"UNKNOWN (16777316)","ev":null,"value":null,"code":16777316,"cameraName":"UNKNOWN (16777316)"},{"name":"UNKNOWN (16777341)","ev":null,"value":null,"code":16777341,"cameraName":"UNKNOWN (16777341)"},{"name":"UNKNOWN (16777376)","ev":null,"value":null,"code":16777376,"cameraName":"UNKNOWN (16777376)"},{"name":"UNKNOWN (16777416)","ev":null,"value":null,"code":16777416,"cameraName":"UNKNOWN (16777416)"},{"name":"UNKNOWN (16777466)","ev":null,"value":null,"code":16777466,"cameraName":"UNKNOWN (16777466)"},{"name":"UNKNOWN (16777536)","ev":null,"value":null,"code":16777536,"cameraName":"UNKNOWN (16777536)"},{"name":"UNKNOWN (16777616)","ev":null,"value":null,"code":16777616,"cameraName":"UNKNOWN (16777616)"},{"name":"UNKNOWN (16777716)","ev":null,"value":null,"code":16777716,"cameraName":"UNKNOWN (16777716)"},{"name":"UNKNOWN (16777856)","ev":null,"value":null,"code":16777856,"cameraName":"UNKNOWN (16777856)"},{"name":"UNKNOWN (16778016)","ev":null,"value":null,"code":16778016,"cameraName":"UNKNOWN (16778016)"},{"name":"UNKNOWN (16778216)","ev":null,"value":null,"code":16778216,"cameraName":"UNKNOWN (16778216)"},{"name":"UNKNOWN (16778466)","ev":null,"value":null,"code":16778466,"cameraName":"UNKNOWN (16778466)"},{"name":"UNKNOWN (16778816)","ev":null,"value":null,"code":16778816,"cameraName":"UNKNOWN (16778816)"},{"name":"UNKNOWN (16779216)","ev":null,"value":null,"code":16779216,"cameraName":"UNKNOWN (16779216)"},{"name":"UNKNOWN (16779716)","ev":null,"value":null,"code":16779716,"cameraName":"UNKNOWN (16779716)"},{"name":"UNKNOWN (16780416)","ev":null,"value":null,"code":16780416,"cameraName":"UNKNOWN (16780416)"},{"name":"UNKNOWN (16781216)","ev":null,"value":null,"code":16781216,"cameraName":"UNKNOWN (16781216)"},{"name":"UNKNOWN (16782216)","ev":null,"value":null,"code":16782216,"cameraName":"UNKNOWN (16782216)"},{"name":"UNKNOWN (16783616)","ev":null,"value":null,"code":16783616,"cameraName":"UNKNOWN (16783616)"},{"name":"UNKNOWN (16785216)","ev":null,"value":null,"code":16785216,"cameraName":"UNKNOWN (16785216)"},{"name":"UNKNOWN (16787216)","ev":null,"value":null,"code":16787216,"cameraName":"UNKNOWN (16787216)"},{"name":"UNKNOWN (16790016)","ev":null,"value":null,"code":16790016,"cameraName":"UNKNOWN (16790016)"},{"name":"UNKNOWN (16793216)","ev":null,"value":null,"code":16793216,"cameraName":"UNKNOWN (16793216)"},{"name":"UNKNOWN (16802816)","ev":null,"value":null,"code":16802816,"cameraName":"UNKNOWN (16802816)"},{"name":"UNKNOWN (16828416)","ev":null,"value":null,"code":16828416,"cameraName":"UNKNOWN (16828416)"},{"name":"UNKNOWN (16879616)","ev":null,"value":null,"code":16879616,"cameraName":"UNKNOWN (16879616)"},{"name":"UNKNOWN (16982016)","ev":null,"value":null,"code":16982016,"cameraName":"UNKNOWN (16982016)"},{"name":"UNKNOWN (17186816)","ev":null,"value":null,"code":17186816,"cameraName":"UNKNOWN (17186816)"},{"name":"UNKNOWN (50331647)","ev":null,"value":null,"code":50331647,"cameraName":"UNKNOWN (50331647)"},{"name":"UNKNOWN (33554532)","ev":null,"value":null,"code":33554532,"cameraName":"UNKNOWN (33554532)"},{"name":"UNKNOWN (33554632)","ev":null,"value":null,"code":33554632,"cameraName":"UNKNOWN (33554632)"},{"name":"UNKNOWN (33554832)","ev":null,"value":null,"code":33554832,"cameraName":"UNKNOWN (33554832)"},{"name":"UNKNOWN (33555232)","ev":null,"value":null,"code":33555232,"cameraName":"UNKNOWN (33555232)"},{"name":"UNKNOWN (33556032)","ev":null,"value":null,"code":33556032,"cameraName":"UNKNOWN (33556032)"},{"name":"UNKNOWN (33557632)","ev":null,"value":null,"code":33557632,"cameraName":"UNKNOWN (33557632)"},{"name":"UNKNOWN (33560832)","ev":null,"value":null,"code":33560832,"cameraName":"UNKNOWN (33560832)"},{"name":"UNKNOWN (33567232)","ev":null,"value":null,"code":33567232,"cameraName":"UNKNOWN (33567232)"},{"name":"UNKNOWN (33580032)","ev":null,"value":null,"code":33580032,"cameraName":"UNKNOWN (33580032)"},{"name":"UNKNOWN (33605632)","ev":null,"value":null,"code":33605632,"cameraName":"UNKNOWN (33605632)"},{"name":"UNKNOWN (33656832)","ev":null,"value":null,"code":33656832,"cameraName":"UNKNOWN (33656832)"},{"name":"UNKNOWN (33759232)","ev":null,"value":null,"code":33759232,"cameraName":"UNKNOWN (33759232)"},{"name":"UNKNOWN (33964032)","ev":null,"value":null,"code":33964032,"cameraName":"UNKNOWN (33964032)"}]},"battery":72,"focusPos":0,"lists":{"shutter":[{"name":"30s","ev":-11,"code":19660810,"duration_ms":32000,"cameraName":"30s"},{"name":"25s","ev":-10.666666666666666,"code":16384010,"duration_ms":27000,"cameraName":"25s"},{"name":"20s","ev":-10.333333333333334,"code":13107210,"duration_ms":21500,"cameraName":"20s"},{"name":"15s","ev":-10,"code":9830410,"duration_ms":16000,"cameraName":"15s"},{"name":"13s","ev":-9.666666666666666,"code":8519690,"duration_ms":13800,"cameraName":"13s"},{"name":"10s","ev":-9.333333333333334,"code":6553610,"duration_ms":10600,"cameraName":"10s"},{"name":"8s","ev":-9,"code":5242890,"duration_ms":8000,"cameraName":"8s"},{"name":"6s","ev":-8.666666666666666,"code":3932170,"duration_ms":6000,"cameraName":"6s"},{"name":"5s","ev":-8.333333333333334,"code":3276810,"duration_ms":5000,"cameraName":"5s"},{"name":"4s","ev":-8,"code":2621450,"duration_ms":4000,"cameraName":"4s"},{"name":"3s","ev":-7.666666666666667,"code":2097162,"duration_ms":3000,"cameraName":"3s"},{"name":"2.5s","ev":-7.333333333333333,"code":1638410,"duration_ms":2500,"cameraName":"2.5s"},{"name":"2s","ev":-7,"code":1310730,"duration_ms":2000,"cameraName":"2s"},{"name":"1.6s","ev":-6.666666666666667,"code":1048586,"duration_ms":1600,"cameraName":"1.6s"},{"name":"1.3s","ev":-6.333333333333333,"code":851978,"duration_ms":1300,"cameraName":"1.3s"},{"name":"1s","ev":-6,"code":655370,"duration_ms":1000,"cameraName":"1s"},{"name":"0.8s","ev":-5.666666666666667,"code":524298,"duration_ms":800,"cameraName":"0.8s"},{"name":"0.6s","ev":-5.333333333333333,"code":393226,"duration_ms":600,"cameraName":"0.6s"},{"name":"1/2","ev":-5,"code":327690,"duration_ms":500,"cameraName":"1/2"},{"name":"0.4s","ev":-4.666666666666667,"code":262154,"duration_ms":400,"cameraName":"0.4s"},{"name":"1/3","ev":-4.333333333333333,"code":65539,"duration_ms":333,"cameraName":"1/3"},{"name":"1/4","ev":-4,"code":65540,"duration_ms":250,"cameraName":"1/4"},{"name":"1/5","ev":-3.6666666666666665,"code":65541,"duration_ms":200,"cameraName":"1/5"},{"name":"1/6","ev":-3.3333333333333335,"code":65542,"duration_ms":150,"cameraName":"1/6"},{"name":"1/8","ev":-3,"code":65544,"duration_ms":125,"cameraName":"1/8"},{"name":"1/10","ev":-2.6666666666666665,"code":65546,"duration_ms":100,"cameraName":"1/10"},{"name":"1/13","ev":-2.3333333333333335,"code":65549,"duration_ms":100,"cameraName":"1/13"},{"name":"1/15","ev":-2,"code":65551,"duration_ms":100,"cameraName":"1/15"},{"name":"1/20","ev":-1.6666666666666665,"code":65556,"duration_ms":100,"cameraName":"1/20"},{"name":"1/25","ev":-1.3333333333333333,"code":65561,"duration_ms":100,"cameraName":"1/25"},{"name":"1/30","ev":-1,"code":65566,"duration_ms":100,"cameraName":"1/30"},{"name":"1/40","ev":-0.6666666666666666,"code":65576,"duration_ms":100,"cameraName":"1/40"},{"name":"1/50","ev":-0.3333333333333333,"code":65586,"duration_ms":100,"cameraName":"1/50"},{"name":"1/60","ev":0,"code":65596,"duration_ms":100,"cameraName":"1/60"},{"name":"1/80","ev":0.3333333333333333,"code":65616,"duration_ms":100,"cameraName":"1/80"},{"name":"1/100","ev":0.6666666666666666,"code":65636,"duration_ms":100,"cameraName":"1/100"},{"name":"1/125","ev":1,"code":65661,"duration_ms":100,"cameraName":"1/125"},{"name":"1/160","ev":1.3333333333333333,"code":65696,"duration_ms":100,"cameraName":"1/160"},{"name":"1/200","ev":1.6666666666666665,"code":65736,"duration_ms":100,"cameraName":"1/200"},{"name":"1/250","ev":2,"code":65786,"duration_ms":100,"cameraName":"1/250"},{"name":"1/320","ev":2.3333333333333335,"code":65856,"duration_ms":100,"cameraName":"1/320"},{"name":"1/400","ev":2.6666666666666665,"code":65936,"duration_ms":100,"cameraName":"1/400"},{"name":"1/500","ev":3,"code":66036,"duration_ms":100,"cameraName":"1/500"},{"name":"1/640","ev":3.3333333333333335,"code":66176,"duration_ms":100,"cameraName":"1/640"},{"name":"1/800","ev":3.6666666666666665,"code":66336,"duration_ms":100,"cameraName":"1/800"},{"name":"1/1000","ev":4,"code":66536,"duration_ms":100,"cameraName":"1/1000"},{"name":"1/1250","ev":4.333333333333333,"code":66786,"duration_ms":100,"cameraName":"1/1250"},{"name":"1/1600","ev":4.666666666666667,"code":67136,"duration_ms":100,"cameraName":"1/1600"},{"name":"1/2000","ev":5,"code":67536,"duration_ms":100,"cameraName":"1/2000"},{"name":"1/2500","ev":5.333333333333333,"code":68036,"duration_ms":100,"cameraName":"1/2500"},{"name":"1/3200","ev":5.666666666666667,"code":68736,"duration_ms":100,"cameraName":"1/3200"},{"name":"1/4000","ev":6,"code":69536,"duration_ms":100,"cameraName":"1/4000"},{"name":"1/5000","ev":6.333333333333333,"code":70536,"duration_ms":100,"cameraName":"1/5000"},{"name":"1/6400","ev":6.666666666666667,"code":71936,"duration_ms":100,"cameraName":"1/6400"},{"name":"1/8000","ev":7,"code":73536,"duration_ms":100,"cameraName":"1/8000"}],"aperture":[{"name":"1.0","ev":-8,"code":100,"cameraName":"1.0"},{"name":"1.1","ev":-7.666666666666667,"code":110,"cameraName":"1.1"},{"name":"1.2","ev":-7.333333333333333,"code":120,"cameraName":"1.2"},{"name":"1.4","ev":-7,"code":140,"cameraName":"1.4"},{"name":"1.6","ev":-6.666666666666667,"code":160,"cameraName":"1.6"},{"name":"1.8","ev":-6.333333333333333,"code":180,"cameraName":"1.8"},{"name":"2.0","ev":-6,"code":200,"cameraName":"2.0"},{"name":"2.2","ev":-5.666666666666667,"code":220,"cameraName":"2.2"},{"name":"2.5","ev":-5.333333333333333,"code":250,"cameraName":"2.5"},{"name":"2.8","ev":-5,"code":280,"cameraName":"2.8"},{"name":"3.2","ev":-4.666666666666667,"code":320,"cameraName":"3.2"},{"name":"3.5","ev":-4.333333333333333,"code":350,"cameraName":"3.5"},{"name":"4.0","ev":-4,"code":400,"cameraName":"4.0"},{"name":"4.5","ev":-3.6666666666666665,"code":450,"cameraName":"4.5"},{"name":"5.0","ev":-3.3333333333333335,"code":500,"cameraName":"5.0"},{"name":"5.6","ev":-3,"code":560,"cameraName":"5.6"},{"name":"6.3","ev":-2.6666666666666665,"code":630,"cameraName":"6.3"},{"name":"7.1","ev":-2.3333333333333335,"code":710,"cameraName":"7.1"},{"name":"8","ev":-2,"code":800,"cameraName":"8"},{"name":"9","ev":-1.6666666666666665,"code":900,"cameraName":"9"},{"name":"10","ev":-1.3333333333333333,"code":1000,"cameraName":"10"},{"name":"11","ev":-1,"code":1100,"cameraName":"11"},{"name":"13","ev":-0.6666666666666666,"code":1300,"cameraName":"13"},{"name":"14","ev":-0.3333333333333333,"code":1400,"cameraName":"14"},{"name":"16","ev":0,"code":1600,"cameraName":"16"},{"name":"18","ev":0.3333333333333333,"code":1800,"cameraName":"18"},{"name":"20","ev":0.6666666666666666,"code":2000,"cameraName":"20"},{"name":"22","ev":1,"code":2200,"cameraName":"22"},{"name":"25","ev":2.3333333333333335,"code":2500,"cameraName":"25"},{"name":"29","ev":2.6666666666666665,"code":2900,"cameraName":"29"},{"name":"32","ev":3,"code":3200,"cameraName":"32"},{"name":"36","ev":3.3333333333333335,"code":3600,"cameraName":"36"},{"name":"42","ev":3.6666666666666665,"code":4200,"cameraName":"42"},{"name":"45","ev":4,"code":4500,"cameraName":"45"},{"name":"50","ev":4.333333333333333,"code":5000,"cameraName":"50"},{"name":"57","ev":4.666666666666667,"code":5700,"cameraName":"57"},{"name":"64","ev":5,"code":6400,"cameraName":"64"}],"iso":[{"name":"AUTO","ev":null,"code":16777215,"cameraName":"AUTO"},{"name":"UNKNOWN (25)","ev":null,"value":null,"code":25,"cameraName":"UNKNOWN (25)"},{"name":"UNKNOWN (50)","ev":null,"value":null,"code":50,"cameraName":"UNKNOWN (50)"},{"name":"UNKNOWN (64)","ev":null,"value":null,"code":64,"cameraName":"UNKNOWN (64)"},{"name":"UNKNOWN (80)","ev":null,"value":null,"code":80,"cameraName":"UNKNOWN (80)"},{"name":"100","ev":0,"code":100,"cameraName":"100"},{"name":"125","ev":-0.3333333333333333,"code":125,"cameraName":"125"},{"name":"160","ev":-0.6666666666666666,"code":160,"cameraName":"160"},{"name":"200","ev":-1,"code":200,"cameraName":"200"},{"name":"250","ev":-1.3333333333333333,"code":250,"cameraName":"250"},{"name":"320","ev":-1.6666666666666665,"code":320,"cameraName":"320"},{"name":"400","ev":-2,"code":400,"cameraName":"400"},{"name":"500","ev":-2.3333333333333335,"code":500,"cameraName":"500"},{"name":"640","ev":-2.6666666666666665,"code":640,"cameraName":"640"},{"name":"800","ev":-3,"code":800,"cameraName":"800"},{"name":"1000","ev":-3.3333333333333335,"code":1000,"cameraName":"1000"},{"name":"1250","ev":-3.6666666666666665,"code":1250,"cameraName":"1250"},{"name":"1600","ev":-4,"code":1600,"cameraName":"1600"},{"name":"2000","ev":-4.333333333333333,"code":2000,"cameraName":"2000"},{"name":"2500","ev":-4.666666666666667,"code":2500,"cameraName":"2500"},{"name":"3200","ev":-5,"code":3200,"cameraName":"3200"},{"name":"4000","ev":-5.333333333333333,"code":4000,"cameraName":"4000"},{"name":"5000","ev":-5.666666666666667,"code":5000,"cameraName":"5000"},{"name":"6400","ev":-6,"code":6400,"cameraName":"6400"},{"name":"8000","ev":-6.333333333333333,"code":8000,"cameraName":"8000"},{"name":"10000","ev":-6.666666666666667,"code":10000,"cameraName":"10000"},{"name":"12800","ev":-7,"code":12800,"cameraName":"12800"},{"name":"16000","ev":-7.333333333333333,"code":16000,"cameraName":"16000"},{"name":"20000","ev":-7.666666666666667,"code":20000,"cameraName":"20000"},{"name":"25600","ev":-8,"code":25600,"cameraName":"25600"},{"name":"32000","ev":-8.333333333333334,"code":32000,"cameraName":"32000"},{"name":"40000","ev":-8.666666666666666,"code":40000,"cameraName":"40000"},{"name":"51200","ev":-9,"code":51200,"cameraName":"51200"},{"name":"64000","ev":-9.333333333333334,"code":64000,"cameraName":"64000"},{"name":"80000","ev":-9.666666666666666,"code":80000,"cameraName":"80000"},{"name":"102400","ev":-10,"code":102400,"cameraName":"102400"},{"name":"128000","ev":-10.333333333333334,"code":128000,"cameraName":"128000"},{"name":"160000","ev":-10.666666666666666,"code":160000,"cameraName":"160000"},{"name":"204800","ev":-11,"code":204800,"cameraName":"204800"},{"name":"UNKNOWN (256000)","ev":null,"value":null,"code":256000,"cameraName":"UNKNOWN (256000)"},{"name":"UNKNOWN (320000)","ev":null,"value":null,"code":320000,"cameraName":"UNKNOWN (320000)"},{"name":"UNKNOWN (409600)","ev":null,"value":null,"code":409600,"cameraName":"UNKNOWN (409600)"},{"name":"UNKNOWN (33554431)","ev":null,"value":null,"code":33554431,"cameraName":"UNKNOWN (33554431)"},{"name":"UNKNOWN (16777241)","ev":null,"value":null,"code":16777241,"cameraName":"UNKNOWN (16777241)"},{"name":"UNKNOWN (16777266)","ev":null,"value":null,"code":16777266,"cameraName":"UNKNOWN (16777266)"},{"name":"UNKNOWN (16777280)","ev":null,"value":null,"code":16777280,"cameraName":"UNKNOWN (16777280)"},{"name":"UNKNOWN (16777296)","ev":null,"value":null,"code":16777296,"cameraName":"UNKNOWN (16777296)"},{"name":"UNKNOWN (16777316)","ev":null,"value":null,"code":16777316,"cameraName":"UNKNOWN (16777316)"},{"name":"UNKNOWN (16777341)","ev":null,"value":null,"code":16777341,"cameraName":"UNKNOWN (16777341)"},{"name":"UNKNOWN (16777376)","ev":null,"value":null,"code":16777376,"cameraName":"UNKNOWN (16777376)"},{"name":"UNKNOWN (16777416)","ev":null,"value":null,"code":16777416,"cameraName":"UNKNOWN (16777416)"},{"name":"UNKNOWN (16777466)","ev":null,"value":null,"code":16777466,"cameraName":"UNKNOWN (16777466)"},{"name":"UNKNOWN (16777536)","ev":null,"value":null,"code":16777536,"cameraName":"UNKNOWN (16777536)"},{"name":"UNKNOWN (16777616)","ev":null,"value":null,"code":16777616,"cameraName":"UNKNOWN (16777616)"},{"name":"UNKNOWN (16777716)","ev":null,"value":null,"code":16777716,"cameraName":"UNKNOWN (16777716)"},{"name":"UNKNOWN (16777856)","ev":null,"value":null,"code":16777856,"cameraName":"UNKNOWN (16777856)"},{"name":"UNKNOWN (16778016)","ev":null,"value":null,"code":16778016,"cameraName":"UNKNOWN (16778016)"},{"name":"UNKNOWN (16778216)","ev":null,"value":null,"code":16778216,"cameraName":"UNKNOWN (16778216)"},{"name":"UNKNOWN (16778466)","ev":null,"value":null,"code":16778466,"cameraName":"UNKNOWN (16778466)"},{"name":"UNKNOWN (16778816)","ev":null,"value":null,"code":16778816,"cameraName":"UNKNOWN (16778816)"},{"name":"UNKNOWN (16779216)","ev":null,"value":null,"code":16779216,"cameraName":"UNKNOWN (16779216)"},{"name":"UNKNOWN (16779716)","ev":null,"value":null,"code":16779716,"cameraName":"UNKNOWN (16779716)"},{"name":"UNKNOWN (16780416)","ev":null,"value":null,"code":16780416,"cameraName":"UNKNOWN (16780416)"},{"name":"UNKNOWN (16781216)","ev":null,"value":null,"code":16781216,"cameraName":"UNKNOWN (16781216)"},{"name":"UNKNOWN (16782216)","ev":null,"value":null,"code":16782216,"cameraName":"UNKNOWN (16782216)"},{"name":"UNKNOWN (16783616)","ev":null,"value":null,"code":16783616,"cameraName":"UNKNOWN (16783616)"},{"name":"UNKNOWN (16785216)","ev":null,"value":null,"code":16785216,"cameraName":"UNKNOWN (16785216)"},{"name":"UNKNOWN (16787216)","ev":null,"value":null,"code":16787216,"cameraName":"UNKNOWN (16787216)"},{"name":"UNKNOWN (16790016)","ev":null,"value":null,"code":16790016,"cameraName":"UNKNOWN (16790016)"},{"name":"UNKNOWN (16793216)","ev":null,"value":null,"code":16793216,"cameraName":"UNKNOWN (16793216)"},{"name":"UNKNOWN (16802816)","ev":null,"value":null,"code":16802816,"cameraName":"UNKNOWN (16802816)"},{"name":"UNKNOWN (16828416)","ev":null,"value":null,"code":16828416,"cameraName":"UNKNOWN (16828416)"},{"name":"UNKNOWN (16879616)","ev":null,"value":null,"code":16879616,"cameraName":"UNKNOWN (16879616)"},{"name":"UNKNOWN (16982016)","ev":null,"value":null,"code":16982016,"cameraName":"UNKNOWN (16982016)"},{"name":"UNKNOWN (17186816)","ev":null,"value":null,"code":17186816,"cameraName":"UNKNOWN (17186816)"},{"name":"UNKNOWN (50331647)","ev":null,"value":null,"code":50331647,"cameraName":"UNKNOWN (50331647)"},{"name":"UNKNOWN (33554532)","ev":null,"value":null,"code":33554532,"cameraName":"UNKNOWN (33554532)"},{"name":"UNKNOWN (33554632)","ev":null,"value":null,"code":33554632,"cameraName":"UNKNOWN (33554632)"},{"name":"UNKNOWN (33554832)","ev":null,"value":null,"code":33554832,"cameraName":"UNKNOWN (33554832)"},{"name":"UNKNOWN (33555232)","ev":null,"value":null,"code":33555232,"cameraName":"UNKNOWN (33555232)"},{"name":"UNKNOWN (33556032)","ev":null,"value":null,"code":33556032,"cameraName":"UNKNOWN (33556032)"},{"name":"UNKNOWN (33557632)","ev":null,"value":null,"code":33557632,"cameraName":"UNKNOWN (33557632)"},{"name":"UNKNOWN (33560832)","ev":null,"value":null,"code":33560832,"cameraName":"UNKNOWN (33560832)"},{"name":"UNKNOWN (33567232)","ev":null,"value":null,"code":33567232,"cameraName":"UNKNOWN (33567232)"},{"name":"UNKNOWN (33580032)","ev":null,"value":null,"code":33580032,"cameraName":"UNKNOWN (33580032)"},{"name":"UNKNOWN (33605632)","ev":null,"value":null,"code":33605632,"cameraName":"UNKNOWN (33605632)"},{"name":"UNKNOWN (33656832)","ev":null,"value":null,"code":33656832,"cameraName":"UNKNOWN (33656832)"},{"name":"UNKNOWN (33759232)","ev":null,"value":null,"code":33759232,"cameraName":"UNKNOWN (33759232)"},{"name":"UNKNOWN (33964032)","ev":null,"value":null,"code":33964032,"cameraName":"UNKNOWN (33964032)"}]}}},"type":"settings"}"""

//language=json
val json = """
    {
      "info": {
        "id": 295,
        "name": "tl-329",
        "date": "2024-11-22T07:32:50.143Z",
        "program": {
          "rampMode": "auto",
          "lrtDirection": "auto",
          "intervalMode": "auto",
          "rampAlgorithm": "lum",
          "highlightProtection": true,
          "interval": "5",
          "dayInterval": 5,
          "nightInterval": 30,
          "frames": 300,
          "destination": "camera",
          "nightLuminance": "-1.3",
          "dayLuminance": 0,
          "isoMax": -3,
          "isoMin": 0,
          "rampParameters": "S=A=I",
          "apertureMax": -1,
          "apertureMin": -7,
          "manualAperture": -6,
          "hdrCount": 0,
          "hdrStops": 1,
          "exposurePlans": {},
          "trackingTarget": "moon",
          "autoRestart": false,
          "keyframes": null,
          "savedExposurePlans": {},
          "tracking": "none",
          "delay": 1,
          "scheduled": null,
          "axes": {
            "focus": {
              "type": "disabled"
            }
          },
          "focusPos": 0,
          "coords": {
            "lat": 53,
            "lon": 27,
            "alt": 0,
            "src": "manual"
          },
          "loaded": true,
          "durationSeconds": 1800,
          "schedMonday": true,
          "schedTuesday": true,
          "schedWednesday": true,
          "schedThursday": true,
          "schedFriday": true,
          "schedSaturday": true,
          "schedSunday": true,
          "schedStop": "07:00",
          "eclipseInfo": "",
          "shutterMax": -10.333333333333334,
          "schedStart": "17:30",
          "_timeOffsetSeconds": 10494,
          "_exposureReferenceEv": -3.214070651666502
        },
        "status": {
          "running": true,
          "frames": 0,
          "framesRemaining": null,
          "rampRate": 0,
          "intervalMs": 5000,
          "message": "starting",
          "rampEv": null,
          "autoSettings": {
            "paddingTimeMs": 2000
          },
          "exposure": {
            "status": {
              "rampEv": null,
              "highlights": null,
              "rate": null,
              "direction": null,
              "highlightProtection": 0
            },
            "config": {
              "sunrise": {
                "p": 0.97,
                "i": 0.5,
                "d": 0.6,
                "targetTimeSeconds": 360,
                "evIntegrationSeconds": 360,
                "historyIntegrationSeconds": 480,
                "highlightIntegrationFrames": 3
              },
              "sunset": {
                "p": 1.1,
                "i": 0.6,
                "d": 0.4,
                "targetTimeSeconds": 480,
                "evIntegrationSeconds": 480,
                "historyIntegrationSeconds": 480,
                "highlightIntegrationFrames": 3
              },
              "maxEv": 20,
              "minEv": -3.333333333333332,
              "maxRate": 30,
              "hysteresis": 0.4,
              "nightCompensationDayEv": 10,
              "nightCompensationNightEv": -1,
              "nightCompensation": "auto",
              "nightLuminance": "-1.3",
              "dayLuminance": 0,
              "highlightProtection": true,
              "highlightProtectionLimit": 1
            }
          },
          "stopping": false,
          "timeOffsetSeconds": 0,
          "exposureReferenceEv": null,
          "tlName": "tl-329",
          "timelapseFolder": "/root/time-lapse/tl-329",
          "first": true,
          "rampMode": "auto",
          "startTime": 1732260769.096,
          "bufferSeconds": 0,
          "cameraSettings": {
            "shutter": "1/640",
            "aperture": "8",
            "iso": "100",
            "details": {
              "shutter": {
                "name": "1/640",
                "ev": 3.3333333333333335,
                "code": 66176,
                "duration_ms": 100,
                "cameraName": "1/640",
                "list": {
                  "0": {
                    "name": "30s",
                    "ev": -11,
                    "code": 19660810,
                    "duration_ms": 32000,
                    "cameraName": "30s"
                  },
                  "1": {
                    "name": "25s",
                    "ev": -10.666666666666666,
                    "code": 16384010,
                    "duration_ms": 27000,
                    "cameraName": "25s"
                  },
                  "2": {
                    "name": "20s",
                    "ev": -10.333333333333334,
                    "code": 13107210,
                    "duration_ms": 21500,
                    "cameraName": "20s"
                  },
                  "3": {
                    "name": "15s",
                    "ev": -10,
                    "code": 9830410,
                    "duration_ms": 16000,
                    "cameraName": "15s"
                  }
                  
                }
              },
              "aperture": {
                "name": "8",
                "ev": -2,
                "code": 800,
                "cameraName": "8",
                "list": {
                  "0": {
                    "name": "1.0",
                    "ev": -8,
                    "code": 100,
                    "cameraName": "1.0"
                  },
                  "1": {
                    "name": "1.1",
                    "ev": -7.666666666666667,
                    "code": 110,
                    "cameraName": "1.1"
                  },
                  "2": {
                    "name": "1.2",
                    "ev": -7.333333333333333,
                    "code": 120,
                    "cameraName": "1.2"
                  },
                  "3": {
                    "name": "1.4",
                    "ev": -7,
                    "code": 140,
                    "cameraName": "1.4"
                  }
                  
                }
              },
              "iso": {
                "name": "100",
                "ev": 0,
                "code": 100,
                "cameraName": "100",
                "list": {
                  "0": {
                    "name": "AUTO",
                    "ev": null,
                    "code": 16777215,
                    "cameraName": "AUTO"
                  },
                  "1": {
                    "name": "UNKNOWN (25)",
                    "ev": null,
                    "value": null,
                    "code": 25
                  },
                  "2": {
                    "name": "UNKNOWN (50)",
                    "ev": null,
                    "value": null,
                    "code": 50
                  },
                  "3": {
                    "name": "UNKNOWN (64)",
                    "ev": null,
                    "value": null,
                    "code": 64
                  },
                  "4": {
                    "name": "UNKNOWN (80)",
                    "ev": null,
                    "value": null,
                    "code": 80
                  }
                }
              }
            }
          },
          "hdrSet": {},
          "hdrIndex": 0,
          "hdrCount": 0,
          "currentPlanIndex": null,
          "panDiffNew": 0,
          "tiltDiffNew": 0,
          "focusDiffNew": 0,
          "panDiff": 0,
          "tiltDiff": 0,
          "trackingPanEnabled": false,
          "trackingTiltEnabled": false,
          "dynamicChange": {},
          "trackingTilt": 0,
          "trackingPan": 0,
          "latitude": 53,
          "longitude": 27,
          "altitude": 0,
          "sunPos": {
            "azimuth": -0.6005138746936612,
            "altitude": 0.18072392063048126
          },
          "moonPos": {
            "azimuth": 1.2754728906292,
            "altitude": 0.5784258706842446
          },
          "useLiveview": false
        },
        "logfile": "/var/log/view-core-20241122-072900.txt",
        "cameras": 1,
        "primary_camera": 1,
        "thumbnail": "/root/time-lapse/tl-329/cam-1-00001.jpg",
        "frames": 3786
      },
      "ack": "sd133bs5ip",
      "type": "timelapse-clip-info"
    }
""".trimIndent()

val json2 = """
    {"id":101,"name":"tl-131","date":"2024-02-27T12:29:59.926Z","program":{"rampMode":"auto","lrtDirection":"auto","intervalMode":"auto","rampAlgorithm":"lum","highlightProtection":true,"interval":5,"dayInterval":4,"nightInterval":20,"frames":300,"destination":"camera","nightLuminance":-1,"dayLuminance":0,"isoMax":-5,"isoMin":0,"rampParameters":"S=A=I","apertureMax":-2,"apertureMin":-7,"manualAperture":-5,"hdrCount":0,"hdrStops":1,"exposurePlans":{},"trackingTarget":"moon","autoRestart":false,"keyframes":null,"savedExposurePlans":{},"tracking":"none","delay":1,"scheduled":null,"axes":{"focus":{"type":"disabled"}},"focusPos":0,"coords":{"lat":15.66427,"lon":73.71072166666667,"alt":76.1,"src":"gps"}},"status":{"running":true,"frames":0,"framesRemaining":null,"rampRate":0,"intervalMs":5000,"message":"starting","rampEv":null,"autoSettings":{"paddingTimeMs":2675.451815917741},"exposure":{"status":{"rampEv":null,"highlights":null,"rate":null,"direction":null,"highlightProtection":0},"config":{"sunrise":{"p":0.9,"i":0.4,"d":0.6,"targetTimeSeconds":300,"evIntegrationSeconds":300,"historyIntegrationSeconds":480,"highlightIntegrationFrames":3},"sunset":{"p":1.1,"i":0.6,"d":0.4,"targetTimeSeconds":480,"evIntegrationSeconds":480,"historyIntegrationSeconds":480,"highlightIntegrationFrames":5},"maxEv":19,"minEv":-5,"maxRate":30,"hysteresis":0.4,"nightCompensationDayEv":10,"nightCompensationNightEv":-2,"nightCompensation":"auto","nightLuminance":-1,"dayLuminance":0,"highlightProtection":true,"highlightProtectionLimit":1}},"stopping":false,"timeOffsetSeconds":0,"exposureReferenceEv":null,"tlName":"tl-131","timelapseFolder":"/root/time-lapse/tl-131","first":true,"rampMode":"auto","startTime":1709036999.54,"bufferSeconds":0,"cameraSettings":{"shutter":"1/2500","aperture":"4.0","iso":"100","details":{"shutter":{"name":"1/2500","ev":5.333333333333333,"code":68036,"duration_ms":100,"list":{"0":{"name":"30s","ev":-11,"code":19660810,"duration_ms":32000,"cameraName":"30s"},"1":{"name":"25s","ev":-10.666666666666666,"code":16384010,"duration_ms":27000,"cameraName":"25s"},"2":{"name":"20s","ev":-10.333333333333334,"code":13107210,"duration_ms":21500,"cameraName":"20s"},"3":{"name":"15s","ev":-10,"code":9830410,"duration_ms":16000,"cameraName":"15s"},"4":{"name":"13s","ev":-9.666666666666666,"code":8519690,"duration_ms":13800,"cameraName":"13s"},"5":{"name":"10s","ev":-9.333333333333334,"code":6553610,"duration_ms":10600,"cameraName":"10s"},"6":{"name":"8s","ev":-9,"code":5242890,"duration_ms":8000,"cameraName":"8s"},"7":{"name":"6s","ev":-8.666666666666666,"code":3932170,"duration_ms":6000,"cameraName":"6s"},"8":{"name":"5s","ev":-8.333333333333334,"code":3276810,"duration_ms":5000,"cameraName":"5s"},"9":{"name":"4s","ev":-8,"code":2621450,"duration_ms":4000,"cameraName":"4s"},"10":{"name":"3s","ev":-7.666666666666667,"code":2097162,"duration_ms":3000,"cameraName":"3s"},"11":{"name":"2.5s","ev":-7.333333333333333,"code":1638410,"duration_ms":2500,"cameraName":"2.5s"},"12":{"name":"2s","ev":-7,"code":1310730,"duration_ms":2000,"cameraName":"2s"},"13":{"name":"1.6s","ev":-6.666666666666667,"code":1048586,"duration_ms":1600,"cameraName":"1.6s"},"14":{"name":"1.3s","ev":-6.333333333333333,"code":851978,"duration_ms":1300,"cameraName":"1.3s"},"15":{"name":"1s","ev":-6,"code":655370,"duration_ms":1000,"cameraName":"1s"},"16":{"name":"0.8s","ev":-5.666666666666667,"code":524298,"duration_ms":800,"cameraName":"0.8s"},"17":{"name":"0.6s","ev":-5.333333333333333,"code":393226,"duration_ms":600,"cameraName":"0.6s"},"18":{"name":"1/2","ev":-5,"code":327690,"duration_ms":500,"cameraName":"1/2"},"19":{"name":"0.4s","ev":-4.666666666666667,"code":262154,"duration_ms":400,"cameraName":"0.4s"},"20":{"name":"1/3","ev":-4.333333333333333,"code":65539,"duration_ms":333,"cameraName":"1/3"},"21":{"name":"1/4","ev":-4,"code":65540,"duration_ms":250,"cameraName":"1/4"},"22":{"name":"1/5","ev":-3.6666666666666665,"code":65541,"duration_ms":200,"cameraName":"1/5"},"23":{"name":"1/6","ev":-3.3333333333333335,"code":65542,"duration_ms":150,"cameraName":"1/6"},"24":{"name":"1/8","ev":-3,"code":65544,"duration_ms":125,"cameraName":"1/8"},"25":{"name":"1/10","ev":-2.6666666666666665,"code":65546,"duration_ms":100,"cameraName":"1/10"},"26":{"name":"1/13","ev":-2.3333333333333335,"code":65549,"duration_ms":100,"cameraName":"1/13"},"27":{"name":"1/15","ev":-2,"code":65551,"duration_ms":100,"cameraName":"1/15"},"28":{"name":"1/20","ev":-1.6666666666666665,"code":65556,"duration_ms":100,"cameraName":"1/20"},"29":{"name":"1/25","ev":-1.3333333333333333,"code":65561,"duration_ms":100,"cameraName":"1/25"},"30":{"name":"1/30","ev":-1,"code":65566,"duration_ms":100,"cameraName":"1/30"},"31":{"name":"1/40","ev":-0.6666666666666666,"code":65576,"duration_ms":100,"cameraName":"1/40"},"32":{"name":"1/50","ev":-0.3333333333333333,"code":65586,"duration_ms":100,"cameraName":"1/50"},"33":{"name":"1/60","ev":0,"code":65596,"duration_ms":100,"cameraName":"1/60"},"34":{"name":"1/80","ev":0.3333333333333333,"code":65616,"duration_ms":100,"cameraName":"1/80"},"35":{"name":"1/100","ev":0.6666666666666666,"code":65636,"duration_ms":100,"cameraName":"1/100"},"36":{"name":"1/125","ev":1,"code":65661,"duration_ms":100,"cameraName":"1/125"},"37":{"name":"1/160","ev":1.3333333333333333,"code":65696,"duration_ms":100,"cameraName":"1/160"},"38":{"name":"1/200","ev":1.6666666666666665,"code":65736,"duration_ms":100,"cameraName":"1/200"},"39":{"name":"1/250","ev":2,"code":65786,"duration_ms":100,"cameraName":"1/250"},"40":{"name":"1/320","ev":2.3333333333333335,"code":65856,"duration_ms":100,"cameraName":"1/320"},"41":{"name":"1/400","ev":2.6666666666666665,"code":65936,"duration_ms":100,"cameraName":"1/400"},"42":{"name":"1/500","ev":3,"code":66036,"duration_ms":100,"cameraName":"1/500"},"43":{"name":"1/640","ev":3.3333333333333335,"code":66176,"duration_ms":100,"cameraName":"1/640"},"44":{"name":"1/800","ev":3.6666666666666665,"code":66336,"duration_ms":100,"cameraName":"1/800"},"45":{"name":"1/1000","ev":4,"code":66536,"duration_ms":100,"cameraName":"1/1000"},"46":{"name":"1/1250","ev":4.333333333333333,"code":66786,"duration_ms":100,"cameraName":"1/1250"},"47":{"name":"1/1600","ev":4.666666666666667,"code":67136,"duration_ms":100,"cameraName":"1/1600"},"48":{"name":"1/2000","ev":5,"code":67536,"duration_ms":100,"cameraName":"1/2000"},"49":{"name":"1/2500","ev":5.333333333333333,"code":68036,"duration_ms":100,"cameraName":"1/2500"},"50":{"name":"1/3200","ev":5.666666666666667,"code":68736,"duration_ms":100,"cameraName":"1/3200"},"51":{"name":"1/4000","ev":6,"code":69536,"duration_ms":100,"cameraName":"1/4000"},"52":{"name":"1/5000","ev":6.333333333333333,"code":70536,"duration_ms":100,"cameraName":"1/5000"},"53":{"name":"1/6400","ev":6.666666666666667,"code":71936,"duration_ms":100,"cameraName":"1/6400"},"54":{"name":"1/8000","ev":7,"code":73536,"duration_ms":100,"cameraName":"1/8000"}}},"aperture":{"name":"4.0","ev":-4,"code":400,"list":{"0":{"name":"1.0","ev":-8,"code":100,"cameraName":"1.0"},"1":{"name":"1.1","ev":-7.666666666666667,"code":110,"cameraName":"1.1"},"2":{"name":"1.2","ev":-7.333333333333333,"code":120,"cameraName":"1.2"},"3":{"name":"1.4","ev":-7,"code":140,"cameraName":"1.4"},"4":{"name":"1.6","ev":-6.666666666666667,"code":160,"cameraName":"1.6"},"5":{"name":"1.7","ev":-6.5,"code":170,"cameraName":"1.7"},"6":{"name":"1.8","ev":-6.333333333333333,"code":180,"cameraName":"1.8"},"7":{"name":"2.0","ev":-6,"code":200,"cameraName":"2.0"},"8":{"name":"2.2","ev":-5.666666666666667,"code":220,"cameraName":"2.2"},"9":{"name":"2.5","ev":-5.333333333333333,"code":250,"cameraName":"2.5"},"10":{"name":"2.8","ev":-5,"code":280,"cameraName":"2.8"},"11":{"name":"3.2","ev":-4.666666666666667,"code":320,"cameraName":"3.2"},"12":{"name":"3.5","ev":-4.333333333333333,"code":350,"cameraName":"3.5"},"13":{"name":"4.0","ev":-4,"code":400,"cameraName":"4.0"},"14":{"name":"4.5","ev":-3.6666666666666665,"code":450,"cameraName":"4.5"},"15":{"name":"5.0","ev":-3.3333333333333335,"code":500,"cameraName":"5.0"},"16":{"name":"5.6","ev":-3,"code":560,"cameraName":"5.6"},"17":{"name":"6.3","ev":-2.6666666666666665,"code":630,"cameraName":"6.3"},"18":{"name":"7.1","ev":-2.3333333333333335,"code":710,"cameraName":"7.1"},"19":{"name":"8","ev":-2,"code":800,"cameraName":"8"},"20":{"name":"9","ev":-1.6666666666666665,"code":900,"cameraName":"9"},"21":{"name":"10","ev":-1.3333333333333333,"code":1000,"cameraName":"10"},"22":{"name":"11","ev":-1,"code":1100,"cameraName":"11"},"23":{"name":"13","ev":-0.6666666666666666,"code":1300,"cameraName":"13"},"24":{"name":"14","ev":-0.3333333333333333,"code":1400,"cameraName":"14"},"25":{"name":"16","ev":0,"code":1600,"cameraName":"16"},"26":{"name":"18","ev":0.3333333333333333,"code":1800,"cameraName":"18"},"27":{"name":"20","ev":0.6666666666666666,"code":2000,"cameraName":"20"},"28":{"name":"22","ev":1,"code":2200,"cameraName":"22"},"29":{"name":"25","ev":2.3333333333333335,"code":2500,"cameraName":"25"},"30":{"name":"29","ev":2.6666666666666665,"code":2900,"cameraName":"29"},"31":{"name":"32","ev":3,"code":3200,"cameraName":"32"},"32":{"name":"36","ev":3.3333333333333335,"code":3600,"cameraName":"36"},"33":{"name":"42","ev":3.6666666666666665,"code":4200,"cameraName":"42"},"34":{"name":"45","ev":4,"code":4500,"cameraName":"45"},"35":{"name":"50","ev":4.333333333333333,"code":5000,"cameraName":"50"},"36":{"name":"57","ev":4.666666666666667,"code":5700,"cameraName":"57"},"37":{"name":"64","ev":5,"code":6400,"cameraName":"64"}}},"iso":{"name":"100","ev":0,"code":100,"list":{"0":{"name":"AUTO","ev":null,"code":16777215,"cameraName":"AUTO"},"1":{"name":"UNKNOWN (25)","ev":null,"value":null,"code":25,"cameraName":"UNKNOWN (25)"},"2":{"name":"UNKNOWN (50)","ev":null,"value":null,"code":50,"cameraName":"UNKNOWN (50)"},"3":{"name":"UNKNOWN (64)","ev":null,"value":null,"code":64,"cameraName":"UNKNOWN (64)"},"4":{"name":"UNKNOWN (80)","ev":null,"value":null,"code":80,"cameraName":"UNKNOWN (80)"},"5":{"name":"100","ev":0,"code":100,"cameraName":"100"},"6":{"name":"125","ev":-0.3333333333333333,"code":125,"cameraName":"125"},"7":{"name":"160","ev":-0.6666666666666666,"code":160,"cameraName":"160"},"8":{"name":"200","ev":-1,"code":200,"cameraName":"200"},"9":{"name":"250","ev":-1.3333333333333333,"code":250,"cameraName":"250"},"10":{"name":"320","ev":-1.6666666666666665,"code":320,"cameraName":"320"},"11":{"name":"400","ev":-2,"code":400,"cameraName":"400"},"12":{"name":"500","ev":-2.3333333333333335,"code":500,"cameraName":"500"},"13":{"name":"640","ev":-2.6666666666666665,"code":640,"cameraName":"640"},"14":{"name":"800","ev":-3,"code":800,"cameraName":"800"},"15":{"name":"1000","ev":-3.3333333333333335,"code":1000,"cameraName":"1000"},"16":{"name":"1250","ev":-3.6666666666666665,"code":1250,"cameraName":"1250"},"17":{"name":"1600","ev":-4,"code":1600,"cameraName":"1600"},"18":{"name":"2000","ev":-4.333333333333333,"code":2000,"cameraName":"2000"},"19":{"name":"2500","ev":-4.666666666666667,"code":2500,"cameraName":"2500"},"20":{"name":"3200","ev":-5,"code":3200,"cameraName":"3200"},"21":{"name":"4000","ev":-5.333333333333333,"code":4000,"cameraName":"4000"},"22":{"name":"5000","ev":-5.666666666666667,"code":5000,"cameraName":"5000"},"23":{"name":"6400","ev":-6,"code":6400,"cameraName":"6400"},"24":{"name":"8000","ev":-6.333333333333333,"code":8000,"cameraName":"8000"},"25":{"name":"10000","ev":-6.666666666666667,"code":10000,"cameraName":"10000"},"26":{"name":"12800","ev":-7,"code":12800,"cameraName":"12800"},"27":{"name":"16000","ev":-7.333333333333333,"code":16000,"cameraName":"16000"},"28":{"name":"20000","ev":-7.666666666666667,"code":20000,"cameraName":"20000"},"29":{"name":"25600","ev":-8,"code":25600,"cameraName":"25600"},"30":{"name":"32000","ev":-8.333333333333334,"code":32000,"cameraName":"32000"},"31":{"name":"40000","ev":-8.666666666666666,"code":40000,"cameraName":"40000"},"32":{"name":"51200","ev":-9,"code":51200,"cameraName":"51200"},"33":{"name":"64000","ev":-9.333333333333334,"code":64000,"cameraName":"64000"},"34":{"name":"80000","ev":-9.666666666666666,"code":80000,"cameraName":"80000"},"35":{"name":"102400","ev":-10,"code":102400,"cameraName":"102400"},"36":{"name":"128000","ev":-10.333333333333334,"code":128000,"cameraName":"128000"},"37":{"name":"160000","ev":-10.666666666666666,"code":160000,"cameraName":"160000"},"38":{"name":"204800","ev":-11,"code":204800,"cameraName":"204800"},"39":{"name":"UNKNOWN (256000)","ev":null,"value":null,"code":256000,"cameraName":"UNKNOWN (256000)"},"40":{"name":"UNKNOWN (320000)","ev":null,"value":null,"code":320000,"cameraName":"UNKNOWN (320000)"},"41":{"name":"UNKNOWN (409600)","ev":null,"value":null,"code":409600,"cameraName":"UNKNOWN (409600)"},"42":{"name":"UNKNOWN (33554431)","ev":null,"value":null,"code":33554431,"cameraName":"UNKNOWN (33554431)"},"43":{"name":"UNKNOWN (16777241)","ev":null,"value":null,"code":16777241,"cameraName":"UNKNOWN (16777241)"},"44":{"name":"UNKNOWN (16777266)","ev":null,"value":null,"code":16777266,"cameraName":"UNKNOWN (16777266)"},"45":{"name":"UNKNOWN (16777280)","ev":null,"value":null,"code":16777280,"cameraName":"UNKNOWN (16777280)"},"46":{"name":"UNKNOWN (16777296)","ev":null,"value":null,"code":16777296,"cameraName":"UNKNOWN (16777296)"},"47":{"name":"UNKNOWN (16777316)","ev":null,"value":null,"code":16777316,"cameraName":"UNKNOWN (16777316)"},"48":{"name":"UNKNOWN (16777341)","ev":null,"value":null,"code":16777341,"cameraName":"UNKNOWN (16777341)"},"49":{"name":"UNKNOWN (16777376)","ev":null,"value":null,"code":16777376,"cameraName":"UNKNOWN (16777376)"},"50":{"name":"UNKNOWN (16777416)","ev":null,"value":null,"code":16777416,"cameraName":"UNKNOWN (16777416)"},"51":{"name":"UNKNOWN (16777466)","ev":null,"value":null,"code":16777466,"cameraName":"UNKNOWN (16777466)"},"52":{"name":"UNKNOWN (16777536)","ev":null,"value":null,"code":16777536,"cameraName":"UNKNOWN (16777536)"},"53":{"name":"UNKNOWN (16777616)","ev":null,"value":null,"code":16777616,"cameraName":"UNKNOWN (16777616)"},"54":{"name":"UNKNOWN (16777716)","ev":null,"value":null,"code":16777716,"cameraName":"UNKNOWN (16777716)"},"55":{"name":"UNKNOWN (16777856)","ev":null,"value":null,"code":16777856,"cameraName":"UNKNOWN (16777856)"},"56":{"name":"UNKNOWN (16778016)","ev":null,"value":null,"code":16778016,"cameraName":"UNKNOWN (16778016)"},"57":{"name":"UNKNOWN (16778216)","ev":null,"value":null,"code":16778216,"cameraName":"UNKNOWN (16778216)"},"58":{"name":"UNKNOWN (16778466)","ev":null,"value":null,"code":16778466,"cameraName":"UNKNOWN (16778466)"},"59":{"name":"UNKNOWN (16778816)","ev":null,"value":null,"code":16778816,"cameraName":"UNKNOWN (16778816)"},"60":{"name":"UNKNOWN (16779216)","ev":null,"value":null,"code":16779216,"cameraName":"UNKNOWN (16779216)"},"61":{"name":"UNKNOWN (16779716)","ev":null,"value":null,"code":16779716,"cameraName":"UNKNOWN (16779716)"},"62":{"name":"UNKNOWN (16780416)","ev":null,"value":null,"code":16780416,"cameraName":"UNKNOWN (16780416)"},"63":{"name":"UNKNOWN (16781216)","ev":null,"value":null,"code":16781216,"cameraName":"UNKNOWN (16781216)"},"64":{"name":"UNKNOWN (16782216)","ev":null,"value":null,"code":16782216,"cameraName":"UNKNOWN (16782216)"},"65":{"name":"UNKNOWN (16783616)","ev":null,"value":null,"code":16783616,"cameraName":"UNKNOWN (16783616)"},"66":{"name":"UNKNOWN (16785216)","ev":null,"value":null,"code":16785216,"cameraName":"UNKNOWN (16785216)"},"67":{"name":"UNKNOWN (16787216)","ev":null,"value":null,"code":16787216,"cameraName":"UNKNOWN (16787216)"},"68":{"name":"UNKNOWN (16790016)","ev":null,"value":null,"code":16790016,"cameraName":"UNKNOWN (16790016)"},"69":{"name":"UNKNOWN (16793216)","ev":null,"value":null,"code":16793216,"cameraName":"UNKNOWN (16793216)"},"70":{"name":"UNKNOWN (16802816)","ev":null,"value":null,"code":16802816,"cameraName":"UNKNOWN (16802816)"},"71":{"name":"UNKNOWN (16828416)","ev":null,"value":null,"code":16828416,"cameraName":"UNKNOWN (16828416)"},"72":{"name":"UNKNOWN (16879616)","ev":null,"value":null,"code":16879616,"cameraName":"UNKNOWN (16879616)"},"73":{"name":"UNKNOWN (16982016)","ev":null,"value":null,"code":16982016,"cameraName":"UNKNOWN (16982016)"},"74":{"name":"UNKNOWN (17186816)","ev":null,"value":null,"code":17186816,"cameraName":"UNKNOWN (17186816)"},"75":{"name":"UNKNOWN (50331647)","ev":null,"value":null,"code":50331647,"cameraName":"UNKNOWN (50331647)"},"76":{"name":"UNKNOWN (33554532)","ev":null,"value":null,"code":33554532,"cameraName":"UNKNOWN (33554532)"},"77":{"name":"UNKNOWN (33554632)","ev":null,"value":null,"code":33554632,"cameraName":"UNKNOWN (33554632)"},"78":{"name":"UNKNOWN (33554832)","ev":null,"value":null,"code":33554832,"cameraName":"UNKNOWN (33554832)"},"79":{"name":"UNKNOWN (33555232)","ev":null,"value":null,"code":33555232,"cameraName":"UNKNOWN (33555232)"},"80":{"name":"UNKNOWN (33556032)","ev":null,"value":null,"code":33556032,"cameraName":"UNKNOWN (33556032)"},"81":{"name":"UNKNOWN (33557632)","ev":null,"value":null,"code":33557632,"cameraName":"UNKNOWN (33557632)"},"82":{"name":"UNKNOWN (33560832)","ev":null,"value":null,"code":33560832,"cameraName":"UNKNOWN (33560832)"},"83":{"name":"UNKNOWN (33567232)","ev":null,"value":null,"code":33567232,"cameraName":"UNKNOWN (33567232)"},"84":{"name":"UNKNOWN (33580032)","ev":null,"value":null,"code":33580032,"cameraName":"UNKNOWN (33580032)"},"85":{"name":"UNKNOWN (33605632)","ev":null,"value":null,"code":33605632,"cameraName":"UNKNOWN (33605632)"},"86":{"name":"UNKNOWN (33656832)","ev":null,"value":null,"code":33656832,"cameraName":"UNKNOWN (33656832)"},"87":{"name":"UNKNOWN (33759232)","ev":null,"value":null,"code":33759232,"cameraName":"UNKNOWN (33759232)"},"88":{"name":"UNKNOWN (33964032)","ev":null,"value":null,"code":33964032,"cameraName":"UNKNOWN (33964032)"}}}}},"hdrSet":{},"hdrIndex":0,"hdrCount":0,"currentPlanIndex":null,"panDiffNew":0,"tiltDiffNew":0,"focusDiffNew":0,"panDiff":0,"tiltDiff":0,"trackingPanEnabled":false,"trackingTiltEnabled":false,"dynamicChange":{},"trackingTilt":0,"trackingPan":0,"latitude":15.66427,"longitude":73.71072166666667,"altitude":76.1,"sunPos":{"azimuth":1.3705184836192577,"altitude":0.16060140155634858},"moonPos":{"azimuth":-1.7259206602609676,"altitude":-0.7252874246111188},"useLiveview":false,"id":100,"cameraEv":17.333333333333332,"evDiff":0.23839156562833708,"captureStartTime":1709036918.299,"lastPhotoTime":729.5989999771118,"path":"/capt0251.jpg"},"logfile":"/var/log/view-core-20240227-120054.txt","cameras":1,"primary_camera":1,"thumbnail":"/root/time-lapse/tl-131/cam-1-00001.jpg","frames":1052}
""".trimIndent()
val json3 = """
    {"id":131,"name":"tl-161","date":"2024-04-27T12:56:25.795Z","program":{"rampMode":"auto","lrtDirection":"auto","intervalMode":"auto","rampAlgorithm":"lum","highlightProtection":true,"interval":5,"dayInterval":24,"nightInterval":60,"frames":300,"destination":"camera","nightLuminance":0,"dayLuminance":1,"isoMax":-2,"isoMin":0,"rampParameters":"S=A=I","apertureMax":-2,"apertureMin":-8,"manualAperture":-5,"hdrCount":0,"hdrStops":1,"exposurePlans":{},"trackingTarget":"moon","autoRestart":true,"keyframes":{"0":{"focus":0,"ev":"not set","motor":{}}},"savedExposurePlans":{},"tracking":"none","delay":0,"scheduled":true,"axes":{"focus":{"kf":{"0":{"seconds":0,"position":0}},"type":"disabled","pos":0,"present":true,"id":"focus","colorIndex":0}},"focusPos":0,"coords":{"lat":53,"lon":27,"alt":0,"src":"manual"},"loaded":true,"durationSeconds":1800,"schedMonday":true,"schedSunday":true,"schedWednesday":true,"schedThursday":true,"schedStart":"19:30","schedStop":"07:00","schedFriday":true,"schedSaturday":true,"schedTuesday":true},"status":{"running":true,"frames":0,"framesRemaining":null,"rampRate":0,"intervalMs":5000,"message":"starting","rampEv":null,"autoSettings":{"paddingTimeMs":2000},"exposure":{"status":{"rampEv":null,"highlights":null,"rate":null,"direction":null,"highlightProtection":0},"config":{"sunrise":{"p":0.97,"i":0.5,"d":0.6,"targetTimeSeconds":360,"evIntegrationSeconds":360,"historyIntegrationSeconds":480,"highlightIntegrationFrames":3},"sunset":{"p":1.1,"i":0.6,"d":0.4,"targetTimeSeconds":480,"evIntegrationSeconds":480,"historyIntegrationSeconds":480,"highlightIntegrationFrames":5},"maxEv":19,"minEv":-3.333333333333332,"maxRate":30,"hysteresis":0.4,"nightCompensationDayEv":10,"nightCompensationNightEv":-2,"nightCompensation":"auto","nightLuminance":0,"dayLuminance":1,"highlightProtection":true,"highlightProtectionLimit":1}},"stopping":false,"timeOffsetSeconds":10798,"exposureReferenceEv":null,"tlName":"tl-161","timelapseFolder":"/root/time-lapse/tl-161","first":true,"rampMode":"auto","startTime":1714222585.263,"bufferSeconds":0,"cameraSettings":{"shutter":"1/200","aperture":"7.1","iso":"100","details":{"shutter":{"name":"1/200","ev":1.6666666666666665,"code":65736,"duration_ms":100,"cameraName":"1/200","list":{"0":{"name":"30s","ev":-11,"code":19660810,"duration_ms":32000,"cameraName":"30s"},"1":{"name":"25s","ev":-10.666666666666666,"code":16384010,"duration_ms":27000,"cameraName":"25s"},"2":{"name":"20s","ev":-10.333333333333334,"code":13107210,"duration_ms":21500,"cameraName":"20s"},"3":{"name":"15s","ev":-10,"code":9830410,"duration_ms":16000,"cameraName":"15s"},"4":{"name":"13s","ev":-9.666666666666666,"code":8519690,"duration_ms":13800,"cameraName":"13s"},"5":{"name":"10s","ev":-9.333333333333334,"code":6553610,"duration_ms":10600,"cameraName":"10s"},"6":{"name":"8s","ev":-9,"code":5242890,"duration_ms":8000,"cameraName":"8s"},"7":{"name":"6s","ev":-8.666666666666666,"code":3932170,"duration_ms":6000,"cameraName":"6s"},"8":{"name":"5s","ev":-8.333333333333334,"code":3276810,"duration_ms":5000,"cameraName":"5s"},"9":{"name":"4s","ev":-8,"code":2621450,"duration_ms":4000,"cameraName":"4s"},"10":{"name":"3s","ev":-7.666666666666667,"code":2097162,"duration_ms":3000,"cameraName":"3s"},"11":{"name":"2.5s","ev":-7.333333333333333,"code":1638410,"duration_ms":2500,"cameraName":"2.5s"},"12":{"name":"2s","ev":-7,"code":1310730,"duration_ms":2000,"cameraName":"2s"},"13":{"name":"1.6s","ev":-6.666666666666667,"code":1048586,"duration_ms":1600,"cameraName":"1.6s"},"14":{"name":"1.3s","ev":-6.333333333333333,"code":851978,"duration_ms":1300,"cameraName":"1.3s"},"15":{"name":"1s","ev":-6,"code":655370,"duration_ms":1000,"cameraName":"1s"},"16":{"name":"0.8s","ev":-5.666666666666667,"code":524298,"duration_ms":800,"cameraName":"0.8s"},"17":{"name":"0.6s","ev":-5.333333333333333,"code":393226,"duration_ms":600,"cameraName":"0.6s"},"18":{"name":"1/2","ev":-5,"code":327690,"duration_ms":500,"cameraName":"1/2"},"19":{"name":"0.4s","ev":-4.666666666666667,"code":262154,"duration_ms":400,"cameraName":"0.4s"},"20":{"name":"1/3","ev":-4.333333333333333,"code":65539,"duration_ms":333,"cameraName":"1/3"},"21":{"name":"1/4","ev":-4,"code":65540,"duration_ms":250,"cameraName":"1/4"},"22":{"name":"1/5","ev":-3.6666666666666665,"code":65541,"duration_ms":200,"cameraName":"1/5"},"23":{"name":"1/6","ev":-3.3333333333333335,"code":65542,"duration_ms":150,"cameraName":"1/6"},"24":{"name":"1/8","ev":-3,"code":65544,"duration_ms":125,"cameraName":"1/8"},"25":{"name":"1/10","ev":-2.6666666666666665,"code":65546,"duration_ms":100,"cameraName":"1/10"},"26":{"name":"1/13","ev":-2.3333333333333335,"code":65549,"duration_ms":100,"cameraName":"1/13"},"27":{"name":"1/15","ev":-2,"code":65551,"duration_ms":100,"cameraName":"1/15"},"28":{"name":"1/20","ev":-1.6666666666666665,"code":65556,"duration_ms":100,"cameraName":"1/20"},"29":{"name":"1/25","ev":-1.3333333333333333,"code":65561,"duration_ms":100,"cameraName":"1/25"},"30":{"name":"1/30","ev":-1,"code":65566,"duration_ms":100,"cameraName":"1/30"},"31":{"name":"1/40","ev":-0.6666666666666666,"code":65576,"duration_ms":100,"cameraName":"1/40"},"32":{"name":"1/50","ev":-0.3333333333333333,"code":65586,"duration_ms":100,"cameraName":"1/50"},"33":{"name":"1/60","ev":0,"code":65596,"duration_ms":100,"cameraName":"1/60"},"34":{"name":"1/80","ev":0.3333333333333333,"code":65616,"duration_ms":100,"cameraName":"1/80"},"35":{"name":"1/100","ev":0.6666666666666666,"code":65636,"duration_ms":100,"cameraName":"1/100"},"36":{"name":"1/125","ev":1,"code":65661,"duration_ms":100,"cameraName":"1/125"},"37":{"name":"1/160","ev":1.3333333333333333,"code":65696,"duration_ms":100,"cameraName":"1/160"},"38":{"name":"1/200","ev":1.6666666666666665,"code":65736,"duration_ms":100,"cameraName":"1/200"},"39":{"name":"1/250","ev":2,"code":65786,"duration_ms":100,"cameraName":"1/250"},"40":{"name":"1/320","ev":2.3333333333333335,"code":65856,"duration_ms":100,"cameraName":"1/320"},"41":{"name":"1/400","ev":2.6666666666666665,"code":65936,"duration_ms":100,"cameraName":"1/400"},"42":{"name":"1/500","ev":3,"code":66036,"duration_ms":100,"cameraName":"1/500"},"43":{"name":"1/640","ev":3.3333333333333335,"code":66176,"duration_ms":100,"cameraName":"1/640"},"44":{"name":"1/800","ev":3.6666666666666665,"code":66336,"duration_ms":100,"cameraName":"1/800"},"45":{"name":"1/1000","ev":4,"code":66536,"duration_ms":100,"cameraName":"1/1000"},"46":{"name":"1/1250","ev":4.333333333333333,"code":66786,"duration_ms":100,"cameraName":"1/1250"},"47":{"name":"1/1600","ev":4.666666666666667,"code":67136,"duration_ms":100,"cameraName":"1/1600"},"48":{"name":"1/2000","ev":5,"code":67536,"duration_ms":100,"cameraName":"1/2000"},"49":{"name":"1/2500","ev":5.333333333333333,"code":68036,"duration_ms":100,"cameraName":"1/2500"},"50":{"name":"1/3200","ev":5.666666666666667,"code":68736,"duration_ms":100,"cameraName":"1/3200"},"51":{"name":"1/4000","ev":6,"code":69536,"duration_ms":100,"cameraName":"1/4000"},"52":{"name":"1/5000","ev":6.333333333333333,"code":70536,"duration_ms":100,"cameraName":"1/5000"},"53":{"name":"1/6400","ev":6.666666666666667,"code":71936,"duration_ms":100,"cameraName":"1/6400"},"54":{"name":"1/8000","ev":7,"code":73536,"duration_ms":100,"cameraName":"1/8000"}}},"aperture":{"name":"7.1","ev":-2.3333333333333335,"code":710,"cameraName":"7.1","list":{"0":{"name":"1.0","ev":-8,"code":100,"cameraName":"1.0"},"1":{"name":"1.1","ev":-7.666666666666667,"code":110,"cameraName":"1.1"},"2":{"name":"1.2","ev":-7.333333333333333,"code":120,"cameraName":"1.2"},"3":{"name":"1.4","ev":-7,"code":140,"cameraName":"1.4"},"4":{"name":"1.6","ev":-6.666666666666667,"code":160,"cameraName":"1.6"},"5":{"name":"1.7","ev":-6.5,"code":170,"cameraName":"1.7"},"6":{"name":"1.8","ev":-6.333333333333333,"code":180,"cameraName":"1.8"},"7":{"name":"2.0","ev":-6,"code":200,"cameraName":"2.0"},"8":{"name":"2.2","ev":-5.666666666666667,"code":220,"cameraName":"2.2"},"9":{"name":"2.5","ev":-5.333333333333333,"code":250,"cameraName":"2.5"},"10":{"name":"2.8","ev":-5,"code":280,"cameraName":"2.8"},"11":{"name":"3.2","ev":-4.666666666666667,"code":320,"cameraName":"3.2"},"12":{"name":"3.5","ev":-4.333333333333333,"code":350,"cameraName":"3.5"},"13":{"name":"4.0","ev":-4,"code":400,"cameraName":"4.0"},"14":{"name":"4.5","ev":-3.6666666666666665,"code":450,"cameraName":"4.5"},"15":{"name":"5.0","ev":-3.3333333333333335,"code":500,"cameraName":"5.0"},"16":{"name":"5.6","ev":-3,"code":560,"cameraName":"5.6"},"17":{"name":"6.3","ev":-2.6666666666666665,"code":630,"cameraName":"6.3"},"18":{"name":"7.1","ev":-2.3333333333333335,"code":710,"cameraName":"7.1"},"19":{"name":"8","ev":-2,"code":800,"cameraName":"8"},"20":{"name":"9","ev":-1.6666666666666665,"code":900,"cameraName":"9"},"21":{"name":"10","ev":-1.3333333333333333,"code":1000,"cameraName":"10"},"22":{"name":"11","ev":-1,"code":1100,"cameraName":"11"},"23":{"name":"13","ev":-0.6666666666666666,"code":1300,"cameraName":"13"},"24":{"name":"14","ev":-0.3333333333333333,"code":1400,"cameraName":"14"},"25":{"name":"16","ev":0,"code":1600,"cameraName":"16"},"26":{"name":"18","ev":0.3333333333333333,"code":1800,"cameraName":"18"},"27":{"name":"20","ev":0.6666666666666666,"code":2000,"cameraName":"20"},"28":{"name":"22","ev":1,"code":2200,"cameraName":"22"},"29":{"name":"25","ev":2.3333333333333335,"code":2500,"cameraName":"25"},"30":{"name":"29","ev":2.6666666666666665,"code":2900,"cameraName":"29"},"31":{"name":"32","ev":3,"code":3200,"cameraName":"32"},"32":{"name":"36","ev":3.3333333333333335,"code":3600,"cameraName":"36"},"33":{"name":"42","ev":3.6666666666666665,"code":4200,"cameraName":"42"},"34":{"name":"45","ev":4,"code":4500,"cameraName":"45"},"35":{"name":"50","ev":4.333333333333333,"code":5000,"cameraName":"50"},"36":{"name":"57","ev":4.666666666666667,"code":5700,"cameraName":"57"},"37":{"name":"64","ev":5,"code":6400,"cameraName":"64"}}},"iso":{"name":"100","ev":0,"code":100,"cameraName":"100","list":{"0":{"name":"AUTO","ev":null,"code":16777215,"cameraName":"AUTO"},"1":{"name":"UNKNOWN (25)","ev":null,"value":null,"code":25,"cameraName":"UNKNOWN (25)"},"2":{"name":"UNKNOWN (50)","ev":null,"value":null,"code":50,"cameraName":"UNKNOWN (50)"},"3":{"name":"UNKNOWN (64)","ev":null,"value":null,"code":64,"cameraName":"UNKNOWN (64)"},"4":{"name":"UNKNOWN (80)","ev":null,"value":null,"code":80,"cameraName":"UNKNOWN (80)"},"5":{"name":"100","ev":0,"code":100,"cameraName":"100"},"6":{"name":"125","ev":-0.3333333333333333,"code":125,"cameraName":"125"},"7":{"name":"160","ev":-0.6666666666666666,"code":160,"cameraName":"160"},"8":{"name":"200","ev":-1,"code":200,"cameraName":"200"},"9":{"name":"250","ev":-1.3333333333333333,"code":250,"cameraName":"250"},"10":{"name":"320","ev":-1.6666666666666665,"code":320,"cameraName":"320"},"11":{"name":"400","ev":-2,"code":400,"cameraName":"400"},"12":{"name":"500","ev":-2.3333333333333335,"code":500,"cameraName":"500"},"13":{"name":"640","ev":-2.6666666666666665,"code":640,"cameraName":"640"},"14":{"name":"800","ev":-3,"code":800,"cameraName":"800"},"15":{"name":"1000","ev":-3.3333333333333335,"code":1000,"cameraName":"1000"},"16":{"name":"1250","ev":-3.6666666666666665,"code":1250,"cameraName":"1250"},"17":{"name":"1600","ev":-4,"code":1600,"cameraName":"1600"},"18":{"name":"2000","ev":-4.333333333333333,"code":2000,"cameraName":"2000"},"19":{"name":"2500","ev":-4.666666666666667,"code":2500,"cameraName":"2500"},"20":{"name":"3200","ev":-5,"code":3200,"cameraName":"3200"},"21":{"name":"4000","ev":-5.333333333333333,"code":4000,"cameraName":"4000"},"22":{"name":"5000","ev":-5.666666666666667,"code":5000,"cameraName":"5000"},"23":{"name":"6400","ev":-6,"code":6400,"cameraName":"6400"},"24":{"name":"8000","ev":-6.333333333333333,"code":8000,"cameraName":"8000"},"25":{"name":"10000","ev":-6.666666666666667,"code":10000,"cameraName":"10000"},"26":{"name":"12800","ev":-7,"code":12800,"cameraName":"12800"},"27":{"name":"16000","ev":-7.333333333333333,"code":16000,"cameraName":"16000"},"28":{"name":"20000","ev":-7.666666666666667,"code":20000,"cameraName":"20000"},"29":{"name":"25600","ev":-8,"code":25600,"cameraName":"25600"},"30":{"name":"32000","ev":-8.333333333333334,"code":32000,"cameraName":"32000"},"31":{"name":"40000","ev":-8.666666666666666,"code":40000,"cameraName":"40000"},"32":{"name":"51200","ev":-9,"code":51200,"cameraName":"51200"},"33":{"name":"64000","ev":-9.333333333333334,"code":64000,"cameraName":"64000"},"34":{"name":"80000","ev":-9.666666666666666,"code":80000,"cameraName":"80000"},"35":{"name":"102400","ev":-10,"code":102400,"cameraName":"102400"},"36":{"name":"128000","ev":-10.333333333333334,"code":128000,"cameraName":"128000"},"37":{"name":"160000","ev":-10.666666666666666,"code":160000,"cameraName":"160000"},"38":{"name":"204800","ev":-11,"code":204800,"cameraName":"204800"},"39":{"name":"UNKNOWN (256000)","ev":null,"value":null,"code":256000,"cameraName":"UNKNOWN (256000)"},"40":{"name":"UNKNOWN (320000)","ev":null,"value":null,"code":320000,"cameraName":"UNKNOWN (320000)"},"41":{"name":"UNKNOWN (409600)","ev":null,"value":null,"code":409600,"cameraName":"UNKNOWN (409600)"},"42":{"name":"UNKNOWN (33554431)","ev":null,"value":null,"code":33554431,"cameraName":"UNKNOWN (33554431)"},"43":{"name":"UNKNOWN (16777241)","ev":null,"value":null,"code":16777241,"cameraName":"UNKNOWN (16777241)"},"44":{"name":"UNKNOWN (16777266)","ev":null,"value":null,"code":16777266,"cameraName":"UNKNOWN (16777266)"},"45":{"name":"UNKNOWN (16777280)","ev":null,"value":null,"code":16777280,"cameraName":"UNKNOWN (16777280)"},"46":{"name":"UNKNOWN (16777296)","ev":null,"value":null,"code":16777296,"cameraName":"UNKNOWN (16777296)"},"47":{"name":"UNKNOWN (16777316)","ev":null,"value":null,"code":16777316,"cameraName":"UNKNOWN (16777316)"},"48":{"name":"UNKNOWN (16777341)","ev":null,"value":null,"code":16777341,"cameraName":"UNKNOWN (16777341)"},"49":{"name":"UNKNOWN (16777376)","ev":null,"value":null,"code":16777376,"cameraName":"UNKNOWN (16777376)"},"50":{"name":"UNKNOWN (16777416)","ev":null,"value":null,"code":16777416,"cameraName":"UNKNOWN (16777416)"},"51":{"name":"UNKNOWN (16777466)","ev":null,"value":null,"code":16777466,"cameraName":"UNKNOWN (16777466)"},"52":{"name":"UNKNOWN (16777536)","ev":null,"value":null,"code":16777536,"cameraName":"UNKNOWN (16777536)"},"53":{"name":"UNKNOWN (16777616)","ev":null,"value":null,"code":16777616,"cameraName":"UNKNOWN (16777616)"},"54":{"name":"UNKNOWN (16777716)","ev":null,"value":null,"code":16777716,"cameraName":"UNKNOWN (16777716)"},"55":{"name":"UNKNOWN (16777856)","ev":null,"value":null,"code":16777856,"cameraName":"UNKNOWN (16777856)"},"56":{"name":"UNKNOWN (16778016)","ev":null,"value":null,"code":16778016,"cameraName":"UNKNOWN (16778016)"},"57":{"name":"UNKNOWN (16778216)","ev":null,"value":null,"code":16778216,"cameraName":"UNKNOWN (16778216)"},"58":{"name":"UNKNOWN (16778466)","ev":null,"value":null,"code":16778466,"cameraName":"UNKNOWN (16778466)"},"59":{"name":"UNKNOWN (16778816)","ev":null,"value":null,"code":16778816,"cameraName":"UNKNOWN (16778816)"},"60":{"name":"UNKNOWN (16779216)","ev":null,"value":null,"code":16779216,"cameraName":"UNKNOWN (16779216)"},"61":{"name":"UNKNOWN (16779716)","ev":null,"value":null,"code":16779716,"cameraName":"UNKNOWN (16779716)"},"62":{"name":"UNKNOWN (16780416)","ev":null,"value":null,"code":16780416,"cameraName":"UNKNOWN (16780416)"},"63":{"name":"UNKNOWN (16781216)","ev":null,"value":null,"code":16781216,"cameraName":"UNKNOWN (16781216)"},"64":{"name":"UNKNOWN (16782216)","ev":null,"value":null,"code":16782216,"cameraName":"UNKNOWN (16782216)"},"65":{"name":"UNKNOWN (16783616)","ev":null,"value":null,"code":16783616,"cameraName":"UNKNOWN (16783616)"},"66":{"name":"UNKNOWN (16785216)","ev":null,"value":null,"code":16785216,"cameraName":"UNKNOWN (16785216)"},"67":{"name":"UNKNOWN (16787216)","ev":null,"value":null,"code":16787216,"cameraName":"UNKNOWN (16787216)"},"68":{"name":"UNKNOWN (16790016)","ev":null,"value":null,"code":16790016,"cameraName":"UNKNOWN (16790016)"},"69":{"name":"UNKNOWN (16793216)","ev":null,"value":null,"code":16793216,"cameraName":"UNKNOWN (16793216)"},"70":{"name":"UNKNOWN (16802816)","ev":null,"value":null,"code":16802816,"cameraName":"UNKNOWN (16802816)"},"71":{"name":"UNKNOWN (16828416)","ev":null,"value":null,"code":16828416,"cameraName":"UNKNOWN (16828416)"},"72":{"name":"UNKNOWN (16879616)","ev":null,"value":null,"code":16879616,"cameraName":"UNKNOWN (16879616)"},"73":{"name":"UNKNOWN (16982016)","ev":null,"value":null,"code":16982016,"cameraName":"UNKNOWN (16982016)"},"74":{"name":"UNKNOWN (17186816)","ev":null,"value":null,"code":17186816,"cameraName":"UNKNOWN (17186816)"},"75":{"name":"UNKNOWN (50331647)","ev":null,"value":null,"code":50331647,"cameraName":"UNKNOWN (50331647)"},"76":{"name":"UNKNOWN (33554532)","ev":null,"value":null,"code":33554532,"cameraName":"UNKNOWN (33554532)"},"77":{"name":"UNKNOWN (33554632)","ev":null,"value":null,"code":33554632,"cameraName":"UNKNOWN (33554632)"},"78":{"name":"UNKNOWN (33554832)","ev":null,"value":null,"code":33554832,"cameraName":"UNKNOWN (33554832)"},"79":{"name":"UNKNOWN (33555232)","ev":null,"value":null,"code":33555232,"cameraName":"UNKNOWN (33555232)"},"80":{"name":"UNKNOWN (33556032)","ev":null,"value":null,"code":33556032,"cameraName":"UNKNOWN (33556032)"},"81":{"name":"UNKNOWN (33557632)","ev":null,"value":null,"code":33557632,"cameraName":"UNKNOWN (33557632)"},"82":{"name":"UNKNOWN (33560832)","ev":null,"value":null,"code":33560832,"cameraName":"UNKNOWN (33560832)"},"83":{"name":"UNKNOWN (33567232)","ev":null,"value":null,"code":33567232,"cameraName":"UNKNOWN (33567232)"},"84":{"name":"UNKNOWN (33580032)","ev":null,"value":null,"code":33580032,"cameraName":"UNKNOWN (33580032)"},"85":{"name":"UNKNOWN (33605632)","ev":null,"value":null,"code":33605632,"cameraName":"UNKNOWN (33605632)"},"86":{"name":"UNKNOWN (33656832)","ev":null,"value":null,"code":33656832,"cameraName":"UNKNOWN (33656832)"},"87":{"name":"UNKNOWN (33759232)","ev":null,"value":null,"code":33759232,"cameraName":"UNKNOWN (33759232)"},"88":{"name":"UNKNOWN (33964032)","ev":null,"value":null,"code":33964032,"cameraName":"UNKNOWN (33964032)"}}}}},"hdrSet":{},"hdrIndex":0,"hdrCount":0,"currentPlanIndex":null,"panDiffNew":0,"tiltDiffNew":0,"focusDiffNew":0,"panDiff":0,"tiltDiff":0,"trackingPanEnabled":false,"trackingTiltEnabled":false,"dynamicChange":{},"trackingTilt":0,"trackingPan":0,"latitude":53,"longitude":27,"altitude":0,"sunPos":{"azimuth":0.9812817398768362,"altitude":0.6821604932307003},"moonPos":{"azimuth":3.132797670005653,"altitude":-1.124878395402841},"useLiveview":false,"id":130,"minutesUntilStart":215},"logfile":"/var/log/view-core-20240427-125027.txt","cameras":1,"primary_camera":1,"thumbnail":null,"frames":null}
""".trimIndent()
val json4 = """
    {"id":230,"name":"tl-261","date":"2024-06-10T12:44:09.753Z","program":{"rampMode":"auto","lrtDirection":"auto","intervalMode":"auto","rampAlgorithm":"lum","highlightProtection":false,"interval":5,"dayInterval":3,"nightInterval":"12","frames":300,"destination":"camera","nightLuminance":-1.5,"dayLuminance":0,"isoMax":-6,"isoMin":0,"rampParameters":"S=A=I","apertureMax":-2,"apertureMin":-7,"manualAperture":-5,"hdrCount":0,"hdrStops":1,"exposurePlans":{},"trackingTarget":"moon","autoRestart":false,"keyframes":null,"savedExposurePlans":{},"tracking":"none","delay":1,"scheduled":null,"axes":{"focus":{"type":"disabled"}},"focusPos":0,"coords":null,"loaded":true,"durationSeconds":1800},"status":{"running":true,"frames":0,"framesRemaining":null,"rampRate":0,"intervalMs":5000,"message":"starting","rampEv":null,"autoSettings":{"paddingTimeMs":2000},"exposure":{"status":{"rampEv":null,"highlights":null,"rate":null,"direction":null,"highlightProtection":0},"config":{"sunrise":{"p":0.97,"i":0.5,"d":0.6,"targetTimeSeconds":360,"evIntegrationSeconds":360,"historyIntegrationSeconds":480,"highlightIntegrationFrames":3},"sunset":{"p":1.1,"i":0.6,"d":0.4,"targetTimeSeconds":480,"evIntegrationSeconds":480,"historyIntegrationSeconds":480,"highlightIntegrationFrames":3},"maxEv":19,"minEv":-6.333333333333332,"maxRate":30,"hysteresis":0.4,"nightCompensationDayEv":10,"nightCompensationNightEv":-1,"nightCompensation":"auto","nightLuminance":-1.5,"dayLuminance":0,"highlightProtection":false,"highlightProtectionLimit":1}},"stopping":false,"timeOffsetSeconds":0,"exposureReferenceEv":null,"tlName":"tl-261","timelapseFolder":"/root/time-lapse/tl-261","first":true,"rampMode":"auto","startTime":1718023449.246,"bufferSeconds":0,"cameraSettings":{"shutter":"1/800","aperture":"8","iso":"100","details":{"shutter":{"name":"1/800","ev":3.6666666666666665,"code":66336,"duration_ms":100,"list":{"0":{"name":"30s","ev":-11,"code":19660810,"duration_ms":32000,"cameraName":"30s"},"1":{"name":"25s","ev":-10.666666666666666,"code":16384010,"duration_ms":27000,"cameraName":"25s"},"2":{"name":"20s","ev":-10.333333333333334,"code":13107210,"duration_ms":21500,"cameraName":"20s"},"3":{"name":"15s","ev":-10,"code":9830410,"duration_ms":16000,"cameraName":"15s"},"4":{"name":"13s","ev":-9.666666666666666,"code":8519690,"duration_ms":13800,"cameraName":"13s"},"5":{"name":"10s","ev":-9.333333333333334,"code":6553610,"duration_ms":10600,"cameraName":"10s"},"6":{"name":"8s","ev":-9,"code":5242890,"duration_ms":8000,"cameraName":"8s"},"7":{"name":"6s","ev":-8.666666666666666,"code":3932170,"duration_ms":6000,"cameraName":"6s"},"8":{"name":"5s","ev":-8.333333333333334,"code":3276810,"duration_ms":5000,"cameraName":"5s"},"9":{"name":"4s","ev":-8,"code":2621450,"duration_ms":4000,"cameraName":"4s"},"10":{"name":"3s","ev":-7.666666666666667,"code":2097162,"duration_ms":3000,"cameraName":"3s"},"11":{"name":"2.5s","ev":-7.333333333333333,"code":1638410,"duration_ms":2500,"cameraName":"2.5s"},"12":{"name":"2s","ev":-7,"code":1310730,"duration_ms":2000,"cameraName":"2s"},"13":{"name":"1.6s","ev":-6.666666666666667,"code":1048586,"duration_ms":1600,"cameraName":"1.6s"},"14":{"name":"1.3s","ev":-6.333333333333333,"code":851978,"duration_ms":1300,"cameraName":"1.3s"},"15":{"name":"1s","ev":-6,"code":655370,"duration_ms":1000,"cameraName":"1s"},"16":{"name":"0.8s","ev":-5.666666666666667,"code":524298,"duration_ms":800,"cameraName":"0.8s"},"17":{"name":"0.6s","ev":-5.333333333333333,"code":393226,"duration_ms":600,"cameraName":"0.6s"},"18":{"name":"1/2","ev":-5,"code":327690,"duration_ms":500,"cameraName":"1/2"},"19":{"name":"0.4s","ev":-4.666666666666667,"code":262154,"duration_ms":400,"cameraName":"0.4s"},"20":{"name":"1/3","ev":-4.333333333333333,"code":65539,"duration_ms":333,"cameraName":"1/3"},"21":{"name":"1/4","ev":-4,"code":65540,"duration_ms":250,"cameraName":"1/4"},"22":{"name":"1/5","ev":-3.6666666666666665,"code":65541,"duration_ms":200,"cameraName":"1/5"},"23":{"name":"1/6","ev":-3.3333333333333335,"code":65542,"duration_ms":150,"cameraName":"1/6"},"24":{"name":"1/8","ev":-3,"code":65544,"duration_ms":125,"cameraName":"1/8"},"25":{"name":"1/10","ev":-2.6666666666666665,"code":65546,"duration_ms":100,"cameraName":"1/10"},"26":{"name":"1/13","ev":-2.3333333333333335,"code":65549,"duration_ms":100,"cameraName":"1/13"},"27":{"name":"1/15","ev":-2,"code":65551,"duration_ms":100,"cameraName":"1/15"},"28":{"name":"1/20","ev":-1.6666666666666665,"code":65556,"duration_ms":100,"cameraName":"1/20"},"29":{"name":"1/25","ev":-1.3333333333333333,"code":65561,"duration_ms":100,"cameraName":"1/25"},"30":{"name":"1/30","ev":-1,"code":65566,"duration_ms":100,"cameraName":"1/30"},"31":{"name":"1/40","ev":-0.6666666666666666,"code":65576,"duration_ms":100,"cameraName":"1/40"},"32":{"name":"1/50","ev":-0.3333333333333333,"code":65586,"duration_ms":100,"cameraName":"1/50"},"33":{"name":"1/60","ev":0,"code":65596,"duration_ms":100,"cameraName":"1/60"},"34":{"name":"1/80","ev":0.3333333333333333,"code":65616,"duration_ms":100,"cameraName":"1/80"},"35":{"name":"1/100","ev":0.6666666666666666,"code":65636,"duration_ms":100,"cameraName":"1/100"},"36":{"name":"1/125","ev":1,"code":65661,"duration_ms":100,"cameraName":"1/125"},"37":{"name":"1/160","ev":1.3333333333333333,"code":65696,"duration_ms":100,"cameraName":"1/160"},"38":{"name":"1/200","ev":1.6666666666666665,"code":65736,"duration_ms":100,"cameraName":"1/200"},"39":{"name":"1/250","ev":2,"code":65786,"duration_ms":100,"cameraName":"1/250"},"40":{"name":"1/320","ev":2.3333333333333335,"code":65856,"duration_ms":100,"cameraName":"1/320"},"41":{"name":"1/400","ev":2.6666666666666665,"code":65936,"duration_ms":100,"cameraName":"1/400"},"42":{"name":"1/500","ev":3,"code":66036,"duration_ms":100,"cameraName":"1/500"},"43":{"name":"1/640","ev":3.3333333333333335,"code":66176,"duration_ms":100,"cameraName":"1/640"},"44":{"name":"1/800","ev":3.6666666666666665,"code":66336,"duration_ms":100,"cameraName":"1/800"},"45":{"name":"1/1000","ev":4,"code":66536,"duration_ms":100,"cameraName":"1/1000"},"46":{"name":"1/1250","ev":4.333333333333333,"code":66786,"duration_ms":100,"cameraName":"1/1250"},"47":{"name":"1/1600","ev":4.666666666666667,"code":67136,"duration_ms":100,"cameraName":"1/1600"},"48":{"name":"1/2000","ev":5,"code":67536,"duration_ms":100,"cameraName":"1/2000"},"49":{"name":"1/2500","ev":5.333333333333333,"code":68036,"duration_ms":100,"cameraName":"1/2500"},"50":{"name":"1/3200","ev":5.666666666666667,"code":68736,"duration_ms":100,"cameraName":"1/3200"},"51":{"name":"1/4000","ev":6,"code":69536,"duration_ms":100,"cameraName":"1/4000"},"52":{"name":"1/5000","ev":6.333333333333333,"code":70536,"duration_ms":100,"cameraName":"1/5000"},"53":{"name":"1/6400","ev":6.666666666666667,"code":71936,"duration_ms":100,"cameraName":"1/6400"},"54":{"name":"1/8000","ev":7,"code":73536,"duration_ms":100,"cameraName":"1/8000"}}},"aperture":{"name":"8","ev":-2,"code":800,"list":{"0":{"name":"1.0","ev":-8,"code":100,"cameraName":"1.0"},"1":{"name":"1.1","ev":-7.666666666666667,"code":110,"cameraName":"1.1"},"2":{"name":"1.2","ev":-7.333333333333333,"code":120,"cameraName":"1.2"},"3":{"name":"1.4","ev":-7,"code":140,"cameraName":"1.4"},"4":{"name":"1.6","ev":-6.666666666666667,"code":160,"cameraName":"1.6"},"5":{"name":"1.8","ev":-6.333333333333333,"code":180,"cameraName":"1.8"},"6":{"name":"2.0","ev":-6,"code":200,"cameraName":"2.0"},"7":{"name":"2.2","ev":-5.666666666666667,"code":220,"cameraName":"2.2"},"8":{"name":"2.5","ev":-5.333333333333333,"code":250,"cameraName":"2.5"},"9":{"name":"2.8","ev":-5,"code":280,"cameraName":"2.8"},"10":{"name":"3.2","ev":-4.666666666666667,"code":320,"cameraName":"3.2"},"11":{"name":"3.5","ev":-4.333333333333333,"code":350,"cameraName":"3.5"},"12":{"name":"4.0","ev":-4,"code":400,"cameraName":"4.0"},"13":{"name":"4.5","ev":-3.6666666666666665,"code":450,"cameraName":"4.5"},"14":{"name":"5.0","ev":-3.3333333333333335,"code":500,"cameraName":"5.0"},"15":{"name":"5.6","ev":-3,"code":560,"cameraName":"5.6"},"16":{"name":"6.3","ev":-2.6666666666666665,"code":630,"cameraName":"6.3"},"17":{"name":"7.1","ev":-2.3333333333333335,"code":710,"cameraName":"7.1"},"18":{"name":"8","ev":-2,"code":800,"cameraName":"8"},"19":{"name":"9","ev":-1.6666666666666665,"code":900,"cameraName":"9"},"20":{"name":"10","ev":-1.3333333333333333,"code":1000,"cameraName":"10"},"21":{"name":"11","ev":-1,"code":1100,"cameraName":"11"},"22":{"name":"13","ev":-0.6666666666666666,"code":1300,"cameraName":"13"},"23":{"name":"14","ev":-0.3333333333333333,"code":1400,"cameraName":"14"},"24":{"name":"16","ev":0,"code":1600,"cameraName":"16"},"25":{"name":"18","ev":0.3333333333333333,"code":1800,"cameraName":"18"},"26":{"name":"20","ev":0.6666666666666666,"code":2000,"cameraName":"20"},"27":{"name":"22","ev":1,"code":2200,"cameraName":"22"},"28":{"name":"25","ev":2.3333333333333335,"code":2500,"cameraName":"25"},"29":{"name":"29","ev":2.6666666666666665,"code":2900,"cameraName":"29"},"30":{"name":"32","ev":3,"code":3200,"cameraName":"32"},"31":{"name":"36","ev":3.3333333333333335,"code":3600,"cameraName":"36"},"32":{"name":"42","ev":3.6666666666666665,"code":4200,"cameraName":"42"},"33":{"name":"45","ev":4,"code":4500,"cameraName":"45"},"34":{"name":"50","ev":4.333333333333333,"code":5000,"cameraName":"50"},"35":{"name":"57","ev":4.666666666666667,"code":5700,"cameraName":"57"},"36":{"name":"64","ev":5,"code":6400,"cameraName":"64"}}},"iso":{"name":"100","ev":0,"code":100,"list":{"0":{"name":"AUTO","ev":null,"code":16777215,"cameraName":"AUTO"},"1":{"name":"UNKNOWN (25)","ev":null,"value":null,"code":25,"cameraName":"UNKNOWN (25)"},"2":{"name":"UNKNOWN (50)","ev":null,"value":null,"code":50,"cameraName":"UNKNOWN (50)"},"3":{"name":"UNKNOWN (64)","ev":null,"value":null,"code":64,"cameraName":"UNKNOWN (64)"},"4":{"name":"UNKNOWN (80)","ev":null,"value":null,"code":80,"cameraName":"UNKNOWN (80)"},"5":{"name":"100","ev":0,"code":100,"cameraName":"100"},"6":{"name":"125","ev":-0.3333333333333333,"code":125,"cameraName":"125"},"7":{"name":"160","ev":-0.6666666666666666,"code":160,"cameraName":"160"},"8":{"name":"200","ev":-1,"code":200,"cameraName":"200"},"9":{"name":"250","ev":-1.3333333333333333,"code":250,"cameraName":"250"},"10":{"name":"320","ev":-1.6666666666666665,"code":320,"cameraName":"320"},"11":{"name":"400","ev":-2,"code":400,"cameraName":"400"},"12":{"name":"500","ev":-2.3333333333333335,"code":500,"cameraName":"500"},"13":{"name":"640","ev":-2.6666666666666665,"code":640,"cameraName":"640"},"14":{"name":"800","ev":-3,"code":800,"cameraName":"800"},"15":{"name":"1000","ev":-3.3333333333333335,"code":1000,"cameraName":"1000"},"16":{"name":"1250","ev":-3.6666666666666665,"code":1250,"cameraName":"1250"},"17":{"name":"1600","ev":-4,"code":1600,"cameraName":"1600"},"18":{"name":"2000","ev":-4.333333333333333,"code":2000,"cameraName":"2000"},"19":{"name":"2500","ev":-4.666666666666667,"code":2500,"cameraName":"2500"},"20":{"name":"3200","ev":-5,"code":3200,"cameraName":"3200"},"21":{"name":"4000","ev":-5.333333333333333,"code":4000,"cameraName":"4000"},"22":{"name":"5000","ev":-5.666666666666667,"code":5000,"cameraName":"5000"},"23":{"name":"6400","ev":-6,"code":6400,"cameraName":"6400"},"24":{"name":"8000","ev":-6.333333333333333,"code":8000,"cameraName":"8000"},"25":{"name":"10000","ev":-6.666666666666667,"code":10000,"cameraName":"10000"},"26":{"name":"12800","ev":-7,"code":12800,"cameraName":"12800"},"27":{"name":"16000","ev":-7.333333333333333,"code":16000,"cameraName":"16000"},"28":{"name":"20000","ev":-7.666666666666667,"code":20000,"cameraName":"20000"},"29":{"name":"25600","ev":-8,"code":25600,"cameraName":"25600"},"30":{"name":"32000","ev":-8.333333333333334,"code":32000,"cameraName":"32000"},"31":{"name":"40000","ev":-8.666666666666666,"code":40000,"cameraName":"40000"},"32":{"name":"51200","ev":-9,"code":51200,"cameraName":"51200"},"33":{"name":"64000","ev":-9.333333333333334,"code":64000,"cameraName":"64000"},"34":{"name":"80000","ev":-9.666666666666666,"code":80000,"cameraName":"80000"},"35":{"name":"102400","ev":-10,"code":102400,"cameraName":"102400"},"36":{"name":"128000","ev":-10.333333333333334,"code":128000,"cameraName":"128000"},"37":{"name":"160000","ev":-10.666666666666666,"code":160000,"cameraName":"160000"},"38":{"name":"204800","ev":-11,"code":204800,"cameraName":"204800"},"39":{"name":"UNKNOWN (256000)","ev":null,"value":null,"code":256000,"cameraName":"UNKNOWN (256000)"},"40":{"name":"UNKNOWN (320000)","ev":null,"value":null,"code":320000,"cameraName":"UNKNOWN (320000)"},"41":{"name":"UNKNOWN (409600)","ev":null,"value":null,"code":409600,"cameraName":"UNKNOWN (409600)"},"42":{"name":"UNKNOWN (33554431)","ev":null,"value":null,"code":33554431,"cameraName":"UNKNOWN (33554431)"},"43":{"name":"UNKNOWN (16777241)","ev":null,"value":null,"code":16777241,"cameraName":"UNKNOWN (16777241)"},"44":{"name":"UNKNOWN (16777266)","ev":null,"value":null,"code":16777266,"cameraName":"UNKNOWN (16777266)"},"45":{"name":"UNKNOWN (16777280)","ev":null,"value":null,"code":16777280,"cameraName":"UNKNOWN (16777280)"},"46":{"name":"UNKNOWN (16777296)","ev":null,"value":null,"code":16777296,"cameraName":"UNKNOWN (16777296)"},"47":{"name":"UNKNOWN (16777316)","ev":null,"value":null,"code":16777316,"cameraName":"UNKNOWN (16777316)"},"48":{"name":"UNKNOWN (16777341)","ev":null,"value":null,"code":16777341,"cameraName":"UNKNOWN (16777341)"},"49":{"name":"UNKNOWN (16777376)","ev":null,"value":null,"code":16777376,"cameraName":"UNKNOWN (16777376)"},"50":{"name":"UNKNOWN (16777416)","ev":null,"value":null,"code":16777416,"cameraName":"UNKNOWN (16777416)"},"51":{"name":"UNKNOWN (16777466)","ev":null,"value":null,"code":16777466,"cameraName":"UNKNOWN (16777466)"},"52":{"name":"UNKNOWN (16777536)","ev":null,"value":null,"code":16777536,"cameraName":"UNKNOWN (16777536)"},"53":{"name":"UNKNOWN (16777616)","ev":null,"value":null,"code":16777616,"cameraName":"UNKNOWN (16777616)"},"54":{"name":"UNKNOWN (16777716)","ev":null,"value":null,"code":16777716,"cameraName":"UNKNOWN (16777716)"},"55":{"name":"UNKNOWN (16777856)","ev":null,"value":null,"code":16777856,"cameraName":"UNKNOWN (16777856)"},"56":{"name":"UNKNOWN (16778016)","ev":null,"value":null,"code":16778016,"cameraName":"UNKNOWN (16778016)"},"57":{"name":"UNKNOWN (16778216)","ev":null,"value":null,"code":16778216,"cameraName":"UNKNOWN (16778216)"},"58":{"name":"UNKNOWN (16778466)","ev":null,"value":null,"code":16778466,"cameraName":"UNKNOWN (16778466)"},"59":{"name":"UNKNOWN (16778816)","ev":null,"value":null,"code":16778816,"cameraName":"UNKNOWN (16778816)"},"60":{"name":"UNKNOWN (16779216)","ev":null,"value":null,"code":16779216,"cameraName":"UNKNOWN (16779216)"},"61":{"name":"UNKNOWN (16779716)","ev":null,"value":null,"code":16779716,"cameraName":"UNKNOWN (16779716)"},"62":{"name":"UNKNOWN (16780416)","ev":null,"value":null,"code":16780416,"cameraName":"UNKNOWN (16780416)"},"63":{"name":"UNKNOWN (16781216)","ev":null,"value":null,"code":16781216,"cameraName":"UNKNOWN (16781216)"},"64":{"name":"UNKNOWN (16782216)","ev":null,"value":null,"code":16782216,"cameraName":"UNKNOWN (16782216)"},"65":{"name":"UNKNOWN (16783616)","ev":null,"value":null,"code":16783616,"cameraName":"UNKNOWN (16783616)"},"66":{"name":"UNKNOWN (16785216)","ev":null,"value":null,"code":16785216,"cameraName":"UNKNOWN (16785216)"},"67":{"name":"UNKNOWN (16787216)","ev":null,"value":null,"code":16787216,"cameraName":"UNKNOWN (16787216)"},"68":{"name":"UNKNOWN (16790016)","ev":null,"value":null,"code":16790016,"cameraName":"UNKNOWN (16790016)"},"69":{"name":"UNKNOWN (16793216)","ev":null,"value":null,"code":16793216,"cameraName":"UNKNOWN (16793216)"},"70":{"name":"UNKNOWN (16802816)","ev":null,"value":null,"code":16802816,"cameraName":"UNKNOWN (16802816)"},"71":{"name":"UNKNOWN (16828416)","ev":null,"value":null,"code":16828416,"cameraName":"UNKNOWN (16828416)"},"72":{"name":"UNKNOWN (16879616)","ev":null,"value":null,"code":16879616,"cameraName":"UNKNOWN (16879616)"},"73":{"name":"UNKNOWN (16982016)","ev":null,"value":null,"code":16982016,"cameraName":"UNKNOWN (16982016)"},"74":{"name":"UNKNOWN (17186816)","ev":null,"value":null,"code":17186816,"cameraName":"UNKNOWN (17186816)"},"75":{"name":"UNKNOWN (50331647)","ev":null,"value":null,"code":50331647,"cameraName":"UNKNOWN (50331647)"},"76":{"name":"UNKNOWN (33554532)","ev":null,"value":null,"code":33554532,"cameraName":"UNKNOWN (33554532)"},"77":{"name":"UNKNOWN (33554632)","ev":null,"value":null,"code":33554632,"cameraName":"UNKNOWN (33554632)"},"78":{"name":"UNKNOWN (33554832)","ev":null,"value":null,"code":33554832,"cameraName":"UNKNOWN (33554832)"},"79":{"name":"UNKNOWN (33555232)","ev":null,"value":null,"code":33555232,"cameraName":"UNKNOWN (33555232)"},"80":{"name":"UNKNOWN (33556032)","ev":null,"value":null,"code":33556032,"cameraName":"UNKNOWN (33556032)"},"81":{"name":"UNKNOWN (33557632)","ev":null,"value":null,"code":33557632,"cameraName":"UNKNOWN (33557632)"},"82":{"name":"UNKNOWN (33560832)","ev":null,"value":null,"code":33560832,"cameraName":"UNKNOWN (33560832)"},"83":{"name":"UNKNOWN (33567232)","ev":null,"value":null,"code":33567232,"cameraName":"UNKNOWN (33567232)"},"84":{"name":"UNKNOWN (33580032)","ev":null,"value":null,"code":33580032,"cameraName":"UNKNOWN (33580032)"},"85":{"name":"UNKNOWN (33605632)","ev":null,"value":null,"code":33605632,"cameraName":"UNKNOWN (33605632)"},"86":{"name":"UNKNOWN (33656832)","ev":null,"value":null,"code":33656832,"cameraName":"UNKNOWN (33656832)"},"87":{"name":"UNKNOWN (33759232)","ev":null,"value":null,"code":33759232,"cameraName":"UNKNOWN (33759232)"},"88":{"name":"UNKNOWN (33964032)","ev":null,"value":null,"code":33964032,"cameraName":"UNKNOWN (33964032)"}}}}},"hdrSet":{},"hdrIndex":0,"hdrCount":0,"currentPlanIndex":null,"panDiffNew":0,"tiltDiffNew":0,"focusDiffNew":0,"panDiff":0,"tiltDiff":0,"trackingPanEnabled":false,"trackingTiltEnabled":false,"dynamicChange":{},"trackingTilt":0,"trackingPan":0,"useLiveview":false},"logfile":"20240610-124314 warn:    --minUptime not set. Defaulting to: 1000ms\n20240610-124314 warn:    --spinSleepTime not set. Your script will exit if it does not stay up for at least 1000ms\n20240610-124323 drivers path /home/view/v1.8.52/camera/ptpjs/drivers\n20240610-124323 ready\n20240610-124323 SD card added: /dev/mmcblk1p1\n20240610-124324 >>>>>>> Starting camera module >>>>>>>>","cameras":1,"primary_camera":1,"thumbnail":"/root/time-lapse/tl-261/cam-1-00001.jpg","frames":12}
""".trimIndent()
val json5 = """
    {"id":267,"name":"tl-301","date":"2024-08-08T19:48:11.139Z","program":{"rampMode":"auto","lrtDirection":"auto","intervalMode":"auto","rampAlgorithm":"lum","highlightProtection":true,"interval":5,"dayInterval":"13","nightInterval":"17","frames":300,"destination":"camera","nightLuminance":"-1.3","dayLuminance":0,"isoMax":-4.333333333333333,"isoMin":0,"rampParameters":"S=A=I","apertureMax":-1,"apertureMin":-7,"manualAperture":-5,"hdrCount":0,"hdrStops":1,"exposurePlans":{},"trackingTarget":"moon","autoRestart":true,"keyframes":null,"savedExposurePlans":{},"tracking":"none","delay":1,"scheduled":null,"axes":{"focus":{"type":"disabled"}},"focusPos":0,"coords":null,"loaded":true,"durationSeconds":1800,"schedMonday":true,"schedTuesday":true,"schedWednesday":true,"schedThursday":true,"schedFriday":true,"schedSaturday":true,"schedSunday":true,"schedStop":"06:30","eclipseInfo":"","shutterMax":-9.666666666666666,"schedStart":"17:30"},"status":{"running":true,"frames":0,"framesRemaining":null,"rampRate":-3.427458626685703,"intervalMs":5000,"message":"starting","rampEv":null,"autoSettings":{"paddingTimeMs":3090.04164526859},"exposure":{"status":{"rampEv":null,"highlights":null,"rate":null,"direction":null,"highlightProtection":0},"config":{"sunrise":{"p":0.97,"i":0.5,"d":0.6,"targetTimeSeconds":360,"evIntegrationSeconds":360,"historyIntegrationSeconds":480,"highlightIntegrationFrames":3},"sunset":{"p":1.1,"i":0.6,"d":0.4,"targetTimeSeconds":480,"evIntegrationSeconds":480,"historyIntegrationSeconds":480,"highlightIntegrationFrames":3},"maxEv":20,"minEv":-3.333333333333333,"maxRate":30,"hysteresis":0.4,"nightCompensationDayEv":10,"nightCompensationNightEv":-1,"nightCompensation":"auto","nightLuminance":"-1.3","dayLuminance":0,"highlightProtection":true,"highlightProtectionLimit":1}},"stopping":false,"timeOffsetSeconds":10518,"exposureReferenceEv":null,"tlName":"tl-301","timelapseFolder":"/root/time-lapse/tl-301","first":true,"rampMode":"auto","startTime":1723146490.762,"bufferSeconds":0,"cameraSettings":{"shutter":"13s","aperture":"1.4","iso":"800","details":{"shutter":{"name":"13s","ev":-9.666666666666666,"code":8519690,"duration_ms":13800,"cameraName":"13s","list":{"0":{"name":"30s","ev":-11,"code":19660810,"duration_ms":32000,"cameraName":"30s"},"1":{"name":"25s","ev":-10.666666666666666,"code":16384010,"duration_ms":27000,"cameraName":"25s"},"2":{"name":"20s","ev":-10.333333333333334,"code":13107210,"duration_ms":21500,"cameraName":"20s"},"3":{"name":"15s","ev":-10,"code":9830410,"duration_ms":16000,"cameraName":"15s"},"4":{"name":"13s","ev":-9.666666666666666,"code":8519690,"duration_ms":13800,"cameraName":"13s"},"5":{"name":"10s","ev":-9.333333333333334,"code":6553610,"duration_ms":10600,"cameraName":"10s"},"6":{"name":"8s","ev":-9,"code":5242890,"duration_ms":8000,"cameraName":"8s"},"7":{"name":"6s","ev":-8.666666666666666,"code":3932170,"duration_ms":6000,"cameraName":"6s"},"8":{"name":"5s","ev":-8.333333333333334,"code":3276810,"duration_ms":5000,"cameraName":"5s"},"9":{"name":"4s","ev":-8,"code":2621450,"duration_ms":4000,"cameraName":"4s"},"10":{"name":"3s","ev":-7.666666666666667,"code":2097162,"duration_ms":3000,"cameraName":"3s"},"11":{"name":"2.5s","ev":-7.333333333333333,"code":1638410,"duration_ms":2500,"cameraName":"2.5s"},"12":{"name":"2s","ev":-7,"code":1310730,"duration_ms":2000,"cameraName":"2s"},"13":{"name":"1.6s","ev":-6.666666666666667,"code":1048586,"duration_ms":1600,"cameraName":"1.6s"},"14":{"name":"1.3s","ev":-6.333333333333333,"code":851978,"duration_ms":1300,"cameraName":"1.3s"},"15":{"name":"1s","ev":-6,"code":655370,"duration_ms":1000,"cameraName":"1s"},"16":{"name":"0.8s","ev":-5.666666666666667,"code":524298,"duration_ms":800,"cameraName":"0.8s"},"17":{"name":"0.6s","ev":-5.333333333333333,"code":393226,"duration_ms":600,"cameraName":"0.6s"},"18":{"name":"1/2","ev":-5,"code":327690,"duration_ms":500,"cameraName":"1/2"},"19":{"name":"0.4s","ev":-4.666666666666667,"code":262154,"duration_ms":400,"cameraName":"0.4s"},"20":{"name":"1/3","ev":-4.333333333333333,"code":65539,"duration_ms":333,"cameraName":"1/3"},"21":{"name":"1/4","ev":-4,"code":65540,"duration_ms":250,"cameraName":"1/4"},"22":{"name":"1/5","ev":-3.6666666666666665,"code":65541,"duration_ms":200,"cameraName":"1/5"},"23":{"name":"1/6","ev":-3.3333333333333335,"code":65542,"duration_ms":150,"cameraName":"1/6"},"24":{"name":"1/8","ev":-3,"code":65544,"duration_ms":125,"cameraName":"1/8"},"25":{"name":"1/10","ev":-2.6666666666666665,"code":65546,"duration_ms":100,"cameraName":"1/10"},"26":{"name":"1/13","ev":-2.3333333333333335,"code":65549,"duration_ms":100,"cameraName":"1/13"},"27":{"name":"1/15","ev":-2,"code":65551,"duration_ms":100,"cameraName":"1/15"},"28":{"name":"1/20","ev":-1.6666666666666665,"code":65556,"duration_ms":100,"cameraName":"1/20"},"29":{"name":"1/25","ev":-1.3333333333333333,"code":65561,"duration_ms":100,"cameraName":"1/25"},"30":{"name":"1/30","ev":-1,"code":65566,"duration_ms":100,"cameraName":"1/30"},"31":{"name":"1/40","ev":-0.6666666666666666,"code":65576,"duration_ms":100,"cameraName":"1/40"},"32":{"name":"1/50","ev":-0.3333333333333333,"code":65586,"duration_ms":100,"cameraName":"1/50"},"33":{"name":"1/60","ev":0,"code":65596,"duration_ms":100,"cameraName":"1/60"},"34":{"name":"1/80","ev":0.3333333333333333,"code":65616,"duration_ms":100,"cameraName":"1/80"},"35":{"name":"1/100","ev":0.6666666666666666,"code":65636,"duration_ms":100,"cameraName":"1/100"},"36":{"name":"1/125","ev":1,"code":65661,"duration_ms":100,"cameraName":"1/125"},"37":{"name":"1/160","ev":1.3333333333333333,"code":65696,"duration_ms":100,"cameraName":"1/160"},"38":{"name":"1/200","ev":1.6666666666666665,"code":65736,"duration_ms":100,"cameraName":"1/200"},"39":{"name":"1/250","ev":2,"code":65786,"duration_ms":100,"cameraName":"1/250"},"40":{"name":"1/320","ev":2.3333333333333335,"code":65856,"duration_ms":100,"cameraName":"1/320"},"41":{"name":"1/400","ev":2.6666666666666665,"code":65936,"duration_ms":100,"cameraName":"1/400"},"42":{"name":"1/500","ev":3,"code":66036,"duration_ms":100,"cameraName":"1/500"},"43":{"name":"1/640","ev":3.3333333333333335,"code":66176,"duration_ms":100,"cameraName":"1/640"},"44":{"name":"1/800","ev":3.6666666666666665,"code":66336,"duration_ms":100,"cameraName":"1/800"},"45":{"name":"1/1000","ev":4,"code":66536,"duration_ms":100,"cameraName":"1/1000"},"46":{"name":"1/1250","ev":4.333333333333333,"code":66786,"duration_ms":100,"cameraName":"1/1250"},"47":{"name":"1/1600","ev":4.666666666666667,"code":67136,"duration_ms":100,"cameraName":"1/1600"},"48":{"name":"1/2000","ev":5,"code":67536,"duration_ms":100,"cameraName":"1/2000"},"49":{"name":"1/2500","ev":5.333333333333333,"code":68036,"duration_ms":100,"cameraName":"1/2500"},"50":{"name":"1/3200","ev":5.666666666666667,"code":68736,"duration_ms":100,"cameraName":"1/3200"},"51":{"name":"1/4000","ev":6,"code":69536,"duration_ms":100,"cameraName":"1/4000"},"52":{"name":"1/5000","ev":6.333333333333333,"code":70536,"duration_ms":100,"cameraName":"1/5000"},"53":{"name":"1/6400","ev":6.666666666666667,"code":71936,"duration_ms":100,"cameraName":"1/6400"},"54":{"name":"1/8000","ev":7,"code":73536,"duration_ms":100,"cameraName":"1/8000"}}},"aperture":{"name":"1.4","ev":-7,"code":140,"cameraName":"1.4","list":{"0":{"name":"1.0","ev":-8,"code":100,"cameraName":"1.0"},"1":{"name":"1.1","ev":-7.666666666666667,"code":110,"cameraName":"1.1"},"2":{"name":"1.2","ev":-7.333333333333333,"code":120,"cameraName":"1.2"},"3":{"name":"1.4","ev":-7,"code":140,"cameraName":"1.4"},"4":{"name":"1.6","ev":-6.666666666666667,"code":160,"cameraName":"1.6"},"5":{"name":"1.8","ev":-6.333333333333333,"code":180,"cameraName":"1.8"},"6":{"name":"2.0","ev":-6,"code":200,"cameraName":"2.0"},"7":{"name":"2.2","ev":-5.666666666666667,"code":220,"cameraName":"2.2"},"8":{"name":"2.5","ev":-5.333333333333333,"code":250,"cameraName":"2.5"},"9":{"name":"2.8","ev":-5,"code":280,"cameraName":"2.8"},"10":{"name":"3.2","ev":-4.666666666666667,"code":320,"cameraName":"3.2"},"11":{"name":"3.5","ev":-4.333333333333333,"code":350,"cameraName":"3.5"},"12":{"name":"4.0","ev":-4,"code":400,"cameraName":"4.0"},"13":{"name":"4.5","ev":-3.6666666666666665,"code":450,"cameraName":"4.5"},"14":{"name":"5.0","ev":-3.3333333333333335,"code":500,"cameraName":"5.0"},"15":{"name":"5.6","ev":-3,"code":560,"cameraName":"5.6"},"16":{"name":"6.3","ev":-2.6666666666666665,"code":630,"cameraName":"6.3"},"17":{"name":"7.1","ev":-2.3333333333333335,"code":710,"cameraName":"7.1"},"18":{"name":"8","ev":-2,"code":800,"cameraName":"8"},"19":{"name":"9","ev":-1.6666666666666665,"code":900,"cameraName":"9"},"20":{"name":"10","ev":-1.3333333333333333,"code":1000,"cameraName":"10"},"21":{"name":"11","ev":-1,"code":1100,"cameraName":"11"},"22":{"name":"13","ev":-0.6666666666666666,"code":1300,"cameraName":"13"},"23":{"name":"14","ev":-0.3333333333333333,"code":1400,"cameraName":"14"},"24":{"name":"16","ev":0,"code":1600,"cameraName":"16"},"25":{"name":"18","ev":0.3333333333333333,"code":1800,"cameraName":"18"},"26":{"name":"20","ev":0.6666666666666666,"code":2000,"cameraName":"20"},"27":{"name":"22","ev":1,"code":2200,"cameraName":"22"},"28":{"name":"25","ev":2.3333333333333335,"code":2500,"cameraName":"25"},"29":{"name":"29","ev":2.6666666666666665,"code":2900,"cameraName":"29"},"30":{"name":"32","ev":3,"code":3200,"cameraName":"32"},"31":{"name":"36","ev":3.3333333333333335,"code":3600,"cameraName":"36"},"32":{"name":"42","ev":3.6666666666666665,"code":4200,"cameraName":"42"},"33":{"name":"45","ev":4,"code":4500,"cameraName":"45"},"34":{"name":"50","ev":4.333333333333333,"code":5000,"cameraName":"50"},"35":{"name":"57","ev":4.666666666666667,"code":5700,"cameraName":"57"},"36":{"name":"64","ev":5,"code":6400,"cameraName":"64"}}},"iso":{"name":"800","ev":-3,"code":800,"cameraName":"800","list":{"0":{"name":"AUTO","ev":null,"code":16777215,"cameraName":"AUTO"},"1":{"name":"UNKNOWN (25)","ev":null,"value":null,"code":25},"2":{"name":"UNKNOWN (50)","ev":null,"value":null,"code":50},"3":{"name":"UNKNOWN (64)","ev":null,"value":null,"code":64},"4":{"name":"UNKNOWN (80)","ev":null,"value":null,"code":80},"5":{"name":"100","ev":0,"code":100,"cameraName":"100"},"6":{"name":"125","ev":-0.3333333333333333,"code":125,"cameraName":"125"},"7":{"name":"160","ev":-0.6666666666666666,"code":160,"cameraName":"160"},"8":{"name":"200","ev":-1,"code":200,"cameraName":"200"},"9":{"name":"250","ev":-1.3333333333333333,"code":250,"cameraName":"250"},"10":{"name":"320","ev":-1.6666666666666665,"code":320,"cameraName":"320"},"11":{"name":"400","ev":-2,"code":400,"cameraName":"400"},"12":{"name":"500","ev":-2.3333333333333335,"code":500,"cameraName":"500"},"13":{"name":"640","ev":-2.6666666666666665,"code":640,"cameraName":"640"},"14":{"name":"800","ev":-3,"code":800,"cameraName":"800"},"15":{"name":"1000","ev":-3.3333333333333335,"code":1000,"cameraName":"1000"},"16":{"name":"1250","ev":-3.6666666666666665,"code":1250,"cameraName":"1250"},"17":{"name":"1600","ev":-4,"code":1600,"cameraName":"1600"},"18":{"name":"2000","ev":-4.333333333333333,"code":2000,"cameraName":"2000"},"19":{"name":"2500","ev":-4.666666666666667,"code":2500,"cameraName":"2500"},"20":{"name":"3200","ev":-5,"code":3200,"cameraName":"3200"},"21":{"name":"4000","ev":-5.333333333333333,"code":4000,"cameraName":"4000"},"22":{"name":"5000","ev":-5.666666666666667,"code":5000,"cameraName":"5000"},"23":{"name":"6400","ev":-6,"code":6400,"cameraName":"6400"},"24":{"name":"8000","ev":-6.333333333333333,"code":8000,"cameraName":"8000"},"25":{"name":"10000","ev":-6.666666666666667,"code":10000,"cameraName":"10000"},"26":{"name":"12800","ev":-7,"code":12800,"cameraName":"12800"},"27":{"name":"16000","ev":-7.333333333333333,"code":16000,"cameraName":"16000"},"28":{"name":"20000","ev":-7.666666666666667,"code":20000,"cameraName":"20000"},"29":{"name":"25600","ev":-8,"code":25600,"cameraName":"25600"},"30":{"name":"32000","ev":-8.333333333333334,"code":32000,"cameraName":"32000"},"31":{"name":"40000","ev":-8.666666666666666,"code":40000,"cameraName":"40000"},"32":{"name":"51200","ev":-9,"code":51200,"cameraName":"51200"},"33":{"name":"64000","ev":-9.333333333333334,"code":64000,"cameraName":"64000"},"34":{"name":"80000","ev":-9.666666666666666,"code":80000,"cameraName":"80000"},"35":{"name":"102400","ev":-10,"code":102400,"cameraName":"102400"},"36":{"name":"128000","ev":-10.333333333333334,"code":128000,"cameraName":"128000"},"37":{"name":"160000","ev":-10.666666666666666,"code":160000,"cameraName":"160000"},"38":{"name":"204800","ev":-11,"code":204800,"cameraName":"204800"},"39":{"name":"UNKNOWN (256000)","ev":null,"value":null,"code":256000},"40":{"name":"UNKNOWN (320000)","ev":null,"value":null,"code":320000},"41":{"name":"UNKNOWN (409600)","ev":null,"value":null,"code":409600},"42":{"name":"UNKNOWN (33554431)","ev":null,"value":null,"code":33554431},"43":{"name":"UNKNOWN (16777241)","ev":null,"value":null,"code":16777241},"44":{"name":"UNKNOWN (16777266)","ev":null,"value":null,"code":16777266},"45":{"name":"UNKNOWN (16777280)","ev":null,"value":null,"code":16777280},"46":{"name":"UNKNOWN (16777296)","ev":null,"value":null,"code":16777296},"47":{"name":"UNKNOWN (16777316)","ev":null,"value":null,"code":16777316},"48":{"name":"UNKNOWN (16777341)","ev":null,"value":null,"code":16777341},"49":{"name":"UNKNOWN (16777376)","ev":null,"value":null,"code":16777376},"50":{"name":"UNKNOWN (16777416)","ev":null,"value":null,"code":16777416},"51":{"name":"UNKNOWN (16777466)","ev":null,"value":null,"code":16777466},"52":{"name":"UNKNOWN (16777536)","ev":null,"value":null,"code":16777536},"53":{"name":"UNKNOWN (16777616)","ev":null,"value":null,"code":16777616},"54":{"name":"UNKNOWN (16777716)","ev":null,"value":null,"code":16777716},"55":{"name":"UNKNOWN (16777856)","ev":null,"value":null,"code":16777856},"56":{"name":"UNKNOWN (16778016)","ev":null,"value":null,"code":16778016},"57":{"name":"UNKNOWN (16778216)","ev":null,"value":null,"code":16778216},"58":{"name":"UNKNOWN (16778466)","ev":null,"value":null,"code":16778466},"59":{"name":"UNKNOWN (16778816)","ev":null,"value":null,"code":16778816},"60":{"name":"UNKNOWN (16779216)","ev":null,"value":null,"code":16779216},"61":{"name":"UNKNOWN (16779716)","ev":null,"value":null,"code":16779716},"62":{"name":"UNKNOWN (16780416)","ev":null,"value":null,"code":16780416},"63":{"name":"UNKNOWN (16781216)","ev":null,"value":null,"code":16781216},"64":{"name":"UNKNOWN (16782216)","ev":null,"value":null,"code":16782216},"65":{"name":"UNKNOWN (16783616)","ev":null,"value":null,"code":16783616},"66":{"name":"UNKNOWN (16785216)","ev":null,"value":null,"code":16785216},"67":{"name":"UNKNOWN (16787216)","ev":null,"value":null,"code":16787216},"68":{"name":"UNKNOWN (16790016)","ev":null,"value":null,"code":16790016},"69":{"name":"UNKNOWN (16793216)","ev":null,"value":null,"code":16793216},"70":{"name":"UNKNOWN (16802816)","ev":null,"value":null,"code":16802816},"71":{"name":"UNKNOWN (16828416)","ev":null,"value":null,"code":16828416},"72":{"name":"UNKNOWN (16879616)","ev":null,"value":null,"code":16879616},"73":{"name":"UNKNOWN (16982016)","ev":null,"value":null,"code":16982016},"74":{"name":"UNKNOWN (17186816)","ev":null,"value":null,"code":17186816},"75":{"name":"UNKNOWN (50331647)","ev":null,"value":null,"code":50331647},"76":{"name":"UNKNOWN (33554532)","ev":null,"value":null,"code":33554532},"77":{"name":"UNKNOWN (33554632)","ev":null,"value":null,"code":33554632},"78":{"name":"UNKNOWN (33554832)","ev":null,"value":null,"code":33554832},"79":{"name":"UNKNOWN (33555232)","ev":null,"value":null,"code":33555232},"80":{"name":"UNKNOWN (33556032)","ev":null,"value":null,"code":33556032},"81":{"name":"UNKNOWN (33557632)","ev":null,"value":null,"code":33557632},"82":{"name":"UNKNOWN (33560832)","ev":null,"value":null,"code":33560832},"83":{"name":"UNKNOWN (33567232)","ev":null,"value":null,"code":33567232},"84":{"name":"UNKNOWN (33580032)","ev":null,"value":null,"code":33580032},"85":{"name":"UNKNOWN (33605632)","ev":null,"value":null,"code":33605632},"86":{"name":"UNKNOWN (33656832)","ev":null,"value":null,"code":33656832},"87":{"name":"UNKNOWN (33759232)","ev":null,"value":null,"code":33759232},"88":{"name":"UNKNOWN (33964032)","ev":null,"value":null,"code":33964032}}}}},"hdrSet":{},"hdrIndex":0,"hdrCount":0,"currentPlanIndex":null,"panDiffNew":0,"tiltDiffNew":0,"focusDiffNew":0,"panDiff":0,"tiltDiff":0,"trackingPanEnabled":false,"trackingTiltEnabled":false,"dynamicChange":{},"trackingTilt":0,"trackingPan":0,"useLiveview":false,"id":266,"minutesUntilStart":-312,"cameraEv":-5.666666666666666,"evDiff":0.06952869795379257,"captureStartTime":1723146458.577,"lastPhotoTime":85.83200001716614,"path":"DSC09185.JPG","lastPhotoLum":-1.6079412017307015},"logfile":"/var/log/view-core-20240808-194047.txt","cameras":1,"primary_camera":1,"thumbnail":"/root/time-lapse/tl-301/cam-1-00001.jpg","frames":120}
""".trimIndent()

//language=json
val program = """
    {
      "program": {
        "rampMode": "auto",
        "lrtDirection": "auto",
        "intervalMode": "auto",
        "rampAlgorithm": "lum",
        "highlightProtection": true,
        "interval": "5",
        "dayInterval": 5,
        "nightInterval": 30,
        "frames": 300,
        "destination": "camera",
        "nightLuminance": "-1.3",
        "dayLuminance": 0,
        "isoMax": -3,
        "isoMin": 0,
        "rampParameters": "S=A=I",
        "apertureMax": -1,
        "apertureMin": -7,
        "manualAperture": -6,
        "hdrCount": 0,
        "hdrStops": 1,
        "exposurePlans": [],
        "trackingTarget": "moon",
        "autoRestart": false,
        "keyframes": [
          {
            "focus": 0,
            "ev": "not set",
            "motor": {}
          }
        ],
        "savedExposurePlans": [
          {
            "name": "Pre-eclipse",
            "start": "2025-02-23T06:50:49.164Z",
            "mode": "locked",
            "hdrCount": 0,
            "intervalMode": "fixed",
            "interval": 12
          },
          {
            "name": "Partial (C1-C4)",
            "start": 1743246290100,
            "mode": "preset",
            "hdrCount": 0,
            "intervalMode": "fixed",
            "interval": 12,
            "shutter": 6,
            "iso": 0,
            "aperture": -3
          },
          {
            "name": "Post-eclipse",
            "start": 1743250025800,
            "mode": "auto",
            "hdrCount": 0,
            "intervalMode": "auto",
            "dayInterval": 12,
            "nightInterval": 36
          }
        ],
        "tracking": "none",
        "delay": 1,
        "scheduled": null,
        "axes": {},
        "focusPos": 0,
        "coords": {
          "lat": 53,
          "lon": 27,
          "alt": 0,
          "src": "manual"
        },
        "loaded": true,
        "durationSeconds": 1800,
        "schedMonday": true,
        "schedTuesday": true,
        "schedWednesday": true,
        "schedThursday": true,
        "schedFriday": true,
        "schedSaturday": true,
        "schedSunday": true,
        "schedStop": "07:00",
        "eclipseInfo": "Next eclipse: 29 Mar 2025, with first contact starting at 2:04:50 PM +0300.\nThis is a partial eclipse with 4% coverage observable from the current location (53, 27).",
        "shutterMax": -10.333333333333334,
        "schedStart": "17:30",
        "_timeOffsetSeconds": 10494,
        "_exposureReferenceEv": -3.214070651666502,
        "utcOffset": 180,
        "eclipseFirstContact": 1743246290100
      },
      "ack": "2dwh2b0yg9",
      "type": "timelapseProgram"
    }
""".trimIndent()

@Serializable
@SerialName("timelapse-clip-info")
data class TimelapseClipInfoMessage(
    val info: TimelapseClipInfo,
    override val type: String,
    val ack: String
) : BaseMessage() {

}

@Serializable
data class TimelapseClipInfo(
    val id: Int,
    val name: String,
    val date: String,
    val program: Program,
    val status: Status,
    val logfile: String,
    val cameras: Int,
    val primary_camera: Int,
    val thumbnail: String? = null,
    val frames: Int?,
    val path: String? = null,
    val cameraEv: Double? = null,
    val minutesUntilStart: Int? = null
)

open class DynamicListSerializer<T>(val baseSer: KSerializer<T>) : KSerializer<List<T>> {

    override val descriptor = ListSerializer(baseSer).descriptor

    override fun deserialize(decoder: Decoder): List<T> {
        return when (val element = decoder.decodeSerializableValue(JsonElement.serializer())) {
            is JsonArray -> Json.decodeFromJsonElement(ListSerializer(baseSer), element)
            else         -> emptyList()
        }
    }

    override fun serialize(encoder: Encoder, value: List<T>) {
        encoder.encodeSerializableValue(ListSerializer(baseSer), value)
    }
}

@Serializable
data class Program(
    val rampMode: String,
    val lrtDirection: String,
    val intervalMode: String,
    val rampAlgorithm: String,
    val highlightProtection: Boolean,
    val interval: String,
    val dayInterval: Double,
    val nightInterval: Double,
    val frames: Int,
    val destination: String,
    val nightLuminance: Double,
    val dayLuminance: Double,
    val isoMax: Double,
    val isoMin: Int,
    val rampParameters: String,
    val apertureMax: Int,
    val apertureMin: Int,
    val manualAperture: Int,
    val hdrCount: Int,
    val hdrStops: Int,
    @Serializable(with = SavedExposurePlanSerializer::class)
    val exposurePlans: List<SavedExposurePlan> = emptyList(),
    val trackingTarget: String,
    val autoRestart: Boolean,
    //val keyframes: Map<String, @Contextual Any>? = null,
    @Serializable(with = SavedExposurePlanSerializer::class)
    val savedExposurePlans: List<SavedExposurePlan> = emptyList(),
    val tracking: String,
    val delay: Int,
    val scheduled: Boolean? = null,
    val axes: Axes,
    val focusPos: Int,
    val coords: Coords? = null,
    val loaded: Boolean? = null,
    val durationSeconds: Int? = null,
    val schedMonday: Boolean? = null,
    val schedTuesday: Boolean? = null,
    val schedWednesday: Boolean? = null,
    val schedThursday: Boolean? = null,
    val schedFriday: Boolean? = null,
    val schedSaturday: Boolean? = null,
    val schedSunday: Boolean? = null,
    val schedStop: String? = null,
    val eclipseInfo: String? = null,
    val shutterMax: Double? = null,
    val schedStart: String? = null,
    val utcOffset: Int? = null,  // Missing field
    val eclipseFirstContact: Long? = null, // Missing field
    val _timeOffsetSeconds: Int? = null,
    val _exposureReferenceEv: Double? = null
)

object SavedExposurePlanSerializer : DynamicListSerializer<SavedExposurePlan>(SavedExposurePlan.serializer())

@Serializable
data class SavedExposurePlan(
    val name: String,
    val start: String,
    val mode: String,
    val hdrCount: Int,
    val intervalMode: String,
    val interval: Int,
    val shutter: Int? = null,
    val iso: Int? = null,
    val aperture: Int? = null
)

@Serializable
data class Axes(
    val focus: Focus? = null
)

@Serializable
data class Focus(
    val type: String
)

@Serializable
data class Coords(
    val lat: Double,
    val lon: Double,
    val alt: Double,
    val src: String
)

@Serializable
data class Status(
    val running: Boolean,
    val frames: Int,
    val framesRemaining: Int? = null,
    val rampRate: Double? = null,
    val intervalMs: Int,
    val message: String,
    val rampEv: Double? = null,
    val autoSettings: AutoSettings,
    val exposure: Exposure,
    val stopping: Boolean? = false,
    val timeOffsetSeconds: Int? = 0,
    val exposureReferenceEv: Double? = null,
    val tlName: String,
    val timelapseFolder: String,
    val first: Boolean = true,
    val rampMode: String? = null,
    val startTime: Double,
    val bufferSeconds: Int = 0,
    val cameraSettings: CameraSettings,
    //val hdrSet: Map<String, @Contextual Any?>,
    val hdrIndex: Int,
    val hdrCount: Int,
    val currentPlanIndex: @Contextual Any?,
    val panDiffNew: Int,
    val tiltDiffNew: Int,
    val focusDiffNew: Int,
    val panDiff: Int,
    val tiltDiff: Int,
    val trackingPanEnabled: Boolean,
    val trackingTiltEnabled: Boolean,
    //val dynamicChange: Map<String, @Contextual Any?>,
    val trackingTilt: Int,
    val trackingPan: Int,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val sunPos: SunPos? = null,
    val moonPos: MoonPos? = null,
    val useLiveview: Boolean = false,
    val minutesUntilStart: Int? = null,
    val cameraEv: Double? = null

)

@Serializable
data class AutoSettings(
    val paddingTimeMs: Double
)

@Serializable
data class Exposure(
    val status: ExposureStatus,
    val config: ExposureConfig
)

@Serializable
data class ExposureStatus(
    val rampEv: @Contextual Any? = null,
    val highlights: @Contextual Any? = null,
    val rate: @Contextual Any? = null,
    val direction: @Contextual Any? = null,
    val highlightProtection: Int? = null
)

@Serializable
data class ExposureConfig(
    val sunrise: Sunrise,
    val sunset: Sunset,
    val maxEv: Double,
    val minEv: Double,
    val maxRate: Int,
    val hysteresis: Double,
    val nightCompensationDayEv: Int,
    val nightCompensationNightEv: Int,
    val nightCompensation: String,
    val nightLuminance: String,
    val dayLuminance: Double,
    val highlightProtection: Boolean,
    val highlightProtectionLimit: Int
)

@Serializable
data class Sunrise(
    val p: Double,
    val i: Double,
    val d: Double,
    val targetTimeSeconds: Int,
    val evIntegrationSeconds: Int,
    val historyIntegrationSeconds: Int,
    val highlightIntegrationFrames: Int
)

@Serializable
data class Sunset(
    val p: Double,
    val i: Double,
    val d: Double,
    val targetTimeSeconds: Int,
    val evIntegrationSeconds: Int,
    val historyIntegrationSeconds: Int,
    val highlightIntegrationFrames: Int
)

@Serializable
data class CameraSettings(
    val shutter: String,
    val aperture: String,
    val battery: Double? = null,
    val focusPos: Double? = null,
    val iso: String,
    val details: CameraDetails
)

@Serializable
data class CameraDetails(
    val shutter: Shutter,
    val aperture: Aperture? = null,
    val iso: ISO
)

@Serializable
data class Shutter(
    val name: String,
    val ev: Double,
    val code: Int? = null,
    val duration_ms: Int? = null,
    val cameraName: String? = null,

    @Serializable(ShutterEntrySerializer::class)
    val list: List<ShutterEntry> = emptyList()
)

object ShutterEntrySerializer : DynamicListSerializer<ShutterEntry>(ShutterEntry.serializer())

@Serializable
data class ShutterEntry(
    val name: String,
    val ev: Double,
    val code: Int,
    val duration_ms: Int,
    val cameraName: String
)

@Serializable
data class Aperture(
    val name: String,
    val ev: Double? = null,
    val code: Int? = null,
    val cameraName: String? = null,
    @Serializable(ApertureEntrySerializer::class)
    val list: List<ApertureEntry> = emptyList()
)

object ApertureEntrySerializer : DynamicListSerializer<ApertureEntry>(ApertureEntry.serializer())

@Serializable
data class ApertureEntry(
    val name: String,
    val ev: Double,
    val code: Int,
    val cameraName: String
)

@Serializable
data class ISO(
    val name: String,
    val ev: Double,
    val code: Int? = null,
    val cameraName: String? = null,
    @Serializable(ISOEntrySerializer::class)
    val list: List<ISOEntry> = emptyList()
)

object ISOEntrySerializer : DynamicListSerializer<ISOEntry>(ISOEntry.serializer())

@Serializable
data class ISOEntry(
    val name: String,
    val ev: Double? = null,
    val code: Int,
    val cameraName: String? = null,
    val value: Double? = null
)

@Serializable
data class SunPos(
    val azimuth: Double,
    val altitude: Double
)

@Serializable
data class MoonPos(
    val azimuth: Double,
    val altitude: Double
)
