package github.naixx.db

import android.content.Context
import androidx.room.Room
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/** Room for the Android graph; [RoomClipRepository] contributes itself as the [github.naixx.core.ClipRepository]. */
@ContributesTo(AppScope::class)
@BindingContainer
object DbBindings {

    @Provides
    @SingleIn(AppScope::class)
    fun database(context: Context): ClipDatabase = Room.databaseBuilder(
        context,
        ClipDatabase::class.java,
        "clips_database"
    )
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun clipDao(database: ClipDatabase): ClipDao = database.clipDao()

    @Provides
    fun clipInfoDao(database: ClipDatabase): TimelapseClipInfoDao = database.timelapseClipInfoDao()
}
