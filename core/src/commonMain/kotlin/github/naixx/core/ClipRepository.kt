package github.naixx.core

import github.naixx.network.Clip
import github.naixx.network.TimelapseClipInfo
import github.naixx.network.ViewApi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*

/**
 * The device's clips and their details as this client remembers them, so the Clips tab has something to show
 * before (and without) the device answering. Android keeps them in Room (`:db`); the browser in memory — the list
 * comes from `/clips` on every connect anyway.
 */
interface ClipRepository {

    fun getAllClips(): Flow<ImmutableList<Clip>>

    /** Replaces the remembered list with the device's: clips the device no longer has go, the rest are upserted. */
    suspend fun refreshClips(api: ViewApi)

    suspend fun clearClips()

    fun clipInfo(name: String): Flow<TimelapseClipInfo?>

    suspend fun saveClipInfo(info: TimelapseClipInfo)
}

/** The browser's repository and the test double. */
class MemoryClipRepository : ClipRepository {

    private val clips = MutableStateFlow<ImmutableList<Clip>>(persistentListOf())
    private val infos = MutableStateFlow<Map<String, TimelapseClipInfo>>(emptyMap())

    override fun getAllClips(): Flow<ImmutableList<Clip>> = clips

    override suspend fun refreshClips(api: ViewApi) {
        clips.value = api.clips().toImmutableList()
    }

    override suspend fun clearClips() {
        clips.value = persistentListOf()
    }

    override fun clipInfo(name: String): Flow<TimelapseClipInfo?> = infos.map { it[name.lowercase()] }

    override suspend fun saveClipInfo(info: TimelapseClipInfo) {
        infos.update { it + (info.name.lowercase() to info) }
    }
}
