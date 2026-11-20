package com.group3.spendwise.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Uses the official per-app language APIs. AppCompat persists the selected
 * language on Android 12 and below; Android 13+ syncs it with system settings.
 * An empty language tag means "follow the device language".
 */
object AppLanguageManager {
    fun selectedLanguageTag(): String =
        AppCompatDelegate.getApplicationLocales().get(0)?.language.orEmpty()

    fun selectLanguage(languageTag: String) {
        val locales = if (languageTag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
