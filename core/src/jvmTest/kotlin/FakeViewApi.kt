import github.naixx.network.*

/** A [ViewApi] whose endpoints are lambdas; what a test doesn't set must not be reached. */
class FakeViewApi(
    private val onDownload: suspend (String) -> ByteArray = { error("download: unused") },
    private val onClips: suspend () -> List<Clip> = { error("clips: unused") },
) : ViewApi {
    val requested = mutableListOf<String>()
    override suspend fun download(file: String): ByteArray { requested += file; return onDownload(file) }
    override suspend fun clips(): List<Clip> = onClips()
    override suspend fun auth(username: Any): Auth = error("auth: unused")
    override suspend fun socketUrl(): AddressResponse = error("socketUrl: unused")
    override suspend fun clipInfo(name: String): TimelapseClipInfo = error("clipInfo: unused")
    override suspend fun socketUrl(url: String): AddressResponse = error("socketUrl: unused")
}
