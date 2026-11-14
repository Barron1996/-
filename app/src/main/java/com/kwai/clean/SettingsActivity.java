package com.kwai.clean;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

/**
 * 设置界面全部用代码构建，不依赖 XML 布局。
 * 每个净化项一个独立开关，外加一个总开关。
 */
public class SettingsActivity extends Activity {

    private static final String[] LABELS = new String[]{
            "隐藏关注按钮",
            "隐藏点赞按钮",
            "隐藏评论按钮",
            "隐藏收藏按钮",
            "隐藏分享按钮",
            "隐藏整个右侧互动栏",
            "隐藏背景音乐条",
            "隐藏底部导航栏",
            "视频全屏适配"
    };

    private Prefs prefs;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#FF101010"));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("快手净化");
        title.setTextSize(22f);
        title.setTextColor(Color.WHITE);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("作用域：仅 com.smile.gifmaker。勾选开关后重启快手即可生效。");
        sub.setTextSize(12f);
        sub.setTextColor(Color.parseColor("#9E9E9E"));
        sub.setPadding(0, dp(6), 0, dp(14));
        root.addView(sub);

        addSwitch(root, Prefs.MASTER_ENABLE, "总开关");
        addDivider(root);

        for (int i = 0; i < Prefs.FEATURE_KEYS.length; i++) {
            String label = i < LABELS.length ? LABELS[i] : Prefs.FEATURE_KEYS[i];
            addSwitch(root, Prefs.FEATURE_KEYS[i], label);
        }

        addDivider(root);

        status = new TextView(this);
        status.setTextSize(12f);
        status.setTextColor(Color.parseColor("#4CAF50"));
        status.setPadding(0, dp(12), 0, 0);
        status.setText("设置已自动保存。修改后请强行停止并重新打开快手。");
        root.addView(status);

        TextView hint = new TextView(this);
        hint.setTextSize(11f);
        hint.setTextColor(Color.parseColor("#666666"));
        hint.setPadding(0, dp(18), 0, 0);
        hint.setText("提示：本模块依赖 LSPosed 注入。若修改后仍无效果，请确认已在 LSPosed 中启用本模块、作用域勾选了快手，并重启一次手机让 Zygisk 重新加载模块。");
        root.addView(hint);

        setContentView(scroll);
    }

    private void addSwitch(LinearLayout parent, final String key, String label) {
        Switch sw = new Switch(this);
        sw.setText(label);
        sw.setTextSize(15f);
        sw.setTextColor(Color.WHITE);
        sw.setPadding(0, dp(10), 0, dp(10));
        boolean def = Prefs.MASTER_ENABLE.equals(key);
        sw.setChecked(prefs.get(key, def));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.set(key, isChecked);
                if (status != null) {
                    status.setText("已保存：" + (isChecked ? "开启" : "关闭") + "。重启快手后生效。");
                }
            }
        });
        parent.addView(sw);
    }

    private void addDivider(LinearLayout parent) {
        View line = new View(this);
        line.setBackgroundColor(Color.parseColor("#333333"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.topMargin = dp(8);
        lp.bottomMargin = dp(8);
        line.setLayoutParams(lp);
        parent.addView(line);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
