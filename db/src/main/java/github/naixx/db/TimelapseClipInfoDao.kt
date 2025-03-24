package github.naixx.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import github.naixx.network.TimelapseClipInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelapseClipInfoDao {
    @Query("SELECT * FROM timelapseClipInfo ORDER BY id ASC")
    fun getAllTimelapseClipInfo(): Flow<List<TimelapseClipInfo>>

    @Query("SELECT * FROM timelapseClipInfo WHERE LOWER(name) = LOWER(:clipName) LIMIT 1")
    fun getTimelapseClipInfo(clipName: String): Flow<TimelapseClipInfo?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimelapseClipInfo(clipInfos: List<TimelapseClipInfo>)

    @Query("DELETE FROM timelapseClipInfo")
    suspend fun deleteAllTimelapseClipInfo()
}
