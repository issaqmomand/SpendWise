package com.group3.spendwise.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Uses the official per-app language APIs. AppCompat persists the selected
 * language on Android 12 and below; Android 13+ syncs it with system settings.
 * An empty language tag means "follow the device language".
 */
object AppLanguageManager {
    /** Returns the language code the user picked in the app (e.g. "fr"), or "" when following the device. */
    fun selectedLanguageTag(): String =
        AppCompatDelegate.getApplicationLocales().get(0)?.language.orEmpty()

    /**
     * Switches the app to [languageTag] (e.g. "en" or "fr").
     * Passing a blank tag clears the override so the app follows the device language.
     * AppCompat recreates the activity automatically so the new strings are loaded.
     */
    fun selectLanguage(languageTag: String) {
        val locales = if (languageTag.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
