package github.naixx.network.internal

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/**
 * Builds the shared [HttpClient] on the platform's engine (OkHttp on the JVM) and applies the common
 * configuration from `networkModule` on top. Engine-specific setup such as request logging stays in the actuals.
 */
internal expect fun platformHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient
