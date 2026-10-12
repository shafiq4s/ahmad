package com.ahmad.quran.data

/** Comparison keys only. Never store them or use them as displayed Quran text. */
object QuranSearch {
    fun matches(text: String, queryKey: String): Boolean = key(text).contains(queryKey) ||
        key(text, true).contains(queryKey) || key(text.replace("و\u0670", "ا"), true).contains(queryKey)

    fun key(value: String, expandDaggerAlif: Boolean = false): String = buildString(value.length) {
        var space = false
        for (c in value) {
            if (c == '\u0670' && expandDaggerAlif) { append('ا');continue }
            val type = Character.getType(c)
            if (type == Character.NON_SPACING_MARK.toInt() || type == Character.COMBINING_SPACING_MARK.toInt() ||
                type == Character.ENCLOSING_MARK.toInt() || c == '\u0640' || c == '\u06E5' || c == '\u06E6') continue
            if (c.isWhitespace()) {
                space = isNotEmpty()
            } else {
                if (space) append(' ')
                space = false
                append(when (c) { 'أ', 'إ', 'آ', 'ٱ' -> 'ا'; 'ى' -> 'ي'; else -> c })
            }
        }
    }
}
