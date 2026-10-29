package com.example.sharely;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Tiny programmatic green/white UI, avoiding changes to existing XML resources. */
final class SharelyUi {
    static final int GREEN = Color.rgb(24, 106, 78);
    static final int DARK = Color.rgb(28, 49, 37);
    static final int GRAY = Color.rgb(85, 100, 92);
    private SharelyUi() {}
    static int dp(Activity activity, int size) { return Math.round(size * activity.getResources().getDisplayMetrics().density); }
    static LinearLayout page(Activity activity) {
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.WHITE);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(activity, 18), dp(activity, 26), dp(activity, 18), dp(activity, 28));
        scroll.addView(content);
        activity.setContentView(scroll);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(scroll, (v, insets) -> {
            androidx.core.graphics.Insets safe = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars()
                    | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            content.setPadding(dp(activity,18)+safe.left, dp(activity,26)+safe.top,
                dp(activity,18)+safe.right, dp(activity,28)+safe.bottom);
            return insets;
        });
        androidx.core.view.ViewCompat.requestApplyInsets(scroll);
        return content;
    }
    static TextView text(Activity activity, LinearLayout parent, String message, boolean heading) {
        TextView t = new TextView(activity);
        t.setText(message);
        t.setTextColor(heading ? GREEN : DARK);
        t.setTextSize(heading ? 21 : 15);
        if (heading) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(0, dp(activity, heading ? 16 : 5), 0, dp(activity, 8));
        parent.addView(t);
        return t;
    }
    static EditText input(Activity activity, LinearLayout parent, String hint) {
        EditText e = new EditText(activity);
        e.setSingleLine(true);
        e.setTextSize(15);
        e.setHint(hint);
        parent.addView(e, new LinearLayout.LayoutParams(-1, dp(activity, 52)));
        return e;
    }
    static Button button(Activity activity, LinearLayout parent, String title, View.OnClickListener action) {
        Button b = new Button(activity);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GREEN));
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, dp(activity, 50));
        layout.topMargin = dp(activity, 7);
        parent.addView(b, layout);
        b.setOnClickListener(action);
        return b;
    }
    static void notice(Activity a, String message) { Toast.makeText(a, message, Toast.LENGTH_LONG).show(); }
}
