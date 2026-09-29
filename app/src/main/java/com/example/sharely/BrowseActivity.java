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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sharely.data.ItemRepository;
import com.example.sharely.service.ItemSearchService;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class BrowseActivity extends AppCompatActivity {

    // ---------------------------------------------------------
    // UI FIELDS
    // ---------------------------------------------------------

    private EditText searchField;
    private LinearLayout itemsContainer;

    private TextView textItemCount;


    // ---------------------------------------------------------
    // SEARCH / FILTER LOGIC
    // ---------------------------------------------------------

    private ItemSearchService searchService;

    // null means that particular filter is not active
    private String selectedCategory = null;

    private Double selectedMaxPrice = null;

    private Double selectedMaxDistanceKm = null;

    private final List<DocumentSnapshot> allItems = new ArrayList<>();

    private ItemRepository itemRepository;

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

        searchField = findViewById(R.id.editSearch);

        itemsContainer = findViewById(R.id.itemsContainer);

        textItemCount = findViewById(R.id.textItemCount);


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


        // -----------------------------------------------------
        // CREATE SEARCH SERVICE
        // -----------------------------------------------------

        searchService =
                new ItemSearchService();

        // -----------------------------------------------------
        // INITIAL itemRepository
        // -----------------------------------------------------
        itemRepository = new ItemRepository();

        // -----------------------------------------------------
        // BACK BUTTON
        // -----------------------------------------------------

        backButton.setOnClickListener(v -> {
            finish();
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

        loadItems();
    }

    // =========================================================
    // LOAD PAGE INFORMATION
    // =========================================================

    private void loadItems() {

        itemRepository.getAllItems(
                new ItemRepository.OnItemsLoadedListener() {

                    @Override
                    public void onSuccess(
                            com.google.firebase.firestore.QuerySnapshot querySnapshot
                    ) {

                        allItems.clear();

                        allItems.addAll(
                                querySnapshot.getDocuments()
                        );

                        applyFilters();
                    }


                    @Override
                    public void onFailure(
                            Exception e
                    ) {

                        Toast.makeText(
                                BrowseActivity.this,
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

        String query =
                searchField
                        .getText()
                        .toString();


        // Clear old cards
        itemsContainer.removeAllViews();


        int visibleCount = 0;


        for (DocumentSnapshot document : allItems) {

            String itemId =
                    document.getString("itemId");

            String itemName =
                    document.getString("itemName");

            String description =
                    document.getString("description");

            String category =
                    document.getString("category");

            String location =
                    document.getString("location");

            Double price =
                    document.getDouble("price");

            ArrayList<String> imageUrls =

                    (ArrayList<String>) document.get("imageUrls");

            if (itemName == null) {
                continue;
            }

            if (price == null) {
                price = 0.0;
            }


            // Keyword filter
            boolean matchesKeyword =
                    searchService.matchesKeyword(
                            itemName,
                            query
                    );


            // Category filter
            boolean matchesCategory =
                    searchService.matchesCategory(
                            category,
                            selectedCategory
                    );


            // Price filter
            boolean matchesPrice =
                    searchService.matchesMaxPrice(
                            price,
                            selectedMaxPrice
                    );


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


        textItemCount.setText(
                visibleCount == 1
                        ? "1 item"
                        : visibleCount + " items"
        );
    }

    //DISPLAY CARD
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

        View detailsButton =
                card.findViewById(
                        R.id.buttonViewDetails
                );

        ImageView itemImage = card.findViewById(
                R.id.imageItem);

        // ---------------------------------------------------------
        // DISPLAY DATA
        // ---------------------------------------------------------

        if (imageUrls != null && !imageUrls.isEmpty()) {

            Glide.with(this)
                    .load(imageUrls.get(0))
                    .fitCenter()
                    .into(itemImage);
        }
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


        // ---------------------------------------------------------
        // VIEW DETAILS
        // ---------------------------------------------------------

        detailsButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            BrowseActivity.this,
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
            if (imageUrls != null) {

                intent.putStringArrayListExtra(
                        "imageUrls",
                        new ArrayList<>(imageUrls)
                );
            }
            startActivity(intent);
        });


        // Add card to Browse page
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