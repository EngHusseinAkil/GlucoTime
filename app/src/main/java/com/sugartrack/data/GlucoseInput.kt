package com.sugartrack.data

/** Input-only safeguards; these are storage bounds, not clinical thresholds. */
object GlucoseInput {
    const val MIN_GLUCOSE_MG_DL = 1.0
    const val MAX_GLUCOSE_MG_DL = 600.0

    fun normalizeDigits(input: String): String = buildString(input.length) {
        input.forEach { character ->
            append(
                when (character) {
                    in '\u0660'..'\u0669' -> ('0'.code + (character.code - '\u0660'.code)).toChar()
                    in '\u06F0'..'\u06F9' -> ('0'.code + (character.code - '\u06F0'.code)).toChar()
                    else -> character
                }
            )
        }
    }

    /**
     * Keeps the field editable while the user is typing, but rejects a
     * completed numeric value above the input maximum.
     */
    fun isWithinMaximum(input: String): Boolean {
        val numericValue = normalizeDigits(input).toDoubleOrNull() ?: return true
        return numericValue <= MAX_GLUCOSE_MG_DL
    }

    fun formatValue(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }
}
