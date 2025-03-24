package github.naixx.network

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "timelapseClipInfo")
@Serializable
data class TimelapseClipInfo(
    @PrimaryKey
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
