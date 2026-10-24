package github.naixx.network.internal

import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.FlowConverterFactory
import de.jensklingenberg.ktorfit.ktorfit
import github.naixx.network.ViewApi
import io.ktor.client.HttpClient

/** One Ktorfit per base URL on the shared [HttpClient]: the device's addresses are only known at runtime. */
internal class KtorfitClientProvider(private val httpClient: HttpClient) {

    private val clients = mutableMapOf<String, Ktorfit>()

    fun api(baseUrl: String): ViewApi = clients.getOrPut(baseUrl) {
        ktorfit {
            baseUrl(baseUrl)
            httpClient(httpClient)
            converterFactories(FlowConverterFactory())
        }
    }.create<ViewApi>()
}
