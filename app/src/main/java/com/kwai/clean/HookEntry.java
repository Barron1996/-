package com.kwai.clean;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG = "KwaiClean";
    private static final String TARGET = "com.smile.gifmaker";

    // resource ids confirmed from kwai 14.10.10 reverse analysis
    private static final int ID_FOLLOW_BTN = 0x7f091208;
    private static final int ID_EMOTION_ROOT = 0x7f096496;
    private static final int ID_EMOTION_COMMENT = 0x7f096493;
    private static final int ID_EMOTION_LIKE = 0x7f096497;
    private static final int ID_EMOTION_SHARE = 0x7f09649a;
    private static final int ID_MUSIC_CONTAINER = 0x7f093005;
    private static final int ID_MUSIC_TEXT = 0x7f093006;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (lpparam == null) return;
        if (!TARGET.equals(lpparam.packageName)) return;
        XposedBridge.log(TAG + ": loaded in " + lpparam.processName);
        try {
            XposedHelpers.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Object obj = param.thisObject;
                    if (obj instanceof Activity) {
                        applyTo((Activity) obj);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": hook error " + t);
        }
    }

    private static void applyTo(final Activity act) {
        try {
            final View root = act.getWindow().getDecorView();
            if (root == null) return;
            root.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        scan(act, root);
                    } catch (Throwable t) {
                        XposedBridge.log(TAG + ": scan error " + t);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": apply error " + t);
        }
    }

    private static void scan(Activity act, View root) {
        Prefs p = new Prefs(act);
        if (!p.isOn(Prefs.MASTER_ENABLE)) return;
        if (p.isOn(Prefs.HIDE_FOLLOW))   hideId(root, ID_FOLLOW_BTN);
        if (p.isOn(Prefs.HIDE_EMOTION_BAR)) hideId(root, ID_EMOTION_ROOT);
        if (p.isOn(Prefs.HIDE_LIKE))     hideId(root, ID_EMOTION_LIKE);
        if (p.isOn(Prefs.HIDE_COMMENT))  hideId(root, ID_EMOTION_COMMENT);
        if (p.isOn(Prefs.HIDE_SHARE))    hideId(root, ID_EMOTION_SHARE);
        if (p.isOn(Prefs.HIDE_MUSIC)) {
            hideId(root, ID_MUSIC_CONTAINER);
            hideId(root, ID_MUSIC_TEXT);
        }
        if (p.isOn(Prefs.HIDE_NAVI))  hideByNaviHeuristic(root);
        if (p.isOn(Prefs.HIDE_FAVORITE)) hideByFavoriteHeuristic(root);
        if (p.isOn(Prefs.ADAPT_VIDEO)) { /* full screen area: handled by kwai itself */ }
    }

    private static void hideId(View root, int id) {
        if (root == null) return;
        if (root.getId() == id) {
            root.setVisibility(View.GONE);
            return;
        }
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            int n = g.getChildCount();
            for (int i = 0; i < n; i++) {
                View c = g.getChildAt(i);
                if (c != null) hideId(c, id);
            }
        }
    }

    private static void hideByNaviHeuristic(View v) {
        // fallback: hide clickable image buttons in the bottom navigation row
        collectAndHide(v, new Matcher() {
            @Override
            public boolean match(View x) {
                String d = describe(x);
                return d != null && d.contains("navi");
            }
        });
    }

    private static void hideByFavoriteHeuristic(View v) {
        collectAndHide(v, new Matcher() {
            @Override
            public boolean match(View x) {
                String d = describe(x);
                return d != null && d.contains("favorite");
            }
        });
    }

    private interface Matcher {
        boolean match(View v);
    }

    private static void collectAndHide(View v, Matcher m) {
        if (v == null) return;
        try {
            if (m.match(v)) {
                v.setVisibility(View.GONE);
                return;
            }
        } catch (Throwable ignored) {
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            int n = g.getChildCount();
            for (int i = 0; i < n; i++) {
                collectAndHide(g.getChildAt(i), m);
            }
        }
    }

    private static String describe(View v) {
        try {
            Object name = v.getClass().getName();
            return name == null ? null : name.toString();
        } catch (Throwable t) {
            return null;
        }
    }

}
