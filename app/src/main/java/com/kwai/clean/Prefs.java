package com.kwai.clean;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 设置界面与 Hook 代码共用的开关存储。
 * 设置界面用宿主的 getSharedPreferences 写入模块自己的私有目录；
 * Hook 侧通过 XSharedPreferences 读取同一份文件（跨沙盒）。
 */
public final class Prefs {

    public static final String TAG = "KwaiClean";
    public static final String PREFS_NAME = "kwai_clean";

    public static final String MASTER_ENABLE    = "master_enable";
    public static final String HIDE_FOLLOW      = "hide_follow";
    public static final String HIDE_LIKE        = "hide_like";
    public static final String HIDE_COMMENT     = "hide_comment";
    public static final String HIDE_FAVORITE    = "hide_favorite";
    public static final String HIDE_SHARE       = "hide_share";
    public static final String HIDE_EMOTION_BAR = "hide_emotion_bar";
    public static final String HIDE_MUSIC       = "hide_music";
    public static final String HIDE_NAVI        = "hide_navi";
    public static final String ADAPT_VIDEO      = "adapt_video";

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

    /** 设置界面侧：常规写法。 */
    public static Prefs of(Context context) {
        return new Prefs(context);
    }

    /**
     * Hook 进程内使用：通过 XSharedPreferences 读取模块私有目录中的同一份配置。
     * 优先用文件名构造器（LSPosed 会解析到模块包名目录），
     * 失败再反射尝试 File 构造器（传入 prefs 文件绝对路径）。
     */
    public static Prefs forHook(String modulePath) {
        try {
            de.robv.android.xposed.XSharedPreferences xsp =
                    new de.robv.android.xposed.XSharedPreferences(PREFS_NAME);
            try { xsp.makeWorldReadable(); } catch (Throwable ignored) { }
            xsp.reload();
            return new Prefs(xsp);
        } catch (Throwable t) {
            de.robv.android.xposed.XposedBridge.log("KwaiClean: XSP(name) failed " + t);
        }

        try {
            java.io.File prefFile = new java.io.File(prefFileFromApk(modulePath));
            Object obj = de.robv.android.xposed.XSharedPreferences.class
                    .getConstructor(java.io.File.class)
                    .newInstance(prefFile);
            de.robv.android.xposed.XSharedPreferences typed =
                    (de.robv.android.xposed.XSharedPreferences) obj;
            try { typed.makeWorldReadable(); } catch (Throwable ignored) { }
            typed.reload();
            return new Prefs(typed);
        } catch (Throwable t) {
            de.robv.android.xposed.XposedBridge.log("KwaiClean: XSP(file) failed " + t);
        }

        return new Prefs(null);
    }

    /**
     * 把模块 APK 路径换算成模块 prefs 的 xml 文件路径。
     * /data/app/~~x==/com.kwai.clean-~~y==/base.apk
     *   -> /data/user_de/0/com.kwai.clean/shared_prefs/kwai_clean.xml
     */
    private static String prefFileFromApk(String modulePath) {
        String pkg = null;
        if (modulePath != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("/([A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)+)-[^/]+/base\.apk")
                    .matcher(modulePath);
            if (m.find()) {
                pkg = m.group(1);
            }
        }
        if (pkg == null || pkg.isEmpty()) {
            pkg = "com.kwai.clean";
        }
        return "/data/user_de/0/" + pkg + "/shared_prefs/" + PREFS_NAME + ".xml";
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
