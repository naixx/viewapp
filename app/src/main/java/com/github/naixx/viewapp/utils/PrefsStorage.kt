package com.github.naixx.viewapp.utils

import github.naixx.network.StorageProvider

class PrefsStorage : StorageProvider {

    override fun session(): String? {
        return Prefs.session
    }

    override fun session(session: String) {
        Prefs.session = session
    }

    override fun lastSuccessfulAddress(ip: String) {
        Prefs.lastConnectedIp += ip
    }

    override fun email(): String? = Prefs.email

    override fun password(): String? = Prefs.password
}
