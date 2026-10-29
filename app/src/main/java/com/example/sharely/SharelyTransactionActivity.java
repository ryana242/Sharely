package com.example.sharely;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Sharely pickup/return transaction screen. Launch with extra "bookingId".
 * All transitions are authorised by callable server functions, never client-written.
 * Demo payments do not charge money. OTPs are displayed only on the correct device.
 */
public final class SharelyTransactionActivity extends SharelyChromeActivity {
    private static final int SELECT_BEFORE = 731;
    private static final int SELECT_AFTER = 732;
    private String bookingId;
    private Map<String, Object> flow;
    private LinearLayout page;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        bookingId = getIntent().getStringExtra("bookingId");
        if (bookingId == null || !bookingId.matches("[A-Za-z0-9_-]{1,128}")) {
            SharelyUi.notice(this, "Please open a confirmed booking from Activity.");
            finish(); return;
        }
        draw();
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        if (bookingId != null) refresh();
    }

    private void refresh() {
        SharelyFunctions.call(this, "sharelyGetFlow", SharelyFunctions.args("bookingId", bookingId), result -> {
            flow = result;
            draw();
        });
    }

    private void draw() {
        page = SharelyUi.page(this);
        SharelyUi.text(this, page, "Pickup and return", true);
        SharelyUi.button(this, page, "Back to bookings", v -> finish());
        SharelyUi.text(this, page, "Coordinate handover with the other participant. Demo payments do not transfer money.", false);
        if (flow == null) {
            SharelyUi.text(this, page, "Initialize after the owner has accepted the booking.", false);
            action("Start accepted booking", "sharelyStartFlow");
            action("Refresh", "sharelyGetFlow");
            return;
        }
        String phase = string(flow.get("phase"), "UNKNOWN");
        boolean owner = Objects.equals(SharelyFunctions.uid(), flow.get("ownerId"));
        boolean borrower = Objects.equals(SharelyFunctions.uid(), flow.get("borrowerId"));
        if (!owner && !borrower) {
            SharelyUi.text(this, page, "Your account is not a participant of this booking.", false);
            return;
        }
        SharelyUi.text(this, page, "Stage: " + phase + "  |  Your role: " + (owner ? "Owner" : "Borrower"), true);
        if (flow.containsKey("dueAt")) SharelyUi.text(this, page, "Agreed due: " + date(flow.get("dueAt")), false);
        if (flow.containsKey("pickupAt")) SharelyUi.text(this, page, "Pickup meeting: " + date(flow.get("pickupAt")), false);
        if (flow.containsKey("scheduledReturnAt")) SharelyUi.text(this, page, "Return meeting: " + date(flow.get("scheduledReturnAt")), false);
        if (flow.containsKey("rentalCents")) {
            long rental = number(flow.get("rentalCents"));
            SharelyUi.text(this, page, String.format(java.util.Locale.US,
                    "Rental quote (DEMO): $%.2f", rental / 100d), false);
        }
        if (flow.containsKey("lateFeeCents")) {
            long cents = number(flow.get("lateFeeCents"));
            SharelyUi.text(this, page, String.format(java.util.Locale.US, "Late fee (DEMO): $%.2f", cents / 100d), false);
        }
        SharelyUi.text(this, page, "Demo only — no real money is charged. "
                + "Late-fee simulation: 15-minute grace, then 10% of the daily rental "
                + "price per started late hour, capped at one day of rent.", false);

        if (flow.get("beforePhotoPath") instanceof String) {
            showPhoto("Condition before pickup", (String) flow.get("beforePhotoPath"));
        }
        if (flow.get("afterPhotos") instanceof Map) {
            Object after = ((Map<?, ?>) flow.get("afterPhotos")).get(flow.get("borrowerId"));
            if (after instanceof String) showPhoto("Condition at return", (String) after);
        }

        switch (phase) {
            case "ACCEPTED":
                SharelyUi.text(this, page, "1. Arrange the pickup", true);
                chooseTime("Schedule pickup", "sharelySchedulePickup", "pickupAt");
                break;
            case "PICKUP_SCHEDULED":
                SharelyUi.text(this, page, "2. Record condition before pickup", true);
                if (owner) photoButton("Owner: upload pre-borrow condition photo", SELECT_BEFORE);
                else SharelyUi.text(this, page, "Ask the owner to photograph the item before handover.", false);
                chooseTime("Reschedule pickup", "sharelySchedulePickup", "pickupAt");
                break;
            case "PICKUP_READY":
                SharelyUi.text(this, page, "3. Confirm demo payment and verify pickup", true);
                if (borrower) {
                    if (!Boolean.TRUE.equals(flow.get("simulatedRentalPaid"))) {
                        action("Simulate rental payment (demo)", "sharelyConfirmDemoRentalPayment");
                    } else {
                        SharelyUi.text(this, page, "Rental payment simulation recorded. No money was charged.", false);
                        issueCode("Show my pickup OTP", "sharelyIssuePickupOtp");
                    }
                } else {
                    SharelyUi.text(this, page, "Ask the borrower for their current 6-digit pickup OTP.", false);
                    enterCode("Confirm pickup OTP", "sharelyVerifyPickupOtp");
                }
                break;
            case "ACTIVE":
                SharelyUi.text(this, page, "4. Item is being borrowed", true);
                chooseTime("Schedule return meeting", "sharelyScheduleReturn", "returnAt");
                if (borrower) {
                    if (flow.get("scheduledReturnAt") != null) action("Request return (calculate late fee)", "sharelyRequestReturn");
                    else SharelyUi.text(this, page, "Set a return meeting time before requesting return.", false);
                }
                break;
            case "RETURN_REQUESTED":
            case "RETURN_READY":
                SharelyUi.text(this, page, "5. Confirm return", true);
                long fee = number(flow.get("lateFeeCents"));
                if (borrower) {
                    photoButton("Add after-use condition photo", SELECT_AFTER);
                    if (fee > 0 && !Boolean.TRUE.equals(flow.get("simulatedLateFeePaid"))) {
                        action("Simulate late-fee payment (demo)", "sharelyConfirmDemoLateFee");
                    } else if ("RETURN_READY".equals(phase)) {
                        SharelyUi.text(this, page, "Owner: inspect the item, then give the borrower the return OTP.", false);
                        enterCode("Enter owner's return OTP", "sharelyVerifyReturnOtp");
                    } else {
                        SharelyUi.text(this, page, "Waiting for the owner to confirm receipt and issue their OTP.", false);
                    }
                } else {
                    if (Boolean.TRUE.equals(flow.get("simulatedLateFeePaid"))) {
                        issueCode("Owner: show return OTP", "sharelyIssueReturnOtp");
                    } else {
                        SharelyUi.text(this, page, "Waiting for demo late-fee confirmation.", false);
                    }
                }
                break;
            case "COMPLETED":
                SharelyUi.text(this, page, "6. Completed booking", true);
                SharelyUi.text(this, page, "You may attach after-return condition evidence for this booking.", false);
                SharelyUi.text(this, page, "Item returned. Both participants can review this completed booking.", false);
                break;
            default:
                SharelyUi.text(this, page, "Unknown booking stage. Check Firebase fields and server implementation.", false);
        }
        SharelyUi.button(this, page, "Refresh booking", v -> refresh());
    }

    /** Fetch evidence through Storage rules, without publishing a public download token. */
    private void showPhoto(String title, String path) {
        SharelyUi.text(this, page, title, true);
        android.widget.ImageView preview = new android.widget.ImageView(this);
        preview.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        page.addView(preview, new LinearLayout.LayoutParams(-1, SharelyUi.dp(this, 160)));
        FirebaseStorage.getInstance().getReference().child(path).getBytes(4L*1024*1024)
            .addOnSuccessListener(bytes -> {
                android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                options.inSampleSize = 2;
                android.graphics.Bitmap image = android.graphics.BitmapFactory.decodeByteArray(
                        bytes, 0, bytes.length, options);
                if (!isFinishing() && image != null) preview.setImageBitmap(image);
            }).addOnFailureListener(error -> {
                if (!isFinishing()) preview.setContentDescription("Could not load private condition photo");
            });
    }

    private void action(String label, String function) {
        SharelyUi.button(this, page, label, v -> execute(function, new HashMap<>()));
    }
    private void execute(String function, Map<String, Object> other) {
        other.put("bookingId", bookingId);
        SharelyFunctions.call(this, function, other, data -> {
            if (Boolean.TRUE.equals(data.get("simulated"))) SharelyUi.notice(this, "Demo only: no real money was charged.");
            refresh();
        });
    }
    private void issueCode(String label, String function) {
        SharelyUi.button(this, page, label, v -> SharelyFunctions.call(this, function,
            SharelyFunctions.args("bookingId", bookingId), data -> {
                String code = string(data.get("code"), "");
                if (!code.matches("\\d{6}")) { SharelyUi.notice(this, "No valid code returned."); return; }
                new AlertDialog.Builder(this).setTitle("Private one-time code")
                    .setMessage("Code: " + code + "\nExpires in 5 minutes.\nShare verbally with the other participant. Do not post in public chat.")
                    .setPositiveButton("Done", (d, w) -> refresh()).show();
            }));
    }
    private void enterCode(String label, String function) {
        SharelyUi.button(this, page, label, v -> {
            EditText field = new EditText(this);
            field.setSingleLine(true);
            field.setHint("6-digit OTP");
            field.setInputType(InputType.TYPE_CLASS_NUMBER);
            field.setPadding(SharelyUi.dp(this, 16), 8, SharelyUi.dp(this, 16), 8);
            new AlertDialog.Builder(this).setTitle(label).setView(field)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String code = field.getText().toString().trim();
                    if (!code.matches("\\d{6}")) { SharelyUi.notice(this, "Enter exactly 6 digits."); return; }
                    execute(function, SharelyFunctions.args("code", code));
                }).setNegativeButton("Cancel", null).show();
        });
    }
    private void chooseTime(String label, String function, String field) {
        SharelyUi.button(this, page, label, v -> {
            Calendar proposed = Calendar.getInstance();
            proposed.add(Calendar.HOUR_OF_DAY, 2);
            DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, day) -> {
                proposed.set(Calendar.YEAR, year);
                proposed.set(Calendar.MONTH, month);
                proposed.set(Calendar.DAY_OF_MONTH, day);
                new TimePickerDialog(this, (timeView, hour, minute) -> {
                    proposed.set(Calendar.HOUR_OF_DAY, hour);
                    proposed.set(Calendar.MINUTE, minute);
                    proposed.set(Calendar.SECOND, 0);
                    if (proposed.getTimeInMillis() <= System.currentTimeMillis()) {
                        SharelyUi.notice(this, "Choose a future time."); return;
                    }
                    execute(function, SharelyFunctions.args(field, proposed.getTimeInMillis()));
                }, proposed.get(Calendar.HOUR_OF_DAY), proposed.get(Calendar.MINUTE), false).show();
            }, proposed.get(Calendar.YEAR), proposed.get(Calendar.MONTH), proposed.get(Calendar.DAY_OF_MONTH));
            datePicker.getDatePicker().setMinDate(System.currentTimeMillis() - 30000);
            datePicker.show();
        });
    }
    private void photoButton(String label, int requestCode) {
        SharelyUi.button(this, page, label, v -> {
            Intent choose = new Intent(Intent.ACTION_GET_CONTENT);
            choose.setType("image/*");
            startActivityForResult(Intent.createChooser(choose, "Select item condition photo"), requestCode);
        });
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode, resultCode, result);
        if (resultCode != RESULT_OK || result == null || result.getData() == null) return;
        if (requestCode != SELECT_BEFORE && requestCode != SELECT_AFTER) return;
        String uid = SharelyFunctions.uid();
        if (uid == null) { SharelyUi.notice(this, "Please sign in."); return; }
        Uri uri = result.getData();
        String mime = getContentResolver().getType(uri);
        if (mime == null || !mime.startsWith("image/")) { SharelyUi.notice(this, "Choose an image file."); return; }
        String path = "transactionEvidence/" + bookingId + "/" + uid + "/" + System.currentTimeMillis() + "_" + requestCode + ".jpg";
        StorageMetadata meta = new StorageMetadata.Builder().setContentType(mime).build();
        SharelyUi.notice(this, "Uploading photo...");
        FirebaseStorage.getInstance().getReference().child(path).putFile(uri, meta)
            .addOnSuccessListener(task -> {
                Map<String, Object> args = SharelyFunctions.args("storagePath", path);
                execute(requestCode == SELECT_BEFORE ? "sharelyAttachBeforePhoto" : "sharelyAttachAfterPhoto", args);
            })
            .addOnFailureListener(error -> SharelyUi.notice(this, "Photo upload failed: " + error.getMessage()));
    }
    private static String string(Object value, String fallback) { return value == null ? fallback : String.valueOf(value); }
    private static long number(Object value) { return value instanceof Number ? ((Number) value).longValue() : 0L; }
    private static String date(Object value) {
        if (!(value instanceof Number)) return "Not available";
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(new java.util.Date(((Number) value).longValue()));
    }
}
