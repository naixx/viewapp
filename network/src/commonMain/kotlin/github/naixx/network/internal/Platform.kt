package github.naixx.network.internal

import kotlinx.coroutines.CoroutineDispatcher

/** Where blocking-ish network scans run: Dispatchers.IO on the JVM; the browser has only Default. */
internal expect val ioDispatcher: CoroutineDispatcher
