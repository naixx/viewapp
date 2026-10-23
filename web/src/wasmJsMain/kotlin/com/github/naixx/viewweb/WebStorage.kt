package com.github.naixx.viewweb

import github.naixx.network.StorageProvider
import kotlinx.browser.localStorage

/**
 * [StorageProvider] for the browser. The page is served by the VIEW itself, so there is no address to remember
 * and no relay login: only the session survives, in localStorage.
 */
class WebStorage : StorageProvider {

    override fun session(): String? = localStorage.getItem(SESSION)

    override fun session(session: String) = localStorage.setItem(SESSION, session)

    override fun lastSuccessfulAddress(string: String) = Unit

    override fun email(): String? = null

    override fun password(): String? = null

    private companion object {
        const val SESSION = "session"
    }
}
