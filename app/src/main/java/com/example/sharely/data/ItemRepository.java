package com.example.sharely.data;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
public class ItemRepository {

    private final FirebaseFirestore db;

    public ItemRepository() {

        db = FirebaseFirestore.getInstance();

    }

    public void getItem(String userId) {

        db.collection("items")

                .document(userId)

                .get()

                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        String name = document.getString("item");
                        Log.d("Firestore", "Item name: " + name);
                    }

                });

    }

}