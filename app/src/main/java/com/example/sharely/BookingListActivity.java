package com.example.sharely;
import android.content.res.ColorStateList;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.sharely.data.BookingRepository;
import com.example.sharely.data.ItemRepository;
import com.example.sharely.data.ReviewRepository;
import com.example.sharely.data.UserRepository;
import com.example.sharely.enums.BookingStatus;
import com.example.sharely.model.Booking;
import com.example.sharely.model.Item;
import com.example.sharely.service.BookingActivityService;
import com.example.sharely.service.ReviewService;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public class BookingListActivity extends AppCompatActivity {
    public static final String EXTRA_FOCUS_BOOKING_ID = "sharely_focus_booking_id";
    private LinearLayout container;
    private TextView titleView;
    private TextView countView;
    private final BookingActivityService activityService =
            new BookingActivityService();
    private BookingRepository bookingRepository;
    private ItemRepository itemRepository;
    private UserRepository userRepository;
    private ReviewRepository reviewRepository;
    private ReviewService reviewService;
    private String type;
    private int loadVersion;
    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_booking_list);
        container = findViewById(R.id.bookingsContainer);
        titleView = findViewById(R.id.textBookingListTitle);
        countView = findViewById(R.id.textBookingCount);
        bookingRepository = new BookingRepository();
        itemRepository = new ItemRepository();
        userRepository = new UserRepository();
        reviewRepository = new ReviewRepository();
        reviewService = new ReviewService();
        findViewById(R.id.buttonBack)
                .setOnClickListener(v -> finish());
        type = getIntent().getStringExtra("type");
        if (type == null) {
            type = "ONGOING";
        }
        switch (type) {
            case "OWNER":
                titleView.setText("Booking requests");
                break;
            case "UPCOMING":
                titleView.setText("Upcoming & pending");
                break;
            case "HISTORY":
                titleView.setText("History");
                break;
            default:
                titleView.setText("Ongoing");
                break;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadBookings();
    }
    private boolean isCurrent(int token) {
        return token == loadVersion
                && !isFinishing()
                && !isDestroyed();
    }
    private void loadBookings() {
        int token = ++loadVersion;
        container.removeAllViews();
        countView.setText("Loading...");
        String uid = userRepository.getCurrentUserId();
        if (uid == null) {
            showError("Please sign in to view bookings");
            return;
        }
        if ("OWNER".equals(type)) {
            loadOwnerBookings(uid, token);
            return;
        }
        bookingRepository.getBookingsByBorrower(
                uid,
                new BookingRepository.OnBookingsLoadedListener() {
                    @Override
                    public void onSuccess(List<Booking> all) {
                        if (!isCurrent(token)) return;
                        List<Booking> visible;
                        if ("UPCOMING".equals(type)) {
                            visible = activityService.getUpcomingBookings(all);
                        } else if ("HISTORY".equals(type)) {
                            visible = activityService.getHistoryBookings(all);
                        } else {
                            visible = activityService.getOngoingBookings(all);
                        }
                        renderList(visible, null, false);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        if (isCurrent(token)) {
                            showError(
                                    "Unable to load bookings: "
                                            + e.getMessage()
                            );
                        }
                    }
                }
        );
    }
    private void loadOwnerBookings(String uid, int token) {
        itemRepository.getItemsByOwner(
                uid,
                new ItemRepository.OnOwnerItemsLoadedListener() {
                    @Override
                    public void onSuccess(List<Item> items) {
                        if (!isCurrent(token)) return;
                        Map<String, Item> byId = new HashMap<>();
                        if (items != null) {
                            for (Item item : items) {
                                if (item != null
                                        && item.getItemId() != null) {
                                    byId.put(
                                            item.getItemId(),
                                            item
                                    );
                                }
                            }
                        }
                        if (byId.isEmpty()) {
                            renderList(
                                    Collections.emptyList(),
                                    byId,
                                    true
                            );
                            return;
                        }
                        List<Booking> all = new ArrayList<>();
                        int[] remaining = {byId.size()};
                        boolean[] failed = {false};
                        for (String id : byId.keySet()) {
                            bookingRepository.getBookingsByItem(
                                    id,
                                    new BookingRepository.OnBookingsLoadedListener() {
                                        @Override
                                        public void onSuccess(
                                                List<Booking> bookings
                                        ) {
                                            if (!isCurrent(token) || failed[0]) {
                                                return;
                                            }
                                            all.addAll(bookings);
                                            if (--remaining[0] == 0) {
                                                all.sort((a, b) -> {
                                                    boolean aPending =
                                                            a.getStatus()
                                                                    == BookingStatus.PENDING;
                                                    boolean bPending =
                                                            b.getStatus()
                                                                    == BookingStatus.PENDING;
                                                    if (aPending != bPending) {
                                                        return aPending ? -1 : 1;
                                                    }
                                                    return Long.compare(
                                                            b.getBookingSlot()
                                                                    .getStartTime(),
                                                            a.getBookingSlot()
                                                                    .getStartTime()
                                                    );
                                                });
                                                renderList(all, byId, true);
                                            }
                                        }
                                        @Override
                                        public void onFailure(Exception e) {
                                            if (!isCurrent(token) || failed[0]) {
                                                return;
                                            }
                                            failed[0] = true;
                                            showError(
                                                    "Could not load owner requests: "
                                                            + e.getMessage()
                                            );
                                        }
                                    }
                            );
                        }
                    }
                    @Override
                    public void onFailure(Exception e) {
                        if (isCurrent(token)) {
                            showError(
                                    "Could not load listings: "
                                            + e.getMessage()
                            );
                        }
                    }
                }
        );
    }
    private void renderList(
            List<Booking> bookings,
            Map<String, Item> knownItems,
            boolean ownerMode
    ) {
        container.removeAllViews();
        int pending = 0;
        for (Booking booking : bookings) {
            if (booking.getStatus() == BookingStatus.PENDING) {
                pending++;
            }
        }
        countView.setText(
                ownerMode
                        ? pending + " pending • "
                          + bookings.size() + " total requests"
                        : bookings.size()
                          + (bookings.size() == 1
                             ? " booking"
                             : " bookings")
        );
        if (bookings.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(
                    ownerMode
                            ? "No requests for your listings yet."
                            : "No bookings found."
            );
            empty.setTextColor(
                    getColor(R.color.sharely_text_secondary)
            );
            empty.setTextSize(16);
            empty.setPadding(
                    0,
                    dp(32),
                    0,
                    dp(32)
            );
            container.addView(empty);
            return;
        }
        String focusId = getIntent().getStringExtra(EXTRA_FOCUS_BOOKING_ID);
        boolean focusedFound = false;
        for (Booking booking : bookings) {
            Item item = knownItems == null
                    ? null
                    : knownItems.get(booking.getItemId());
            addCard(booking, item, ownerMode);
            if (focusId != null && focusId.equals(booking.getBookingId())) {
                focusedFound = true;
                View focus = container.getChildAt(container.getChildCount() - 1);
                focus.setBackgroundColor(0xFFEAF7EE);
                focus.post(() -> {
                    android.view.ViewParent parent = container.getParent();
                    while (parent != null && !(parent instanceof android.widget.ScrollView)) {
                        parent = parent.getParent();
                    }
                    if (parent instanceof android.widget.ScrollView) {
                        ((android.widget.ScrollView) parent).smoothScrollTo(0, container.getTop() + focus.getTop());
                    }
                });
            }
        }
        if (focusId != null && !focusedFound) {
            TextView hint = new TextView(this);
            hint.setText("This booking is no longer in this section. Check your booking history or requests.");
            hint.setTextSize(14);
            hint.setTextColor(getColor(R.color.sharely_text_secondary));
            container.addView(hint, 0);
        }
    }
    private String date(long millis) {
        return new SimpleDateFormat(
                "dd MMM yyyy, h:mm a",
                Locale.getDefault()
        ).format(new Date(millis));
    }
    private void addCard(
            Booking booking,
            Item item,
            boolean ownerMode
    ) {
        View card = getLayoutInflater().inflate(
                R.layout.item_card,
                container,
                false
        );
        ImageView image =
                card.findViewById(R.id.imageItem);
        TextView name =
                card.findViewById(R.id.textItemName);
        TextView info =
                card.findViewById(R.id.textItemInfo);
        TextView status =
                card.findViewById(R.id.textItemPrice);
        Button details =
                card.findViewById(R.id.buttonViewDetails);
        details.setVisibility(View.GONE);
        status.setText(booking.getStatus().name());
        name.setText(
                item == null
                        ? "Loading item..."
                        : item.getItemName()
        );
        String dates =
                "Start: "
                        + date(booking.getBookingSlot().getStartTime())
                        + "\nReturn: "
                        + date(booking.getBookingSlot().getEndTime());
        if (ownerMode) {
            info.setText(
                    "Borrower: Loading name...\n" + dates
            );
            int token = loadVersion;
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(booking.getBorrowerId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!isCurrent(token)) return;
                        String person = doc.getString("name");
                        if (person == null
                                || person.trim().isEmpty()) {
                            person = "Sharely user";
                        }
                        info.setText(
                                "Borrower: "
                                        + person.trim()
                                        + "\n"
                                        + dates
                        );
                    })
                    .addOnFailureListener(error -> {
                        if (isCurrent(token)) {
                            info.setText(
                                    "Borrower: Sharely user\n" + dates
                            );
                        }
                    });
        } else {
            info.setText(dates);
        }
        container.addView(card);
        if (item != null) {
            showPhoto(image, item);
        } else {
            int token = loadVersion;
            itemRepository.getItem(
                    booking.getItemId(),
                    new ItemRepository.OnItemLoadedListener() {
                        @Override
                        public void onSuccess(Item loaded) {
                            if (isCurrent(token)
                                    && loaded != null) {
                                name.setText(loaded.getItemName());
                                showPhoto(image, loaded);
                            }
                        }
                        @Override
                        public void onFailure(Exception e) {
                            if (isCurrent(token)) {
                                name.setText("Item unavailable");
                            }
                        }
                    }
            );
        }
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        BookingStatus current = booking.getStatus();
        long now = System.currentTimeMillis();
        if (ownerMode && current == BookingStatus.PENDING) {
            addAction(
                    actions,
                    "Accept",
                    true,
                    () -> confirmAction(
                            booking,
                            "Accept booking?",
                            "Confirm these borrowing dates?",
                            "Accept",
                            actions,
                            cb -> bookingRepository.decideBooking(
                                    booking.getBookingId(),
                                    true,
                                    cb
                            )
                    )
            );
            addAction(
                    actions,
                    "Reject",
                    false,
                    () -> confirmAction(
                            booking,
                            "Reject booking?",
                            "Reject this borrower's request?",
                            "Reject",
                            actions,
                            cb -> bookingRepository.decideBooking(
                                    booking.getBookingId(),
                                    false,
                                    cb
                            )
                    )
            );
        } else if (ownerMode
                && current == BookingStatus.ACCEPTED
                && now >= booking.getBookingSlot().getStartTime()
                && now < booking.getBookingSlot().getEndTime()) {
            addAction(
                    actions,
                    "Mark as started",
                    true,
                    () -> confirmAction(
                            booking,
                            "Start borrowing?",
                            "Confirm the borrower has collected this item.",
                            "Start",
                            actions,
                            cb -> bookingRepository.advanceBooking(
                                    booking.getBookingId(),
                                    cb
                            )
                    )
            );
        } else if (ownerMode
                && current == BookingStatus.ACTIVE) {
            addAction(
                    actions,
                    "Mark as returned",
                    true,
                    () -> confirmAction(
                            booking,
                            "Complete booking?",
                            "Confirm the item has been returned.",
                            "Complete",
                            actions,
                            cb -> bookingRepository.advanceBooking(
                                    booking.getBookingId(),
                                    cb
                            )
                    )
            );
        } else if (!ownerMode
                && (current == BookingStatus.PENDING
                || (current == BookingStatus.ACCEPTED
                && now < booking.getBookingSlot().getStartTime()))) {
            addAction(
                    actions,
                    "Cancel request",
                    false,
                    () -> confirmAction(
                            booking,
                            "Cancel booking?",
                            "Cancel this request? Accepted reservations will be released.",
                            "Cancel booking",
                            actions,
                            cb -> bookingRepository.cancelBooking(
                                    booking.getBookingId(),
                                    cb
                            )
                    )
            );
        }
        if (actions.getChildCount() > 0) {
            ((LinearLayout) card).addView(actions);
        }
        if (current == BookingStatus.COMPLETED) {
            addReviewSection((LinearLayout) card, booking, ownerMode);
        }
    }
    private void addReviewSection(
            LinearLayout card,
            Booking booking,
            boolean ownerMode
    ) {
        final String uid = userRepository.getCurrentUserId();
        if (uid == null || uid.trim().isEmpty()) return;
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(0, dp(12), 0, 0);
        card.addView(section, new LinearLayout.LayoutParams(-1, -2));
        TextView reviewState = new TextView(this);
        reviewState.setText("Checking your review...");
        reviewState.setTextColor(getColor(R.color.sharely_text_secondary));
        reviewState.setTextSize(14);
        section.addView(reviewState);
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        section.addView(buttons);
        String person = ownerMode ? "borrower" : "owner";
        addAction(
                buttons,
                "Rate " + person,
                true,
                () -> showReviewDialog(booking, ownerMode)
        );
        buttons.setVisibility(View.GONE);
        int token = loadVersion;
        reviewRepository.hasReviewed(
                booking.getBookingId(),
                uid,
                new ReviewRepository.OnReviewCheckListener() {
                    @Override
                    public void onResult(boolean hasReviewed) {
                        if (!isCurrent(token)) return;
                        if (hasReviewed) {
                            reviewState.setText("✓ Your review has been submitted");
                            buttons.setVisibility(View.GONE);
                        } else {
                            reviewState.setText(
                                    "How was your experience with this "
                                            + person + "?"
                            );
                            buttons.setVisibility(View.VISIBLE);
                        }
                    }
                    @Override
                    public void onFailure(Exception e) {
                        if (!isCurrent(token)) return;
                        reviewState.setText(
                                "Unable to check review status. "
                                        + "Reopen this screen to retry."
                        );
                        buttons.setVisibility(View.GONE);
                    }
                }
        );
    }
    private void showReviewDialog(Booking booking, boolean ownerMode) {
        if (isFinishing() || isDestroyed()) return;
        String person = ownerMode ? "borrower" : "owner";
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(22), dp(16), dp(22), dp(8));
        TextView prompt = new TextView(this);
        prompt.setText("Rate the " + person + " from 1 to 5 stars");
        prompt.setTextColor(getColor(R.color.sharely_text_primary));
        prompt.setTextSize(15);
        form.addView(prompt);
        RatingBar stars = new RatingBar(this);
        stars.setNumStars(5);
        stars.setStepSize(1.0f);
        stars.setRating(0);
        LinearLayout.LayoutParams starParams =
                new LinearLayout.LayoutParams(-2, -2);
        starParams.topMargin = dp(8);
        form.addView(stars, starParams);
        TextView commentLabel = new TextView(this);
        commentLabel.setText("Your experience (optional)");
        commentLabel.setTextSize(14);
        commentLabel.setTextColor(getColor(R.color.sharely_text_primary));
        LinearLayout.LayoutParams labelParams =
                new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = dp(14);
        form.addView(commentLabel, labelParams);
        EditText comment = new EditText(this);
        comment.setHint("What went well? Anything to improve?");
        comment.setTextSize(14);
        comment.setTextColor(getColor(R.color.sharely_text_primary));
        comment.setHintTextColor(getColor(R.color.sharely_text_secondary));
        comment.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );
        comment.setMinLines(3);
        comment.setMaxLines(5);
        comment.setGravity(Gravity.TOP | Gravity.START);
        comment.setFilters(
                new InputFilter[]{new InputFilter.LengthFilter(1000)}
        );
        form.addView(comment, new LinearLayout.LayoutParams(-1, -2));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Review completed booking")
                .setView(scroll)
                .setNegativeButton("Not now", null)
                .setPositiveButton("Submit review", null)
                .create();
        dialog.setOnShowListener(unused -> {
            Button submit = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            submit.setOnClickListener(view -> {
                int rating = Math.round(stars.getRating());
                if (rating < 1 || rating > 5) {
                    Toast.makeText(
                            this,
                            "Choose a star rating first",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }
                submit.setEnabled(false);
                reviewService.submitReview(
                        booking.getBookingId(),
                        rating,
                        comment.getText().toString().trim(),
                        new ReviewRepository.OnReviewSavedListener() {
                            @Override
                            public void onSuccess() {
                                if (isFinishing() || isDestroyed()) return;
                                dialog.dismiss();
                                Toast.makeText(
                                        BookingListActivity.this,
                                        "Review submitted successfully",
                                        Toast.LENGTH_SHORT
                                ).show();
                                loadBookings();
                            }
                            @Override
                            public void onFailure(Exception e) {
                                if (isFinishing() || isDestroyed()) return;
                                submit.setEnabled(true);
                                Toast.makeText(
                                        BookingListActivity.this,
                                        "Review not saved: " + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
            });
        });
        dialog.show();
    }
    private void showPhoto(ImageView image, Item item) {
        if (item.getImageUrls() != null
                && !item.getImageUrls().isEmpty()) {
            Glide.with(this)
                    .load(item.getImageUrls().get(0))
                    .fitCenter()
                    .into(image);
        }
    }
    private void addAction(
            LinearLayout actions,
            String text,
            boolean primary,
            Runnable handler
    ) {
        MaterialButton button = new MaterialButton(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setCornerRadius(dp(12));
        int green = getColor(R.color.sharely_primary);
        button.setBackgroundTintList(
                ColorStateList.valueOf(
                        primary ? green : 0xFFFFFFFF
                )
        );
        button.setTextColor(
                primary ? 0xFFFFFFFF : green
        );
        button.setStrokeColor(
                ColorStateList.valueOf(green)
        );
        button.setStrokeWidth(
                primary ? 0 : dp(1)
        );
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        dp(54),
                        1f
                );
        params.setMargins(
                dp(4),
                dp(12),
                dp(4),
                0
        );
        actions.addView(button, params);
        button.setOnClickListener(
                v -> handler.run()
        );
    }
    private interface StatusAction {
        void run(
                BookingRepository.OnBookingStatusUpdatedListener callback
        );
    }
    private void confirmAction(
            Booking booking,
            String title,
            String message,
            String label,
            LinearLayout actions,
            StatusAction operation
    ) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Back", null)
                .setPositiveButton(
                        label,
                        (dialog, which) -> {
                            for (int i = 0;
                                 i < actions.getChildCount();
                                 i++) {
                                actions.getChildAt(i).setEnabled(false);
                            }
                            operation.run(
                                    new BookingRepository.OnBookingStatusUpdatedListener() {
                                        @Override
                                        public void onSuccess() {
                                            if (isFinishing()
                                                    || isDestroyed()) {
                                                return;
                                            }
                                            Toast.makeText(
                                                    BookingListActivity.this,
                                                    "Booking updated",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                            loadBookings();
                                        }
                                        @Override
                                        public void onFailure(Exception e) {
                                            if (isFinishing()
                                                    || isDestroyed()) {
                                                return;
                                            }
                                            for (int i = 0;
                                                 i < actions.getChildCount();
                                                 i++) {
                                                actions.getChildAt(i)
                                                        .setEnabled(true);
                                            }
                                            Toast.makeText(
                                                    BookingListActivity.this,
                                                    "Action failed: "
                                                            + e.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        }
                                    }
                            );
                        }
                )
                .show();
    }
    private void showError(String message) {
        container.removeAllViews();
        countView.setText("Unable to load");
        TextView error = new TextView(this);
        error.setText(message);
        error.setTextColor(
                getColor(R.color.sharely_text_primary)
        );
        error.setPadding(
                0,
                dp(24),
                0,
                dp(24)
        );
        container.addView(error);
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
