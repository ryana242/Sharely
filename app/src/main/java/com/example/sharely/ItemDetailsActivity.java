package com.example.sharely;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
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
import com.example.sharely.service.AvailabilityCalendarEngine;
import com.example.sharely.service.BookingService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/** Item details and an inline reservation-aware, range-selection calendar. */
public class ItemDetailsActivity extends AppCompatActivity {

    private static final int GREEN = Color.rgb(23, 107, 82);
    private static final int INK = Color.rgb(18, 61, 49);
    private static final int MUTED = Color.rgb(104, 124, 114);
    private static final int LIGHT_GREEN = Color.rgb(231, 245, 238);
    private static final int AMBER = Color.rgb(166, 99, 17);
    private static final int PALE_AMBER = Color.rgb(255, 246, 226);
    private static final int DISABLED = Color.rgb(240, 242, 241);
    private static final long ONE_MINUTE = AvailabilityCalendarEngine.MINUTE;
    private static final long HALF_HOUR = 30L * ONE_MINUTE;

    private final Calendar selectedStart = Calendar.getInstance();
    private final Calendar selectedEnd = Calendar.getInstance();
    private final Calendar shownMonth = Calendar.getInstance();
    private final List<AvailabilitySlot> reserved = new ArrayList<>();
    private final BookingService bookingService = new BookingService();

    private ItemRepository itemRepository;
    private BookingRepository bookingRepository;
    private UserRepository userRepository;
    private Item currentItem;
    private String itemId;

    private boolean itemLoaded;
    private boolean availabilityReady;
    private boolean startSelected;
    private boolean endSelected;
    private boolean choosingStart = true;
    private boolean requestInProgress;
    private int loadVersion;

    private TextView itemName, itemPrice, itemInfo, itemDescription;
    private TextView ownerAvailability, startLabel, endLabel;
    private TextView monthLabel, calendarHint, reservationsSummary;
    private GridLayout daysGrid;
    private LinearLayout photos;
    private Button requestButton, messageButton, startButton, endButton;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_item_details);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.itemDetailsMain), (view, insets) -> {
                    Insets safe = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                                    | WindowInsetsCompat.Type.displayCutout());
                    view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                    return insets;
                });

        itemRepository = new ItemRepository();
        bookingRepository = new BookingRepository();
        userRepository = new UserRepository();

        itemName = findViewById(R.id.textItemName);
        itemPrice = findViewById(R.id.textItemPrice);
        itemInfo = findViewById(R.id.textItemInfo);
        itemDescription = findViewById(R.id.textItemDescription);
        ownerAvailability = findViewById(R.id.textOwnerAvailability);
        startLabel = findViewById(R.id.textStartTime);
        endLabel = findViewById(R.id.textEndTime);
        photos = findViewById(R.id.photoContainer);
        requestButton = findViewById(R.id.buttonRequestBooking);
        messageButton = findViewById(R.id.buttonMessageOwner);
        startButton = findViewById(R.id.buttonSelectStart);
        endButton = findViewById(R.id.buttonSelectEnd);

        itemId = getIntent().getStringExtra("itemId");
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        // Both existing buttons control the visible calendar rather than
        // opening unrestricted Android date/time pickers.
        startButton.setOnClickListener(v -> {
            choosingStart = true;
            renderCalendar();
            scrollToCalendar();
        });
        endButton.setOnClickListener(v -> {
            if (!startSelected) {
                toast("Choose a pickup time first.");
                return;
            }
            choosingStart = false;
            renderCalendar();
            scrollToCalendar();
        });
        requestButton.setOnClickListener(v -> submitBooking());
        messageButton.setOnClickListener(v -> openOwnerConversation());

        setMonth(System.currentTimeMillis());
        buildCalendar();
        ownerAvailability.setText("Loading availability and reservations…");
        itemName.setText("Loading item…");
        updateUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (itemId == null || itemId.trim().isEmpty()) {
            showLoadError("This listing cannot be opened.");
        } else {
            refreshAvailability();
        }
    }

    private void refreshAvailability() {
        final int version = ++loadVersion;
        itemLoaded = false;
        availabilityReady = false;
        updateUi();
        ownerAvailability.setText("Checking live availability…");

        itemRepository.getItem(itemId, new ItemRepository.OnItemLoadedListener() {
            @Override public void onSuccess(Item item) {
                if (!current(version)) return;
                if (item == null) {
                    showLoadError("Item no longer exists.");
                    return;
                }
                currentItem = item;
                itemLoaded = true;
                renderItem(item);

                // The repository exposes accepted/active reservations via the
                // item's ledger to borrowers, without exposing other users' UIDs.
                bookingRepository.getBookingsByItem(itemId,
                        new BookingRepository.OnBookingsLoadedListener() {
                            @Override public void onSuccess(List<Booking> bookings) {
                                if (!current(version)) return;
                                reserved.clear();
                                for (Booking booking : bookings) {
                                    if (booking != null && booking.blocksAvailability()
                                            && booking.getBookingSlot() != null) {
                                        reserved.add(booking.getBookingSlot());
                                    }
                                }
                                availabilityReady = true;
                                invalidateSelectionIfNeeded();
                                renderCalendar();
                                updateUi();
                            }
                            @Override public void onFailure(Exception e) {
                                if (!current(version)) return;
                                availabilityReady = false;
                                calendarHint.setText("Could not check reservations. "
                                        + "Reopen this listing to retry.");
                                updateUi();
                            }
                        });
            }
            @Override public void onFailure(Exception e) {
                if (current(version)) showLoadError(
                        "Unable to load listing. Reopen it to retry.");
            }
        });
    }

    private boolean current(int version) {
        return version == loadVersion && !isFinishing() && !isDestroyed();
    }

    private void showLoadError(String message) {
        currentItem = null;
        itemLoaded = false;
        availabilityReady = false;
        itemName.setText("Item unavailable");
        ownerAvailability.setText(message);
        renderCalendar();
        updateUi();
    }

    private void renderItem(Item item) {
        itemName.setText(nonEmpty(item.getItemName(), "Unnamed item"));
        itemPrice.setText(String.format(Locale.getDefault(),
                "$%.2f / day", item.getPrice()));
        itemInfo.setText(nonEmpty(item.getLocation(), "Location not specified"));
        itemDescription.setText(nonEmpty(item.getDescription(), "No description"));

        AvailabilitySlot window = ownerWindow();
        if (item.getStatus() != ItemStatus.ACTIVE) {
            ownerAvailability.setText("This listing is currently unavailable.");
        } else if (window == null) {
            ownerAvailability.setText("The owner has not set a valid availability period.");
        } else if (window.getEndTime() <= System.currentTimeMillis()) {
            ownerAvailability.setText("This listing's availability has expired.");
        } else {
            ownerAvailability.setText("Owner available: "
                    + format(window.getStartTime(), "d MMM, h:mm a")
                    + " – " + format(window.getEndTime(), "d MMM, h:mm a"));
            if (!startSelected) setMonth(Math.max(
                    System.currentTimeMillis(), window.getStartTime()));
        }

        photos.removeAllViews();
        ArrayList<String> urls = item.getImageUrls();
        if (urls != null) {
            for (String url : urls) {
                if (url == null || url.trim().isEmpty()) continue;
                ImageView image = new ImageView(this);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                        dp(320), LinearLayout.LayoutParams.MATCH_PARENT);
                p.rightMargin = dp(12);
                image.setLayoutParams(p);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                photos.addView(image);
                Glide.with(this).load(url).fitCenter().into(image);
            }
        }
    }

    private AvailabilitySlot ownerWindow() {
        if (currentItem == null) return null;
        AvailabilitySlot window = currentItem.getAvailabilitySlot();
        return window != null && window.isValid() ? window : null;
    }

    private boolean isOwner() {
        String uid = userRepository.getCurrentUserId();
        return uid != null && currentItem != null
                && uid.equals(currentItem.getOwnerId());
    }

    private boolean canChoose() {
        return availabilityReady && itemLoaded && currentItem != null
                && currentItem.getStatus() == ItemStatus.ACTIVE
                && ownerWindow() != null
                && ownerWindow().getEndTime() > System.currentTimeMillis()
                && userRepository.getCurrentUserId() != null && !isOwner();
    }

    private void updateUi() {
        boolean ready = canChoose();
        startButton.setEnabled(ready);
        endButton.setEnabled(ready && startSelected);
        messageButton.setVisibility(isOwner() ? View.GONE : View.VISIBLE);
        messageButton.setEnabled(itemLoaded && currentItem != null
                && currentItem.getStatus() == ItemStatus.ACTIVE
                && userRepository.getCurrentUserId() != null && !isOwner());

        startLabel.setText(startSelected
                ? "Pickup: " + format(selectedStart.getTimeInMillis(), "EEE d MMM, h:mm a")
                : "Pickup: Not selected");
        endLabel.setText(endSelected
                ? "Return: " + format(selectedEnd.getTimeInMillis(), "EEE d MMM, h:mm a")
                : "Return: Not selected");
        startButton.setText(startSelected
                ? "Change pickup date & time" : "Choose pickup date & time");
        endButton.setText("Choose return date & time");

        boolean valid = ready && startSelected && endSelected
                && AvailabilityCalendarEngine.isSelectable(
                        selectedStart.getTimeInMillis(),
                        selectedEnd.getTimeInMillis(),
                        ownerWindow(), reserved, System.currentTimeMillis());
        requestButton.setEnabled(valid && !requestInProgress);
        requestButton.setText(requestInProgress ? "Checking request…"
                : isOwner() ? "You own this item"
                : "Request booking");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String format(long value, String pattern) {
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date(value));
    }

    private String nonEmpty(String text, String fallback) {
        return text == null || text.trim().isEmpty() ? fallback : text;
    }

    private TextView label(String content, int sp, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(content);
        text.setTextSize(sp);
        text.setTextColor(color);
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }

    private GradientDrawable surface(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    /** Add the calendar directly to the existing booking card; no XML changes. */
    private void buildCalendar() {
        LinearLayout card = (LinearLayout) ownerAvailability.getParent();
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(10), dp(14), dp(10), dp(12));
        panel.setBackground(surface(Color.WHITE, Color.rgb(213, 232, 223), 16));
        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(-1, -2);
        panelParams.topMargin = dp(16);
        card.addView(panel, card.indexOfChild(ownerAvailability) + 1, panelParams);

        TextView title = label("Availability calendar", 18, INK, true);
        panel.addView(title);
        TextView explanation = label(
                "Green = available  ·  Amber = partly available  ·  Grey = unavailable",
                11, MUTED, false);
        explanation.setPadding(0, dp(7), 0, dp(11));
        panel.addView(explanation);

        LinearLayout monthRow = new LinearLayout(this);
        monthRow.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(monthRow, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView prev = label("‹", 32, GREEN, true);
        prev.setGravity(Gravity.CENTER);
        monthRow.addView(prev, new LinearLayout.LayoutParams(dp(42), -1));
        prev.setOnClickListener(v -> moveMonth(-1));

        monthLabel = label("", 17, INK, true);
        monthLabel.setGravity(Gravity.CENTER);
        monthRow.addView(monthLabel, new LinearLayout.LayoutParams(0, -2, 1));

        TextView next = label("›", 32, GREEN, true);
        next.setGravity(Gravity.CENTER);
        monthRow.addView(next, new LinearLayout.LayoutParams(dp(42), -1));
        next.setOnClickListener(v -> moveMonth(1));

        GridLayout weekdayGrid = new GridLayout(this);
        weekdayGrid.setColumnCount(7);
        panel.addView(weekdayGrid, new LinearLayout.LayoutParams(-1, dp(28)));
        for (int i = 0; i < 7; i++) {
            String name = new String[]{"M", "T", "W", "T", "F", "S", "S"}[i];
            TextView dayName = label(name, 12, MUTED, true);
            dayName.setGravity(Gravity.CENTER);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 0;
            p.height = -1;
            p.columnSpec = GridLayout.spec(i, 1f);
            weekdayGrid.addView(dayName, p);
        }

        daysGrid = new GridLayout(this);
        daysGrid.setColumnCount(7);
        panel.addView(daysGrid, new LinearLayout.LayoutParams(-1, -2));

        calendarHint = label("Choose a pickup day to see available times.", 13, INK, false);
        calendarHint.setPadding(dp(4), dp(14), dp(4), dp(9));
        panel.addView(calendarHint);

        reservationsSummary = label("", 12, MUTED, false);
        reservationsSummary.setPadding(dp(4), dp(7), dp(4), dp(2));
        panel.addView(reservationsSummary);
    }

    private void setMonth(long time) {
        shownMonth.setTimeInMillis(time);
        shownMonth.set(Calendar.DAY_OF_MONTH, 1);
        shownMonth.set(Calendar.HOUR_OF_DAY, 0);
        shownMonth.set(Calendar.MINUTE, 0);
        shownMonth.set(Calendar.SECOND, 0);
        shownMonth.set(Calendar.MILLISECOND, 0);
    }

    private void moveMonth(int delta) {
        Calendar proposed = (Calendar) shownMonth.clone();
        proposed.add(Calendar.MONTH, delta);
        AvailabilitySlot window = ownerWindow();
        if (window == null) return;
        Calendar min = Calendar.getInstance();
        min.setTimeInMillis(Math.max(window.getStartTime(), System.currentTimeMillis()));
        min.set(Calendar.DAY_OF_MONTH, 1);
        min.set(Calendar.HOUR_OF_DAY, 0);
        min.set(Calendar.MINUTE, 0);
        min.set(Calendar.SECOND, 0);
        min.set(Calendar.MILLISECOND, 0);
        Calendar max = Calendar.getInstance();
        max.setTimeInMillis(window.getEndTime());
        max.set(Calendar.DAY_OF_MONTH, 1);
        max.set(Calendar.HOUR_OF_DAY, 0);
        max.set(Calendar.MINUTE, 0);
        max.set(Calendar.SECOND, 0);
        max.set(Calendar.MILLISECOND, 0);
        if (!proposed.before(min) && !proposed.after(max)) {
            shownMonth.setTimeInMillis(proposed.getTimeInMillis());
            renderCalendar();
        }
    }

    private long nextDay(long startOfDay) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(startOfDay);
        c.add(Calendar.DAY_OF_MONTH, 1); // handles daylight-saving transitions
        return c.getTimeInMillis();
    }

    private List<AvailabilitySlot> freeOnDay(long startOfDay) {
        return AvailabilityCalendarEngine.freeWindows(
                ownerWindow(), reserved, startOfDay,
                nextDay(startOfDay), System.currentTimeMillis() + ONE_MINUTE);
    }

    /** End selection must remain within the single continuous free period. */
    private long freeUntilStart() {
        return startSelected ? AvailabilityCalendarEngine.freeUntil(
                selectedStart.getTimeInMillis(), ownerWindow(), reserved) : 0L;
    }

    private List<Long> choicesForDay(long startOfDay, boolean forStart) {
        TreeSet<Long> candidates = new TreeSet<>();
        long dayEnd = nextDay(startOfDay);
        if (forStart) {
            for (AvailabilitySlot free : freeOnDay(startOfDay)) {
                long low = free.getStartTime();
                long high = free.getEndTime() - ONE_MINUTE;
                if (low > high) continue;
                candidates.add(low); // exact owner/reservation boundary
                long rounded = ((low + HALF_HOUR - 1) / HALF_HOUR) * HALF_HOUR;
                for (long t = rounded; t <= high; t += HALF_HOUR) {
                    candidates.add(t);
                }
            }
        } else if (startSelected) {
            long low = Math.max(startOfDay, selectedStart.getTimeInMillis() + ONE_MINUTE);
            long high = Math.min(dayEnd, freeUntilStart());
            if (high < low) return new ArrayList<>();
            candidates.add(low);
            // End exactly at a reservation boundary is permitted.
            if (high < dayEnd || high == ownerWindow().getEndTime()) candidates.add(high);
            long rounded = ((low + HALF_HOUR - 1) / HALF_HOUR) * HALF_HOUR;
            for (long t = rounded; t < dayEnd && t <= high; t += HALF_HOUR) {
                candidates.add(t);
            }
        }
        return new ArrayList<>(candidates);
    }

    private boolean overlappingReservation(long dayStart, long dayEnd) {
        for (AvailabilitySlot slot : reserved) {
            if (slot.getStartTime() < dayEnd && slot.getEndTime() > dayStart) {
                return true;
            }
        }
        return false;
    }

    private void renderCalendar() {
        if (daysGrid == null) return;
        daysGrid.removeAllViews();
        monthLabel.setText(format(shownMonth.getTimeInMillis(), "MMMM yyyy"));

        Calendar month = (Calendar) shownMonth.clone();
        int offset = (month.get(Calendar.DAY_OF_WEEK) + 5) % 7; // Monday first
        int monthDays = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        AvailabilitySlot owner = ownerWindow();
        boolean selectable = canChoose();
        long startDay = startSelected ? dayStart(selectedStart.getTimeInMillis()) : -1;
        long endDay = endSelected ? dayStart(selectedEnd.getTimeInMillis()) : -1;

        for (int slotIndex = 0; slotIndex < 42; slotIndex++) {
            int day = slotIndex - offset + 1;
            LinearLayout cell = new LinearLayout(this);
            cell.setGravity(Gravity.CENTER);
            cell.setOrientation(LinearLayout.VERTICAL);

            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 0;
            p.height = dp(58);
            p.columnSpec = GridLayout.spec(slotIndex % 7, 1f);
            p.rowSpec = GridLayout.spec(slotIndex / 7);
            p.setMargins(dp(1), dp(2), dp(1), dp(2));
            daysGrid.addView(cell, p);
            if (day < 1 || day > monthDays) continue;

            Calendar d = (Calendar) shownMonth.clone();
            d.set(Calendar.DAY_OF_MONTH, day);
            long from = d.getTimeInMillis();
            long to = nextDay(from);
            List<Long> choices = selectable
                    ? choicesForDay(from, choosingStart) : Collections.emptyList();
            boolean enabled = !choices.isEmpty();
            boolean reservedToday = overlappingReservation(from, to);
            boolean withinOwner = owner != null
                    && owner.getStartTime() < to && owner.getEndTime() > from;
            boolean selected = from == startDay || from == endDay;
            boolean inRange = startDay >= 0 && endDay >= 0
                    && from > startDay && from < endDay;
            boolean partial = reservedToday || (owner != null && withinOwner
                    && (owner.getStartTime() > from || owner.getEndTime() < to));

            int bg = !enabled ? DISABLED
                    : selected ? GREEN : inRange ? LIGHT_GREEN
                    : partial ? PALE_AMBER : Color.WHITE;
            int stroke = selected ? GREEN
                    : enabled ? Color.rgb(217, 231, 223) : Color.TRANSPARENT;
            cell.setBackground(surface(bg, stroke, 10));

            TextView dateNumber = label(String.valueOf(day), 15,
                    selected ? Color.WHITE : enabled ? INK : Color.rgb(171, 181, 176),
                    selected);
            dateNumber.setGravity(Gravity.CENTER);
            cell.addView(dateNumber);

            String marker = !withinOwner ? "" : !availabilityReady
                    ? "Wait" : !canChoose() ? "—" : !enabled
                    ? (reservedToday ? "Booked" : "Closed")
                    : partial ? "Part" : "Free";
            TextView status = label(marker, 9,
                    selected ? Color.WHITE : !enabled ? MUTED
                            : partial ? AMBER : GREEN, false);
            status.setGravity(Gravity.CENTER);
            cell.addView(status);

            if (enabled) {
                final long chosenDay = from;
                cell.setOnClickListener(v -> chooseTimeOnDay(chosenDay));
            }
        }

        if (!availabilityReady) {
            calendarHint.setText("Loading live availability. Date selection is disabled.");
        } else if (!canChoose()) {
            calendarHint.setText("Date selection is unavailable for this listing or account.");
        } else if (choosingStart) {
            calendarHint.setText("1 of 2 · Tap a green/amber date to choose pickup time.");
        } else if (!endSelected) {
            calendarHint.setText("2 of 2 · Choose a return date. "
                    + "Dates beyond the next reservation are disabled.");
        } else {
            calendarHint.setText("Selected range is free. "
                    + "Use Change pickup/return to adjust it.");
        }
        renderBookedSummary();
    }

    private long dayStart(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private void renderBookedSummary() {
        if (!availabilityReady) {
            reservationsSummary.setText("");
            return;
        }
        Calendar endMonth = (Calendar) shownMonth.clone();
        endMonth.add(Calendar.MONTH, 1);
        long from = shownMonth.getTimeInMillis();
        long to = endMonth.getTimeInMillis();
        StringBuilder summary = new StringBuilder();
        int count = 0;
        List<AvailabilitySlot> chronological = new ArrayList<>(reserved);
        chronological.sort((a, b) -> Long.compare(a.getStartTime(), b.getStartTime()));
        for (AvailabilitySlot slot : chronological) {
            if (slot.getStartTime() >= to || slot.getEndTime() <= from) continue;
            if (count++ == 0) summary.append("Already booked in this month:\n");
            if (count > 5) {
                summary.append("• More reserved periods exist…");
                break;
            }
            summary.append("• ")
                    .append(format(slot.getStartTime(), "d MMM, h:mm a"))
                    .append(" → ")
                    .append(format(slot.getEndTime(), "d MMM, h:mm a"))
                    .append("\n");
        }
        if (count == 0) summary.append("No accepted reservations in this month.");
        reservationsSummary.setText(summary.toString().trim());
    }

    private void scrollToCalendar() {
        View root = findViewById(R.id.itemDetailsMain);
        if (!(root instanceof ScrollView)) return;
        ScrollView scroll = (ScrollView) root;
        scroll.post(() -> {
            int y = 0;
            View current = ownerAvailability;
            while (current != scroll && current != null) {
                y += current.getTop();
                if (!(current.getParent() instanceof View)) break;
                current = (View) current.getParent();
            }
            scroll.smoothScrollTo(0, Math.max(0, y - dp(24)));
        });
    }

    private void chooseTimeOnDay(long day) {
        if (!canChoose()) return;
        boolean startMode = choosingStart;
        List<Long> choices = choicesForDay(day, startMode);
        if (choices.isEmpty()) return;
        String[] labels = new String[choices.size()];
        for (int i = 0; i < choices.size(); i++) {
            long timestamp = choices.get(i);
            labels[i] = format(timestamp, "h:mm a");
            if (startMode) {
                labels[i] += "  ·  free until " + format(
                        AvailabilityCalendarEngine.freeUntil(timestamp,
                                ownerWindow(), reserved), "h:mm a");
            }
        }
        String title = (startMode ? "Select pickup · " : "Select return · ")
                + format(day, "EEE d MMM");
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(labels, (dialog, which) -> {
                    long value = choices.get(which);
                    if (!canChoose()) return;
                    if (startMode) {
                        if (AvailabilityCalendarEngine.freeUntil(
                                value, ownerWindow(), reserved) <= value
                                || value <= System.currentTimeMillis()) {
                            toast("That pickup time is no longer available.");
                            refreshAvailability();
                            return;
                        }
                        selectedStart.setTimeInMillis(value);
                        startSelected = true;
                        endSelected = false;
                        choosingStart = false;
                    } else {
                        if (!startSelected || !AvailabilityCalendarEngine.isSelectable(
                                selectedStart.getTimeInMillis(), value,
                                ownerWindow(), reserved, System.currentTimeMillis())) {
                            toast("That return time is unavailable. Choose another one.");
                            return;
                        }
                        selectedEnd.setTimeInMillis(value);
                        endSelected = true;
                    }
                    updateUi();
                    renderCalendar();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void invalidateSelectionIfNeeded() {
        if (!startSelected) return;
        long now = System.currentTimeMillis();
        long start = selectedStart.getTimeInMillis();
        if (AvailabilityCalendarEngine.freeUntil(start, ownerWindow(), reserved)
                <= start || start <= now) {
            startSelected = false;
            endSelected = false;
            choosingStart = true;
            toast("Availability changed. Please select your dates again.");
            return;
        }
        if (endSelected && !AvailabilityCalendarEngine.isSelectable(
                start, selectedEnd.getTimeInMillis(), ownerWindow(), reserved, now)) {
            endSelected = false;
            choosingStart = false;
            toast("Reserved dates changed. Please choose a new return time.");
        }
    }

    private void submitBooking() {
        if (!canChoose() || requestInProgress || !startSelected || !endSelected) return;
        long from = selectedStart.getTimeInMillis();
        long to = selectedEnd.getTimeInMillis();
        if (!AvailabilityCalendarEngine.isSelectable(from, to,
                ownerWindow(), reserved, System.currentTimeMillis())) {
            toast("These dates are no longer free. Choose another period.");
            refreshAvailability();
            return;
        }
        String borrowerId = userRepository.getCurrentUserId();
        if (borrowerId == null || borrowerId.equals(currentItem.getOwnerId())) return;
        AvailabilitySlot requestWindow = new AvailabilitySlot(from, to);
        requestInProgress = true;
        updateUi();

        // Re-check the current listing and the repository's conflict ledger
        // immediately before saving; the UI alone is not authoritative.
        itemRepository.getItem(itemId, new ItemRepository.OnItemLoadedListener() {
            @Override public void onSuccess(Item latest) {
                if (isFinishing() || isDestroyed()) return;
                currentItem = latest;
                if (latest == null || latest.getStatus() != ItemStatus.ACTIVE
                        || latest.getAvailabilitySlot() == null
                        || !latest.getAvailabilitySlot().contains(requestWindow)) {
                    failRequest("This listing's availability has changed.");
                    return;
                }
                bookingRepository.getBookingsByItem(itemId,
                        new BookingRepository.OnBookingsLoadedListener() {
                            @Override public void onSuccess(List<Booking> bookings) {
                                if (isFinishing() || isDestroyed()) return;
                                List<AvailabilitySlot> latestReserved = new ArrayList<>();
                                for (Booking b : bookings) {
                                    if (b != null && b.blocksAvailability()) {
                                        latestReserved.add(b.getBookingSlot());
                                    }
                                }
                                if (!AvailabilityCalendarEngine.isSelectable(from, to,
                                        latest.getAvailabilitySlot(), latestReserved,
                                        System.currentTimeMillis())) {
                                    failRequest("Those dates were just reserved. "
                                            + "Choose another period.");
                                    refreshAvailability();
                                    return;
                                }
                                Booking booking = new Booking(
                                        bookingRepository.generateBookingId(),
                                        borrowerId, itemId, requestWindow);
                                if (!bookingService.canRequestBooking(
                                        booking,
                                        Collections.singletonList(latest.getAvailabilitySlot()),
                                        bookings)) {
                                    failRequest("This period conflicts with an existing request.");
                                    return;
                                }
                                bookingRepository.addBooking(booking,
                                        new BookingRepository.OnBookingSavedListener() {
                                            @Override public void onSuccess() {
                                                if (isFinishing() || isDestroyed()) return;
                                                requestInProgress = false;
                                                toast("Booking request sent. Waiting for approval.");
                                                Intent intent = new Intent(
                                                        ItemDetailsActivity.this,
                                                        BookingListActivity.class);
                                                intent.putExtra("type", "UPCOMING");
                                                startActivity(intent);
                                                finish();
                                            }
                                            @Override public void onFailure(Exception e) {
                                                if (!isFinishing() && !isDestroyed())
                                                    failRequest("Couldn't submit booking. Please retry.");
                                            }
                                        });
                            }
                            @Override public void onFailure(Exception e) {
                                if (!isFinishing() && !isDestroyed())
                                    failRequest("Couldn't check reservations. Please retry.");
                            }
                        });
            }
            @Override public void onFailure(Exception e) {
                if (!isFinishing() && !isDestroyed())
                    failRequest("Couldn't refresh item. Please retry.");
            }
        });
    }

    private void failRequest(String message) {
        requestInProgress = false;
        updateUi();
        toast(message);
    }

    private void openOwnerConversation() {
        String uid = userRepository.getCurrentUserId();
        if (!itemLoaded || currentItem == null || uid == null
                || uid.equals(currentItem.getOwnerId())
                || currentItem.getStatus() != ItemStatus.ACTIVE) return;
        Intent chat = new Intent(this, MainAppActivity.class);
        chat.putExtra(MainAppActivity.EXTRA_CHAT_ITEM_ID, itemId);
        chat.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(chat);
        finish();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
