package com.kwai.clean;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

/**
 * 设置界面与 Hook 代码共用的开关存储。
 * 设置界面用宿主的 getSharedPreferences 写入模块自己的私有目录；
 * Hook 侧通过 XSharedPreferences 读取同一文件（跨沙盒）。
 */
public final class Prefs {

    public static final String TAG = "KwaiClean";
    public static final String PREFS_NAME = "kwai_clean";

    public static final String MASTER_ENABLE   = "master_enable";
    public static final String HIDE_FOLLOW     = "hide_follow";
    public static final String HIDE_LIKE       = "hide_like";
    public static final String HIDE_COMMENT    = "hide_comment";
    public static final String HIDE_FAVORITE   = "hide_favorite";
    public static final String HIDE_SHARE      = "hide_share";
    public static final String HIDE_EMOTION_BAR = "hide_emotion_bar";
    public static final String HIDE_MUSIC      = "hide_music";
    public static final String HIDE_NAVI       = "hide_navi";
    public static final String ADAPT_VIDEO     = "adapt_video";

    public static final String[] FEATURE_KEYS = new String[]{
            HIDE_FOLLOW, HIDE_LIKE, HIDE_COMMENT, HIDE_FAVORITE, HIDE_SHARE,
            HIDE_EMOTION_BAR, HIDE_MUSIC, HIDE_NAVI, ADAPT_VIDEO
    };

    private final SharedPreferences sp;

    public Prefs(Context context) {
        this.sp = context == null ? null
                : context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public Prefs(SharedPreferences sp) {
        this.sp = sp;
    }

    /**
     * Hook 进程内使用：通过 XSharedPreferences 读取模块私有目录中的同一份配置。
     * 这是跨进程读配置的标准做法——宿主自己的 getSharedPreferences 指向的是
     * 快手沙盒，拿不到模块界面保存的值。
     */
    public static Prefs forHook(String modulePath) {
        try {
            de.robv.android.xposed.XSharedPreferences xsp =
                    new de.robv.android.xposed.XSharedPreferences(
                            new File(modulePath), PREFS_NAME);
            xsp.makeWorldReadable();
            xsp.reload();
            return new Prefs(xsp);
        } catch (Throwable t) {
            de.robv.android.xposed.XposedBridge.log("KwaiClean: XSharedPreferences error " + t);
            try {
                de.robv.android.xposed.XSharedPreferences fallback =
                        new de.robv.android.xposed.XSharedPreferences(PREFS_NAME);
                fallback.reload();
                return new Prefs(fallback);
            } catch (Throwable t2) {
                de.robv.android.xposed.XposedBridge.log("KwaiClean: fallback prefs error " + t2);
                return new Prefs(sp_null());
            }
        }
    }

    private static SharedPreferences sp_null() {
        return null;
    }

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

    public boolean isOn(String key) {
        if (!get(MASTER_ENABLE, true)) return false;
        return get(key, false);
    }

    public boolean isMasterOn() {
        return get(MASTER_ENABLE, true);
    }

    public void set(String key, boolean value) {
        if (sp == null) return;
        try {
            sp.edit().putBoolean(key, value).apply();
        } catch (Throwable ignored) {
        }
    }
}
