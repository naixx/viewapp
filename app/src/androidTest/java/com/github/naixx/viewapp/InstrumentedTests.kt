package com.github.naixx.viewapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.naixx.viewapp.network.*
import github.naixx.network.*
import io.ktor.client.HttpClient
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.koin.core.component.get
import org.koin.core.context.*
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.assertNotNull
import kotlin.time.measureTime

val testModule = module {
    single<StorageProvider> {
        mockk<StorageProvider>(relaxed = true).apply {
            every { session() } returns InstrumentationRegistry.getArguments().getString("session")
        }
    }
}

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest : KoinTest {

    lateinit var api: ViewApi

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.github.naixx.viewapp", appContext.packageName)
    }

    @Before
    fun setup() {
        stopKoin()
        startKoin {
            modules(listOf(networkModule, testModule))
        }
        api = get<ViewApi>(parameters = { parametersOf(WIFI_URL) })
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testSessionExists() {
        val session = get<StorageProvider>().session()
        assertNotNull(session)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun findFirstAvailableServer() = runTest {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val result: AddressResponse?
        val client = get<HttpClient>()
        val dur = measureTime {
            val urls = generateLocalServer(appContext)
            result = client.scanHosts(urls + listOf(REMOTE_URL))
        }

        println(dur)
        assertNotNull(result, "No responsive server found in network")
    }
}
