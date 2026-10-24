package com.github.naixx.viewapp.utils

fun String.removeTrailingZeros(): String {
    val decimal = decimalSeparator().toString()
    val escapedDecimal = Regex.escape(decimal)

    return replace(
        Regex("($escapedDecimal\\d*?)0+([A-Za-z]|$)"),
        "$1$2"
    ) // Trailing zeros before letter/end "1.200K" → "1.2K"
        .replace(Regex("$escapedDecimal+?([A-Za-z]|$)"), "$1")     // Decimal point followed by letter/end "1.K" → "1K"
        .replace(Regex("$escapedDecimal$"), "")                    // Lone decimal at end "12." → "12"
}

/** The number in the current locale with grouping and up to [digits] decimals, trailing zeros dropped. */
expect fun Number.formatted(digits: Int = 2): String

/** The current locale's decimal separator (the one [formatted] uses). */
internal expect fun decimalSeparator(): Char
