package com.example.sharely.data;

import com.example.sharely.enums.ReviewRole;
import com.example.sharely.model.Review;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewRepository {

    private final FirebaseFirestore db;

    public ReviewRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // Convert Firestore document to Review object
    private Review documentToReview(DocumentSnapshot document) {

        String bookingId = document.getString("bookingId");
        String reviewerId = document.getString("reviewerId");
        String revieweeId = document.getString("revieweeId");
        String roleString = document.getString("revieweeRole");
        Long rating = document.getLong("rating");
        String comment = document.getString("comment");
        Long createdAt = document.getLong("createdAt");

        if (bookingId == null
                || reviewerId == null
                || revieweeId == null
                || roleString == null
                || rating == null
                || createdAt == null
                || comment == null) {
            return null;
        }

        ReviewRole role;

        try {
            role = ReviewRole.valueOf(roleString);
        } catch (IllegalArgumentException e) {
            return null;
        }

        if (rating < 1 || rating > 5) {
            return null;
        }

        return new Review(
                bookingId,
                reviewerId,
                revieweeId,
                role,
                rating.intValue(),
                comment,
                createdAt
        );
    }

    // Save a review without overwriting an existing review
    public void addReview(
            Review review,
            OnReviewSavedListener listener
    ) {

        if (review == null) {
            listener.onFailure(
                    new IllegalArgumentException("Review cannot be null")
            );
            return;
        }

        Map<String, Object> reviewData = new HashMap<>();

        reviewData.put("reviewId", review.getReviewId());
        reviewData.put("bookingId", review.getBookingId());
        reviewData.put("reviewerId", review.getReviewerId());
        reviewData.put("revieweeId", review.getRevieweeId());
        reviewData.put("revieweeRole", review.getRevieweeRole().name());
        reviewData.put("rating", review.getRating());
        reviewData.put("comment", review.getComment());
        reviewData.put("createdAt", review.getCreatedAt());

        db.runTransaction(transaction -> {

                    com.google.firebase.firestore.DocumentReference ref =
                            db.collection("reviews")
                                    .document(review.getReviewId());

                    DocumentSnapshot existing = transaction.get(ref);

                    if (existing.exists()) {
                        throw new IllegalStateException(
                                "You have already reviewed this booking"
                        );
                    }

                    transaction.set(ref, reviewData);

                    return null;

                }).addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    // Get reviews received by a user
    public void getReviewsByUser(
            String userId,
            OnReviewsLoadedListener listener
    ) {

        db.collection("reviews")
                .whereEqualTo("revieweeId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    List<Review> reviews = new ArrayList<>();

                    for (DocumentSnapshot document
                            : querySnapshot.getDocuments()) {

                        Review review = documentToReview(document);

                        if (review != null) {
                            reviews.add(review);
                        }
                    }

                    listener.onSuccess(reviews);
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Get reviews received by a user in a specific role
    public void getReviewsByRole(
            String userId,
            ReviewRole role,
            OnReviewsLoadedListener listener
    ) {

        db.collection("reviews")
                .whereEqualTo("revieweeId", userId)
                .whereEqualTo("revieweeRole", role.name())
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    List<Review> reviews = new ArrayList<>();

                    for (DocumentSnapshot document
                            : querySnapshot.getDocuments()) {

                        Review review = documentToReview(document);

                        if (review != null) {
                            reviews.add(review);
                        }
                    }

                    listener.onSuccess(reviews);
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Check whether a review already exists
    public void hasReviewed(
            String bookingId,
            String reviewerId,
            OnReviewCheckListener listener
    ) {

        String reviewId = bookingId + "_" + reviewerId;

        db.collection("reviews")
                .document(reviewId)
                .get()
                .addOnSuccessListener(document ->
                        listener.onResult(document.exists()))
                .addOnFailureListener(listener::onFailure);
    }

    // Callbacks
    public interface OnReviewSavedListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnReviewsLoadedListener {
        void onSuccess(List<Review> reviews);
        void onFailure(Exception e);
    }

    public interface OnReviewCheckListener {
        void onResult(boolean hasReviewed);
        void onFailure(Exception e);
    }
}