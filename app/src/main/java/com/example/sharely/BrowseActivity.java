package com.example.sharely;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sharely.service.ItemSearchService;

public class BrowseActivity extends AppCompatActivity {

    // ---------------------------------------------------------
    // UI FIELDS
    // ---------------------------------------------------------

    private EditText searchField;

    private LinearLayout cardDrill;
    private LinearLayout cardTent;


    // ---------------------------------------------------------
    // SEARCH / FILTER LOGIC
    // ---------------------------------------------------------

    private ItemSearchService searchService;

    // null means that particular filter is not active
    private String selectedCategory = null;

    private Double selectedMaxPrice = null;

    private Double selectedMaxDistanceKm = null;


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_browse);


        // -----------------------------------------------------
        // HANDLE SYSTEM BARS
        // -----------------------------------------------------

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.browseMain),
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
        // FIND MAIN VIEWS
        // -----------------------------------------------------

        searchField =
                findViewById(R.id.editSearch);

        cardDrill =
                findViewById(R.id.cardDrill);

        cardTent =
                findViewById(R.id.cardTent);


        // -----------------------------------------------------
        // FILTER CONTROLS
        // -----------------------------------------------------

        TextView categoryButton =
                findViewById(R.id.buttonCategory);

        TextView priceButton =
                findViewById(R.id.buttonPrice);

        TextView distanceButton =
                findViewById(R.id.buttonFilter);


        // -----------------------------------------------------
        // NAVIGATION CONTROLS
        // -----------------------------------------------------

        View backButton =
                findViewById(R.id.buttonBack);

        View viewDrillButton =
                findViewById(R.id.buttonViewDrill);

        View viewTentButton =
                findViewById(R.id.buttonViewTent);


        // -----------------------------------------------------
        // CREATE SEARCH SERVICE
        // -----------------------------------------------------

        searchService =
                new ItemSearchService();


        // -----------------------------------------------------
        // BACK BUTTON
        // -----------------------------------------------------

        backButton.setOnClickListener(v -> {
            finish();
        });


        // -----------------------------------------------------
        // DRILL → ITEM DETAILS
        // -----------------------------------------------------

        viewDrillButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            BrowseActivity.this,
                            ItemDetailsActivity.class
                    );

            intent.putExtra(
                    "itemId",
                    "ITEM1"
            );

            intent.putExtra(
                    "name",
                    "Cordless Drill"
            );

            intent.putExtra(
                    "price",
                    "$5 per day"
            );

            intent.putExtra(
                    "info",
                    "1.2 km away • ★ 4.8"
            );

            intent.putExtra(
                    "description",
                    "A reliable cordless drill suitable for small home projects."
            );

            startActivity(intent);
        });


        // -----------------------------------------------------
        // TENT → ITEM DETAILS
        // -----------------------------------------------------

        viewTentButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            BrowseActivity.this,
                            ItemDetailsActivity.class
                    );

            intent.putExtra(
                    "itemId",
                    "ITEM2"
            );

            intent.putExtra(
                    "name",
                    "Camping Tent"
            );

            intent.putExtra(
                    "price",
                    "$8 per day"
            );

            intent.putExtra(
                    "info",
                    "2.4 km away • ★ 4.6"
            );

            intent.putExtra(
                    "description",
                    "A spacious camping tent suitable for weekend trips."
            );

            startActivity(intent);
        });


        // -----------------------------------------------------
        // LIVE KEYWORD SEARCH
        // -----------------------------------------------------

        searchField.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                        // Nothing needed
                    }


                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        applyFilters();
                    }


                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                        // Nothing needed
                    }
                }
        );


        // -----------------------------------------------------
        // CATEGORY FILTER
        // -----------------------------------------------------

        categoryButton.setOnClickListener(v -> {

            showCategoryDialog(
                    categoryButton
            );
        });


        // -----------------------------------------------------
        // PRICE FILTER
        // -----------------------------------------------------

        priceButton.setOnClickListener(v -> {

            showPriceDialog(
                    priceButton
            );
        });


        // -----------------------------------------------------
        // DISTANCE FILTER
        // -----------------------------------------------------

        distanceButton.setOnClickListener(v -> {

            showDistanceDialog(
                    distanceButton
            );
        });


        // -----------------------------------------------------
        // HOME SEARCH → FOCUS SEARCH FIELD
        // -----------------------------------------------------

        boolean focusSearch =
                getIntent().getBooleanExtra(
                        "focusSearch",
                        false
                );

        if (focusSearch) {

            searchField.requestFocus();

            searchField.post(() -> {

                InputMethodManager keyboard =
                        (InputMethodManager)
                                getSystemService(
                                        Context.INPUT_METHOD_SERVICE
                                );

                keyboard.showSoftInput(
                        searchField,
                        InputMethodManager.SHOW_IMPLICIT
                );
            });
        }


        // -----------------------------------------------------
        // INITIAL FILTER STATE
        // -----------------------------------------------------

        applyFilters();
    }


    // =========================================================
    // APPLY ALL FILTERS
    // =========================================================

    private void applyFilters() {

        String query =
                searchField
                        .getText()
                        .toString();


        // -----------------------------------------------------
        // DRILL
        // -----------------------------------------------------

        boolean drillMatches =
                searchService.matchesKeyword(
                        "Cordless Drill",
                        query
                )

                        &&

                        searchService.matchesCategory(
                                "Tools",
                                selectedCategory
                        )

                        &&

                        searchService.matchesMaxPrice(
                                5.0,
                                selectedMaxPrice
                        )

                        &&

                        searchService.matchesMaxDistance(
                                1.2,
                                selectedMaxDistanceKm
                        );


        // -----------------------------------------------------
        // TENT
        // -----------------------------------------------------

        boolean tentMatches =
                searchService.matchesKeyword(
                        "Camping Tent",
                        query
                )

                        &&

                        searchService.matchesCategory(
                                "Camping",
                                selectedCategory
                        )

                        &&

                        searchService.matchesMaxPrice(
                                8.0,
                                selectedMaxPrice
                        )

                        &&

                        searchService.matchesMaxDistance(
                                2.4,
                                selectedMaxDistanceKm
                        );


        // -----------------------------------------------------
        // SHOW / HIDE DRILL
        // -----------------------------------------------------

        cardDrill.setVisibility(
                drillMatches
                        ? View.VISIBLE
                        : View.GONE
        );


        // -----------------------------------------------------
        // SHOW / HIDE TENT
        // -----------------------------------------------------

        cardTent.setVisibility(
                tentMatches
                        ? View.VISIBLE
                        : View.GONE
        );
    }


    // =========================================================
    // CATEGORY FILTER DIALOG
    // =========================================================

    private void showCategoryDialog(
            TextView categoryButton
    ) {

        String[] categories = {
                "All categories",
                "Tools",
                "Camping"
        };


        new AlertDialog.Builder(this)

                .setTitle(
                        "Choose category"
                )

                .setItems(
                        categories,
                        (dialog, which) -> {

                            if (which == 0) {

                                selectedCategory = null;

                                categoryButton.setText(
                                        "Category"
                                );

                            } else {

                                selectedCategory =
                                        categories[which];

                                categoryButton.setText(
                                        selectedCategory
                                );
                            }


                            applyFilters();
                        }
                )

                .show();
    }


    // =========================================================
    // PRICE FILTER DIALOG
    // =========================================================

    private void showPriceDialog(
            TextView priceButton
    ) {

        String[] priceOptions = {
                "Any price",
                "Up to $5/day",
                "Up to $10/day",
                "Up to $20/day"
        };


        new AlertDialog.Builder(this)

                .setTitle(
                        "Maximum price"
                )

                .setItems(
                        priceOptions,
                        (dialog, which) -> {

                            if (which == 0) {

                                selectedMaxPrice = null;

                                priceButton.setText(
                                        "Price"
                                );

                            } else if (which == 1) {

                                selectedMaxPrice = 5.0;

                                priceButton.setText(
                                        "≤ $5"
                                );

                            } else if (which == 2) {

                                selectedMaxPrice = 10.0;

                                priceButton.setText(
                                        "≤ $10"
                                );

                            } else {

                                selectedMaxPrice = 20.0;

                                priceButton.setText(
                                        "≤ $20"
                                );
                            }


                            applyFilters();
                        }
                )

                .show();
    }


    // =========================================================
    // DISTANCE FILTER DIALOG
    // =========================================================

    private void showDistanceDialog(
            TextView distanceButton
    ) {

        String[] distanceOptions = {
                "Any distance",
                "Within 1 km",
                "Within 2 km",
                "Within 3 km",
                "Within 5 km"
        };


        new AlertDialog.Builder(this)

                .setTitle(
                        "Maximum distance"
                )

                .setItems(
                        distanceOptions,
                        (dialog, which) -> {

                            if (which == 0) {

                                selectedMaxDistanceKm = null;

                                distanceButton.setText(
                                        "Distance"
                                );

                            } else if (which == 1) {

                                selectedMaxDistanceKm = 1.0;

                                distanceButton.setText(
                                        "≤ 1 km"
                                );

                            } else if (which == 2) {

                                selectedMaxDistanceKm = 2.0;

                                distanceButton.setText(
                                        "≤ 2 km"
                                );

                            } else if (which == 3) {

                                selectedMaxDistanceKm = 3.0;

                                distanceButton.setText(
                                        "≤ 3 km"
                                );

                            } else {

                                selectedMaxDistanceKm = 5.0;

                                distanceButton.setText(
                                        "≤ 5 km"
                                );
                            }


                            applyFilters();
                        }
                )

                .show();
    }
}