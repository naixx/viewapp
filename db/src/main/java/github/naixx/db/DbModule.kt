package github.naixx.db

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dbModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            ClipDatabase::class.java,
            "clips_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    single { get<ClipDatabase>().clipDao() }

    single { get<ClipDatabase>().timelapseClipInfoDao() }

    single<ClipRepository> { ClipRepositoryImpl(get()) }
}
