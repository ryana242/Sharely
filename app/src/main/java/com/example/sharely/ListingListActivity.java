package com.example.sharely;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import com.example.sharely.data.ItemRepository;
import com.example.sharely.data.UserRepository;
import com.example.sharely.enums.ItemStatus;
import com.example.sharely.model.Item;

import java.util.ArrayList;
import java.util.List;


public class ListingListActivity
        extends AppCompatActivity {


    private LinearLayout itemsContainer;

    private TextView titleText;

    private TextView countText;

    private ItemRepository itemRepository;

    private UserRepository userRepository;

    private String type;


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );


        setContentView(
                R.layout.activity_listing_list
        );


        // -----------------------------------------------------
        // FIND VIEWS
        // -----------------------------------------------------

        View backButton =
                findViewById(
                        R.id.buttonBack
                );


        titleText =
                findViewById(
                        R.id.textTitle
                );


        countText =
                findViewById(
                        R.id.textItemCount
                );


        itemsContainer =
                findViewById(
                        R.id.itemsContainer
                );


        // -----------------------------------------------------
        // REPOSITORIES
        // -----------------------------------------------------

        itemRepository =
                new ItemRepository();


        userRepository =
                new UserRepository();


        // -----------------------------------------------------
        // GET LIST TYPE
        // -----------------------------------------------------

        type =
                getIntent()
                        .getStringExtra(
                                "type"
                        );


        if (type == null) {

            type =
                    "ACTIVE";
        }


        // -----------------------------------------------------
        // SET TITLE
        // -----------------------------------------------------

        if (
                type.equals(
                        "ACTIVE"
                )
        ) {

            titleText.setText(
                    "Active listings"
            );

        } else {

            titleText.setText(
                    "Unavailable & past"
            );
        }


        // -----------------------------------------------------
        // BACK BUTTON
        // -----------------------------------------------------

        backButton.setOnClickListener(v -> {

            finish();
        });


        // -----------------------------------------------------
        // LOAD LISTINGS
        // -----------------------------------------------------

        loadListings();
    }


    // =========================================================
    // LOAD CURRENT USER LISTINGS
    // =========================================================

    private void loadListings() {

        String currentUserId =
                userRepository
                        .getCurrentUserId();


        if (currentUserId == null) {

            countText.setText(
                    "0 listings"
            );

            itemsContainer.removeAllViews();

            return;
        }


        itemRepository.getItemsByOwner(

                currentUserId,

                new ItemRepository.OnOwnerItemsLoadedListener() {

                    @Override
                    public void onSuccess(
                            List<Item> items
                    ) {

                        displayItems(
                                items
                        );
                    }


                    @Override
                    public void onFailure(
                            Exception e
                    ) {

                        Toast.makeText(
                                ListingListActivity.this,
                                "Failed to load listings",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }


    // =========================================================
    // FILTER AND DISPLAY ITEMS
    // =========================================================

    private void displayItems(
            List<Item> items
    ) {

        itemsContainer.removeAllViews();


        int count = 0;


        for (Item item : items) {


            // -------------------------------------------------
            // ACTIVE LISTINGS PAGE
            // -------------------------------------------------

            if (
                    type.equals(
                            "ACTIVE"
                    )
                            &&
                            item.getStatus()
                                    != ItemStatus.ACTIVE
            ) {

                continue;
            }


            // -------------------------------------------------
            // UNAVAILABLE & PAST PAGE
            // -------------------------------------------------

            if (
                    type.equals(
                            "PAST"
                    )
                            &&
                            item.getStatus()
                                    != ItemStatus.UNAVAILABLE
                            &&
                            item.getStatus()
                                    != ItemStatus.PAST
            ) {

                continue;
            }


            // -------------------------------------------------
            // ADD ITEM CARD
            // -------------------------------------------------

            addItemCard(
                    item,
                    false
            );


            count++;
        }


        // -----------------------------------------------------
        // DISPLAY COUNT
        // -----------------------------------------------------

        countText.setText(
                count
                        + " listing"
                        + (
                        count == 1
                                ? ""
                                : "s"
                )
        );
    }


    // =========================================================
    // ADD ITEM CARD
    // =========================================================

    private void addItemCard(
            Item item,
            boolean showViewDetails
    ) {

        View card =
                LayoutInflater
                        .from(this)
                        .inflate(
                                R.layout.item_card,
                                itemsContainer,
                                false
                        );


        // -----------------------------------------------------
        // FIND ITEM CARD VIEWS
        // -----------------------------------------------------

        ImageView itemImage =
                card.findViewById(
                        R.id.imageItem
                );


        TextView itemName =
                card.findViewById(
                        R.id.textItemName
                );


        TextView itemInfo =
                card.findViewById(
                        R.id.textItemInfo
                );


        TextView itemPrice =
                card.findViewById(
                        R.id.textItemPrice
                );


        Button viewDetailsButton =
                card.findViewById(
                        R.id.buttonViewDetails
                );
        if (showViewDetails) {

            viewDetailsButton.setVisibility(

                    View.GONE

            );

        } else {

            viewDetailsButton.setVisibility(

                    View.GONE

            );

        }

        // -----------------------------------------------------
        // DISPLAY ITEM NAME
        // -----------------------------------------------------

        itemName.setText(
                item.getItemName()
        );


        // -----------------------------------------------------
        // DISPLAY LOCATION
        // -----------------------------------------------------

        if (
                item.getLocation() != null
        ) {

            itemInfo.setText(
                    item.getLocation()
            );

        } else {

            itemInfo.setText(
                    ""
            );
        }


        // -----------------------------------------------------
        // DISPLAY PRICE
        // -----------------------------------------------------

        itemPrice.setText(
                "$"
                        + item.getPrice()
                        + " / day"
        );


        // -----------------------------------------------------
        // DISPLAY IMAGE
        // -----------------------------------------------------

        ArrayList<String> imageUrls =
                item.getImageUrls();


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


        // =====================================================
        // VIEW DETAILS
        // =====================================================

        if (showViewDetails) {

            viewDetailsButton.setVisibility(
                    View.VISIBLE
            );


            viewDetailsButton.setOnClickListener(v -> {

                Intent intent =
                        new Intent(
                                ListingListActivity.this,
                                ItemDetailsActivity.class
                        );


                intent.putExtra(
                        "itemId",
                        item.getItemId()
                );


                intent.putExtra(
                        "name",
                        item.getItemName()
                );


                intent.putExtra(
                        "price",
                        "$"
                                + item.getPrice()
                                + " per day"
                );


                intent.putExtra(
                        "info",
                        item.getLocation()
                );


                intent.putExtra(
                        "description",
                        item.getDescription()
                );


                if (item.getImageUrls() != null) {

                    intent.putStringArrayListExtra(
                            "imageUrls",
                            item.getImageUrls()
                    );
                }


                startActivity(
                        intent
                );
            });

        } else {

            viewDetailsButton.setVisibility(
                    View.GONE
            );
        }


        // -----------------------------------------------------
        // ADD MANAGEMENT BUTTONS
        // -----------------------------------------------------

        addManagementButtons(
                card,
                item
        );


        // -----------------------------------------------------
        // ADD CARD TO PAGE
        // -----------------------------------------------------

        itemsContainer.addView(
                card
        );
    }


    // =========================================================
    // ADD MANAGEMENT BUTTONS
    // =========================================================

    private void addManagementButtons(
            View card,
            Item item
    ) {

        LinearLayout cardLayout =
                (LinearLayout) card;


        // -----------------------------------------------------
        // PAST ITEM IS ALREADY TAKEN DOWN
        // -----------------------------------------------------

        if (
                item.getStatus()
                        == ItemStatus.PAST
        ) {

            return;
        }


        // =====================================================
        // STATUS BUTTON ROW
        // =====================================================

        LinearLayout statusRow =
                new LinearLayout(
                        this
                );


        statusRow.setOrientation(
                LinearLayout.HORIZONTAL
        );


        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );


        rowParams.topMargin =
                dpToPx(
                        12
                );


        statusRow.setLayoutParams(
                rowParams
        );


        // =====================================================
        // ACTIVE BUTTON
        // =====================================================

        Button activeButton =
                new Button(
                        this
                );


        activeButton.setText(
                "Active"
        );


        LinearLayout.LayoutParams activeParams =
                new LinearLayout.LayoutParams(
                        0,
                        dpToPx(50),
                        1
                );


        activeButton.setLayoutParams(
                activeParams
        );


        // =====================================================
        // UNAVAILABLE BUTTON
        // =====================================================

        Button unavailableButton =
                new Button(
                        this
                );


        unavailableButton.setText(
                "Unavailable"
        );


        LinearLayout.LayoutParams unavailableParams =
                new LinearLayout.LayoutParams(
                        0,
                        dpToPx(50),
                        1
                );


        unavailableParams.leftMargin =
                dpToPx(
                        8
                );


        unavailableButton.setLayoutParams(
                unavailableParams
        );


        // =====================================================
        // TAKE DOWN BUTTON
        // =====================================================

        Button takeDownButton =
                new Button(
                        this
                );


        takeDownButton.setText(
                "Take down"
        );


        LinearLayout.LayoutParams takeDownParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(50)
                );


        takeDownParams.topMargin =
                dpToPx(
                        8
                );


        takeDownButton.setLayoutParams(
                takeDownParams
        );


        // =====================================================
        // ACTIVE CLICK
        // =====================================================

        activeButton.setOnClickListener(v -> {

            updateStatus(
                    item,
                    ItemStatus.ACTIVE
            );
        });


        // =====================================================
        // UNAVAILABLE CLICK
        // =====================================================

        unavailableButton.setOnClickListener(v -> {

            updateStatus(
                    item,
                    ItemStatus.UNAVAILABLE
            );
        });


        // =====================================================
        // TAKE DOWN CLICK
        // =====================================================

        takeDownButton.setOnClickListener(v -> {

            updateStatus(
                    item,
                    ItemStatus.PAST
            );
        });


        // -----------------------------------------------------
        // ADD STATUS BUTTONS
        // -----------------------------------------------------

        statusRow.addView(
                activeButton
        );


        statusRow.addView(
                unavailableButton
        );


        // -----------------------------------------------------
        // ADD MANAGEMENT AREA TO CARD
        // -----------------------------------------------------

        cardLayout.addView(
                statusRow
        );


        cardLayout.addView(
                takeDownButton
        );
    }


    // =========================================================
    // UPDATE ITEM STATUS
    // =========================================================

    private void updateStatus(
            Item item,
            ItemStatus status
    ) {

        itemRepository.updateItemStatus(

                item.getItemId(),

                status,

                new ItemRepository.OnItemStatusUpdatedListener() {

                    @Override
                    public void onSuccess() {

                        Toast.makeText(
                                ListingListActivity.this,
                                "Listing updated",
                                Toast.LENGTH_SHORT
                        ).show();


                        // Reload list after status change
                        loadListings();
                    }


                    @Override
                    public void onFailure(
                            Exception e
                    ) {

                        Toast.makeText(
                                ListingListActivity.this,
                                "Failed to update listing",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }


    // =========================================================
    // DP TO PX
    // =========================================================

    private int dpToPx(
            int dp
    ) {

        return (int) (
                dp
                        * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}