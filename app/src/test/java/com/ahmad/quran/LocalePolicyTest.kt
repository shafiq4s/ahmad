package com.ahmad.quran

import com.ahmad.quran.ui.InterfaceLocales
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

class LocalePolicyTest {
    @Test fun systemUsesTheFirstSupportedLanguageAndDistinguishesIranianPersian() {
        val cases = mapOf("ar-EG" to "ar", "en-GB" to "en", "fa" to "fa-AF", "fa-AF" to "fa-AF",
            "ps-PK" to "ps-AF", "uz-Cyrl-UZ" to "uz-UZ", "tg" to "tg-TJ", "haz" to "haz-AF")
        cases.forEach { (device, expected) -> assertEquals(expected, InterfaceLocales.resolveSystem(listOf(Locale.forLanguageTag(device)))) }
        assertNull(InterfaceLocales.supported(Locale.forLanguageTag("fa-IR")))
        assertEquals("ar", InterfaceLocales.resolveSystem(listOf(Locale.forLanguageTag("fa-IR"))))
        assertEquals("ar", InterfaceLocales.resolveSystem(listOf(Locale.GERMAN)))
        assertEquals("en", InterfaceLocales.resolveSystem(listOf(Locale.forLanguageTag("fa-IR"), Locale.UK)))
        assertEquals("ar", InterfaceLocales.resolveSystem(emptyList()))
        assertEquals(listOf("ar", "fa-AF", "ps-AF", "haz-AF"), InterfaceLocales.tags.filter(InterfaceLocales::isRtl))
    }

    @Test fun everyLanguageHasAllUiMessagesAndIdenticalFormatArguments() {
        val root = File(requireNotNull(System.getProperty("quran.sourceDir")), "../../../app/src/main/res")
        fun strings(folder: String): Map<String, Pair<String, Boolean>> {
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(root, "$folder/strings.xml")).getElementsByTagName("string")
            return (0 until nodes.length).associate { i ->
                val n = nodes.item(i) as org.w3c.dom.Element
                n.getAttribute("name") to (n.textContent to (n.getAttribute("translatable") != "false"))
            }
        }
        val base = strings("values").filterValues { it.second }
        assertEquals(116, base.size)
        val arguments = Regex("%[0-9]+\\\$[sd]")
        for (tag in InterfaceLocales.tags) {
            val folder = when(tag) { "ar" -> "values-ar"; "en" -> "values-en"; else -> "values-b+${tag.replace('-', '+')}" }
            val translated = strings(folder)
            assertEquals(tag, base.keys, translated.keys)
            for ((key, original) in base) {
                val value = translated.getValue(key).first
                assertTrue("$tag/$key must not be empty", value.isNotBlank())
                assertEquals("$tag/$key arguments", arguments.findAll(original.first).map { it.value }.sorted().toList(),
                    arguments.findAll(value).map { it.value }.sorted().toList())
            }
        }
        val uzbek = strings("values-b+uz+UZ").values.joinToString { it.first }
        assertFalse(uzbek.any { it in '\u0400'..'\u04FF' })
        assertTrue(strings("values-b+tg+TJ").getValue("app_name").first.any { it in '\u0400'..'\u04FF' })
    }
}
