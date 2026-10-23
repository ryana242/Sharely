
package com.example.sharely;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.sharely.data.BookingRepository;
import com.example.sharely.data.ItemRepository;
import com.example.sharely.data.UserRepository;
import com.example.sharely.enums.ItemStatus;
import com.example.sharely.model.AvailabilitySlot;
import com.example.sharely.model.Booking;
import com.example.sharely.model.Item;
import com.example.sharely.service.BookingService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ItemDetailsActivity extends AppCompatActivity {

    private final Calendar startCalendar =
            Calendar.getInstance();

    private final Calendar endCalendar =
            Calendar.getInstance();

    private boolean startSelected;
    private boolean endSelected;
    private boolean itemLoaded;
    private boolean requestInProgress;

    private String itemId;
    private Item currentItem;

    private TextView itemNameText;
    private TextView itemPriceText;
    private TextView itemInfoText;
    private TextView itemDescriptionText;
    private TextView ownerAvailabilityText;
    private TextView startTimeText;
    private TextView endTimeText;

    private LinearLayout photoContainer;
    private Button requestBookingButton;

    private ItemRepository itemRepository;
    private BookingRepository bookingRepository;
    private UserRepository userRepository;

    private final BookingService bookingService =
            new BookingService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_item_details);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.itemDetailsMain),
                (view, insets) -> {

                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            bars.left,
                            bars.top,
                            bars.right,
                            bars.bottom
                    );

                    return insets;
                }
        );

        itemRepository = new ItemRepository();
        bookingRepository = new BookingRepository();
        userRepository = new UserRepository();

        itemNameText = findViewById(R.id.textItemName);
        itemPriceText = findViewById(R.id.textItemPrice);
        itemInfoText = findViewById(R.id.textItemInfo);
        itemDescriptionText =
                findViewById(R.id.textItemDescription);

        ownerAvailabilityText =
                findViewById(R.id.textOwnerAvailability);

        startTimeText = findViewById(R.id.textStartTime);
        endTimeText = findViewById(R.id.textEndTime);

        photoContainer = findViewById(R.id.photoContainer);

        requestBookingButton =
                findViewById(R.id.buttonRequestBooking);

        itemId = getIntent().getStringExtra("itemId");

        findViewById(R.id.buttonBack).setOnClickListener(
                v -> finish()
        );

        findViewById(R.id.buttonSelectStart)
                .setOnClickListener(
                        v -> chooseDateTime(startCalendar, true)
                );

        findViewById(R.id.buttonSelectEnd)
                .setOnClickListener(
                        v -> chooseDateTime(endCalendar, false)
                );

        requestBookingButton.setOnClickListener(
                v -> submitBookingRequest()
        );

        requestBookingButton.setEnabled(false);

        ownerAvailabilityText.setText(
                "Loading owner's availability..."
        );

        itemNameText.setText("Loading item...");

        if (itemId == null || itemId.trim().isEmpty()) {

            ownerAvailabilityText.setText(
                    "This listing cannot be opened."
            );

            itemNameText.setText("Item unavailable");
            return;
        }

        loadItem();
    }

    // =====================================================
    // LOAD ITEM FROM FIRESTORE
    // =====================================================

    private void loadItem() {

        itemLoaded = false;
        updateRequestButton();

        itemRepository.getItem(
                itemId,
                new ItemRepository.OnItemLoadedListener() {

                    @Override
                    public void onSuccess(Item item) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (item == null) {
                            showItemLoadError("Item not found");
                            return;
                        }

                        currentItem = item;
                        itemLoaded = true;

                        renderItem(item);
                        updateRequestButton();
                    }

                    @Override
                    public void onFailure(Exception e) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        showItemLoadError(
                                "Unable to load item. Please try again."
                        );
                    }
                }
        );
    }

    private void showItemLoadError(String message) {

        currentItem = null;
        itemLoaded = false;

        itemNameText.setText("Item unavailable");
        ownerAvailabilityText.setText(message);

        updateRequestButton();
    }

    // =====================================================
    // DISPLAY ITEM AND OWNER AVAILABILITY
    // =====================================================

    private void renderItem(Item item) {

        itemNameText.setText(
                nonEmpty(item.getItemName(), "Unnamed item")
        );

        itemPriceText.setText(
                String.format(
                        Locale.getDefault(),
                        "$%.2f / day",
                        item.getPrice()
                )
        );

        itemInfoText.setText(
                nonEmpty(
                        item.getLocation(),
                        "Location not specified"
                )
        );

        itemDescriptionText.setText(
                nonEmpty(
                        item.getDescription(),
                        "No description"
                )
        );

        AvailabilitySlot available =
                item.getAvailabilitySlot();

        if (item.getStatus() != ItemStatus.ACTIVE) {

            ownerAvailabilityText.setText(
                    "This listing is currently unavailable."
            );

        } else if (available == null || !available.isValid()) {

            ownerAvailabilityText.setText(
                    "Owner availability has not been set. "
                            + "Booking is disabled."
            );

        } else if (available.getEndTime()
                <= System.currentTimeMillis()) {

            ownerAvailabilityText.setText(
                    "This item's availability has expired."
            );

        } else {

            ownerAvailabilityText.setText(
                    "Owner available: "
                            + formatDate(available.getStartTime())
                            + "  –  "
                            + formatDate(available.getEndTime())
            );
        }

        photoContainer.removeAllViews();

        ArrayList<String> imageUrls =
                item.getImageUrls();

        if (imageUrls != null) {

            for (String url : imageUrls) {

                if (url == null || url.trim().isEmpty()) {
                    continue;
                }

                ImageView photo = new ImageView(this);

                LinearLayout.LayoutParams params =
                        new LinearLayout.LayoutParams(
                                dpToPx(320),
                                LinearLayout.LayoutParams.MATCH_PARENT
                        );

                params.setMargins(
                        0, 0, dpToPx(12), 0
                );

                photo.setLayoutParams(params);

                photo.setScaleType(
                        ImageView.ScaleType.FIT_CENTER
                );

                photoContainer.addView(photo);

                Glide.with(this)
                        .load(url)
                        .fitCenter()
                        .into(photo);
            }
        }
    }

    private String nonEmpty(
            String value,
            String fallback
    ) {

        return value == null || value.trim().isEmpty()
                ? fallback
                : value;
    }

    private String formatDate(long time) {

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "dd MMM yyyy, h:mm a",
                        Locale.getDefault()
                );

        return formatter.format(new Date(time));
    }

    // =====================================================
    // REQUEST BUTTON STATE
    // =====================================================

    private void updateRequestButton() {

        String currentUser =
                userRepository.getCurrentUserId();

        boolean owner =
                currentItem != null
                        && currentUser != null
                        && currentUser.equals(
                        currentItem.getOwnerId()
                );

        AvailabilitySlot available =
                currentItem == null
                        ? null
                        : currentItem.getAvailabilitySlot();

        boolean canBook =
                itemLoaded
                        && currentItem != null
                        && currentItem.getStatus()
                        == ItemStatus.ACTIVE
                        && available != null
                        && available.isValid()
                        && available.getEndTime()
                        > System.currentTimeMillis()
                        && !owner;

        requestBookingButton.setEnabled(
                canBook && !requestInProgress
        );

        if (requestInProgress) {
            requestBookingButton.setText(
                    "Checking request..."
            );
        } else if (owner) {
            requestBookingButton.setText(
                    "You own this item"
            );
        } else {
            requestBookingButton.setText(
                    "Request booking"
            );
        }
    }

    // =====================================================
    // DATE AND TIME PICKERS
    // =====================================================

    private void chooseDateTime(
            Calendar target,
            boolean isStart
    ) {

        DatePickerDialog datePicker =
                new DatePickerDialog(
                        this,
                        (dateView, year, month, dayOfMonth) -> {

                            Calendar choice =
                                    (Calendar) target.clone();

                            choice.set(
                                    year,
                                    month,
                                    dayOfMonth
                            );

                            TimePickerDialog timePicker =
                                    new TimePickerDialog(
                                            this,
                                            (timeView, hour, minute) -> {

                                                choice.set(
                                                        Calendar.HOUR_OF_DAY,
                                                        hour
                                                );

                                                choice.set(
                                                        Calendar.MINUTE,
                                                        minute
                                                );

                                                choice.set(
                                                        Calendar.SECOND,
                                                        0
                                                );

                                                choice.set(
                                                        Calendar.MILLISECOND,
                                                        0
                                                );

                                                target.setTimeInMillis(
                                                        choice.getTimeInMillis()
                                                );

                                                if (isStart) {

                                                    startSelected = true;

                                                    startTimeText.setText(
                                                            "Start: "
                                                                    + formatDate(
                                                                    target.getTimeInMillis()
                                                            )
                                                    );

                                                } else {

                                                    endSelected = true;

                                                    endTimeText.setText(
                                                            "End: "
                                                                    + formatDate(
                                                                    target.getTimeInMillis()
                                                            )
                                                    );
                                                }
                                            },
                                            choice.get(
                                                    Calendar.HOUR_OF_DAY
                                            ),
                                            choice.get(
                                                    Calendar.MINUTE
                                            ),
                                            true
                                    );

                            timePicker.show();
                        },
                        target.get(Calendar.YEAR),
                        target.get(Calendar.MONTH),
                        target.get(Calendar.DAY_OF_MONTH)
                );

        datePicker.show();
    }

    // =====================================================
    // SUBMIT BOOKING REQUEST
    // =====================================================

    private void submitBookingRequest() {

        if (requestInProgress
                || !itemLoaded
                || currentItem == null) {
            return;
        }

        if (!startSelected || !endSelected) {

            showToast(
                    "Please choose both borrowing dates and times."
            );

            return;
        }

        AvailabilitySlot requested =
                new AvailabilitySlot(
                        startCalendar.getTimeInMillis(),
                        endCalendar.getTimeInMillis()
                );

        if (!requested.isValid()) {

            showToast(
                    "End time must be after start time."
            );

            return;
        }

        if (requested.getStartTime()
                <= System.currentTimeMillis()) {

            showToast(
                    "Start time must be in the future."
            );

            return;
        }

        String borrowerId =
                userRepository.getCurrentUserId();

        if (borrowerId == null
                || borrowerId.trim().isEmpty()) {

            showToast(
                    "Please log in before requesting a booking."
            );

            return;
        }

        requestInProgress = true;
        updateRequestButton();

        // Reload latest listing before validating.
        itemRepository.getItem(
                itemId,
                new ItemRepository.OnItemLoadedListener() {

                    @Override
                    public void onSuccess(Item latestItem) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        currentItem = latestItem;
                        itemLoaded = latestItem != null;

                        if (latestItem != null) {
                            renderItem(latestItem);
                        }

                        if (latestItem == null
                                || latestItem.getStatus()
                                != ItemStatus.ACTIVE) {

                            requestFailed(
                                    "This listing is no longer available."
                            );

                            return;
                        }

                        if (borrowerId.equals(
                                latestItem.getOwnerId())) {

                            requestFailed(
                                    "You cannot borrow your own item."
                            );

                            return;
                        }

                        AvailabilitySlot ownerSlot =
                                latestItem.getAvailabilitySlot();

                        if (ownerSlot == null
                                || !ownerSlot.contains(requested)) {

                            requestFailed(
                                    "Your dates must be entirely within "
                                            + "the owner's availability."
                            );

                            return;
                        }

                        loadExistingBookings(
                                borrowerId,
                                requested,
                                ownerSlot
                        );
                    }

                    @Override
                    public void onFailure(Exception error) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        requestFailed(
                                "Could not verify item availability. "
                                        + "Please retry."
                        );
                    }
                }
        );
    }

    // =====================================================
    // LOAD EXISTING BOOKINGS
    // =====================================================

    private void loadExistingBookings(
            String borrowerId,
            AvailabilitySlot requested,
            AvailabilitySlot ownerSlot
    ) {

        bookingRepository.getBookingsByItem(
                itemId,
                new BookingRepository.OnBookingsLoadedListener() {

                    @Override
                    public void onSuccess(
                            List<Booking> existingBookings
                    ) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        Booking request = new Booking(
                                bookingRepository.generateBookingId(),
                                borrowerId,
                                itemId,
                                requested
                        );

                        List<AvailabilitySlot> allowed =
                                Collections.singletonList(ownerSlot);

                        boolean valid =
                                bookingService.canRequestBooking(
                                        request,
                                        allowed,
                                        existingBookings
                                );

                        if (!valid) {

                            requestFailed(
                                    "This period conflicts with another "
                                            + "booking or existing request."
                            );

                            return;
                        }

                        savePendingBooking(request);
                    }

                    @Override
                    public void onFailure(Exception error) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        requestFailed(
                                "Could not check existing bookings. "
                                        + "Please retry."
                        );
                    }
                }
        );
    }

    // =====================================================
    // SAVE REQUEST
    // =====================================================

    private void savePendingBooking(Booking booking) {

        bookingRepository.addBooking(
                booking,
                new BookingRepository.OnBookingSavedListener() {

                    @Override
                    public void onSuccess() {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        requestInProgress = false;

                        showToast(
                                "Booking request sent. "
                                        + "Waiting for owner approval."
                        );

                        Intent bookings = new Intent(
                                ItemDetailsActivity.this,
                                BookingListActivity.class
                        );

                        bookings.putExtra(
                                "type",
                                "UPCOMING"
                        );

                        startActivity(bookings);
                        finish();
                    }

                    @Override
                    public void onFailure(Exception error) {

                        if (isFinishing() || isDestroyed()) {
                            return;
                        }

                        requestFailed(
                                "Couldn't save your request. Please retry."
                        );
                    }
                }
        );
    }

    private void requestFailed(String message) {

        requestInProgress = false;
        updateRequestButton();
        showToast(message);
    }

    private void showToast(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private int dpToPx(int dp) {

        return (int) (
                dp * getResources()
                        .getDisplayMetrics().density
        );
    }
}
