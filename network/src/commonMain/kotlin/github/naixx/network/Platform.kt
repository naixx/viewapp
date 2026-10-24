package github.naixx.network

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Where blocking-ish work runs (network scans here, frame files and JPEG decoding in `:ui`): `Dispatchers.IO` on the
 * JVM; the browser has only `Default`, and nothing blocks there. Public: the view models default their dispatcher to it.
 */
expect val ioDispatcher: CoroutineDispatcher
