package com.example.sharely;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AddItemActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_item);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.addItemMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button backButton = findViewById(R.id.buttonBack);

        backButton.setOnClickListener(v -> {
            finish();
        });
        EditText itemName = findViewById(R.id.editItemName);
        EditText description = findViewById(R.id.editDescription);
        EditText category = findViewById(R.id.editCategory);
        EditText price = findViewById(R.id.editPrice);
        EditText location = findViewById(R.id.editLocation);

        Button publishButton = findViewById(R.id.buttonPublishItem);

        publishButton.setOnClickListener(v -> {

            String itemNameText = itemName.getText().toString().trim();
            String descriptionText = description.getText().toString().trim();
            String categoryText = category.getText().toString().trim();
            String priceText = price.getText().toString().trim();
            String locationText = location.getText().toString().trim();

            boolean isValid = true;

            if (itemNameText.isEmpty()) {
                itemName.setError("Item name is required");
                isValid = false;
            }

            if (descriptionText.isEmpty()) {
                description.setError("Description is required");
                isValid = false;
            }

            if (categoryText.isEmpty()) {
                category.setError("Category is required");
                isValid = false;
            }

            if (priceText.isEmpty()) {
                price.setError("Price is required");
                isValid = false;
            } else {
                try {
                    double priceValue = Double.parseDouble(priceText);

                    if (priceValue <= 0) {
                        price.setError("Price must be greater than 0");
                        isValid = false;
                    }

                } catch (NumberFormatException e) {
                    price.setError("Enter a valid price");
                    isValid = false;
                }
            }

            if (locationText.isEmpty()) {
                location.setError("Location is required");
                isValid = false;
            }

            if (!isValid) {
                return;
            }

            Toast.makeText(
                    AddItemActivity.this,
                    "Item ready to publish",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }
}