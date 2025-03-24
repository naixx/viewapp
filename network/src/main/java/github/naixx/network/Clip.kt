package github.naixx.network

import androidx.room.*
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
    val imageBase64: String
) : com.github.naixx.core.SerializableInterop
