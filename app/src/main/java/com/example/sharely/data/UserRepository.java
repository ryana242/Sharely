package com.example.sharely.data;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
public class UserRepository {

    private final FirebaseFirestore db;

    public UserRepository() {

        db = FirebaseFirestore.getInstance();

    }

    public void getUser(String userId) {

        db.collection("users")

                .document(userId)

                .get()

                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        String name = document.getString("name");
                        Log.d("Firestore", "User name: " + name);
                    }

                });

    }

}