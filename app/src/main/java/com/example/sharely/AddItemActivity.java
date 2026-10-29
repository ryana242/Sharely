package com.example.sharely;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sharely.data.ItemRepository;
import com.example.sharely.model.AvailabilitySlot;
import com.example.sharely.model.Item;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddItemActivity extends SharelyChromeActivity {

    private EditText itemName;
    private EditText description;
    private EditText category;
    private EditText price;
    private EditText location;
    private TextView availabilitySummary;
    private Button publishButton;
    private LinearLayout photoPreviewContainer;
    private HorizontalScrollView photoPreviewScroll;

    private final ArrayList<Uri> selectedImages = new ArrayList<>();
    private AvailabilitySlot selectedAvailability;

    private ItemRepository itemRepository;
    private FirebaseAuth firebaseAuth;
    private FirebaseStorage firebaseStorage;
    private boolean publishing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_item);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.addItemMain),
                (v, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                }
        );

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        itemRepository = new ItemRepository();

        itemName = findViewById(R.id.editItemName);
        description = findViewById(R.id.editDescription);
        category = findViewById(R.id.editCategory);
        price = findViewById(R.id.editPrice);
        location = findViewById(R.id.editLocation);
        availabilitySummary = findViewById(R.id.textAvailabilitySummary);
        publishButton = findViewById(R.id.buttonPublishItem);
        photoPreviewContainer = findViewById(R.id.photoPreviewContainer);
        photoPreviewScroll = findViewById(R.id.photoPreviewScroll);

        android.view.View backButton = findViewById(R.id.buttonBack);
        Button addPhotoButton = findViewById(R.id.buttonAddPhoto);
        Button setAvailabilityButton = findViewById(R.id.buttonSetAvailability);

        backButton.setOnClickListener(v -> finish());
        addPhotoButton.setOnClickListener(v -> imagePicker.launch("image/*"));
        setAvailabilityButton.setOnClickListener(v -> selectAvailability());
        publishButton.setOnClickListener(v -> publishItem());
    }

    // Select a date first and then its time. Cancelling leaves the previous
    // valid availability unchanged.
    private interface OnDateTimeSelected {
        void onSelected(long timestamp);
    }

    private void pickDateTime(OnDateTimeSelected callback) {
        Calendar selectedDateTime = Calendar.getInstance();

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    selectedDateTime.set(year, month, day);

                    TimePickerDialog timePicker = new TimePickerDialog(
                            this,
                            (timeView, hour, minute) -> {
                                selectedDateTime.set(Calendar.HOUR_OF_DAY, hour);
                                selectedDateTime.set(Calendar.MINUTE, minute);
                                selectedDateTime.set(Calendar.SECOND, 0);
                                selectedDateTime.set(Calendar.MILLISECOND, 0);
                                callback.onSelected(
                                        selectedDateTime.getTimeInMillis()
                                );
                            },
                            selectedDateTime.get(Calendar.HOUR_OF_DAY),
                            selectedDateTime.get(Calendar.MINUTE),
                            true
                    );
                    timePicker.show();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    private void selectAvailability() {
        Toast.makeText(this, "Select availability start", Toast.LENGTH_SHORT).show();

        pickDateTime(startTime -> {
            if (startTime <= System.currentTimeMillis()) {
                Toast.makeText(this, "Start time must be in the future",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Select availability end", Toast.LENGTH_SHORT).show();
            pickDateTime(endTime -> {
                AvailabilitySlot slot = new AvailabilitySlot(startTime, endTime);
                if (!slot.isValid()) {
                    Toast.makeText(this, "End time must be after start time",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                selectedAvailability = slot;
                availabilitySummary.setError(null);

                SimpleDateFormat formatter = new SimpleDateFormat(
                        "dd MMM yyyy, h:mm a", Locale.getDefault()
                );
                availabilitySummary.setText(
                        "Available from: " + formatter.format(new Date(startTime))
                                + "\nUntil: " + formatter.format(new Date(endTime))
                );
            });
        });
    }

    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetMultipleContents(),
                    uris -> {
                        if (uris == null) {
                            return;
                        }

                        for (Uri uri : uris) {
                            if (selectedImages.size() >= 6) {
                                break;
                            }
                            if (!selectedImages.contains(uri)) {
                                selectedImages.add(uri);
                            }
                        }

                        updatePhotoPreviews();
                        if (selectedImages.size() >= 6) {
                            Toast.makeText(this, "Maximum 6 photos",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    private void publishItem() {
        if (publishing) {
            return;
        }

        String nameText = itemName.getText().toString().trim();
        String descriptionText = description.getText().toString().trim();
        String categoryText = category.getText().toString().trim();
        String priceText = price.getText().toString().trim();
        String locationText = location.getText().toString().trim();
        boolean valid = true;

        if (nameText.isEmpty()) {
            itemName.setError("Item name is required");
            valid = false;
        }
        if (descriptionText.isEmpty()) {
            description.setError("Description is required");
            valid = false;
        }
        if (categoryText.isEmpty()) {
            category.setError("Category is required");
            valid = false;
        }
        if (locationText.isEmpty()) {
            location.setError("Suburb is required");
            valid = false;
        } else if (locationText.matches(".*\\d+.*") || locationText.contains("@")) {
            location.setError("Enter suburb only, never a street number or home address");
            valid = false;
        }

        double priceValue = 0;
        if (priceText.isEmpty()) {
            price.setError("Price is required");
            valid = false;
        } else {
            try {
                priceValue = Double.parseDouble(priceText);
                if (Double.isNaN(priceValue) || Double.isInfinite(priceValue) || priceValue <= 0) {
                    price.setError("Price must be greater than 0");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                price.setError("Enter a valid price");
                valid = false;
            }
        }

        if (selectedAvailability == null
                || !selectedAvailability.isValid()
                || selectedAvailability.getStartTime() <= System.currentTimeMillis()) {
            availabilitySummary.setError("Please select valid future availability");
            Toast.makeText(this, "Please set valid future availability",
                    Toast.LENGTH_SHORT).show();
            valid = false;
        }

        if (!valid) {
            return;
        }

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in before publishing an item",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        setPublishing(true);
        uploadImagesAndPublish(
                currentUser.getUid(), nameText, descriptionText,
                categoryText, priceValue, locationText
        );
    }

    private void setPublishing(boolean inProgress) {
        publishing = inProgress;
        publishButton.setEnabled(!inProgress);
        publishButton.setText(inProgress ? "Publishing..." : "Publish item");
    }

    private void uploadImagesAndPublish(
            String ownerId,
            String name,
            String descriptionText,
            String categoryText,
            double priceValue,
            String locationText
    ) {
        String itemId = itemRepository.generateItemId();
        ArrayList<Uri> imagesToUpload = new ArrayList<>(selectedImages);
        ArrayList<String> imageUrls = new ArrayList<>();

        if (imagesToUpload.isEmpty()) {
            saveItem(new Item(
                    itemId, ownerId, name, descriptionText,
                    categoryText, priceValue, locationText, imageUrls
            ));
            return;
        }

        for (int i = 0; i < imagesToUpload.size(); i++) {
            imageUrls.add(null);
        }

        final int[] completedCount = {0};
        final boolean[] uploadFailed = {false};

        for (int i = 0; i < imagesToUpload.size(); i++) {
            final int index = i;
            Uri imageUri = imagesToUpload.get(i);
            StorageReference imageReference = firebaseStorage
                    .getReference()
                    .child("items")
                    .child(itemId)
                    .child("image_" + index + ".jpg");

            imageReference.putFile(imageUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            Exception error = task.getException();
                            throw error != null ? error :
                                    new IllegalStateException("Image upload failed");
                        }
                        return imageReference.getDownloadUrl();
                    })
                    .addOnSuccessListener(downloadUri -> {
                        if (uploadFailed[0]) {
                            return;
                        }
                        imageUrls.set(index, downloadUri.toString());
                        completedCount[0]++;

                        if (completedCount[0] == imagesToUpload.size()) {
                            saveItem(new Item(
                                    itemId, ownerId, name, descriptionText,
                                    categoryText, priceValue, locationText, imageUrls
                            ));
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!uploadFailed[0]) {
                            uploadFailed[0] = true;
                            setPublishing(false);
                            Toast.makeText(this,
                                    "Failed to upload image: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void saveItem(Item item) {
        // The repository reads this slot and writes availabilityStart/
        // availabilityEnd to Firestore in the same item document.
        item.setAvailabilitySlot(selectedAvailability);

        itemRepository.addItem(item, new ItemRepository.OnItemAddedListener() {
            @Override
            public void onSuccess() {
                // Success must only be reported after Firestore confirms the write.
                Toast.makeText(AddItemActivity.this,
                        "Item published successfully", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                setPublishing(false);
                Toast.makeText(AddItemActivity.this,
                        "Failed to publish item: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updatePhotoPreviews() {
        photoPreviewContainer.removeAllViews();
        if (selectedImages.isEmpty()) {
            photoPreviewScroll.setVisibility(View.GONE);
            return;
        }

        photoPreviewScroll.setVisibility(View.VISIBLE);
        for (Uri uri : selectedImages) {
            FrameLayout photoFrame = new FrameLayout(this);
            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(
                    dpToPx(100), dpToPx(100)
            );
            frameParams.setMargins(0, 0, dpToPx(12), 0);
            photoFrame.setLayoutParams(frameParams);

            ImageView imageView = new ImageView(this);
            imageView.setImageURI(uri);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));

            TextView deleteButton = new TextView(this);
            deleteButton.setText("✕");
            deleteButton.setTextSize(16);
            deleteButton.setGravity(Gravity.CENTER);
            deleteButton.setTextColor(getColor(android.R.color.white));
            deleteButton.setBackgroundColor(0x99000000);

            FrameLayout.LayoutParams deleteParams = new FrameLayout.LayoutParams(
                    dpToPx(30), dpToPx(30)
            );
            deleteParams.gravity = Gravity.TOP | Gravity.END;
            deleteButton.setLayoutParams(deleteParams);
            deleteButton.setOnClickListener(v -> {
                selectedImages.remove(uri);
                updatePhotoPreviews();
            });

            photoFrame.addView(imageView);
            photoFrame.addView(deleteButton);
            photoPreviewContainer.addView(photoFrame);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
