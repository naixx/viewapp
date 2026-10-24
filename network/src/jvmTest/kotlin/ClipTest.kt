import github.naixx.network.Clip
import github.naixx.network.createJson
import kotlinx.serialization.builtins.ListSerializer
import kotlin.test.Test
import kotlin.test.assertEquals

class ClipTest {

    // GET /clips mixes clips from the device database (with an id) and older ones found on the file system (no id, a
    // path instead). One old clip used to fail the whole list: "Field 'id' is required for type Clip".
    @Test
    fun clipsWithoutIdParseWithTheNegativeIndexAsId() {
        val json = """[{"index":522,"id":487,"frames":8,"name":"TL-522"},""" +
            """{"frames":19,"index":501,"name":"TL-501","path":"/root/time-lapse/tl-501/img%05d.jpg"}]"""
        val clips = createJson().decodeFromString(ListSerializer(Clip.serializer()), json)
        assertEquals(listOf(487, -501), clips.map { it.id })
        assertEquals(listOf("TL-522", "TL-501"), clips.map { it.name })
    }
}
