package com.github.naixx.viewapp

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.naixx.viewapp.network.generateLocalServer
import github.naixx.network.*
import io.ktor.client.HttpClient
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull
import kotlin.time.measureTime

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class ExampleRobolectricTest {

    private val storageProvider: StorageProvider = mockk<StorageProvider>(relaxed = true).apply {
        every { session() } returns System.getProperty("session")
    }
    private lateinit var client: HttpClient

    @Before
    fun setup() {
        client = createHttpClient(createJson(), storageProvider)
    }

    @After
    fun tearDown() {
        client.close()
    }

    @Test
    fun testSessionExists() {
        val session = storageProvider.session()
        assertNotNull(session)
    }

    @Test
    fun findFirstAvailableServer() = runTest {
        // Scans the real Wi-Fi subnet: a live test, skipped unless -Pargs.live=true (see CLAUDE.md, Build & test).
        Assume.assumeTrue("live test: run with -Pargs.live=true and a VIEW on the network", System.getProperty("live") == "true")
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val result: AddressResponse?
        val dur = measureTime {
            val urls = generateLocalServer(appContext)
            result = client.scanHosts(urls + listOf(REMOTE_URL))
        }

        println(dur)
        assertNotNull(result, "No responsive server found in network")
    }
}
