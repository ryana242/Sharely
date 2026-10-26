
package com.example.sharely;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.sharely.data.BookingRepository;
import com.example.sharely.data.ItemRepository;
import com.example.sharely.data.UserRepository;
import com.example.sharely.enums.ItemStatus;
import com.example.sharely.model.Booking;
import com.example.sharely.model.Item;
import com.example.sharely.service.BookingActivityService;

import java.util.List;

public class ActivityFragment extends Fragment {

    private TextView borrowingTab;
    private TextView listingsTab;

    private LinearLayout borrowingContent;
    private LinearLayout listingsContent;

    private TextView ongoingSummary;
    private TextView upcomingSummary;
    private TextView historySummary;
    private TextView activeSummary;
    private TextView pastSummary;

    private BookingRepository bookingRepository;
    private ItemRepository itemRepository;
    private UserRepository userRepository;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup parent,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_activity,
                parent,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle state
    ) {
        super.onViewCreated(view, state);

        borrowingTab = view.findViewById(R.id.tabBorrowing);
        listingsTab = view.findViewById(R.id.tabListings);

        borrowingContent =
                view.findViewById(R.id.borrowingsContent);

        listingsContent =
                view.findViewById(R.id.listingsContent);

        ongoingSummary =
                view.findViewById(R.id.textOngoingSummary);

        upcomingSummary =
                view.findViewById(R.id.textUpcomingSummary);

        historySummary =
                view.findViewById(R.id.textHistorySummary);

        activeSummary =
                view.findViewById(R.id.textActiveListingsSummary);

        pastSummary =
                view.findViewById(R.id.textPastListingsSummary);

        bookingRepository = new BookingRepository();
        itemRepository = new ItemRepository();
        userRepository = new UserRepository();

        // Create the owner-management entry point.

        Button manageRequests = new Button(requireContext());

        manageRequests.setText("Manage booking requests");
        manageRequests.setAllCaps(false);

        manageRequests.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        android.R.color.white
                )
        );

        manageRequests.setBackgroundResource(
                R.drawable.button_primary
        );

        manageRequests.setBackgroundTintList(null);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(54)
                );

        params.bottomMargin = dp(24);

        listingsContent.addView(
                manageRequests,
                0,
                params
        );

        manageRequests.setOnClickListener(
                v -> openBookings("OWNER")
        );

        // Existing borrower navigation.

        ongoingSummary.setOnClickListener(
                v -> openBookings("ONGOING")
        );

        upcomingSummary.setOnClickListener(
                v -> openBookings("UPCOMING")
        );

        historySummary.setOnClickListener(
                v -> openBookings("HISTORY")
        );

        // Existing listing navigation.

        activeSummary.setOnClickListener(
                v -> openListings("ACTIVE")
        );

        pastSummary.setOnClickListener(
                v -> openListings("PAST")
        );

        borrowingTab.setOnClickListener(
                v -> showTab(false)
        );

        listingsTab.setOnClickListener(
                v -> showTab(true)
        );

        showTab(false);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getView() != null) {
            refreshSummaries();
        }
    }

    private int dp(int value) {
        return Math.round(
                value * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private void openBookings(String type) {
        Intent intent = new Intent(
                requireContext(),
                BookingListActivity.class
        );

        intent.putExtra("type", type);
        startActivity(intent);
    }

    private void openListings(String type) {
        Intent intent = new Intent(
                requireContext(),
                ListingListActivity.class
        );

        intent.putExtra("type", type);
        startActivity(intent);
    }

    private void showTab(boolean listings) {

        borrowingContent.setVisibility(
                listings ? View.GONE : View.VISIBLE
        );

        listingsContent.setVisibility(
                listings ? View.VISIBLE : View.GONE
        );

        borrowingTab.setBackgroundResource(
                listings
                        ? R.drawable.button_secondary
                        : R.drawable.button_primary
        );

        listingsTab.setBackgroundResource(
                listings
                        ? R.drawable.button_primary
                        : R.drawable.button_secondary
        );

        borrowingTab.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        listings
                                ? R.color.sharely_primary
                                : android.R.color.white
                )
        );

        listingsTab.setTextColor(
                ContextCompat.getColor(
                        requireContext(),
                        listings
                                ? android.R.color.white
                                : R.color.sharely_primary
                )
        );
    }

    private void refreshSummaries() {

        String uid = userRepository.getCurrentUserId();

        if (uid == null) {
            ongoingSummary.setText(
                    "Sign in to view borrowings"
            );

            upcomingSummary.setText(
                    "Sign in to view bookings"
            );

            historySummary.setText(
                    "Sign in to view history"
            );

            activeSummary.setText(
                    "Sign in to view listings"
            );

            pastSummary.setText(
                    "Sign in to view listings"
            );

            return;
        }

        bookingRepository.getBookingsByBorrower(
                uid,
                new BookingRepository.OnBookingsLoadedListener() {

                    @Override
                    public void onSuccess(List<Booking> bookings) {

                        if (!isAdded() || getView() == null) {
                            return;
                        }

                        BookingActivityService service =
                                new BookingActivityService();

                        int ongoing =
                                service.getOngoingBookings(bookings).size();

                        int upcoming =
                                service.getUpcomingBookings(bookings).size();

                        int history =
                                service.getHistoryBookings(bookings).size();

                        ongoingSummary.setText(
                                ongoing
                                        + " ongoing borrowing"
                                        + (ongoing == 1 ? "" : "s")
                        );

                        upcomingSummary.setText(
                                upcoming + " upcoming or pending"
                        );

                        historySummary.setText(
                                history
                                        + " completed, cancelled or rejected booking"
                                        + (history == 1 ? "" : "s")
                        );
                    }

                    @Override
                    public void onFailure(Exception error) {

                        if (!isAdded() || getView() == null) {
                            return;
                        }

                        ongoingSummary.setText(
                                "Unable to load borrowings"
                        );

                        upcomingSummary.setText(
                                "Unable to load bookings"
                        );

                        historySummary.setText(
                                "Unable to load history"
                        );
                    }
                }
        );

        itemRepository.getItemsByOwner(
                uid,
                new ItemRepository.OnOwnerItemsLoadedListener() {

                    @Override
                    public void onSuccess(List<Item> items) {

                        if (!isAdded() || getView() == null) {
                            return;
                        }

                        int active = 0;
                        int other = 0;

                        for (Item item : items) {

                            if (item.getStatus() == ItemStatus.ACTIVE) {
                                active++;

                            } else if (
                                    item.getStatus() == ItemStatus.UNAVAILABLE
                                            || item.getStatus() == ItemStatus.PAST
                            ) {
                                other++;
                            }
                        }

                        activeSummary.setText(
                                active == 0
                                        ? "No active listings"
                                        : active
                                          + " active listing"
                                          + (active == 1 ? "" : "s")
                        );

                        pastSummary.setText(
                                other == 0
                                        ? "No unavailable or past listings"
                                        : other
                                          + " unavailable or past listing"
                                          + (other == 1 ? "" : "s")
                        );
                    }

                    @Override
                    public void onFailure(Exception error) {

                        if (!isAdded() || getView() == null) {
                            return;
                        }

                        activeSummary.setText(
                                "Unable to load listings"
                        );

                        pastSummary.setText(
                                "Unable to load listings"
                        );
                    }
                }
        );
    }
}
