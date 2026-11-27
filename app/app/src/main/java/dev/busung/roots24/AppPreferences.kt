package dev.busung.roots24

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

enum class AccentColor(val storedValue: String) {
    Dynamic("dynamic"),
    Blue("blue"),
    Violet("violet"),
    Green("green"),
    Orange("orange");

    companion object {
        fun fromStoredValue(value: String?): AccentColor =
            entries.firstOrNull { it.storedValue == value } ?: Dynamic
    }
}

enum class AppThemeMode(val storedValue: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun fromStoredValue(value: String?): AppThemeMode =
            entries.firstOrNull { it.storedValue == value } ?: System
    }
}

enum class RootManager(val storedValue: String, val packageName: String?, val label: String) {
    KernelSU("kernelsu", "me.weishu.kernelsu", "KernelSU"),
    KernelSUNext("ksunext", "com.rifsxd.ksunext", "KernelSU Next"),
    SukiSU("sukisu", "com.sukisu.nova", "SukiSU Nova"),
    SukiSUUltra("sukisu_ultra", "com.sukisu.ultra", "SukiSU Ultra"),
    Auto("auto", null, "Auto-detect");

    companion object {
        fun fromStoredValue(value: String?): RootManager =
            entries.firstOrNull { it.storedValue == value } ?: KernelSU
    }
}

enum class ExploitEngine(val storedValue: String, val label: String) {
    SamTimer("samtimer", "SamTimer"),
    GhostLock("ghostlock", "GhostLock");

    companion object {
        fun fromStoredValue(value: String?): ExploitEngine =
            entries.firstOrNull { it.storedValue == value } ?: SamTimer
    }
}

object AppPreferences {
    private const val PREFERENCES = "appearance"
    private const val ACCENT_COLOR = "accent_color"
    private const val THEME_MODE = "theme_mode"
    private const val SHIZUKU_MODE = "shizuku_mode"
    private const val CONSUMED_INSTALL_REQUEST = "consumed_install_request"

    fun accentColor(context: Context): AccentColor = AccentColor.fromStoredValue(
        prefs(context).getString(ACCENT_COLOR, null),
    )

    fun setAccentColor(context: Context, color: AccentColor) {
        prefs(context).edit().putString(ACCENT_COLOR, color.storedValue).apply()
    }

    fun themeMode(context: Context): AppThemeMode = AppThemeMode.fromStoredValue(
        prefs(context).getString(THEME_MODE, null),
    )

    fun setThemeMode(context: Context, themeMode: AppThemeMode) {
        prefs(context).edit().putString(THEME_MODE, themeMode.storedValue).apply()
    }

    fun shizukuMode(context: Context): Boolean =
        prefs(context).getBoolean(SHIZUKU_MODE, true)

    fun setShizukuMode(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(SHIZUKU_MODE, enabled).apply()
    }

    private const val ROOT_MANAGER = "root_manager"

    fun rootManager(context: Context): RootManager = RootManager.fromStoredValue(
        prefs(context).getString(ROOT_MANAGER, null),
    )

    fun setRootManager(context: Context, manager: RootManager) {
        prefs(context).edit().putString(ROOT_MANAGER, manager.storedValue).apply()
    }

    private const val EXPLOIT_ENGINE = "exploit_engine"

    fun exploitEngine(context: Context): ExploitEngine = ExploitEngine.fromStoredValue(
        prefs(context).getString(EXPLOIT_ENGINE, null),
    )

    fun setExploitEngine(context: Context, engine: ExploitEngine) {
        prefs(context).edit().putString(EXPLOIT_ENGINE, engine.storedValue).apply()
    }

    @Synchronized
    fun consumeInstallRequest(context: Context, requestId: String?): Boolean {
        if (requestId.isNullOrBlank()) return false
        val preferences = prefs(context)
        if (preferences.getString(CONSUMED_INSTALL_REQUEST, null) == requestId) return false
        return preferences.edit().putString(CONSUMED_INSTALL_REQUEST, requestId).commit()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun languageTag(context: Context): String {
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        return if (locales.isEmpty) "" else locales[0].toLanguageTag()
    }

    fun setLanguage(context: Context, languageTag: String) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            LocaleList.forLanguageTags(languageTag)
    }
}
