package com.example.sharely;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.widget.Button;

import android.widget.LinearLayout;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homeMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout browseButton =
                findViewById(R.id.buttonBrowseHome);

        LinearLayout listButton =
                findViewById(R.id.buttonListHome);

        browseButton.setOnClickListener(v -> {
            Intent intent =
                    new Intent(HomeActivity.this, BrowseActivity.class);

            startActivity(intent);
        });

        listButton.setOnClickListener(v -> {
            Intent intent =
                    new Intent(HomeActivity.this, AddItemActivity.class);

            startActivity(intent);
        });
    }
}