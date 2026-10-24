package com.github.naixx.viewapp.utils

import java.text.DecimalFormatSymbols

actual fun Number.formatted(digits: Int): String = "%,.${digits}f".format(this.toDouble()).removeTrailingZeros()

actual fun Number.toFixed(digits: Int): String = "%.${digits}f".format(this.toDouble())

internal actual fun decimalSeparator(): Char = DecimalFormatSymbols.getInstance().decimalSeparator
