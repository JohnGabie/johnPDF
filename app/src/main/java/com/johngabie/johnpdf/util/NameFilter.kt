package com.johngabie.johnpdf.util

import java.text.Normalizer

private val DIACRITICS = Regex("\\p{Mn}+")

fun normalizeForSearch(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(DIACRITICS, "").lowercase()

fun <T> filterByName(items: List<T>, query: String, name: (T) -> String): List<T> {
    val q = normalizeForSearch(query.trim())
    if (q.isEmpty()) return items
    return items.filter { q in normalizeForSearch(name(it)) }
}
