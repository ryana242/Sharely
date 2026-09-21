package com.example.sharely;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;

public class ItemDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_item_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.itemDetailsMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        TextView itemName = findViewById(R.id.textItemName);
        TextView itemPrice = findViewById(R.id.textItemPrice);
        TextView itemInfo = findViewById(R.id.textItemInfo);
        TextView itemDescription = findViewById(R.id.textItemDescription);

        Intent intent = getIntent();

        itemName.setText(intent.getStringExtra("name"));
        itemPrice.setText(intent.getStringExtra("price"));
        itemInfo.setText(intent.getStringExtra("info"));
        itemDescription.setText(intent.getStringExtra("description"));
        Button backButton = findViewById(R.id.buttonBack);

        backButton.setOnClickListener(v -> {
            finish();
        });
    }
}