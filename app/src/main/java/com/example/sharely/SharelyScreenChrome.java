package com.example.sharely;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

/** Shared, notch-safe bottom navigation on full-screen secondary activities. */
public final class SharelyScreenChrome {
    private static final String PREFS = "sharely_navigation";
    private static final String KEY_TAB = "last_selected_tab";

    private SharelyScreenChrome() { }

    public static void rememberTab(Context context, int tabId) {
        if (!isTab(tabId)) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_TAB, tabId).apply();
    }

    private static boolean isTab(int id) {
        return id == R.id.navHome || id == R.id.navBrowse
                || id == R.id.navActivity || id == R.id.navMessages
                || id == R.id.navProfile;
    }

    private static int defaultTab(AppCompatActivity activity) {
        if (activity instanceof BookingListActivity
                || activity instanceof ListingListActivity) {
            return R.id.navActivity;
        }
        int remembered = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_TAB, R.id.navHome);
        return isTab(remembered) ? remembered : R.id.navHome;
    }

    private static int dp(Context context, int n) {
        return Math.round(n * context.getResources().getDisplayMetrics().density);
    }

    public static void attach(@NonNull AppCompatActivity activity) {
        FrameLayout content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() != 1) return;
        View screen = content.getChildAt(0);
        if (screen.getTag(R.id.sharely_chrome_tag) != null) return;
        screen.setTag(R.id.sharely_chrome_tag, Boolean.TRUE);

        // Old pages set their own status-bar padding. The shared shell becomes
        // the sole owner of system insets, preventing double padding.
        ViewCompat.setOnApplyWindowInsetsListener(screen, null);
        content.removeView(screen);

        LinearLayout shell = new LinearLayout(activity);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundResource(R.drawable.bg_sharely_page);
        content.addView(shell, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        shell.addView(screen, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        BottomNavigationView bottom = new BottomNavigationView(activity);
        bottom.inflateMenu(R.menu.bottom_navigation_menu);
        bottom.setBackgroundResource(R.drawable.bg_sharely_nav_surface);
        bottom.setElevation(dp(activity, 6));
        bottom.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);
        bottom.setItemIconTintList(ContextCompat.getColorStateList(
                activity, R.color.bottom_nav_color));
        bottom.setItemTextColor(ContextCompat.getColorStateList(
                activity, R.color.bottom_nav_color));
        bottom.setSelectedItemId(defaultTab(activity));
        bottom.setOnItemSelectedListener(item -> {
            openMainTab(activity, item.getItemId());
            return true;
        });
        bottom.setOnItemReselectedListener(item ->
                openMainTab(activity, item.getItemId()));
        shell.addView(bottom, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        ViewCompat.setOnApplyWindowInsetsListener(shell, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(shell);
    }

    private static void openMainTab(AppCompatActivity activity, int tabId) {
        if (!isTab(tabId)) return;
        rememberTab(activity, tabId);
        Intent intent = new Intent(activity, MainAppActivity.class);
        intent.putExtra(MainAppActivity.EXTRA_SELECT_TAB, tabId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
        activity.finish();
    }
}
