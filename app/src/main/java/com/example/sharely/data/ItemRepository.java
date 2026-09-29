package com.example.sharely.data;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ItemRepository {

    private final FirebaseFirestore db;


    public ItemRepository() {
        db = FirebaseFirestore.getInstance();
    }


    //  GET ONE ITEM
    public void getItem(String itemId) {

        db.collection("items")

                .document(itemId)

                .get()

                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        String itemName = document.getString("itemName");

                        Log.d("Firestore", "Item name: " + itemName);
                    }

                });

    }

    // ADD ITEM
    public void addItem(
            String itemId,
            String ownerId,
            String itemName,
            String description,
            String category,
            double price,
            String location,
            ArrayList<String> imageUrls
    ) {

        Map<String, Object> item = new HashMap<>();

        item.put("itemId", itemId);
        item.put("ownerId", ownerId);
        item.put("itemName", itemName);
        item.put("description", description);
        item.put("category", category);
        item.put("price", price);
        item.put("location", location);

        // Store image URLs
        item.put("imageUrls", imageUrls);


        db.collection("items")
                .document(itemId)
                .set(item)

                .addOnSuccessListener(unused -> {

                    Log.d(
                            "Firestore",
                            "Item added: " + itemId
                    );
                })

                .addOnFailureListener(e -> {

                    Log.e(
                            "Firestore",
                            "Failed to add item",
                            e
                    );
                });
    }

    // CALL BACK
    public interface OnItemsLoadedListener {

        void onSuccess(

                QuerySnapshot querySnapshot

        );

        void onFailure(

                Exception e

        );

    }

    //GET ALL ITEMS
    public void getAllItems(OnItemsLoadedListener listener) {

        db.collection("items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    listener.onSuccess(queryDocumentSnapshots);

                })
                .addOnFailureListener(e -> {

                    Log.e("Firestore", "Failed to get items", e);

                    listener.onFailure(e);

                });
    }
    public String generateItemId() {

        return db.collection("items")
                .document()
                .getId();
    }
}