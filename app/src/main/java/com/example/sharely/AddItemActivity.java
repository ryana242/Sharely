package com.example.sharely;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sharely.data.ItemRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class AddItemActivity extends AppCompatActivity {
    private EditText itemName;
    private EditText description;
    private EditText category;
    private EditText price;
    private EditText location;
    //Create imagesList
    private final ArrayList<Uri> selectedImages = new ArrayList<>();
    private LinearLayout photoPreviewContainer;
    private HorizontalScrollView photoPreviewScroll;
    private ItemRepository itemRepository;
    private FirebaseAuth firebaseAuth;
    private FirebaseStorage firebaseStorage;
    private Button publishButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_item);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.addItemMain),
                (v, insets) -> {

                    Insets systemBars =
                            insets.getInsets(WindowInsetsCompat.Type.systemBars());

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        // Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        itemRepository = new ItemRepository();

        // Views
        Button backButton = findViewById(R.id.buttonBack);
        publishButton = findViewById(R.id.buttonPublishItem);

        photoPreviewContainer = findViewById(R.id.photoPreviewContainer);
        photoPreviewScroll = findViewById(R.id.photoPreviewScroll);

        itemName = findViewById(R.id.editItemName);
        description = findViewById(R.id.editDescription);
        category = findViewById(R.id.editCategory);
        price = findViewById(R.id.editPrice);
        location = findViewById(R.id.editLocation);

        // Back button
        backButton.setOnClickListener(v -> finish());

        // Publish button
        publishButton.setOnClickListener(v -> publishItem());

        //Add photo button
        Button addPhotoButton = findViewById(R.id.buttonAddPhoto);

        addPhotoButton.setOnClickListener(v -> {
            imagePicker.launch("image/*");
        });
    }

    // Open album
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
                            Toast.makeText(
                                    this,
                                    "Maximum 6 photos",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );

    private void publishItem() {

        String itemNameText =
                itemName.getText().toString().trim();

        String descriptionText =
                description.getText().toString().trim();

        String categoryText =
                category.getText().toString().trim();

        String priceText =
                price.getText().toString().trim();

        String locationText =
                location.getText().toString().trim();

        boolean isValid = true;

        // Item name
        if (itemNameText.isEmpty()) {
            itemName.setError("Item name is required");
            isValid = false;
        }

        // Description
        if (descriptionText.isEmpty()) {
            description.setError("Description is required");
            isValid = false;
        }

        // Category
        if (categoryText.isEmpty()) {
            category.setError("Category is required");
            isValid = false;
        }

        // Location
        if (locationText.isEmpty()) {
            location.setError("Location is required");
            isValid = false;
        }

        // Price
        double priceValue = 0;

        if (priceText.isEmpty()) {

            price.setError("Price is required");
            isValid = false;

        } else {

            try {

                priceValue = Double.parseDouble(priceText);

                if (priceValue <= 0) {
                    price.setError("Price must be greater than 0");
                    isValid = false;
                }

            } catch (NumberFormatException e) {

                price.setError("Enter a valid price");
                isValid = false;
            }
        }

        if (!isValid) {
            return;
        }

        // Get current logged-in user
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "Please log in before publishing an item",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String ownerId = currentUser.getUid();

// Change button while publishing
        publishButton.setEnabled(false);
        publishButton.setText("Publishing...");
        uploadImagesAndPublish(
                ownerId,
                itemNameText,
                descriptionText,
                categoryText,
                priceValue,
                locationText
        );
    }

    private void uploadImagesAndPublish(
            String ownerId,
            String itemName,
            String description,
            String category,
            double price,
            String location
    ) {

        String itemId =
                itemRepository.generateItemId();

        ArrayList<String> imageUrls =
                new ArrayList<>();


        // No images
        if (selectedImages.isEmpty()) {

            itemRepository.addItem(
                    itemId,
                    ownerId,
                    itemName,
                    description,
                    category,
                    price,
                    location,
                    imageUrls
            );

            Toast.makeText(
                    this,
                    "Item published successfully",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

            return;
        }


        // Prepare URL list
        for (int i = 0; i < selectedImages.size(); i++) {
            imageUrls.add(null);
        }


        // Number of uploaded images
        final int[] completedCount = {0};


        // Upload all images at the same time
        for (int i = 0; i < selectedImages.size(); i++) {

            int index = i;

            Uri imageUri =
                    selectedImages.get(index);


            StorageReference imageReference =
                    firebaseStorage
                            .getReference()
                            .child("items")
                            .child(itemId)
                            .child("image_" + index + ".jpg");


            imageReference
                    .putFile(imageUri)

                    .continueWithTask(task -> {

                        if (!task.isSuccessful()) {

                            throw task.getException();
                        }

                        return imageReference.getDownloadUrl();
                    })

                    .addOnSuccessListener(downloadUri -> {

                        // Save URL in correct position
                        imageUrls.set(
                                index,
                                downloadUri.toString()
                        );


                        completedCount[0]++;


                        // All images uploaded
                        if (completedCount[0] == selectedImages.size()) {

                            itemRepository.addItem(
                                    itemId,
                                    ownerId,
                                    itemName,
                                    description,
                                    category,
                                    price,
                                    location,
                                    imageUrls
                            );


                            Toast.makeText(
                                    this,
                                    "Item published successfully",
                                    Toast.LENGTH_SHORT
                            ).show();


                            finish();
                        }
                    })

                    .addOnFailureListener(e -> {

                        publishButton.setEnabled(true);
                        publishButton.setText("Publish item");

                        Toast.makeText(
                                this,
                                "Failed to upload image: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    });
        }
    }


    private void updatePhotoPreviews() {

        // Remove old previews
        photoPreviewContainer.removeAllViews();

        // No photos selected
        if (selectedImages.isEmpty()) {
            photoPreviewScroll.setVisibility(View.GONE);
            return;
        }

        photoPreviewScroll.setVisibility(View.VISIBLE);

        for (Uri uri : selectedImages) {

            // Container for one photo
            FrameLayout photoFrame = new FrameLayout(this);

            LinearLayout.LayoutParams frameParams =
                    new LinearLayout.LayoutParams(
                            dpToPx(100),
                            dpToPx(100)
                    );

            frameParams.setMargins(
                    0,
                    0,
                    dpToPx(12),
                    0
            );

            photoFrame.setLayoutParams(frameParams);


            // IMAGE
            ImageView imageView =
                    new ImageView(this);

            imageView.setImageURI(uri);

            imageView.setScaleType(
                    ImageView.ScaleType.CENTER_CROP
            );

            FrameLayout.LayoutParams imageParams =
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                    );

            imageView.setLayoutParams(imageParams);


            // DELETE BUTTON
            TextView deleteButton =
                    new TextView(this);

            deleteButton.setText("✕");
            deleteButton.setTextSize(16);
            deleteButton.setGravity(Gravity.CENTER);

            deleteButton.setTextColor(
                    getColor(android.R.color.white)
            );

            deleteButton.setBackgroundColor(
                    0x99000000
            );

            FrameLayout.LayoutParams deleteParams =
                    new FrameLayout.LayoutParams(
                            dpToPx(30),
                            dpToPx(30)
                    );

            deleteParams.gravity =
                    Gravity.TOP | Gravity.END;

            deleteButton.setLayoutParams(deleteParams);


            // REMOVE PHOTO
            deleteButton.setOnClickListener(v -> {

                selectedImages.remove(uri);

                updatePhotoPreviews();
            });
            // ADD VIEWS
            photoFrame.addView(imageView);
            photoFrame.addView(deleteButton);

            photoPreviewContainer.addView(photoFrame);
        }
    };

    // Convert dp to pixels
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
