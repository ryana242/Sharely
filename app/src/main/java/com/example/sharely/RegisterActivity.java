package com.example.sharely;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        TextView registerButton = findViewById(R.id.registerButton);
        TextView backButton = findViewById(R.id.backButton);

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        registerButton.setOnClickListener(v -> registerUser());
        backButton.setOnClickListener(v -> finish());
    }

    private void registerUser() {

        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                    this,
                    "Please fill in all fields",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser firebaseUser =
                                firebaseAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            saveUserToFirestore(
                                    firebaseUser.getUid(),
                                    name,
                                    email
                            );
                        }

                    } else {

                        Toast.makeText(
                                this,
                                "Registration failed: "
                                        + task.getException().getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void saveUserToFirestore(
            String uid,
            String name,
            String email
    ) {

        Map<String, Object> user = new HashMap<>();

        user.put("name", name);
        user.put("email", email);
        user.put("rating", 0);
        user.put("transactionCount", 0);

        db.collection("users")
                .document(uid)
                .set(user)
                .addOnSuccessListener(unused -> {

                    FirebaseUser firebaseUser =
                            firebaseAuth.getCurrentUser();

                    if (firebaseUser != null) {

                        firebaseUser.sendEmailVerification()
                                .addOnCompleteListener(task -> {

                                    if (task.isSuccessful()) {

                                        Toast.makeText(
                                                RegisterActivity.this,
                                                "Registration successful. Please check your email to verify your account.",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        // Log the user out until email is verified
                                        firebaseAuth.signOut();

                                        // Go back to login page
                                        Intent intent = new Intent(
                                                RegisterActivity.this,
                                                LoginActivity.class
                                        );

                                        startActivity(intent);
                                        finish();

                                    } else {

                                        Toast.makeText(
                                                RegisterActivity.this,
                                                "Failed to send verification email",
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            RegisterActivity.this,
                            "Failed to save user: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}