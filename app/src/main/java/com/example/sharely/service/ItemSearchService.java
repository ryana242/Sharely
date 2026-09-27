package com.example.sharely.service;

import java.util.Locale;

public class ItemSearchService {

    public boolean matchesKeyword(
            String itemName,
            String query
    ) {

        if (query == null || query.trim().isEmpty()) {
            return true;
        }

        String normalizedQuery =
                query.trim().toLowerCase(Locale.ROOT);

        return containsIgnoreCase(
                itemName,
                normalizedQuery
        );
    }


    public boolean matchesCategory(
            String itemCategory,
            String selectedCategory
    ) {

        if (selectedCategory == null
                || selectedCategory.trim().isEmpty()) {
            return true;
        }

        if (itemCategory == null) {
            return false;
        }

        return itemCategory.equalsIgnoreCase(
                selectedCategory.trim()
        );
    }


    public boolean matchesMaxPrice(
            double pricePerDay,
            Double maxPrice
    ) {

        if (maxPrice == null) {
            return true;
        }

        return pricePerDay <= maxPrice;
    }


    public boolean matchesMaxDistance(
            double distanceKm,
            Double maxDistanceKm
    ) {

        if (maxDistanceKm == null) {
            return true;
        }

        return distanceKm <= maxDistanceKm;
    }


    private boolean containsIgnoreCase(
            String value,
            String normalizedQuery
    ) {

        if (value == null) {
            return false;
        }

        return value
                .toLowerCase(Locale.ROOT)
                .contains(normalizedQuery);
    }
}