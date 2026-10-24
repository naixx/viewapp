package com.github.naixx.viewapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.naixx.viewapp.network.*
import de.jensklingenberg.ktorfit.converter.FlowConverterFactory
import de.jensklingenberg.ktorfit.ktorfit
import github.naixx.network.*
import io.ktor.client.HttpClient
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import kotlin.test.assertNotNull
import kotlin.time.measureTime

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    private val storage: StorageProvider = mockk<StorageProvider>(relaxed = true).apply {
        every { session() } returns InstrumentationRegistry.getArguments().getString("session")
    }
    private lateinit var client: HttpClient
    private lateinit var api: ViewApi

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.github.naixx.viewapp", appContext.packageName)
    }

    @Before
    fun setup() {
        client = createHttpClient(createJson(), storage)
        // The dev unit's address directly (the app's ViewApi follows the connection, which this test does not open).
        api = ktorfit { baseUrl(WIFI_URL); httpClient(client); converterFactories(FlowConverterFactory()) }.createViewApi()
    }

    @After
    fun tearDown() {
        client.close()
    }

    @Test
    fun testSessionExists() {
        val session = storage.session()
        assertNotNull(session)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun findFirstAvailableServer() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val result: AddressResponse?
        val dur = measureTime {
            val urls = generateLocalServer(appContext)
            result = client.scanHosts(urls + listOf(REMOTE_URL))
        }

        println(dur)
        assertNotNull(result, "No responsive server found in network")
    }
}
