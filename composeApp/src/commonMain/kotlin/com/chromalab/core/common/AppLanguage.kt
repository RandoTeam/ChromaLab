package com.chromalab.core.common

/**
 * Supported app languages.
 */
enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String,
) {
    RU("ru", "Russian", "Русский", "🇷🇺"),
    EN("en", "English", "English", "🇬🇧"),
    FR("fr", "French", "Français", "🇫🇷"),
    DE("de", "German", "Deutsch", "🇩🇪"),
    ES("es", "Spanish", "Español", "🇪🇸"),
    IT("it", "Italian", "Italiano", "🇮🇹"),
    PL("pl", "Polish", "Polski", "🇵🇱"),
    CS("cs", "Czech", "Čeština", "🇨🇿"),
    ;

    companion object {
        val DEFAULT = RU

        fun fromCode(code: String): AppLanguage =
            entries.find { it.code == code } ?: DEFAULT
    }
}
