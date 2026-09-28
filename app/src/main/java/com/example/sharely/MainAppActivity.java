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

        setContentView(
                R.layout.activity_main_app
        );


        // -----------------------------------------------------
        // SYSTEM BAR / CAMERA CUTOUT SPACING
        // -----------------------------------------------------

        View mainRoot =
                findViewById(
                        R.id.mainAppRoot
                );

        ViewCompat.setOnApplyWindowInsetsListener(
                mainRoot,
                (v, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );


        // -----------------------------------------------------
        // BOTTOM NAVIGATION
        // -----------------------------------------------------

        bottomNavigation =
                findViewById(
                        R.id.bottomNavigation
                );


        // -----------------------------------------------------
        // DEFAULT SCREEN
        // -----------------------------------------------------

        if (savedInstanceState == null) {

            loadFragment(
                    new HomeFragment()
            );

            bottomNavigation.setSelectedItemId(
                    R.id.navHome
            );
        }


        // -----------------------------------------------------
        // TAB SELECTION
        // -----------------------------------------------------

        bottomNavigation.setOnItemSelectedListener(
                item -> {

                    int itemId =
                            item.getItemId();

                    Fragment selectedFragment;


                    if (itemId == R.id.navHome) {

                        selectedFragment =
                                new HomeFragment();

                    } else if (itemId == R.id.navBrowse) {

                        selectedFragment =
                                new BrowseFragment();

                    } else if (itemId == R.id.navActivity) {

                        selectedFragment =
                                new ActivityFragment();

                    } else if (itemId == R.id.navProfile) {

                        selectedFragment =
                                new ProfileFragment();

                    } else {

                        return false;
                    }


                    loadFragment(
                            selectedFragment
                    );

                    return true;
                }
        );
    }


    // =========================================================
    // SWITCH TAB
    // =========================================================

    public void selectTab(
            int menuItemId
    ) {

        bottomNavigation.setSelectedItemId(
                menuItemId
        );
    }


    // =========================================================
    // OPEN BROWSE
    // =========================================================

    public void openBrowse(boolean focusSearch) {

        browseSearchFocusRequested = focusSearch;

        if (bottomNavigation.getSelectedItemId() == R.id.navBrowse) {

            loadFragment(
                    new BrowseFragment()
            );

        } else {

            bottomNavigation.setSelectedItemId(
                    R.id.navBrowse
            );
        }
    }


    // =========================================================
    // BROWSE SEARCH FOCUS
    // =========================================================

    public boolean consumeBrowseSearchFocus() {

        boolean requested =
                browseSearchFocusRequested;

        browseSearchFocusRequested =
                false;

        return requested;
    }


    // =========================================================
    // LOAD FRAGMENT
    // =========================================================

    private void loadFragment(
            Fragment fragment
    ) {

        getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.fragmentContainer,
                        fragment
                )
                .commit();
    }
}