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
    private ItemStatus status;
    private AvailabilitySlot availabilitySlot;


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
        // New items are active by default
        this.status = ItemStatus.ACTIVE;
    }

    // =========================================================
    // CONSTRUCTOR FOR ITEM LOADED FROM FIRESTORE
    // =========================================================

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

    public AvailabilitySlot getAvailabilitySlot() {
        return availabilitySlot;
    }

    public void setAvailabilitySlot(
            AvailabilitySlot availabilitySlot
    ) {
        this.availabilitySlot = availabilitySlot;
    }


    public ArrayList<String> getImageUrls() {
        return imageUrls;
    }
    public ItemStatus getStatus() {

        return status;

    }

    public void setUnavailable() {

        if (status != ItemStatus.PAST) {
            status = ItemStatus.UNAVAILABLE;
        }

    }

    public void setActive() {

        if (status != ItemStatus.PAST) {
            status = ItemStatus.ACTIVE;
        }

    }

    public void setPast() {

        status = ItemStatus.PAST;

    }

}