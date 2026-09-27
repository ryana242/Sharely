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

    // Search/filter UI
    private EditText searchField;

    // Current hardcoded listing cards
    private LinearLayout cardDrill;
    private LinearLayout cardTent;

    // Contains the actual search/filter rules
    private ItemSearchService searchService;

    // null means no category filter is active
    private String selectedCategory = null;
    private Double selectedMaxPrice = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_browse);


        // ---------------------------------------------------------
        // SYSTEM BAR INSETS
        // ---------------------------------------------------------

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


        // ---------------------------------------------------------
        // FIND VIEWS
        // ---------------------------------------------------------

        searchField =
                findViewById(R.id.editSearch);

        cardDrill =
                findViewById(R.id.cardDrill);

        cardTent =
                findViewById(R.id.cardTent);

        TextView categoryButton =
                findViewById(R.id.buttonCategory);

        TextView priceButton =
                findViewById(R.id.buttonPrice);

        priceButton.setOnClickListener(v -> {
            showPriceDialog(priceButton);
        });

        View backButton =
                findViewById(R.id.buttonBack);

        View viewDrillButton =
                findViewById(R.id.buttonViewDrill);

        View viewTentButton =
                findViewById(R.id.buttonViewTent);


        // ---------------------------------------------------------
        // SEARCH SERVICE
        // ---------------------------------------------------------

        searchService =
                new ItemSearchService();


        // ---------------------------------------------------------
        // BACK BUTTON
        // ---------------------------------------------------------

        backButton.setOnClickListener(v -> {
            finish();
        });


        // ---------------------------------------------------------
        // DRILL → ITEM DETAILS
        // ---------------------------------------------------------

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


        // ---------------------------------------------------------
        // TENT → ITEM DETAILS
        // ---------------------------------------------------------

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


        // ---------------------------------------------------------
        // LIVE SEARCH
        // ---------------------------------------------------------

        searchField.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                        // Nothing needed here
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
                        // Nothing needed here
                    }
                }
        );


        // ---------------------------------------------------------
        // CATEGORY FILTER
        // ---------------------------------------------------------

        categoryButton.setOnClickListener(v -> {
            showCategoryDialog(categoryButton);
        });


        // ---------------------------------------------------------
        // HOME SEARCH → AUTOMATICALLY FOCUS SEARCH FIELD
        // ---------------------------------------------------------

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


        // Apply initial state
        applyFilters();
    }


    // =============================================================
    // APPLY ALL ACTIVE FILTERS
    // =============================================================

    private void applyFilters() {

        String query =
                searchField
                        .getText()
                        .toString();


        // ---------------------------------------------------------
        // DRILL
        // ---------------------------------------------------------

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
                        );


        // ---------------------------------------------------------
        // TENT
        // ---------------------------------------------------------

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
                        );


        // ---------------------------------------------------------
        // SHOW / HIDE RESULTS
        // ---------------------------------------------------------

        cardDrill.setVisibility(
                drillMatches
                        ? View.VISIBLE
                        : View.GONE
        );

        cardTent.setVisibility(
                tentMatches
                        ? View.VISIBLE
                        : View.GONE
        );
    }


    // =============================================================
    // CATEGORY SELECTION DIALOG
    // =============================================================

    private void showCategoryDialog(
            TextView categoryButton
    ) {

        String[] categories = {
                "All categories",
                "Tools",
                "Camping"
        };


        new AlertDialog.Builder(this)
                .setTitle("Choose category")

                .setItems(
                        categories,
                        (dialog, which) -> {

                            if (which == 0) {

                                // No category restriction
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


                            // Re-evaluate both search
                            // and category together
                            applyFilters();
                        }
                )

                .show();
    }

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
                .setTitle("Maximum price")
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
}