package com.github.naixx.viewapp.utils

import com.github.naixx.prefs.PrefsObject
import com.github.naixx.prefs.serialized
import com.github.naixx.prefs.stringFlow
import com.russhwolf.settings.Settings
import com.russhwolf.settings.nullableString
import com.russhwolf.settings.observable.makeObservable
import com.russhwolf.settings.string

object Prefs : PrefsObject {

    override var settings = Settings().makeObservable()
    val currentUrl by stringFlow("")
    val email by settings.string(defaultValue = "")
    val password by settings.string(defaultValue = "")
    var session by settings.nullableString()
    var lastConnectedIp by serialized(setOf<String>())
}
