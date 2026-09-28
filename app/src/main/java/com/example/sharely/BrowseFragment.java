package com.example.sharely;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.sharely.service.ItemSearchService;
import com.example.sharely.service.LocationService;

public class BrowseFragment extends Fragment {

    private EditText searchField;

    private LinearLayout cardDrill;
    private LinearLayout cardTent;

    private ItemSearchService searchService;
    private LocationService locationService;

    private String selectedCategory = null;
    private Double selectedMaxPrice = null;
    private Double selectedMaxDistanceKm = null;

    private Double userLatitude = null;
    private Double userLongitude = null;

    /*
     * Until location is known, these remain very large.
     * If no distance filter is selected, this does not matter.
     */
    private double drillDistanceKm = Double.MAX_VALUE;
    private double tentDistanceKm = Double.MAX_VALUE;


    // Temporary demo listing coordinates
    private static final double DRILL_LATITUDE = -33.8731;
    private static final double DRILL_LONGITUDE = 151.2065;

    private static final double TENT_LATITUDE = -33.8790;
    private static final double TENT_LONGITUDE = 151.2150;


    // =========================================================
    // LOCATION PERMISSION
    // =========================================================

    private final ActivityResultLauncher<String[]>
            locationPermissionLauncher =

            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {

                        Boolean fineGranted =
                                result.get(
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                );

                        Boolean coarseGranted =
                                result.get(
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                );

                        boolean granted =
                                Boolean.TRUE.equals(fineGranted)
                                        || Boolean.TRUE.equals(coarseGranted);


                        if (granted) {

                            loadCurrentLocation();

                        } else {

                            Toast.makeText(
                                    requireContext(),
                                    "Location permission is needed for nearby discovery.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );


    public BrowseFragment() {
    }


    // =========================================================
    // CREATE VIEW
    // =========================================================

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


    // =========================================================
    // VIEW CREATED
    // =========================================================

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
        // SERVICES FIRST
        // -----------------------------------------------------

        searchService =
                new ItemSearchService();

        locationService =
                new LocationService();


        // -----------------------------------------------------
        // VIEWS
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

        View drillDetailsButton =
                view.findViewById(
                        R.id.buttonViewDrill
                );

        View tentDetailsButton =
                view.findViewById(
                        R.id.buttonViewTent
                );


        // -----------------------------------------------------
        // BACK → HOME
        // -----------------------------------------------------

        backButton.setOnClickListener(v -> {

            MainAppActivity activity =
                    (MainAppActivity) requireActivity();

            activity.selectTab(
                    R.id.navHome
            );
        });


        // -----------------------------------------------------
        // DRILL DETAILS
        // -----------------------------------------------------

        drillDetailsButton.setOnClickListener(v -> {

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
                    formatDistance(drillDistanceKm)
                            + " • ★ 4.8"
            );

            intent.putExtra(
                    "description",
                    "A reliable cordless drill suitable for small home projects."
            );

            startActivity(intent);
        });


        // -----------------------------------------------------
        // TENT DETAILS
        // -----------------------------------------------------

        tentDetailsButton.setOnClickListener(v -> {

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
                    formatDistance(tentDistanceKm)
                            + " • ★ 4.6"
            );

            intent.putExtra(
                    "description",
                    "A spacious camping tent suitable for weekend trips."
            );

            startActivity(intent);
        });


        // -----------------------------------------------------
        // SEARCH
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
        // FILTER BUTTONS
        // -----------------------------------------------------

        categoryButton.setOnClickListener(v ->
                showCategoryDialog(
                        categoryButton
                )
        );

        priceButton.setOnClickListener(v ->
                showPriceDialog(
                        priceButton
                )
        );

        distanceButton.setOnClickListener(v ->
                showDistanceDialog(
                        distanceButton
                )
        );


        // -----------------------------------------------------
        // HOME SEARCH REQUEST
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


        // First show listings normally
        applyFilters();

        // Then try to obtain real location
        requestCurrentLocation();
    }


    // =========================================================
    // APPLY FILTERS
    // =========================================================

    private void applyFilters() {

        if (searchField == null
                || searchService == null) {

            return;
        }


        String query =
                searchField
                        .getText()
                        .toString();


        boolean drillMatches =
                searchService.matchesKeyword(
                        "Cordless Drill",
                        query
                )
                        && searchService.matchesCategory(
                        "Tools",
                        selectedCategory
                )
                        && searchService.matchesMaxPrice(
                        5.0,
                        selectedMaxPrice
                )
                        && searchService.matchesMaxDistance(
                        drillDistanceKm,
                        selectedMaxDistanceKm
                );


        boolean tentMatches =
                searchService.matchesKeyword(
                        "Camping Tent",
                        query
                )
                        && searchService.matchesCategory(
                        "Camping",
                        selectedCategory
                )
                        && searchService.matchesMaxPrice(
                        8.0,
                        selectedMaxPrice
                )
                        && searchService.matchesMaxDistance(
                        tentDistanceKm,
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

        String[] options = {
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
                        options,
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

        String[] options = {
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
                        options,
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


    // =========================================================
    // REQUEST LOCATION
    // =========================================================

    private void requestCurrentLocation() {

        boolean fineGranted =
                ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;


        if (fineGranted || coarseGranted) {

            loadCurrentLocation();

        } else {

            locationPermissionLauncher.launch(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }
            );
        }
    }


    // =========================================================
    // LOAD LOCATION
    // =========================================================

    private void loadCurrentLocation() {

        if (!isAdded()) {
            return;
        }


        boolean fineGranted =
                ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;


        if (!fineGranted && !coarseGranted) {
            return;
        }


        LocationManager manager =
                (LocationManager)
                        requireContext()
                                .getSystemService(
                                        Context.LOCATION_SERVICE
                                );


        if (manager == null) {
            return;
        }


        Location location = null;


        try {

            if (manager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
            )) {

                location =
                        manager.getLastKnownLocation(
                                LocationManager.GPS_PROVIDER
                        );
            }


            if (location == null
                    && manager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
            )) {

                location =
                        manager.getLastKnownLocation(
                                LocationManager.NETWORK_PROVIDER
                        );
            }

        } catch (SecurityException exception) {

            return;
        }


        if (location != null) {

            userLatitude =
                    location.getLatitude();

            userLongitude =
                    location.getLongitude();


            calculateItemDistances();

            applyFilters();

        } else {

            Toast.makeText(
                    requireContext(),
                    "Current location is not available yet.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    // =========================================================
    // CALCULATE LISTING DISTANCES
    // =========================================================

    private void calculateItemDistances() {

        if (userLatitude == null
                || userLongitude == null
                || locationService == null) {

            return;
        }


        drillDistanceKm =
                locationService.calculateDistanceKm(
                        userLatitude,
                        userLongitude,
                        DRILL_LATITUDE,
                        DRILL_LONGITUDE
                );


        tentDistanceKm =
                locationService.calculateDistanceKm(
                        userLatitude,
                        userLongitude,
                        TENT_LATITUDE,
                        TENT_LONGITUDE
                );
    }


    // =========================================================
    // FORMAT DISTANCE
    // =========================================================

    private String formatDistance(
            double distanceKm
    ) {

        if (distanceKm == Double.MAX_VALUE) {
            return "Distance unavailable";
        }

        return String.format(
                "%.1f km away",
                distanceKm
        );
    }
}