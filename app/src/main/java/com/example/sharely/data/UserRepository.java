package com.example.sharely.data;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
public class UserRepository {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public UserRepository() {

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    // =========================================================
    // GET CURRENT USER ID
    // =========================================================

    public String getCurrentUserId() {

        FirebaseUser currentUser =

                auth.getCurrentUser();

        if (currentUser == null) {

            return null;

        }

        return currentUser.getUid();

    }

    // =========================================================
    // CHECK LOGIN
    // =========================================================

    public boolean isLoggedIn() {

        return auth.getCurrentUser() != null;

    }

}