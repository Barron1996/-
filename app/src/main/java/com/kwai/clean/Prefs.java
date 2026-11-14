package com.kwai.clean;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Settings shared between the settings UI and the hook code.
 * The hook runs inside the Kuaishou process and reads the same prefs file
 * through the application context supplied by LSPosed.
 */
public final class Prefs {

    public static final String TAG = "KwaiClean";
    public static final String PREFS_NAME = "kwai_clean";

    public static final String MASTER_ENABLE = "master_enable";
    public static final String HIDE_FOLLOW = "hide_follow";
    public static final String HIDE_LIKE = "hide_like";
    public static final String HIDE_COMMENT = "hide_comment";
    public static final String HIDE_FAVORITE = "hide_favorite";
    public static final String HIDE_SHARE = "hide_share";
    public static final String HIDE_EMOTION_BAR = "hide_emotion_bar";
    public static final String HIDE_MUSIC = "hide_music";
    public static final String HIDE_NAVI = "hide_navi";
    public static final String ADAPT_VIDEO = "adapt_video";

    /** All feature keys, in display order. Master switch is not included. */
    public static final String[] FEATURE_KEYS = new String[] {
            HIDE_FOLLOW, HIDE_LIKE, HIDE_COMMENT, HIDE_FAVORITE, HIDE_SHARE,
            HIDE_EMOTION_BAR, HIDE_MUSIC, HIDE_NAVI, ADAPT_VIDEO
    };

    private final SharedPreferences sp;

    public Prefs(Context context) {
        this.sp = open(context);
    }

    public Prefs(SharedPreferences sp) {
        this.sp = sp;
    }

    public static SharedPreferences open(Context context) {
        if (context == null) return null;
        try {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Returns the raw stored value, defaulting to off. */
    public boolean get(String key) {
        return get(key, false);
    }

    public boolean get(String key, boolean def) {
        if (sp == null) return def;
        try {
            return sp.getBoolean(key, def);
        } catch (Throwable t) {
            return def;
        }
    }

    /** True only when both the master switch and the feature switch are on. */
    public boolean isOn(String key) {
        if (!get(MASTER_ENABLE, true)) return false;
        return get(key, false);
    }

    public void set(String key, boolean value) {
        if (sp == null) return;
        try {
            sp.edit().putBoolean(key, value).apply();
        } catch (Throwable ignored) {
        }
    }

    public boolean isMasterOn() {
        return get(MASTER_ENABLE, true);
    }

}