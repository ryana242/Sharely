package com.example.sharely;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sharely.data.ItemRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AddItemActivity extends AppCompatActivity {

    private EditText itemName;
    private EditText description;
    private EditText category;
    private EditText price;
    private EditText location;

    private ItemRepository itemRepository;
    private FirebaseAuth firebaseAuth;

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
        itemRepository = new ItemRepository();

        // Views
        Button backButton = findViewById(R.id.buttonBack);
        Button publishButton = findViewById(R.id.buttonPublishItem);

        itemName = findViewById(R.id.editItemName);
        description = findViewById(R.id.editDescription);
        category = findViewById(R.id.editCategory);
        price = findViewById(R.id.editPrice);
        location = findViewById(R.id.editLocation);

        // Back button
        backButton.setOnClickListener(v -> finish());

        // Publish button
        publishButton.setOnClickListener(v -> publishItem());
    }

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

        // Add item to Firestore
        itemRepository.addItem(
                ownerId,
                itemNameText,
                descriptionText,
                categoryText,
                priceValue,
                locationText
        );

        Toast.makeText(
                this,
                "Item published successfully",
                Toast.LENGTH_SHORT
        ).show();

        finish();
    }
}