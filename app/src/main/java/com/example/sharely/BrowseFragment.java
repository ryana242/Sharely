package com.example.sharely;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.sharely.service.ItemSearchService;

public class BrowseFragment extends Fragment {

    private EditText searchField;

    private LinearLayout cardDrill;
    private LinearLayout cardTent;

    private ItemSearchService searchService;

    private String selectedCategory = null;
    private Double selectedMaxPrice = null;
    private Double selectedMaxDistanceKm = null;


    public BrowseFragment() {
    }


    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        return inflater.inflate(
                R.layout.fragment_browse,
                container,
                false
        );
    }


    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {

        super.onViewCreated(
                view,
                savedInstanceState
        );


        // -----------------------------------------------------
        // FIND VIEWS
        // -----------------------------------------------------

        searchField =
                view.findViewById(
                        R.id.editSearch
                );

        cardDrill =
                view.findViewById(
                        R.id.cardDrill
                );

        cardTent =
                view.findViewById(
                        R.id.cardTent
                );


        TextView categoryButton =
                view.findViewById(
                        R.id.buttonCategory
                );

        TextView priceButton =
                view.findViewById(
                        R.id.buttonPrice
                );

        TextView distanceButton =
                view.findViewById(
                        R.id.buttonFilter
                );


        View backButton =
                view.findViewById(
                        R.id.buttonBack
                );

        View viewDrillButton =
                view.findViewById(
                        R.id.buttonViewDrill
                );

        View viewTentButton =
                view.findViewById(
                        R.id.buttonViewTent
                );


        // -----------------------------------------------------
        // SEARCH SERVICE
        // -----------------------------------------------------

        searchService =
                new ItemSearchService();


        // -----------------------------------------------------
        // BACK
        // -----------------------------------------------------

        backButton.setOnClickListener(v -> {

            MainAppActivity mainActivity =
                    (MainAppActivity) requireActivity();

            mainActivity.selectTab(
                    R.id.navHome
            );
        });


        // -----------------------------------------------------
        // DRILL → ITEM DETAILS
        // -----------------------------------------------------

        viewDrillButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            requireContext(),
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
                            requireContext(),
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
        // LIVE SEARCH
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
                    }
                }
        );


        // -----------------------------------------------------
        // CATEGORY
        // -----------------------------------------------------

        categoryButton.setOnClickListener(v -> {

            showCategoryDialog(
                    categoryButton
            );
        });


        // -----------------------------------------------------
        // PRICE
        // -----------------------------------------------------

        priceButton.setOnClickListener(v -> {

            showPriceDialog(
                    priceButton
            );
        });


        // -----------------------------------------------------
        // DISTANCE
        // -----------------------------------------------------

        distanceButton.setOnClickListener(v -> {

            showDistanceDialog(
                    distanceButton
            );
        });


        // -----------------------------------------------------
        // HOME SEARCH → FOCUS BROWSE SEARCH
        // -----------------------------------------------------

        MainAppActivity mainActivity =
                (MainAppActivity) requireActivity();

        boolean focusSearch =
                mainActivity.consumeBrowseSearchFocus();

        if (focusSearch) {

            searchField.requestFocus();

            searchField.post(() -> {

                InputMethodManager keyboard =
                        (InputMethodManager)
                                requireContext()
                                        .getSystemService(
                                                Context.INPUT_METHOD_SERVICE
                                        );

                keyboard.showSoftInput(
                        searchField,
                        InputMethodManager.SHOW_IMPLICIT
                );
            });
        }


        applyFilters();
    }


    // =========================================================
    // APPLY FILTERS
    // =========================================================

    private void applyFilters() {

        String query =
                searchField
                        .getText()
                        .toString();


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


    // =========================================================
    // CATEGORY
    // =========================================================

    private void showCategoryDialog(
            TextView categoryButton
    ) {

        String[] categories = {
                "All categories",
                "Tools",
                "Camping"
        };


        new AlertDialog.Builder(
                requireContext()
        )

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
    // PRICE
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


        new AlertDialog.Builder(
                requireContext()
        )

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
    // DISTANCE
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


        new AlertDialog.Builder(
                requireContext()
        )

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