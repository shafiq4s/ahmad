package com.ahmad.quran.ui

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.text.BidiFormatter
import java.util.Locale

/** UI-only locale policy. Quran identities, text, artwork and coordinates never depend on it. */
object InterfaceLocales {
    const val SYSTEM = "system"
    val tags = listOf("ar", "en", "fa-AF", "ps-AF", "uz-UZ", "tg-TJ", "haz-AF")
    private fun preferences(context: Context) = context.getSharedPreferences("interface_locale", Context.MODE_PRIVATE)

    fun supported(locale: Locale): String? = when (locale.language) {
        "ar" -> "ar"
        "en" -> "en"
        "fa" -> if (locale.country.isEmpty() || locale.country == "AF") "fa-AF" else null
        "ps" -> "ps-AF"
        "uz" -> "uz-UZ"
        "tg" -> "tg-TJ"
        "haz" -> "haz-AF"
        else -> null
    }

    fun resolveSystem(locales: List<Locale>): String = locales.firstNotNullOfOrNull(::supported) ?: "ar"
    fun isRtl(tag: String) = tag in listOf("ar", "fa-AF", "ps-AF", "haz-AF")
    fun direction(tag: String) = if (isRtl(tag)) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR

    // Isolate an unchanged Arabic name inside translated labels; never apply this to Quran text.
    fun displayName(context: Context, name: String): String =
        BidiFormatter.getInstance(isRtl(effective(context))).unicodeWrap(name)

    fun choice(context: Context): String {
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            return if (locales.isEmpty) SYSTEM else supported(locales[0]) ?: SYSTEM
        }
        return preferences(context).getString("choice", SYSTEM).takeIf { it == SYSTEM || it in tags } ?: SYSTEM
    }

    fun effective(context: Context): String {
        val selected = choice(context)
        if (selected != SYSTEM) return selected
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = context.getSystemService(LocaleManager::class.java).systemLocales
            return resolveSystem((0 until locales.size()).map { locales[it] })
        }
        val configuration = Resources.getSystem().configuration
        @Suppress("DEPRECATION")
        return resolveSystem(if (Build.VERSION.SDK_INT >= 24)
            (0 until configuration.locales.size()).map { configuration.locales[it] }
        else listOf(configuration.locale))
    }

    // CLDR lacks reliable Hazaragi calendar data; only date formatting borrows Afghan Dari conventions.
    fun dateLocale(context: Context): Locale = Locale.forLanguageTag(effective(context).let { if (it == "haz-AF") "fa-AF" else it })

    fun wrap(context: Context): Context {
        if (Build.VERSION.SDK_INT < 33) {
            // System remains an owned preference; delegate the resolved language so fa-IR cannot match fa-AF resources.
            val requested = LocaleListCompat.forLanguageTags(effective(context))
            if (AppCompatDelegate.getApplicationLocales() != requested) AppCompatDelegate.setApplicationLocales(requested)
        }
        val tag = effective(context)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(tag))
            screenLayout = (screenLayout and Configuration.SCREENLAYOUT_LAYOUTDIR_MASK.inv()) or
                if (isRtl(tag)) Configuration.SCREENLAYOUT_LAYOUTDIR_RTL else Configuration.SCREENLAYOUT_LAYOUTDIR_LTR
        }
        return context.createConfigurationContext(configuration)
    }

    fun select(context: Context, tag: String) {
        require(tag == SYSTEM || tag in tags)
        if (choice(context) == tag) return
        preferences(context).edit().putString("choice", tag).apply()
        AppCompatDelegate.setApplicationLocales(if (tag == SYSTEM && Build.VERSION.SDK_INT >= 33) LocaleListCompat.getEmptyLocaleList()
            else LocaleListCompat.forLanguageTags(if (Build.VERSION.SDK_INT >= 33) tag else effective(context)))
    }
}
