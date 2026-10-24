package com.github.naixx.viewapp.utils

import java.text.DecimalFormatSymbols

fun String.removeTrailingZeros(): String {
    val decimal = DecimalFormatSymbols.getInstance().decimalSeparator.toString()
    val escapedDecimal = Regex.escape(decimal)

    return replace(
        Regex("($escapedDecimal\\d*?)0+([A-Za-z]|$)"),
        "$1$2"
    ) // Trailing zeros before letter/end "1.200K" → "1.2K"
        .replace(Regex("$escapedDecimal+?([A-Za-z]|$)"), "$1")     // Decimal point followed by letter/end "1.K" → "1K"
        .replace(Regex("$escapedDecimal$"), "")                    // Lone decimal at end "12." → "12"
}

fun Number.formatted(digits: Int = 2): String = "%,.${digits}f".format(this.toDouble()).removeTrailingZeros()
