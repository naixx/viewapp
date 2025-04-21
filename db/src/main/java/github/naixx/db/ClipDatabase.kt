package github.naixx.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import github.naixx.network.Clip
import github.naixx.network.TimelapseClipInfo

@Database(entities = [Clip::class, TimelapseClipInfo::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ClipDatabase : RoomDatabase() {
    abstract fun clipDao(): ClipDao
    abstract fun timelapseClipInfoDao(): TimelapseClipInfoDao
}
