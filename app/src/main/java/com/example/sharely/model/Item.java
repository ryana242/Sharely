
package com.example.sharely.model;

import java.util.ArrayList;

import com.example.sharely.enums.ItemStatus;

public class Item {

    private String itemId;
    private String ownerId;
    private String itemName;
    private String description;
    private String category;
    private double price;
    private String location;
    private ArrayList<String> imageUrls;

    private AvailabilitySlot availability;
    private ItemStatus status;


    // Constructor for new listings
    public Item(
            String itemId,
            String ownerId,
            String itemName,
            String description,
            String category,
            double price,
            String location,
            ArrayList<String> imageUrls
    ) {

        this.itemId = itemId;
        this.ownerId = ownerId;
        this.itemName = itemName;
        this.description = description;
        this.category = category;
        this.price = price;
        this.location = location;
        this.imageUrls = imageUrls;

        this.status = ItemStatus.ACTIVE;
    }


    // Constructor for items loaded from Firestore
    public Item(
            String itemId,
            String ownerId,
            String itemName,
            String description,
            String category,
            double price,
            String location,
            ArrayList<String> imageUrls,
            ItemStatus status
    ) {

        this.itemId = itemId;
        this.ownerId = ownerId;
        this.itemName = itemName;
        this.description = description;
        this.category = category;
        this.price = price;
        this.location = location;
        this.imageUrls = imageUrls;
        this.status = status;
    }


    // Getters
    public String getItemId() {
        return itemId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public double getPrice() {
        return price;
    }

    public String getLocation() {
        return location;
    }

    public ArrayList<String> getImageUrls() {
        return imageUrls;
    }

    public ItemStatus getStatus() {
        return status;
    }


    // Availability
    public AvailabilitySlot getAvailability() {
        return availability;
    }

    public void setAvailability(AvailabilitySlot availability) {

        if (availability == null || !availability.isValid()) {
            throw new IllegalArgumentException(
                    "Item availability must be a valid time period"
            );
        }

        this.availability = availability;
    }


    // Temporarily make a listing unavailable
    public void setUnavailable() {

        if (status == ItemStatus.PAST) {
            throw new IllegalStateException(
                    "A taken-down item cannot be made unavailable"
            );
        }

        status = ItemStatus.UNAVAILABLE;
    }


    // Activate or reactivate a listing
    public void setActive() {

        if (status == ItemStatus.PAST) {
            throw new IllegalStateException(
                    "A taken-down item cannot be reactivated"
            );
        }

        status = ItemStatus.ACTIVE;
    }


    // Permanently take down a listing
    public void setPast() {

        status = ItemStatus.PAST;
    }
}
