package com.example.sharely;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.sharely.data.ItemRepository;
import com.example.sharely.model.Booking;
import com.example.sharely.service.BookingActivityService;
import com.example.sharely.enums.ItemStatus;
import com.example.sharely.model.Item;


import com.example.sharely.data.BookingRepository;
import com.example.sharely.data.UserRepository;
import com.example.sharely.data.ItemRepository;

import java.util.List;

public class ActivityFragment extends Fragment {

    private BookingActivityService bookingActivityService;
    private BookingRepository bookingRepository;
    private UserRepository userRepository;
    private ItemRepository itemRepository;

    public ActivityFragment() {
    }


    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        return inflater.inflate(
                R.layout.fragment_activity,
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
        // TAB VIEWS
        // -----------------------------------------------------

        TextView borrowingTab =
                view.findViewById(
                        R.id.tabBorrowing
                );

        TextView listingsTab =
                view.findViewById(
                        R.id.tabListings
                );

        LinearLayout borrowingsContent =
                view.findViewById(
                        R.id.borrowingsContent
                );

        LinearLayout listingsContent =
                view.findViewById(
                        R.id.listingsContent
                );


        // -----------------------------------------------------
        // BORROWING SUMMARY VIEWS
        // -----------------------------------------------------

        TextView ongoingSummary =
                view.findViewById(
                        R.id.textOngoingSummary
                );

        TextView ongoingDetail =
                view.findViewById(
                        R.id.textOngoingDetail
                );

        TextView upcomingSummary =
                view.findViewById(
                        R.id.textUpcomingSummary
                );

        TextView upcomingDetail =
                view.findViewById(
                        R.id.textUpcomingDetail
                );

        TextView historySummary =
                view.findViewById(
                        R.id.textHistorySummary
                );
        // -----------------------------------------------------
        // LISTING SUMMARY VIEWS
        // -----------------------------------------------------

        TextView activeListingsSummary =
                view.findViewById(
                        R.id.textActiveListingsSummary
                );
        TextView pastListingsSummary =
                view.findViewById(
                        R.id.textPastListingsSummary
                );

        // -----------------------------------------------------
        // SERVICE
        // -----------------------------------------------------

        bookingActivityService = new BookingActivityService();

        bookingRepository = new BookingRepository();

        userRepository = new UserRepository();

        itemRepository = new ItemRepository();

        // -----------------------------------------------------
        // GET CURRENT USER ID
        // -----------------------------------------------------

        String currentUserId =
                userRepository.getCurrentUserId();


        if (currentUserId == null) {

            ongoingSummary.setText(
                    "0 ongoing borrowings"
            );

            upcomingSummary.setText(
                    "0 upcoming or pending"
            );

            historySummary.setText(
                    "0 completed, cancelled or rejected bookings"
            );
            activeListingsSummary.setText(
                    "No active listings"
            );
            pastListingsSummary.setText(
                    "No unavailable or past listings"
            );

        } else {


            // -------------------------------------------------
            // LOAD CURRENT USER BOOKINGS
            // -------------------------------------------------

            bookingRepository.getBookingsByBorrower(
                    currentUserId,
                    new BookingRepository.OnBookingsLoadedListener() {

                        @Override
                        public void onSuccess(
                                List<Booking> bookings
                        ) {


                            // -----------------------------------------
                            // CLASSIFY BOOKINGS
                            // -----------------------------------------

                            List<Booking> ongoingBookings =
                                    bookingActivityService
                                            .getOngoingBookings(
                                                    bookings
                                            );


                            List<Booking> upcomingBookings =
                                    bookingActivityService
                                            .getUpcomingBookings(
                                                    bookings
                                            );


                            List<Booking> historyBookings =
                                    bookingActivityService
                                            .getHistoryBookings(
                                                    bookings
                                            );


                            // -----------------------------------------
                            // DISPLAY COUNTS
                            // -----------------------------------------

                            ongoingSummary.setText(
                                    ongoingBookings.size()
                                            + " ongoing borrowing"
                                            + (
                                            ongoingBookings.size() == 1
                                                    ? ""
                                                    : "s"
                                    )
                            );


                            upcomingSummary.setText(
                                    upcomingBookings.size()
                                            + " upcoming or pending"
                            );


                            historySummary.setText(
                                    historyBookings.size()
                                            + " completed, cancelled or rejected booking"
                                            + (
                                            historyBookings.size() == 1
                                                    ? ""
                                                    : "s"
                                    )
                            );
                        }


                        @Override
                        public void onFailure(Exception e) {

                            ongoingSummary.setText(
                                    "Failed to load bookings"
                            );

                            upcomingSummary.setText(
                                    "Failed to load bookings"
                            );

                            historySummary.setText(
                                    "Failed to load bookings"
                            );
                        }
                    }
            );

            // -------------------------------------------------
            // LOAD CURRENT USER LISTINGS
            // -------------------------------------------------

            itemRepository.getItemsByOwner(
                    currentUserId,
                    new ItemRepository.OnOwnerItemsLoadedListener() {

                        @Override
                        public void onSuccess(
                                List<Item> items
                        ) {
                            int activeCount = 0;
                            int unavailableAndPastCount = 0;

                            // -----------------------------------------
                            // CLASSIFY ITEMS
                            // -----------------------------------------

                            for (Item item : items) {
                                if (
                                        item.getStatus()
                                                == ItemStatus.ACTIVE
                                ) {
                                    activeCount++;
                                } else if (
                                        item.getStatus()
                                                == ItemStatus.UNAVAILABLE
                                                ||
                                                item.getStatus()
                                                        == ItemStatus.PAST
                                ) {

                                    unavailableAndPastCount++;
                                }
                            }

                            // -----------------------------------------
                            // DISPLAY ACTIVE LISTINGS
                            // -----------------------------------------

                            if (activeCount == 0) {

                                activeListingsSummary.setText(
                                        "No active listings"
                                );
                            } else {
                                activeListingsSummary.setText(
                                        activeCount
                                                + " active listing"
                                                + (
                                                activeCount == 1
                                                        ? ""
                                                        : "s"
                                        )
                                );
                            }

                            // -----------------------------------------
                            // DISPLAY UNAVAILABLE / PAST LISTINGS
                            // -----------------------------------------

                            if (unavailableAndPastCount == 0) {
                                pastListingsSummary.setText(
                                        "No unavailable or past listings"
                                );
                            } else {
                                pastListingsSummary.setText(
                                        unavailableAndPastCount
                                                + " unavailable or past listing"
                                                + (
                                                unavailableAndPastCount == 1
                                                        ? ""
                                                        : "s"
                                        )
                                );
                            }
                        }

                        @Override
                        public void onFailure(
                                Exception e
                        ) {
                            activeListingsSummary.setText(
                                    "Failed to load listings"
                            );
                            pastListingsSummary.setText(
                                    "Failed to load listings"
                            );
                        }
                    }
            );
        }


        // -----------------------------------------------------
        // DEFAULT TAB
        // -----------------------------------------------------

        showBorrowing(
                borrowingTab,
                listingsTab,
                borrowingsContent,
                listingsContent
        );


        // -----------------------------------------------------
        // TAB LISTENERS
        // -----------------------------------------------------

        borrowingTab.setOnClickListener(v -> {

            showBorrowing(
                    borrowingTab,
                    listingsTab,
                    borrowingsContent,
                    listingsContent
            );
        });


        listingsTab.setOnClickListener(v -> {

            showListings(
                    borrowingTab,
                    listingsTab,
                    borrowingsContent,
                    listingsContent
            );
        });
        // -----------------------------------------------------
        // BOOKING LIST LISTENERS
        // -----------------------------------------------------

        ongoingSummary.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            requireContext(),
                            BookingListActivity.class
                    );

            intent.putExtra(
                    "type",
                    "ONGOING"
            );

            startActivity(
                    intent
            );
        });

        upcomingSummary.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            requireContext(),
                            BookingListActivity.class
                    );

            intent.putExtra(
                    "type",
                    "UPCOMING"
            );

            startActivity(
                    intent
            );
        });

        historySummary.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            requireContext(),
                            BookingListActivity.class
                    );

            intent.putExtra(
                    "type",
                    "HISTORY"
            );

            startActivity(
                    intent
            );
        });

        activeListingsSummary.setOnClickListener(v -> {

            Intent intent =

                    new Intent(

                            requireContext(),

                            ListingListActivity.class

                    );

            intent.putExtra(

                    "type",

                    "ACTIVE"

            );

            startActivity(

                    intent

            );

        });

        pastListingsSummary.setOnClickListener(v -> {

            Intent intent =

                    new Intent(

                            requireContext(),

                            ListingListActivity.class

                    );

            intent.putExtra(

                    "type",

                    "PAST"

            );

            startActivity(

                    intent

            );

        });
    }


    // =========================================================
    // SHOW BORROWING
    // =========================================================

    private void showBorrowing(
            TextView borrowingTab,
            TextView listingsTab,
            LinearLayout borrowingsContent,
            LinearLayout listingsContent
    ) {

        borrowingsContent.setVisibility(
                View.VISIBLE
        );

        listingsContent.setVisibility(
                View.GONE
        );


        borrowingTab.setBackgroundResource(
                R.drawable.button_primary
        );

        borrowingTab.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        android.R.color.white
                )
        );


        listingsTab.setBackgroundResource(
                R.drawable.button_secondary
        );

        listingsTab.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        R.color.sharely_primary
                )
        );
    }


    // =========================================================
    // SHOW LISTINGS
    // =========================================================

    private void showListings(
            TextView borrowingTab,
            TextView listingsTab,
            LinearLayout borrowingsContent,
            LinearLayout listingsContent
    ) {

        borrowingsContent.setVisibility(
                View.GONE
        );

        listingsContent.setVisibility(
                View.VISIBLE
        );


        listingsTab.setBackgroundResource(
                R.drawable.button_primary
        );

        listingsTab.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        android.R.color.white
                )
        );


        borrowingTab.setBackgroundResource(
                R.drawable.button_secondary
        );

        borrowingTab.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        R.color.sharely_primary
                )
        );
    }
}