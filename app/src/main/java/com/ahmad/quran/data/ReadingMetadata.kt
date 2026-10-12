package com.ahmad.quran.data

import android.content.Context
import android.util.Xml
import java.security.MessageDigest
import org.xmlpull.v1.XmlPullParser

object ReadingMetadata {
    fun getJuzStarts(context: Context): Map<Int, AyahId> {
        return verifiedBytes(context).inputStream().use { input ->
            val parser = Xml.newPullParser().apply { setInput(input, "UTF-8") }
            buildMap {
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "juz") {
                        put(parser.getAttributeValue(null, "index").toInt(), AyahId(
                            parser.getAttributeValue(null, "sura").toInt(), parser.getAttributeValue(null, "aya").toInt()))
                    }
                }
            }.also { check(it.keys == (1..30).toSet()) }
        }
    }
    fun getSurahAyahCounts(context: Context): Map<Int, Int> = verifiedBytes(context).inputStream().use { input ->
        val parser = Xml.newPullParser().apply { setInput(input, "UTF-8") }
        buildMap {
            while(parser.next()!=XmlPullParser.END_DOCUMENT) {
                if(parser.eventType==XmlPullParser.START_TAG && parser.name=="sura")
                    put(parser.getAttributeValue(null,"index").toInt(),parser.getAttributeValue(null,"ayas").toInt())
            }
        }
    }
    private fun verifiedBytes(context: Context): ByteArray {
        val bytes = context.assets.open("quran-data.xml").use { it.readBytes() }
        check(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) } == "8867c1d88191472adec9db694b3cd9f135b1a2ef580574d32cf888dcb22c5c7a")
        return bytes
    }
    // The original approved XML is packaged verbatim; the SQLite corpus is untouched.
    fun getRevelationTypes(context: Context): Map<Int, String> {
        val bytes = verifiedBytes(context)
        return bytes.inputStream().use { input ->
            val parser = Xml.newPullParser().apply { setInput(input, "UTF-8") }
            buildMap {
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "sura") {
                        val number = parser.getAttributeValue(null, "index").toInt()
                        val type = parser.getAttributeValue(null, "type")
                        if (type == "Meccan" || type == "Medinan") put(number, type)
                    }
                }
            }
        }
    }
}
