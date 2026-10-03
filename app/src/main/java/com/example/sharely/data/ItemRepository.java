package com.example.sharely.data;

import com.example.sharely.model.Item;
import com.example.sharely.enums.ItemStatus;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemRepository {

    private final FirebaseFirestore db;


    public ItemRepository() {

        db = FirebaseFirestore.getInstance();
    }

    // GET ONE ITEM
    public void getItem(
            String itemId,
            OnItemLoadedListener listener
    ) {
        db.collection("items")
                .document(itemId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        listener.onFailure(
                                new Exception(
                                        "Item not found"
                                )
                        );
                        return;
                    }
                    Item item = documentToItem( document );

                    listener.onSuccess(
                            item
                    );
                })
                .addOnFailureListener(
                        listener::onFailure
                );
    }

    // ADD ITEM
    public void addItem(
            Item item
    ) {

        Map<String, Object> itemData = new HashMap<>();

        itemData.put(
                "itemId",
                item.getItemId()
        );

        itemData.put(
                "ownerId",
                item.getOwnerId()
        );

        itemData.put(
                "itemName",
                item.getItemName()
        );

        itemData.put(
                "description",
                item.getDescription()
        );

        itemData.put(
                "category",
                item.getCategory()
        );

        itemData.put(
                "price",
                item.getPrice()
        );

        itemData.put(
                "location",
                item.getLocation()
        );

        itemData.put(
                "imageUrls",
                item.getImageUrls()
        );

        itemData.put(
                "status",
                item.getStatus().name()
        );


        db.collection("items")
                .document(item.getItemId())
                .set(
                        itemData
                );
    }

    // CALL BACK
    public interface OnItemsLoadedListener {

        void onSuccess(
                List<Item> items
        );
        void onFailure(
                Exception e
        );

    }

    // CALLBACK FOR ONE ITEM
    public interface OnItemLoadedListener {

        void onSuccess(
                Item item
        );

        void onFailure(
                Exception e
        );
    }

    //GET ALL ITEMS

    public void getAllItems(
            OnItemsLoadedListener listener
    ) {

        db.collection("items")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Item> items =
                            new ArrayList<>();

                    for (
                            DocumentSnapshot document
                            : querySnapshot.getDocuments()
                    ) {
                        Item item =
                                documentToItem(
                                        document
                                );

                        items.add(
                                item
                        );
                    }

                    listener.onSuccess(
                            items
                    );
                })
                .addOnFailureListener(
                        listener::onFailure
                );
    }

    // GET ITEMS BY OWNER

    public void getItemsByOwner(
            String ownerId,
            OnOwnerItemsLoadedListener listener
    ) {

        db.collection("items")

                .whereEqualTo(
                        "ownerId",
                        ownerId
                )

                .get()

                .addOnSuccessListener(querySnapshot -> {

                    List<Item> items =
                            new ArrayList<>();


                    for (
                            DocumentSnapshot document
                            : querySnapshot.getDocuments()
                    ) {

                        Item item =
                                documentToItem(
                                        document
                                );


                        items.add(
                                item
                        );
                    }


                    listener.onSuccess(
                            items
                    );
                })

                .addOnFailureListener(
                        listener::onFailure
                );
    }

    // CALLBACK FOR OWNER ITEMS

    public interface OnOwnerItemsLoadedListener {

        void onSuccess(
                List<Item> items
        );

        void onFailure(
                Exception e
        );
    }

    public String generateItemId() {

        return db.collection("items")
                .document()
                .getId();
    }
    // CONVERT DOCUMENT TO ITEM

    private Item documentToItem(
            DocumentSnapshot document
    ) {

        // IMAGE URLS

        ArrayList<String> imageUrls =
                new ArrayList<>();


        Object imageData =
                document.get(
                        "imageUrls"
                );


        if (imageData instanceof List<?>) {

            for (
                    Object imageUrl
                    : (List<?>) imageData
            ) {

                if (imageUrl instanceof String) {

                    imageUrls.add(
                            (String) imageUrl
                    );
                }
            }
        }


        // STATUS

        String statusText =
                document.getString(
                        "status"
                );


        ItemStatus status =
                ItemStatus.ACTIVE;


        if (statusText != null) {

            try {

                status =
                        ItemStatus.valueOf(
                                statusText
                        );

            } catch (
                    IllegalArgumentException e
            ) {

                status =
                        ItemStatus.ACTIVE;
            }
        }


        // PRICE

        Double priceValue =
                document.getDouble(
                        "price"
                );


        double price =
                priceValue == null
                        ? 0.0
                        : priceValue;


        // CREATE ITEM

        return new Item(
                document.getString("itemId"),
                document.getString("ownerId"),
                document.getString("itemName"),
                document.getString("description"),
                document.getString("category"),
                price,
                document.getString("location"),
                imageUrls,
                status
        );
    }

    // UPDATE ITEM STATUS

    public void updateItemStatus(
            String itemId,
            ItemStatus status,
            OnItemStatusUpdatedListener listener
    ) {

        db.collection("items")
                .document(itemId)
                .update(
                        "status",
                        status.name()
                )
                .addOnSuccessListener(unused -> {

                    listener.onSuccess();
                })
                .addOnFailureListener(
                        listener::onFailure
                );
    }
    // CALLBACK FOR ITEM STATUS UPDATE

    public interface OnItemStatusUpdatedListener {

        void onSuccess();

        void onFailure(
                Exception e
        );
    }

}