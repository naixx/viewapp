package github.naixx.network.internal

import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.FlowConverterFactory
import de.jensklingenberg.ktorfit.ktorfit
import io.ktor.client.HttpClient

internal class KtorfitClientProvider(private val httpClient: HttpClient) {

    private val clients = mutableMapOf<String, Ktorfit>()

    fun getClient(baseUrl: String): Ktorfit = clients.getOrPut(baseUrl) {
        ktorfit {
            baseUrl(baseUrl)
            httpClient(httpClient)
            converterFactories(FlowConverterFactory())
        }
    }
}
