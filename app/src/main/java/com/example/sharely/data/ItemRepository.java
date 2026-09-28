package com.example.sharely.data;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

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
            String ownerId,
            String itemName,
            String description,
            String category,
            double price,
            String location
    ) {

        // Generate a new document ID
        String itemId = db.collection("items")
                .document()
                .getId();

        // Create item data
        Map<String, Object> item = new HashMap<>();

        item.put("itemId", itemId);
        item.put("ownerId", ownerId);
        item.put("itemName", itemName);
        item.put("description", description);
        item.put("category", category);
        item.put("price", price);
        item.put("location", location);
        // Save to Firestore
        db.collection("items")
                .document(itemId)
                .set(item)
                .addOnSuccessListener(unused -> {

                    Log.d("Firestore", "Item added: " + itemId);

                })
                .addOnFailureListener(e -> {

                    Log.e("Firestore", "Failed to add item", e);

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

    public void getItemsByCategory(
            String category,
            OnItemsLoadedListener listener
    ) {

        db.collection("items")
                .whereEqualTo("category", category)
                .get()
                .addOnSuccessListener(listener::onSuccess)
                .addOnFailureListener(listener::onFailure);
    }

    public void getItemsByMaxPrice(
            double maxPrice,
            OnItemsLoadedListener listener
    ) {

        db.collection("items")
                .whereLessThanOrEqualTo("price", maxPrice)
                .get()
                .addOnSuccessListener(listener::onSuccess)
                .addOnFailureListener(listener::onFailure);
    }
}