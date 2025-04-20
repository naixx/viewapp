package github.naixx.network

import androidx.room.*
import com.github.naixx.core.SerializableInterop
import kotlinx.serialization.*

@Entity(tableName = "clips")
@Serializable
data class Clip(
    val index: Int,
    @PrimaryKey
    val id: Int,
    val frames: Int,
    val name: String,
    @SerialName("image")
    override val imageBase64: String
) : SerializableInterop, CoilMapper

interface CoilMapper{
    val imageBase64: String
}
