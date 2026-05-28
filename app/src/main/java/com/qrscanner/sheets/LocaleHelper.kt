package com.qrscanner.sheets

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    const val LANG_INDONESIAN = "in"   // Android uses "in" for Indonesian (id)
    const val LANG_ENGLISH = "en"
    const val DEFAULT_LANGUAGE = LANG_INDONESIAN

    /** Returns the language code saved in prefs, defaulting to Indonesian. */
    fun getSavedLanguage(context: Context): String {
        return context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            .getString("app_language", DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    /** Saves the chosen language code to prefs. */
    fun saveLanguage(context: Context, langCode: String) {
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("app_language", langCode)
            .apply()
    }

    /**
     * Wraps [context] with the saved locale.
     * Call this from every Activity's [attachBaseContext].
     */
    fun wrap(context: Context): Context {
        val lang = getSavedLanguage(context)
        return applyLocale(context, lang)
    }

    /** Applies [langCode] locale to [context] and returns the wrapped context. */
    fun applyLocale(context: Context, langCode: String): Context {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
