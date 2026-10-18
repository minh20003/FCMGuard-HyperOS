package com.reed.fcmguard;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.LocaleManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.Log;

import java.util.Locale;

/** Runs against real Android APIs, with preferences isolated from protection settings. */
public class LocaleHelperSelfCheck extends Instrumentation {
    private Context context;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            Throwable[] error = {null};
            runOnMainSync(() -> {
                try {
                    checkLanguage();
                } catch (Throwable failure) {
                    error[0] = failure;
                }
            });
            if (error[0] != null) throw error[0];
            result.putString("stream", "LocaleHelper self-check passed on API " + Build.VERSION.SDK_INT + "\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            result.putString("stream", Log.getStackTraceString(failure));
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private void checkLanguage() {
        context = new ContextWrapper(getTargetContext()) {
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("locale_self_check", mode);
            }
        };
        prefs = context.getSharedPreferences("guard_state", Context.MODE_PRIVATE);
        Locale previousDefault = Locale.getDefault();
        LocaleList previousLocales = Build.VERSION.SDK_INT >= 33
                ? ((LocaleManager) context.getSystemService(Context.LOCALE_SERVICE)).getApplicationLocales() : null;
        try {
            reset();
            LocaleHelper.apply(context);
            expect("vi", LocaleHelper.getLanguage(context), "fresh default");
            expect("Sửa ngay", LocaleHelper.apply(context).getString(R.string.repair_now), "Vietnamese resource");
            LocaleHelper.apply(context);
            expect("vi", LocaleHelper.getLanguage(context), "repeat initialization");
            LocaleHelper.setLanguage(context, "en");
            LocaleHelper.apply(context);
            expect("en", LocaleHelper.getLanguage(context), "explicit English");
            LocaleHelper.setLanguage(context, "system");
            LocaleHelper.apply(context);
            expect("system", LocaleHelper.getLanguage(context), "explicit follow system");
            LocaleHelper.setLanguage(context, "vi-VN");
            expect("vi", LocaleHelper.getLanguage(context), "regional Vietnamese");

            reset();
            prefs.edit().putString("ui_language", "en").commit();
            LocaleHelper.apply(context);
            expect("en", LocaleHelper.getLanguage(context), "preserve legacy English");
            expect("Repair now", LocaleHelper.apply(context).getString(R.string.repair_now), "English resource");

            reset();
            prefs.edit().putString("ui_language", "system").commit();
            LocaleHelper.apply(context);
            expect("vi", LocaleHelper.getLanguage(context), "upgrade from system default");

            if (Build.VERSION.SDK_INT >= 33) checkNativeMigration();
            if (Build.VERSION.SDK_INT >= 26) checkNotificationChannel();
        } finally {
            prefs.edit().clear().commit();
            if (Build.VERSION.SDK_INT >= 33) {
                ((LocaleManager) context.getSystemService(Context.LOCALE_SERVICE)).setApplicationLocales(previousLocales);
            }
            Locale.setDefault(previousDefault);
        }
    }

    private void checkNotificationChannel() {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String id = "fcm_guard_persistent_v2";
        NotificationChannel previous = manager.getNotificationChannel(id);
        try {
            LocaleHelper.setLanguage(context, "vi");
            GuardService.ensureNotificationChannel(context);
            expect("Bảo vệ FCM Guard", manager.getNotificationChannel(id).getName().toString(), "Vietnamese channel");
            LocaleHelper.setLanguage(context, "en");
            GuardService.ensureNotificationChannel(context);
            expect("FCM Guard protection", manager.getNotificationChannel(id).getName().toString(), "English channel after switch");
        } finally {
            if (previous == null) manager.deleteNotificationChannel(id);
            else manager.createNotificationChannel(previous);
        }
    }

    private void checkNativeMigration() {
        LocaleManager manager = (LocaleManager) context.getSystemService(Context.LOCALE_SERVICE);
        reset();
        prefs.edit().putBoolean("native_locale_migrated", true).commit();
        LocaleHelper.apply(context);
        expect("vi", LocaleHelper.getLanguage(context), "old migration marker does not block Vietnamese default");

        reset();
        prefs.edit().putString("ui_language", "zh-CN").commit();
        manager.setApplicationLocales(LocaleList.forLanguageTags("en"));
        LocaleHelper.apply(context);
        expect("en", LocaleHelper.getLanguage(context), "native choice wins over legacy");

        reset();
        prefs.edit().putString("ui_language", "fr").putBoolean("vietnamese_default_initialized", true).commit();
        LocaleHelper.apply(context);
        expect("fr", LocaleHelper.getLanguage(context), "OS upgrade keeps legacy choice after initialization");

        reset();
        prefs.edit().putString("ui_language", "system").putBoolean("vietnamese_default_initialized", true).commit();
        LocaleHelper.apply(context);
        expect("system", LocaleHelper.getLanguage(context), "OS upgrade keeps explicit system choice");

        reset();
        prefs.edit().putString("ui_language", "fr").commit();
        Context unavailable = new ContextWrapper(context) {
            @Override public Object getSystemService(String name) {
                return Context.LOCALE_SERVICE.equals(name) ? null : super.getSystemService(name);
            }
        };
        LocaleHelper.apply(unavailable);
        expect("fr", prefs.getString("ui_language", null), "unavailable manager keeps legacy value");
        if (prefs.getBoolean("native_locale_migrated", false) ||
                prefs.getBoolean("vietnamese_default_initialized", false)) {
            throw new AssertionError("unavailable manager must allow initialization to retry");
        }
        LocaleHelper.apply(context);
        expect("fr", LocaleHelper.getLanguage(context), "retry migrates legacy choice");
    }

    private void reset() {
        prefs.edit().clear().commit();
        if (Build.VERSION.SDK_INT >= 33) {
            ((LocaleManager) context.getSystemService(Context.LOCALE_SERVICE))
                    .setApplicationLocales(LocaleList.getEmptyLocaleList());
        }
    }

    private static void expect(String expected, String actual, String label) {
        if (!expected.equals(actual)) throw new AssertionError(label + ": expected " + expected + ", got " + actual);
    }
}
