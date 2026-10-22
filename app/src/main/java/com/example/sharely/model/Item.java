package com.example.sharely.model;

import com.example.sharely.enums.ItemStatus;

import java.util.ArrayList;

/** A Sharely listing and its owner-defined availability. */
public class Item {

    private String itemId;
    private String ownerId;
    private String itemName;
    private String description;
    private String category;
    private double price;
    private String location;
    private ArrayList<String> imageUrls;
    private ItemStatus status;

    // One source of truth for both feature branches.
    // May be null when loading older listings from Firestore.
    private AvailabilitySlot availabilitySlot;

    // New listing constructor.
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
        this(itemId, ownerId, itemName, description, category,
                price, location, imageUrls, ItemStatus.ACTIVE);
    }

    // Constructor used when loading a listing from Firestore.
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

    // Leo's repository API: writes/reads Firestore availability timestamps.
    public AvailabilitySlot getAvailabilitySlot() {
        return availabilitySlot;
    }

    public void setAvailabilitySlot(AvailabilitySlot slot) {
        if (slot == null || !slot.isValid()) {
            throw new IllegalArgumentException(
                    "Item availability must be a valid time period"
            );
        }
        availabilitySlot = slot;
    }

    // Compatibility with Ryana's existing code and unit tests.
    public AvailabilitySlot getAvailability() {
        return getAvailabilitySlot();
    }

    public void setAvailability(AvailabilitySlot slot) {
        setAvailabilitySlot(slot);
    }

    public void setUnavailable() {
        if (status == ItemStatus.PAST) {
            throw new IllegalStateException(
                    "A taken-down item cannot be made unavailable"
            );
        }
        status = ItemStatus.UNAVAILABLE;
    }

    public void setActive() {
        if (status == ItemStatus.PAST) {
            throw new IllegalStateException(
                    "A taken-down item cannot be reactivated"
            );
        }
        status = ItemStatus.ACTIVE;
    }

    public void setPast() {
        status = ItemStatus.PAST;
    }
}
