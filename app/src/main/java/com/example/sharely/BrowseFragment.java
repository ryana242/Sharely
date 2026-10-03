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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.sharely.enums.ItemStatus;
import com.example.sharely.model.Item;

import com.bumptech.glide.Glide;
import com.example.sharely.data.ItemRepository;
import com.example.sharely.service.ItemSearchService;

import java.util.ArrayList;
import java.util.List;


public class BrowseFragment extends Fragment {

    // =========================================================
    // UI FIELDS
    // =========================================================

    private EditText searchField;

    private LinearLayout itemsContainer;

    private TextView textItemCount;


    // =========================================================
    // SEARCH / FILTER LOGIC
    // =========================================================

    private ItemSearchService searchService;

    private String selectedCategory = null;

    private Double selectedMaxPrice = null;

    private Double selectedMaxDistanceKm = null;


    // =========================================================
    // FIRESTORE DATA
    // =========================================================

    private final List<Item> allItems = new ArrayList<>();
    private ItemRepository itemRepository;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public BrowseFragment() {
        // Required empty constructor
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
        // FIND MAIN VIEWS
        // -----------------------------------------------------

        searchField =
                view.findViewById(
                        R.id.editSearch
                );

        itemsContainer =
                view.findViewById(
                        R.id.itemsContainer
                );

        textItemCount =
                view.findViewById(
                        R.id.textItemCount
                );


        // -----------------------------------------------------
        // FILTER CONTROLS
        // -----------------------------------------------------

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


        // -----------------------------------------------------
        // NAVIGATION CONTROLS
        // -----------------------------------------------------

        View backButton =
                view.findViewById(
                        R.id.buttonBack
                );


        // -----------------------------------------------------
        // CREATE SERVICES
        // -----------------------------------------------------

        searchService =
                new ItemSearchService();

        itemRepository =
                new ItemRepository();


        // =====================================================
        // BACK BUTTON
        // =====================================================

        backButton.setOnClickListener(v -> {

            MainAppActivity activity =
                    (MainAppActivity)
                            requireActivity();

            activity.selectTab(
                    R.id.navHome
            );
        });


        // =====================================================
        // LIVE KEYWORD SEARCH
        // =====================================================

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


        // =====================================================
        // CATEGORY FILTER
        // =====================================================

        categoryButton.setOnClickListener(v -> {

            showCategoryDialog(
                    categoryButton
            );
        });


        // =====================================================
        // PRICE FILTER
        // =====================================================

        priceButton.setOnClickListener(v -> {

            showPriceDialog(
                    priceButton
            );
        });


        // =====================================================
        // DISTANCE FILTER
        // =====================================================

        distanceButton.setOnClickListener(v -> {

            showDistanceDialog(
                    distanceButton
            );
        });


        // =====================================================
        // CHECK IF HOME REQUESTED SEARCH FOCUS
        // =====================================================

        MainAppActivity mainActivity =
                (MainAppActivity)
                        requireActivity();

        boolean focusSearch =
                mainActivity
                        .consumeBrowseSearchFocus();


        if (focusSearch) {

            searchField.requestFocus();

            searchField.post(() -> {

                InputMethodManager keyboard =
                        (InputMethodManager)
                                requireContext()
                                        .getSystemService(
                                                Context.INPUT_METHOD_SERVICE
                                        );

                if (keyboard != null) {

                    keyboard.showSoftInput(
                            searchField,
                            InputMethodManager.SHOW_IMPLICIT
                    );
                }
            });
        }


        // =====================================================
        // LOAD ITEMS FROM FIRESTORE
        // =====================================================

        loadItems();
    }


    // =========================================================
    // LOAD ITEMS FROM FIRESTORE
    // =========================================================

    private void loadItems() {

        itemRepository.getAllItems(

                new ItemRepository.OnItemsLoadedListener() {

                    @Override
                    public void onSuccess(
                            List<Item> items
                    ) {

                        allItems.clear();

                        allItems.addAll(
                                items
                        );

                        applyFilters();
                    }


                    @Override
                    public void onFailure(
                            Exception e
                    ) {

                        if (!isAdded()) {
                            return;
                        }

                        Toast.makeText(
                                requireContext(),
                                "Failed to load items",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }


    // =========================================================
    // APPLY ALL FILTERS
    // =========================================================

    private void applyFilters() {

        if (
                searchField == null
                        ||
                        itemsContainer == null
                        ||
                        textItemCount == null
                        ||
                        searchService == null
        ) {

            return;
        }


        String query =
                searchField
                        .getText()
                        .toString();


        // Clear old cards
        itemsContainer.removeAllViews();


        int visibleCount = 0;


        // -----------------------------------------------------
        // LOOP THROUGH FIRESTORE ITEMS
        // -----------------------------------------------------

        for (Item item : allItems) {
            // -------------------------------------------------
            // ONLY SHOW ACTIVE ITEMS
            // -------------------------------------------------
            if (item.getStatus()
                    != ItemStatus.ACTIVE) {
                continue;
            }

            // -------------------------------------------------
            // GET ITEM DATA
            // -------------------------------------------------
            String itemId = item.getItemId();
            String itemName = item.getItemName();
            String description = item.getDescription();
            String category = item.getCategory();
            String location = item.getLocation();
            double price = item.getPrice();
            ArrayList<String> imageUrls = item.getImageUrls();


            // -------------------------------------------------
            // CHECK DATA
            // -------------------------------------------------

            if (itemName == null) {
                continue;
            }

            // -------------------------------------------------
            // KEYWORD FILTER
            // -------------------------------------------------

            boolean matchesKeyword =
                    searchService.matchesKeyword(
                            itemName,
                            query
                    );


            // -------------------------------------------------
            // CATEGORY FILTER
            // -------------------------------------------------

            boolean matchesCategory =
                    searchService.matchesCategory(
                            category,
                            selectedCategory
                    );


            // -------------------------------------------------
            // PRICE FILTER
            // -------------------------------------------------

            boolean matchesPrice =
                    searchService.matchesMaxPrice(
                            price,
                            selectedMaxPrice
                    );


            // -------------------------------------------------
            // SHOW MATCHING ITEM
            // -------------------------------------------------

            if (
                    matchesKeyword
                            &&
                            matchesCategory
                            &&
                            matchesPrice
            ) {

                addItemCard(
                        itemId,
                        itemName,
                        description,
                        category,
                        price,
                        location,
                        imageUrls
                );

                visibleCount++;
            }
        }


        // -----------------------------------------------------
        // UPDATE RESULT COUNT
        // -----------------------------------------------------

        textItemCount.setText(

                visibleCount == 1

                        ? "1 item"

                        : visibleCount + " items"
        );
    }


    // =========================================================
    // DISPLAY ITEM CARD
    // =========================================================

    private void addItemCard(
            String itemId,
            String itemName,
            String description,
            String category,
            double price,
            String location,
            ArrayList<String> imageUrls
    ) {

        View card =
                getLayoutInflater().inflate(
                        R.layout.item_card,
                        itemsContainer,
                        false
                );


        // -----------------------------------------------------
        // FIND CARD VIEWS
        // -----------------------------------------------------

        TextView nameView =
                card.findViewById(
                        R.id.textItemName
                );

        TextView priceView =
                card.findViewById(
                        R.id.textItemPrice
                );

        TextView infoView =
                card.findViewById(
                        R.id.textItemInfo
                );

        ImageView itemImage =
                card.findViewById(
                        R.id.imageItem
                );

        View detailsButton =
                card.findViewById(
                        R.id.buttonViewDetails
                );


        // -----------------------------------------------------
        // DISPLAY IMAGE
        // -----------------------------------------------------

        if (
                imageUrls != null
                        &&
                        !imageUrls.isEmpty()
        ) {

            Glide.with(this)
                    .load(
                            imageUrls.get(0)
                    )
                    .fitCenter()
                    .into(
                            itemImage
                    );
        }


        // -----------------------------------------------------
        // DISPLAY ITEM DATA
        // -----------------------------------------------------

        nameView.setText(
                itemName
        );


        priceView.setText(
                "$" + price + " / day"
        );


        if (location != null) {

            infoView.setText(
                    location
            );

        } else {

            infoView.setText(
                    ""
            );
        }


        // =====================================================
        // VIEW DETAILS
        // =====================================================

        detailsButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            requireContext(),
                            ItemDetailsActivity.class
                    );


            intent.putExtra(
                    "itemId",
                    itemId
            );


            intent.putExtra(
                    "name",
                    itemName
            );


            intent.putExtra(
                    "price",
                    "$" + price + " per day"
            );


            intent.putExtra(
                    "info",
                    location
            );


            intent.putExtra(
                    "description",
                    description
            );


            // Pass all images to ItemDetailsActivity
            if (imageUrls != null) {

                intent.putStringArrayListExtra(
                        "imageUrls",
                        new ArrayList<>(
                                imageUrls
                        )
                );
            }


            startActivity(
                    intent
            );
        });


        // -----------------------------------------------------
        // ADD CARD TO BROWSE PAGE
        // -----------------------------------------------------

        itemsContainer.addView(
                card
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
                "Camping",
                "Electronics"
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

                                selectedCategory =
                                        null;

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

                                selectedMaxPrice =
                                        null;

                                priceButton.setText(
                                        "Price"
                                );

                            } else if (which == 1) {

                                selectedMaxPrice =
                                        5.0;

                                priceButton.setText(
                                        "≤ $5"
                                );

                            } else if (which == 2) {

                                selectedMaxPrice =
                                        10.0;

                                priceButton.setText(
                                        "≤ $10"
                                );

                            } else {

                                selectedMaxPrice =
                                        20.0;

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

                                selectedMaxDistanceKm =
                                        null;

                                distanceButton.setText(
                                        "Distance"
                                );

                            } else if (which == 1) {

                                selectedMaxDistanceKm =
                                        1.0;

                                distanceButton.setText(
                                        "≤ 1 km"
                                );

                            } else if (which == 2) {

                                selectedMaxDistanceKm =
                                        2.0;

                                distanceButton.setText(
                                        "≤ 2 km"
                                );

                            } else if (which == 3) {

                                selectedMaxDistanceKm =
                                        3.0;

                                distanceButton.setText(
                                        "≤ 3 km"
                                );

                            } else {

                                selectedMaxDistanceKm =
                                        5.0;

                                distanceButton.setText(
                                        "≤ 5 km"
                                );
                            }


                            /*
                             * Distance is not applied yet because
                             * Firestore items currently do not
                             * contain latitude / longitude.
                             */
                            applyFilters();
                        }
                )

                .show();
    }
}