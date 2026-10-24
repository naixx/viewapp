package github.naixx.db

import github.naixx.core.ClipRepository
import github.naixx.network.Clip
import github.naixx.network.TimelapseClipInfo
import github.naixx.network.ViewApi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The Android [ClipRepository]: Room, so the Clips tab works offline and clip details survive restarts. */
class RoomClipRepository(
    private val clipDao: ClipDao,
    private val clipInfoDao: TimelapseClipInfoDao,
) : ClipRepository {

    override fun getAllClips(): Flow<ImmutableList<Clip>> {
        return clipDao.getAllClips().map { it.toImmutableList() }
    }

    override suspend fun refreshClips(api: ViewApi) {
        val remoteClips = api.clips()
        val remoteIds = remoteClips.map { it.id }
        clipDao.deleteClipsNotIn(remoteIds)
        clipDao.insertClips(remoteClips)
    }

    override suspend fun clearClips() {
        clipDao.deleteAllClips()
    }

    override fun clipInfo(name: String): Flow<TimelapseClipInfo?> = clipInfoDao.getTimelapseClipInfo(name)

    override suspend fun saveClipInfo(info: TimelapseClipInfo) = clipInfoDao.insertTimelapseClipInfo(listOf(info))
}
