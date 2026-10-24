@file:OptIn(ExperimentalWasmJsInterop::class) // js(): the browser's Intl formatting

package com.github.naixx.viewapp.utils

import kotlin.js.ExperimentalWasmJsInterop

// The browser's own locale formatting (Intl), the counterpart of String.format on the JVM.
private fun toLocaleFixed(value: Double, digits: Int): String =
    js("value.toLocaleString(undefined, { minimumFractionDigits: digits, maximumFractionDigits: digits })")

private fun toLocaleFixedPlain(value: Double, digits: Int): String =
    js("value.toLocaleString(undefined, { minimumFractionDigits: digits, maximumFractionDigits: digits, useGrouping: false })")

private fun localeDecimalSeparator(): String = js("(1.1).toLocaleString().charAt(1)")

actual fun Number.formatted(digits: Int): String = toLocaleFixed(this.toDouble(), digits).removeTrailingZeros()

actual fun Number.toFixed(digits: Int): String = toLocaleFixedPlain(this.toDouble(), digits)

internal actual fun decimalSeparator(): Char = localeDecimalSeparator().firstOrNull() ?: '.'
