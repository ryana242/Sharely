package com.example.sharely.data;

import com.example.sharely.model.Item;
import com.example.sharely.enums.ItemStatus;
import com.example.sharely.model.AvailabilitySlot;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;

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
            Item item,
            OnItemAddedListener listener
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

        // AVAILABILITY

        AvailabilitySlot availability =
                item.getAvailabilitySlot();

        if (availability != null) {

            if (!availability.isValid()) {
                throw new IllegalArgumentException(
                        "Invalid item availability"
                );
            }

            itemData.put(
                    "availabilityStart",
                    availability.getStartTime()
            );

            itemData.put(
                    "availabilityEnd",
                    availability.getEndTime()
            );
        }


        db.collection("items")
                .document(item.getItemId())
                .set(itemData)

                .addOnSuccessListener(unused -> {
                    listener.onSuccess();
                })

                .addOnFailureListener(
                        listener::onFailure
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

    // CALLBACK FOR ADD ITEM

    public interface OnItemAddedListener {

        void onSuccess();

        void onFailure(Exception e);
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
        Item item = new Item(
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

        // LOAD AVAILABILITY
        Long startTime =
                document.getLong(
                        "availabilityStart"
                );

        Long endTime =
                document.getLong(
                        "availabilityEnd"
                );

        if (startTime != null && endTime != null) {
            AvailabilitySlot availability =
                    new AvailabilitySlot(
                            startTime,
                            endTime
                    );
            if (availability.isValid()) {
                item.setAvailabilitySlot(
                        availability
                );
            }
        }
        return item;

    }

    // UPDATE ITEM STATUS

    public void updateItemStatus(
            String itemId,
            ItemStatus newStatus,
            OnItemStatusUpdatedListener listener
    ) {

        DocumentReference itemRef =
                db.collection("items")
                        .document(itemId);

        db.runTransaction(transaction -> {

            // Get current item
            DocumentSnapshot document =
                    transaction.get(itemRef);

            if (!document.exists()) {
                throw new IllegalStateException(
                        "Item not found"
                );
            }

            // Get current status
            String statusString =
                    document.getString("status");

            if (statusString == null) {
                throw new IllegalStateException(
                        "Item status not found"
                );
            }

            ItemStatus currentStatus;

            try {
                currentStatus =
                        ItemStatus.valueOf(statusString);

            } catch (IllegalArgumentException e) {

                throw new IllegalStateException(
                        "Invalid item status"
                );
            }

            // Prevent reactivating permanently removed items
            if (currentStatus == ItemStatus.PAST) {

                throw new IllegalStateException(
                        "Permanently removed items cannot be reactivated"
                );
            }

            // Update status
            transaction.update(
                    itemRef,
                    "status",
                    newStatus.name()
            );

            return null;

        }).addOnSuccessListener(unused -> {

            listener.onSuccess();

        }).addOnFailureListener(
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