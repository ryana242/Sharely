package com.example.sharely;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import com.example.sharely.data.BookingRepository;
import com.example.sharely.data.ItemRepository;
import com.example.sharely.data.UserRepository;

import com.example.sharely.model.Booking;

import com.example.sharely.model.Item;
import com.example.sharely.service.BookingActivityService;

import java.util.ArrayList;
import java.util.List;


public class BookingListActivity extends AppCompatActivity {

    // =========================================================
    // VIEWS
    // =========================================================

    private LinearLayout bookingsContainer;


    // =========================================================
    // REPOSITORIES
    // =========================================================

    private BookingRepository bookingRepository;

    private UserRepository userRepository;

    private ItemRepository itemRepository;


    // =========================================================
    // SERVICE
    // =========================================================

    private BookingActivityService bookingActivityService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_booking_list
        );


        // =====================================================
        // VIEWS
        // =====================================================

        TextView title =
                findViewById(
                        R.id.textBookingListTitle
                );


        TextView count =
                findViewById(
                        R.id.textBookingCount
                );


        Button backButton =
                findViewById(
                        R.id.buttonBack
                );


        bookingsContainer =
                findViewById(
                        R.id.bookingsContainer
                );


        // =====================================================
        // INITIALISE REPOSITORIES
        // =====================================================

        bookingRepository =
                new BookingRepository();


        userRepository =
                new UserRepository();


        itemRepository =
                new ItemRepository();


        // =====================================================
        // INITIALISE SERVICE
        // =====================================================

        bookingActivityService =
                new BookingActivityService();


        // =====================================================
        // GET BOOKING TYPE FROM ACTIVITY FRAGMENT
        // =====================================================

        String type =
                getIntent().getStringExtra(
                        "type"
                );


        // If no type was passed,
        // show ongoing bookings by default.

        if (type == null) {

            type =
                    "ONGOING";
        }


        // =====================================================
        // SET PAGE TITLE
        // =====================================================

        switch (type) {

            case "UPCOMING":

                title.setText(
                        "Upcoming & pending"
                );

                break;


            case "HISTORY":

                title.setText(
                        "History"
                );

                break;


            case "ONGOING":

            default:

                title.setText(
                        "Ongoing"
                );

                break;
        }


        // =====================================================
        // BACK BUTTON
        // =====================================================

        backButton.setOnClickListener(
                v -> finish()
        );


        // =====================================================
        // GET CURRENT USER
        // =====================================================

        String currentUserId =
                userRepository
                        .getCurrentUserId();


        if (currentUserId == null) {

            Toast.makeText(
                    this,
                    "Please log in first",
                    Toast.LENGTH_SHORT
            ).show();


            return;
        }


        // =====================================================
        // LOAD BOOKINGS
        // =====================================================

        String finalType =
                type;


        bookingRepository.getBookingsByBorrower(

                currentUserId,

                new BookingRepository.OnBookingsLoadedListener() {

                    @Override
                    public void onSuccess(
                            List<Booking> bookings
                    ) {

                        // =====================================
                        // FILTER BOOKINGS
                        // =====================================

                        List<Booking> filteredBookings;


                        switch (finalType) {

                            // ---------------------------------
                            // UPCOMING
                            // PENDING + ACCEPTED
                            // ---------------------------------

                            case "UPCOMING":

                                filteredBookings =
                                        bookingActivityService
                                                .getUpcomingBookings(
                                                        bookings
                                                );

                                break;


                            // ---------------------------------
                            // HISTORY
                            // COMPLETED + CANCELLED + REJECTED
                            // ---------------------------------

                            case "HISTORY":

                                filteredBookings =
                                        bookingActivityService
                                                .getHistoryBookings(
                                                        bookings
                                                );

                                break;


                            // ---------------------------------
                            // ONGOING
                            // ACTIVE
                            // ---------------------------------

                            case "ONGOING":

                            default:

                                filteredBookings =
                                        bookingActivityService
                                                .getOngoingBookings(
                                                        bookings
                                                );

                                break;
                        }


                        // =====================================
                        // DISPLAY BOOKING COUNT
                        // =====================================

                        count.setText(

                                filteredBookings.size()

                                        + " booking"

                                        + (
                                        filteredBookings.size() == 1
                                                ? ""
                                                : "s"
                                )
                        );


                        // =====================================
                        // DISPLAY BOOKING CARDS
                        // =====================================

                        displayBookings(
                                filteredBookings
                        );
                    }


                    @Override
                    public void onFailure(
                            Exception e
                    ) {

                        Toast.makeText(
                                BookingListActivity.this,
                                "Failed to load bookings",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }


    // =========================================================
    // DISPLAY BOOKINGS
    // =========================================================

    private void displayBookings(
            List<Booking> bookings
    ) {

        // Remove old cards

        bookingsContainer
                .removeAllViews();


        // =====================================================
        // NO BOOKINGS
        // =====================================================

        if (bookings == null
                || bookings.isEmpty()) {


            TextView emptyText =
                    new TextView(this);


            emptyText.setText(
                    "No bookings found."
            );


            emptyText.setTextSize(
                    16
            );


            emptyText.setPadding(
                    0,
                    40,
                    0,
                    40
            );


            bookingsContainer.addView(
                    emptyText
            );


            return;
        }


        // =====================================================
        // CREATE CARD FOR EACH BOOKING
        // =====================================================

        for (Booking booking : bookings) {

            addBookingCard(
                    booking
            );
        }
    }


    // =========================================================
    // ADD BOOKING CARD
    // =========================================================

    private void addBookingCard(
            Booking booking
    ) {

        // =====================================================
        // CREATE CARD
        // =====================================================

        View card =
                getLayoutInflater().inflate(
                        R.layout.item_card,
                        bookingsContainer,
                        false
                );


        // =====================================================
        // CARD VIEWS
        // =====================================================

        ImageView itemImageView =
                card.findViewById(
                        R.id.imageItem
                );


        TextView itemNameText =
                card.findViewById(
                        R.id.textItemName
                );


        TextView itemInfoText =
                card.findViewById(
                        R.id.textItemInfo
                );


        TextView itemPriceText =
                card.findViewById(
                        R.id.textItemPrice
                );


        Button viewDetailsButton =
                card.findViewById(
                        R.id.buttonViewDetails
                );
        viewDetailsButton.setVisibility(
                View.GONE
        );

        // =====================================================
        // TEMPORARY CONTENT
        // =====================================================

        itemNameText.setText(
                "Loading..."
        );


        itemInfoText.setText(
                booking
                        .getStatus()
                        .name()
        );


        itemPriceText.setText(
                ""
        );


        // =====================================================
        // ADD CARD
        // =====================================================

        bookingsContainer.addView(
                card
        );


        // =====================================================
        // LOAD ITEM
        // =====================================================

        itemRepository.getItem(

                booking.getItemId(),

                new ItemRepository.OnItemLoadedListener() {

                    @Override
                    public void onSuccess(
                            Item item
                    ) {

                        // =====================================
                        // ITEM DATA
                        // =====================================

                        String name =
                                item.getItemName();


                        String location =
                                item.getLocation();


                        double price =
                                item.getPrice();


                        String description =
                                item.getDescription();


                        ArrayList<String> imageUrls =
                                item.getImageUrls();


                        // =====================================
                        // NAME
                        // =====================================

                        if (name != null
                                && !name.isEmpty()) {

                            itemNameText.setText(
                                    name
                            );

                        } else {

                            itemNameText.setText(
                                    "Unnamed item"
                            );
                        }


                        // =====================================
                        // BOOKING STATUS + LOCATION
                        // =====================================

                        String info =
                                booking
                                        .getStatus()
                                        .name();


                        if (location != null
                                && !location.isEmpty()) {

                            info =
                                    info
                                            + " • "
                                            + location;
                        }


                        itemInfoText.setText(
                                info
                        );


                        // =====================================
                        // PRICE
                        // =====================================

                        itemPriceText.setText(
                                "$"
                                        + price
                                        + " / day"
                        );


                        // =====================================
                        // IMAGE
                        // =====================================

                        if (imageUrls != null
                                && !imageUrls.isEmpty()) {

                            Glide.with(
                                            BookingListActivity.this
                                    )

                                    .load(
                                            imageUrls.get(0)
                                    )

                                    .fitCenter()

                                    .into(
                                            itemImageView
                                    );
                        }


                        // =====================================
                        // VIEW DETAILS
                        // =====================================

                    }


                    @Override
                    public void onFailure(
                            Exception e
                    ) {

                        itemNameText.setText(
                                "Item unavailable"
                        );


                        itemInfoText.setText(
                                booking
                                        .getStatus()
                                        .name()
                        );


                        itemPriceText.setText(
                                ""
                        );


                        viewDetailsButton.setEnabled(
                                false
                        );
                    }
                }
        );
    }
}