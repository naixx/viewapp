package github.naixx.network

import androidx.room.*
import com.github.naixx.core.SerializableInterop
import kotlinx.serialization.*

@Entity(tableName = "clips")
@Serializable
data class Clip(
    val index: Int,
    /** The device database id. Older clips in `/clips` come from the file system without one (a `path` instead): they key by `-index`, never a device id. */
    @PrimaryKey
    val id: Int = -index,
    val frames: Int,
    val name: String,
    @SerialName("image")
    override val imageBase64: String?
) : SerializableInterop, CoilMapper

interface CoilMapper{
    val imageBase64: String?
}
