package com.kwai.clean;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * 拦截快手主进程，按设置界面保存的开关隐藏对应控件。
 * 配置通过 XSharedPreferences 从模块自身私有目录读取
 * （LSPosed 会把模块的 prefs 目录跨进程共享给宿主），
 * 不能用宿主自己的 getSharedPreferences，因为那是两个不同沙盒。
 */
public class HookEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TARGET = "com.smile.gifmaker";

    // 快手 14.10.10.51087 反编译得到的资源 id
    private static final int ID_FOLLOW_BTN    = 0x7f091208;
    private static final int ID_EMOTION_ROOT  = 0x7f096496;
    private static final int ID_EMOTION_COMMENT = 0x7f096493;
    private static final int ID_EMOTION_LIKE  = 0x7f096497;
    private static final int ID_EMOTION_SHARE = 0x7f09649a;
    private static final int ID_MUSIC_CONTAINER = 0x7f093005;
    private static final int ID_MUSIC_TEXT   = 0x7f093006;

    private static String MODULE_PATH = null;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
        XposedBridge.log("KwaiClean: initZygote, modulePath=" + MODULE_PATH);
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (lpparam == null || !TARGET.equals(lpparam.packageName)) return;

        XposedBridge.log("KwaiClean: loaded in " + lpparam.processName);

        // 第一层：Activity 显示后立刻扫一遍
        try {
            XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object o = param.thisObject;
                    if (o instanceof Activity) {
                        scheduleApply((Activity) o, 0);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("KwaiClean: hook onResume error " + t);
        }

        // 第二层：任何 View 被加入布局时重新扫（列表滑动、切换视频都会触发）
        try {
            XposedHelpers.findAndHookMethod(ViewGroup.class, "addView",
                    View.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object self = param.thisObject;
                    if (self instanceof View) {
                        View v = (View) self;
                        Activity act = findActivity(v);
                        if (act != null) {
                            scheduleApply(act, 80);
                        }
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("KwaiClean: hook addView error " + t);
        }
    }

    private static void scheduleApply(final Activity act, final long delay) {
        try {
            final View root = act.getWindow() != null ? act.getWindow().getDecorView() : null;
            if (root == null) return;
            root.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        scan(act, root);
                    } catch (Throwable t) {
                        XposedBridge.log("KwaiClean: scan error " + t);
                    }
                }
            }, delay);
        } catch (Throwable t) {
            XposedBridge.log("KwaiClean: apply error " + t);
        }
    }

    private static void scan(Activity act, View root) {
        Prefs p = Prefs.forHook(MODULE_PATH);
        if (!p.get(Prefs.MASTER_ENABLE, true)) return;

        if (p.get(Prefs.HIDE_FOLLOW, false))       hideId(root, ID_FOLLOW_BTN);
        if (p.get(Prefs.HIDE_EMOTION_BAR, false))  hideId(root, ID_EMOTION_ROOT);
        if (p.get(Prefs.HIDE_LIKE, false))         hideId(root, ID_EMOTION_LIKE);
        if (p.get(Prefs.HIDE_COMMENT, false))      hideId(root, ID_EMOTION_COMMENT);
        if (p.get(Prefs.HIDE_SHARE, false))        hideId(root, ID_EMOTION_SHARE);
        if (p.get(Prefs.HIDE_MUSIC, false)) {
            hideId(root, ID_MUSIC_CONTAINER);
            hideId(root, ID_MUSIC_TEXT);
        }
        if (p.get(Prefs.HIDE_NAVI, false))     hideByTag(root, "navi");
        if (p.get(Prefs.HIDE_FAVORITE, false)) hideByTag(root, "favorite");
    }

    private static void hideId(View v, int id) {
        if (v == null) return;
        if (v.getId() == id) {
            v.setVisibility(View.GONE);
            return;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0, n = g.getChildCount(); i < n; i++) {
                hideId(g.getChildAt(i), id);
            }
        }
    }

    private static void hideByTag(View v, String needle) {
        if (v == null) return;
        try {
            String cls = v.getClass().getName();
            if (cls != null && cls.toLowerCase().contains(needle)) {
                v.setVisibility(View.GONE);
                return;
            }
        } catch (Throwable ignored) {
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0, n = g.getChildCount(); i < n; i++) {
                hideByTag(g.getChildAt(i), needle);
            }
        }
    }

    private static Activity findActivity(View v) {
        try {
            android.content.Context c = v.getContext();
            while (c != null) {
                if (c instanceof Activity) return (Activity) c;
                if (!(c instanceof android.content.ContextWrapper)) return null;
                c = ((android.content.ContextWrapper) c).getBaseContext();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
