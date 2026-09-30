package com.johngabie.johnpdf.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NameFilterTest {
    private val names = listOf("Fatura_Setembro.pdf", "Receita médica.pdf", "boleto.PDF")

    @Test fun empty_query_returns_all() = assertEquals(names, filterByName(names, "  ") { it })
    @Test fun match_is_case_insensitive() = assertEquals(listOf("Fatura_Setembro.pdf"), filterByName(names, "fatura") { it })
    @Test fun match_ignores_accents_both_ways() {
        assertEquals(listOf("Receita médica.pdf"), filterByName(names, "medica") { it })
        assertEquals(listOf("boleto.PDF"), filterByName(names, "BOLÉTO") { it })
    }
    @Test fun no_match_returns_empty() = assertEquals(emptyList<String>(), filterByName(names, "xyz") { it })
}
