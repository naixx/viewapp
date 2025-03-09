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
import org.koin.core.component.get
import org.koin.core.context.*
import org.koin.dsl.module
import org.koin.test.*
import org.robolectric.annotation.Config
import kotlin.test.assertNotNull
import kotlin.time.measureTime

val testModule = module {
    single<StorageProvider> {
        mockk<StorageProvider>(relaxed = true).apply {
            every { session() } returns System.getProperty("session")
        }
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class ExampleRobolectricTest : KoinTest {

    // Dependency injection
    @Before
    fun setup() {
        stopKoin()
        startKoin {
            modules(listOf(networkModule, testModule))
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testSessionExists() {
        val storageProvider: StorageProvider by inject()
        val session = storageProvider.session()
        assertNotNull(session)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun findFirstAvailableServer() = runTest {
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val result: AddressResponse?
        val client = get<HttpClient>()
        val dur = measureTime {
            val urls = generateLocalServer(appContext)
            result = client.scanUrls(urls + listOf(REMOTE_URL))
        }

        println(dur)
        assertNotNull(result, "No responsive server found in network")
    }
}
