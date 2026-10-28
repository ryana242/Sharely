
package com.example.sharely;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sharely.data.NotificationRepository;
import com.example.sharely.enums.NotificationType;
import com.example.sharely.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsActivity extends AppCompatActivity {

    private static final int GREEN = Color.rgb(23, 107, 82);
    private static final int INK = Color.rgb(18, 61, 49);
    private static final int MUTED = Color.rgb(95, 119, 109);

    private final NotificationRepository repository =
            new NotificationRepository();

    private LinearLayout notificationList;
    private TextView subtitle;
    private ListenerRegistration registration;
    private String userId;

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }

    private TextView text(
            String value,
            int size,
            int color,
            boolean bold
    ) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return view;
    }

    private GradientDrawable background(boolean unread) {
        GradientDrawable drawable = new GradientDrawable();

        drawable.setColor(
                unread
                        ? Color.rgb(238, 248, 243)
                        : Color.WHITE
        );

        drawable.setCornerRadius(dp(16));
        drawable.setStroke(
                dp(1),
                Color.rgb(213, 230, 221)
        );

        return drawable;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        userId = user == null ? null : user.getUid();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        setContentView(root);

        // Keep the entire screen below the status bar and notch,
        // and above the bottom navigation/gesture area.
        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, insets) -> {

                    Insets safe = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                                    | WindowInsetsCompat.Type.displayCutout()
                    );

                    view.setPadding(
                            safe.left,
                            safe.top,
                            safe.right,
                            safe.bottom
                    );

                    return insets;
                }
        );

        ViewCompat.requestApplyInsets(root);

        // Header

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setPadding(
                dp(14),
                dp(18),
                dp(18),
                dp(10)
        );

        root.addView(
                heading,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(82)
                )
        );

        // Back button

        Button back = new Button(this);

        back.setText("‹ Back");
        back.setAllCaps(false);
        back.setTextSize(15);
        back.setTextColor(GREEN);

        back.setBackgroundTintList(
                ColorStateList.valueOf(Color.WHITE)
        );

        back.setMinHeight(dp(48));
        back.setMinimumHeight(dp(48));

        heading.addView(
                back,
                new LinearLayout.LayoutParams(
                        dp(94),
                        dp(48)
                )
        );

        back.setOnClickListener(
                v -> returnToMain()
        );

        // Support the Android system Back button and gesture.
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {
                        returnToMain();
                    }
                }
        );

        // Heading text

        LinearLayout headingText =
                new LinearLayout(this);

        headingText.setOrientation(
                LinearLayout.VERTICAL
        );

        LinearLayout.LayoutParams headingParams =
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                );

        headingParams.leftMargin = dp(8);

        heading.addView(
                headingText,
                headingParams
        );

        headingText.addView(
                text(
                        "Notifications",
                        23,
                        INK,
                        true
                )
        );

        subtitle = text(
                "Loading...",
                13,
                MUTED,
                false
        );

        headingText.addView(subtitle);

        // Scrollable notification list

        ScrollView scroll =
                new ScrollView(this);

        scroll.setFillViewport(true);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        notificationList =
                new LinearLayout(this);

        notificationList.setOrientation(
                LinearLayout.VERTICAL
        );

        notificationList.setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(28)
        );

        scroll.addView(notificationList);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (userId == null) {
            showMessage(
                    "Please sign in to view notifications."
            );
            return;
        }

        registration = FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("recipientId", userId)
                .addSnapshotListener((snapshot, error) -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (error != null) {
                        showMessage(
                                "Could not load notifications: "
                                        + error.getMessage()
                        );
                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    List<Notification> all =
                            new ArrayList<>();

                    for (DocumentSnapshot document
                            : snapshot.getDocuments()) {

                        Notification item =
                                parse(document);

                        if (item != null) {
                            all.add(item);
                        }
                    }

                    Collections.sort(
                            all,
                            (a, b) -> Long.compare(
                                    b.getCreatedAt(),
                                    a.getCreatedAt()
                            )
                    );

                    render(all);
                });
    }

    private Notification parse(DocumentSnapshot doc) {
        try {
            if (!userId.equals(
                    doc.getString("recipientId")
            )) {
                return null;
            }

            String id = doc.getString("notificationId");
            String sender = doc.getString("senderId");
            String type = doc.getString("type");
            String title = doc.getString("title");
            String message = doc.getString("message");
            String related = doc.getString("relatedId");

            Boolean read = doc.getBoolean("isRead");
            Long time = doc.getLong("createdAt");

            if (id == null
                    || !id.equals(doc.getId())
                    || sender == null
                    || type == null
                    || title == null
                    || message == null
                    || related == null
                    || read == null
                    || time == null) {

                return null;
            }

            return new Notification(
                    id,
                    userId,
                    sender,
                    NotificationType.valueOf(type),
                    title,
                    message,
                    related,
                    read,
                    time
            );

        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void showMessage(String message) {
        notificationList.removeAllViews();

        TextView label = text(
                message,
                15,
                MUTED,
                false
        );

        label.setGravity(Gravity.CENTER);

        label.setPadding(
                dp(12),
                dp(76),
                dp(12),
                dp(12)
        );

        notificationList.addView(label);
        subtitle.setText("");
    }

    private void render(List<Notification> all) {
        notificationList.removeAllViews();

        int unread = 0;

        for (Notification notification : all) {
            if (!notification.isRead()) {
                unread++;
            }
        }

        subtitle.setText(
                unread == 0
                        ? "All caught up"
                        : unread + " unread"
        );

        if (all.isEmpty()) {
            showMessage(
                    "No notifications yet. "
                            + "Booking and message updates will appear here."
            );
            return;
        }

        for (Notification notification : all) {
            addNotificationCard(notification);
        }
    }

    private void addNotificationCard(Notification notification) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);

        card.setPadding(
                dp(16),
                dp(15),
                dp(16),
                dp(15)
        );

        card.setBackground(
                background(!notification.isRead())
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        cardParams.bottomMargin = dp(12);

        notificationList.addView(
                card,
                cardParams
        );

        TextView title = text(
                (notification.isRead() ? "" : "●  ")
                        + notification.getTitle(),
                16,
                INK,
                true
        );

        card.addView(title);

        TextView description = text(
                notification.getMessage(),
                14,
                MUTED,
                false
        );

        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        messageParams.topMargin = dp(5);

        card.addView(
                description,
                messageParams
        );

        String date = new SimpleDateFormat(
                "d MMM, h:mm a",
                Locale.getDefault()
        ).format(
                new Date(notification.getCreatedAt())
        );

        TextView stamp = text(
                date + "  ·  Tap to open",
                12,
                MUTED,
                false
        );

        LinearLayout.LayoutParams stampParams =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        stampParams.topMargin = dp(9);

        card.addView(
                stamp,
                stampParams
        );

        card.setOnClickListener(v -> {

            if (notification.isRead()) {
                navigate(notification);
                return;
            }

            card.setEnabled(false);

            repository.markAsRead(
                    notification.getNotificationId(),
                    new NotificationRepository.OnNotificationUpdatedListener() {

                        @Override
                        public void onSuccess() {
                            if (!isFinishing() && !isDestroyed()) {
                                navigate(notification);
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            card.setEnabled(true);

                            Toast.makeText(
                                    NotificationsActivity.this,
                                    "Couldn't mark as read: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );
        });
    }

    // Exit Notifications without creating duplicate Main activities.
    private void returnToMain() {
        if (isFinishing()) {
            return;
        }

        if (isTaskRoot()) {
            // Fallback if Notifications was opened without
            // a parent activity in the back stack.
            Intent home = new Intent(
                    this,
                    MainAppActivity.class
            );

            home.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(home);
        }

        // Normally reveals the existing MainAppActivity.
        finish();
    }

    private void navigate(Notification notification) {
        if (notification.getType()
                == NotificationType.NEW_MESSAGE) {

            Intent intent = new Intent(
                    this,
                    MainAppActivity.class
            );

            intent.putExtra(
                    MainAppActivity.EXTRA_OPEN_MESSAGES,
                    true
            );

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            startActivity(intent);
            finish();
            return;
        }

        Intent intent = new Intent(
                this,
                BookingListActivity.class
        );

        String mode;

        switch (notification.getType()) {

            case BOOKING_REQUEST:
            case BOOKING_CANCELLED:
                mode = "OWNER";
                break;

            case BOOKING_REJECTED:
                mode = "HISTORY";
                break;

            default:
                mode = "UPCOMING";
                break;
        }

        intent.putExtra("type", mode);

        startActivity(intent);

        // Going back from the booking screen now takes
        // the user to Main, not back into Notifications.
        finish();
    }

    @Override
    protected void onStop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }

        super.onStop();
    }
}
