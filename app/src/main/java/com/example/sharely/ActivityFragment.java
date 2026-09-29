package com.example.sharely;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.sharely.model.AvailabilitySlot;
import com.example.sharely.model.Booking;
import com.example.sharely.service.BookingActivityService;

import java.util.ArrayList;
import java.util.List;

public class ActivityFragment extends Fragment {

    private BookingActivityService bookingActivityService;

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
        // SERVICE
        // -----------------------------------------------------

        bookingActivityService =
                new BookingActivityService();


        // -----------------------------------------------------
        // TEMPORARY BOOKINGS
        // Later Leo will replace these with Firestore bookings.
        // -----------------------------------------------------

        List<Booking> bookings =
                createDemoBookings();


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


        // -----------------------------------------------------
        // DISPLAY COUNTS
        // -----------------------------------------------------

        ongoingSummary.setText(
                ongoingBookings.size()
                        + " ongoing borrowing"
                        + (ongoingBookings.size() == 1 ? "" : "s")
        );

        ongoingDetail.setText(
                "Active bookings currently in progress."
        );


        upcomingSummary.setText(
                upcomingBookings.size()
                        + " upcoming or pending"
        );

        upcomingDetail.setText(
                "Pending requests and accepted future bookings."
        );


        historySummary.setText(
                historyBookings.size()
                        + " completed, cancelled or rejected booking"
                        + (historyBookings.size() == 1 ? "" : "s")
        );


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
    }


    // =========================================================
    // TEMPORARY DATA
    // =========================================================

    private List<Booking> createDemoBookings() {

        List<Booking> bookings =
                new ArrayList<>();


        // ACTIVE BOOKING
        Booking activeBooking =
                new Booking(
                        "B001",
                        "USER1",
                        "ITEM1",
                        new AvailabilitySlot(
                                1000,
                                5000
                        )
                );

        activeBooking.acceptBooking();
        activeBooking.startBooking();

        bookings.add(
                activeBooking
        );


        // PENDING BOOKING
        Booking pendingBooking =
                new Booking(
                        "B002",
                        "USER1",
                        "ITEM2",
                        new AvailabilitySlot(
                                6000,
                                9000
                        )
                );

        bookings.add(
                pendingBooking
        );


        // COMPLETED BOOKING
        Booking completedBooking =
                new Booking(
                        "B003",
                        "USER1",
                        "ITEM3",
                        new AvailabilitySlot(
                                10000,
                                15000
                        )
                );

        completedBooking.acceptBooking();
        completedBooking.startBooking();
        completedBooking.completeBooking();

        bookings.add(
                completedBooking
        );


        return bookings;
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