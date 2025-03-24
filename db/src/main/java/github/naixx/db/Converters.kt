package github.naixx.db

import androidx.room.TypeConverter
import github.naixx.network.*
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @TypeConverter
    fun fromProgram(program: Program): String {
        return json.encodeToString(program)
    }

    @TypeConverter
    fun toProgram(programJson: String): Program {
        return json.decodeFromString(programJson)
    }

    @TypeConverter
    fun fromStatus(status: Status): String {
        return json.encodeToString(status)
    }

    @TypeConverter
    fun toStatus(statusJson: String): Status {
        return json.decodeFromString(statusJson)
    }
}
