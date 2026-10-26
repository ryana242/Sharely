package com.example.sharely.service;

import com.example.sharely.data.ReviewRepository;
import com.example.sharely.enums.ReviewRole;
import com.example.sharely.model.Review;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ReviewService {

    private final FirebaseFirestore db;
    private final ReviewRepository reviewRepository;

    public ReviewService() {
        db = FirebaseFirestore.getInstance();
        reviewRepository = new ReviewRepository();
    }

    // Submit a review for a completed booking
    public void submitReview(
            String bookingId,
            int rating,
            String comment,
            ReviewRepository.OnReviewSavedListener listener
    ) {

        FirebaseUser currentUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            listener.onFailure(
                    new IllegalStateException("User is not logged in")
            );
            return;
        }

        if (bookingId == null || bookingId.trim().isEmpty()) {
            listener.onFailure(
                    new IllegalArgumentException("Invalid booking ID")
            );
            return;
        }

        if (rating < 1 || rating > 5) {
            listener.onFailure(
                    new IllegalArgumentException(
                            "Rating must be between 1 and 5"
                    )
            );
            return;
        }

        if (comment == null || comment.length() > 1000) {
            listener.onFailure(
                    new IllegalArgumentException(
                            "Comment must not exceed 1000 characters"
                    )
            );
            return;
        }

        String currentUserId = currentUser.getUid();

        // Step 1: Retrieve the booking
        db.collection("bookings")
                .document(bookingId)
                .get()
                .addOnSuccessListener(bookingDocument -> {

                    if (!bookingDocument.exists()) {
                        listener.onFailure(
                                new IllegalStateException(
                                        "Booking does not exist"
                                )
                        );
                        return;
                    }

                    String status =
                            bookingDocument.getString("status");

                    String borrowerId =
                            bookingDocument.getString("borrowerId");

                    String itemId =
                            bookingDocument.getString("itemId");

                    if (!"COMPLETED".equals(status)) {
                        listener.onFailure(
                                new IllegalStateException(
                                        "Booking is not completed"
                                )
                        );
                        return;
                    }

                    if (borrowerId == null || itemId == null) {
                        listener.onFailure(
                                new IllegalStateException(
                                        "Invalid booking data"
                                )
                        );
                        return;
                    }

                    // Step 2: Retrieve the item
                    db.collection("items")
                            .document(itemId)
                            .get()
                            .addOnSuccessListener(itemDocument -> {

                                if (!itemDocument.exists()) {
                                    listener.onFailure(
                                            new IllegalStateException(
                                                    "Item does not exist"
                                            )
                                    );
                                    return;
                                }

                                String ownerId =
                                        itemDocument.getString("ownerId");

                                if (ownerId == null
                                        || ownerId.equals(borrowerId)) {
                                    listener.onFailure(
                                            new IllegalStateException(
                                                    "Invalid booking participants"
                                            )
                                    );
                                    return;
                                }

                                String revieweeId;
                                ReviewRole revieweeRole;

                                // Step 3: Determine user role
                                if (currentUserId.equals(borrowerId)) {

                                    // Borrower reviews Owner
                                    revieweeId = ownerId;
                                    revieweeRole = ReviewRole.OWNER;

                                } else if (currentUserId.equals(ownerId)) {

                                    // Owner reviews Borrower
                                    revieweeId = borrowerId;
                                    revieweeRole = ReviewRole.BORROWER;

                                } else {

                                    listener.onFailure(
                                            new SecurityException(
                                                    "You are not part of this booking"
                                            )
                                    );
                                    return;
                                }

                                // Step 4: Create review
                                Review review = new Review(
                                        bookingId,
                                        currentUserId,
                                        revieweeId,
                                        revieweeRole,
                                        rating,
                                        comment
                                );

                                // Step 5: Save review
                                reviewRepository.addReview(
                                        review,
                                        listener
                                );

                            })
                            .addOnFailureListener(listener::onFailure);

                })
                .addOnFailureListener(listener::onFailure);
    }
}