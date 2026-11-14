package com.kwai.clean;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Settings screen built entirely in code so the project needs no XML layouts
 * and no external UI dependency. One switch per feature, plus a master switch.
 */
public class SettingsActivity extends Activity {

    private static final String[] LABELS = new String[] {
            "Hide follow button",
            "Hide like button",
            "Hide comment button",
            "Hide favorite button",
            "Hide share button",
            "Hide whole right interaction column",
            "Hide background music bar",
            "Hide bottom navigation bar",
            "Adapt video to full screen"
    };

    private Prefs prefs;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Kwai Cleanup");
        title.setTextSize(22f);
        title.setTextColor(Color.WHITE);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Scope: com.smile.gifmaker only. Turn features on, then restart Kuaishou.");
        sub.setTextSize(12f);
        sub.setTextColor(Color.parseColor("#9E9E9E"));
        sub.setPadding(0, dp(6), 0, dp(14));
        root.addView(sub);

        addSwitch(root, Prefs.MASTER_ENABLE, "Master switch");
        addDivider(root);
        for (int i = 0; i < Prefs.FEATURE_KEYS.length; i++) {
            addSwitch(root, Prefs.FEATURE_KEYS[i], LABELS[i]);
        }

        status = new TextView(this);
        status.setTextSize(12f);
        status.setTextColor(Color.parseColor("#4CAF50"));
        status.setPadding(0, dp(16), 0, 0);
        status.setText("Changes take effect after restarting Kuaishou.");
        root.addView(status);

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
                    status.setText("Saved. Restart Kuaishou to apply.");
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