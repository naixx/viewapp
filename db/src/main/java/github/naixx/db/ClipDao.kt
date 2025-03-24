package github.naixx.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import github.naixx.network.Clip
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
    @Query("SELECT * FROM clips ORDER BY `index` DESC")
    fun getAllClips(): Flow<List<Clip>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClips(clips: List<Clip>)

    @Query("DELETE FROM clips")
    suspend fun deleteAllClips()
}
