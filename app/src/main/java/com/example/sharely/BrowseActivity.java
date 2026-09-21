package com.example.sharely;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.widget.Button;

public class BrowseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_browse);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.browseMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Button viewDrillButton = findViewById(R.id.buttonViewDrill);

        viewDrillButton.setOnClickListener(v -> {
            Intent intent = new Intent(BrowseActivity.this, ItemDetailsActivity.class);

            intent.putExtra("name", "Cordless Drill");
            intent.putExtra("price", "$5 per day");
            intent.putExtra("info", "1.2 km away • ★ 4.8");
            intent.putExtra(
                    "description",
                    "A reliable cordless drill suitable for small home projects."
            );

            startActivity(intent);
        });

        Button viewTentButton = findViewById(R.id.buttonViewTent);

        viewTentButton.setOnClickListener(v -> {
            Intent intent = new Intent(BrowseActivity.this, ItemDetailsActivity.class);

            intent.putExtra("name", "Camping Tent");
            intent.putExtra("price", "$8 per day");
            intent.putExtra("info", "2.4 km away • ★ 4.6");
            intent.putExtra(
                    "description",
                    "A spacious camping tent suitable for weekend trips."
            );

            startActivity(intent);
        });

        Button backButton = findViewById(R.id.buttonBack);

        backButton.setOnClickListener(v -> {
            finish();
        });
    }
}