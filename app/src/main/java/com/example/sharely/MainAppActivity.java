package com.example.sharely;

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

    private BottomNavigationView bottomNavigation;
    private boolean browseSearchFocusRequested = false;

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
                fragment = new MessagesFragment();

            } else if (selected == R.id.navProfile) {
                fragment = new ProfileFragment();

            } else {
                return false;
            }

            loadFragment(fragment);
            return true;
        });

        if (savedInstanceState == null) {
            bottomNavigation.setSelectedItemId(R.id.navHome);
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
