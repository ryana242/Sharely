package com.example.sharely;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();

//        Button browseButton = findViewById(R.id.buttonBrowse);
//        Button listItemButton = findViewById(R.id.buttonListItem);
        Button loginButton = findViewById(R.id.buttonLogin);
        Button registerButton = findViewById(R.id.buttonRegister);

        // Browse does not require login
//        browseButton.setOnClickListener(v -> {
//
//            Intent intent = new Intent(
//                    MainActivity.this,
//                    BrowseActivity.class
//            );
//
//            startActivity(intent);
//        });

        // Listing an item requires login
//        listItemButton.setOnClickListener(v -> {
//
//            FirebaseUser user = firebaseAuth.getCurrentUser();
//
//            if (user != null && user.isEmailVerified()) {
//
//                Intent intent = new Intent(
//                        MainActivity.this,
//                        AddItemActivity.class
//                );
//
//                startActivity(intent);
//
//            } else {
//
//                Toast.makeText(
//                        MainActivity.this,
//                        "Please login before listing an item",
//                        Toast.LENGTH_SHORT
//                ).show();
//
//                Intent intent = new Intent(
//                        MainActivity.this,
//                        LoginActivity.class
//                );
//
//                startActivity(intent);
//            }
//        });

        // Login
        loginButton.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    LoginActivity.class
            );

            startActivity(intent);
        });

        // Register
        registerButton.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    RegisterActivity.class
            );

            startActivity(intent);
        });

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }
}