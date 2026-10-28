package com.example.sharely;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private TextView registerButton;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);
        android.view.View backButton = findViewById(R.id.backButton);

        registerButton.setOnClickListener(v -> registerUser());
        backButton.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (name.isEmpty()) {
            nameEditText.setError("Enter your display name");
            return;
        }
        if (name.length() > 40) {
            nameEditText.setError("Maximum 40 characters");
            return;
        }
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        registerButton.setEnabled(false);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        registerButton.setEnabled(true);
                        String reason = task.getException() == null
                                ? "Unknown error" : task.getException().getMessage();
                        Toast.makeText(this, "Registration failed: " + reason,
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        registerButton.setEnabled(true);
                        Toast.makeText(this, "Account created, but sign-in was lost. Please log in.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    saveAccount(user, name, email);
                });
    }

    private void saveAccount(FirebaseUser user, String name, String email) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("email", email);
        data.put("rating", 0);
        data.put("transactionCount", 0);

        // The Firestore name is Sharely's canonical display name, including chat.
        db.collection("users").document(user.getUid()).set(data)
                .addOnSuccessListener(unused -> {
                    UserProfileChangeRequest update =
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                    // Mirror the name to Firebase Auth for compatibility.
                    user.updateProfile(update).addOnCompleteListener(profileTask -> {
                        if (!profileTask.isSuccessful()) {
                            android.util.Log.w("SharelyRegister",
                                    "Auth display-name mirror failed; Firestore name is saved",
                                    profileTask.getException());
                        }
                        user.sendEmailVerification().addOnCompleteListener(mailTask -> {
                            registerButton.setEnabled(true);
                            if (mailTask.isSuccessful()) {
                                Toast.makeText(this,
                                        "Account created. Check your email to verify it.",
                                        Toast.LENGTH_LONG).show();
                                auth.signOut();
                                Intent intent = new Intent(this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(this,
                                        "Account saved, but verification email could not be sent. Please try again.",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    });
                })
                .addOnFailureListener(error -> {
                    registerButton.setEnabled(true);
                    Toast.makeText(this,
                            "Account created but profile could not be saved: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
