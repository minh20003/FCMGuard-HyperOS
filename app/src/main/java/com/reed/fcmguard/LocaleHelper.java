package com.reed.fcmguard;

import android.app.LocaleManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

public final class LocaleHelper {
    private static final String PREFS = "guard_state";
    private static final String KEY_LANG = "ui_language";
    private static final String KEY_MIGRATED = "native_locale_migrated";
    private static final String KEY_INITIALIZED = "vietnamese_default_initialized";

    private LocaleHelper() {}

    /** Android 13+ uses LocaleManager; older Android keeps a small compatibility path. */
    public static Context apply(Context context) {
        initializeLanguage(context);
        String code = getLanguage(context);
        if ("system".equals(code)) return context;

        Locale locale = Locale.forLanguageTag(code);
        if (Build.VERSION.SDK_INT < 33) Locale.setDefault(locale);
        // LocaleManager's configuration change may still be pending on first launch.
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    public static String getLanguage(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            LocaleManager localeManager = (LocaleManager) context.getSystemService(Context.LOCALE_SERVICE);
            if (localeManager == null) return "system";
            LocaleList locales = localeManager.getApplicationLocales();
            if (locales == null || locales.isEmpty()) return "system";
            return normalize(locales.get(0).toLanguageTag());
        }
        return getLegacyLanguage(context);
    }

    public static void setLanguage(Context context, String code) {
        String normalized = normalize(code);
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        if (Build.VERSION.SDK_INT >= 33) {
            LocaleManager localeManager = (LocaleManager) context.getSystemService(Context.LOCALE_SERVICE);
            if (localeManager == null) return;
            LocaleList locales = "system".equals(normalized)
                    ? LocaleList.getEmptyLocaleList()
                    : LocaleList.forLanguageTags(normalized);
            localeManager.setApplicationLocales(locales);
            editor.putBoolean(KEY_MIGRATED, true).remove(KEY_LANG);
        } else {
            editor.putString(KEY_LANG, normalized);
        }
        editor.putBoolean(KEY_INITIALIZED, true).apply();
    }

    private static void initializeLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean initialized = prefs.getBoolean(KEY_INITIALIZED, false);
        boolean migrated = prefs.getBoolean(KEY_MIGRATED, false);
        if (initialized && (Build.VERSION.SDK_INT < 33 || migrated)) return;

        String legacy = getLegacyLanguage(context);
        SharedPreferences.Editor editor = prefs.edit();
        if (Build.VERSION.SDK_INT >= 33) {
            LocaleManager manager = (LocaleManager) context.getSystemService(Context.LOCALE_SERVICE);
            if (manager == null) return;
            String language = initialized ? "system" : "vi";
            if (!migrated && !"system".equals(legacy)) language = legacy;
            if (manager.getApplicationLocales().isEmpty() && !"system".equals(language)) {
                manager.setApplicationLocales(LocaleList.forLanguageTags(language));
            }
            editor.putBoolean(KEY_MIGRATED, true).remove(KEY_LANG);
        } else {
            editor.putString(KEY_LANG, "system".equals(legacy) ? "vi" : legacy);
        }
        editor.putBoolean(KEY_INITIALIZED, true).apply();
    }

    private static String getLegacyLanguage(Context context) {
        return normalize(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LANG, "system"));
    }

    private static String normalize(String code) {
        if (code == null || code.trim().isEmpty() || "system".equalsIgnoreCase(code)) {
            return "system";
        }

        String lower = code.toLowerCase(Locale.ROOT);
        if (lower.startsWith("vi")) return "vi";
        if (lower.startsWith("zh-tw") || lower.startsWith("zh-hk") ||
                lower.startsWith("zh-mo") || lower.contains("hant")) {
            return "zh-TW";
        }
        if (lower.startsWith("zh")) return "zh-CN";
        if (lower.startsWith("fr")) return "fr";
        if (lower.startsWith("ja")) return "ja";
        if (lower.startsWith("ko")) return "ko";
        if (lower.startsWith("es")) return "es";
        if (lower.startsWith("pt")) return "pt";
        if (lower.startsWith("de")) return "de";
        if (lower.startsWith("ru")) return "ru";
        if (lower.startsWith("en")) return "en";
        return "system";
    }
}
