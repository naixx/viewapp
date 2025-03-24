package github.naixx.db

import github.naixx.network.Clip
import github.naixx.network.ViewApi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ClipRepository {
    fun getAllClips(): Flow<ImmutableList<Clip>>
    suspend fun refreshClips(viewApi: ViewApi)
    suspend fun clearClips()
}

class ClipRepositoryImpl(
    private val clipDao: ClipDao
) : ClipRepository {

    override fun getAllClips(): Flow<ImmutableList<Clip>> {
        return clipDao.getAllClips().map { it.toImmutableList() }
    }

    override suspend fun refreshClips(viewApi: ViewApi) {
        val remoteClips = viewApi.clips()
        clipDao.insertClips(remoteClips)
    }

    override suspend fun clearClips() {
        clipDao.deleteAllClips()
    }
}
