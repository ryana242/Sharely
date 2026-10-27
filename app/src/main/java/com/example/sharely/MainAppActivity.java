package com.example.sharely;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainAppActivity extends AppCompatActivity {

    public static final String EXTRA_CHAT_ITEM_ID = "sharely_chat_item_id";

    private BottomNavigationView bottomNavigation;
    private boolean browseSearchFocusRequested = false;
    private String pendingChatItemId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_app);

        View root = findViewById(R.id.mainAppRoot);

        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            bars.left,
                            bars.top,
                            bars.right,
                            bars.bottom
                    );

                    return insets;
                }
        );

        bottomNavigation = findViewById(R.id.bottomNavigation);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int selected = item.getItemId();
            Fragment fragment;

            if (selected == R.id.navHome) {
                fragment = new HomeFragment();

            } else if (selected == R.id.navBrowse) {
                fragment = new BrowseFragment();

            } else if (selected == R.id.navActivity) {
                fragment = new ActivityFragment();

            } else if (selected == R.id.navMessages) {
                if (pendingChatItemId != null) {
                    fragment = MessagesFragment.forItem(pendingChatItemId);
                    pendingChatItemId = null;
                } else {
                    fragment = new MessagesFragment();
                }

            } else if (selected == R.id.navProfile) {
                fragment = new ProfileFragment();

            } else {
                return false;
            }

            loadFragment(fragment);
            return true;
        });

        if (savedInstanceState == null) {
            String initialItem = getIntent().getStringExtra(EXTRA_CHAT_ITEM_ID);
            if (initialItem != null && !initialItem.trim().isEmpty()) {
                openMessagesForItem(initialItem);
            } else {
                bottomNavigation.setSelectedItemId(R.id.navHome);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String itemId = intent.getStringExtra(EXTRA_CHAT_ITEM_ID);
        if (itemId != null && !itemId.trim().isEmpty()) {
            openMessagesForItem(itemId);
        }
    }

    private void openMessagesForItem(String itemId) {
        pendingChatItemId = itemId;
        if (bottomNavigation.getSelectedItemId() == R.id.navMessages) {
            loadFragment(MessagesFragment.forItem(pendingChatItemId));
            pendingChatItemId = null;
        } else {
            bottomNavigation.setSelectedItemId(R.id.navMessages);
        }
    }

    public void selectTab(int menuItemId) {
        bottomNavigation.setSelectedItemId(menuItemId);
    }

    public void openBrowse(boolean focusSearch) {
        browseSearchFocusRequested = focusSearch;

        if (bottomNavigation.getSelectedItemId() == R.id.navBrowse) {
            loadFragment(new BrowseFragment());
        } else {
            bottomNavigation.setSelectedItemId(R.id.navBrowse);
        }
    }

    public boolean consumeBrowseSearchFocus() {
        boolean value = browseSearchFocusRequested;
        browseSearchFocusRequested = false;
        return value;
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
