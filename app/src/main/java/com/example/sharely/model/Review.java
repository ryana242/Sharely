package com.example.sharely.model;

import com.example.sharely.enums.ReviewRole;

public class Review {

    private String reviewId;
    private String bookingId;
    private String reviewerId;
    private String revieweeId;

    //Judge Reviewee is owner/ borrower
    private ReviewRole revieweeRole;

    private int rating;
    private String comment;

    private long createdAt;

    public Review(
            String bookingId,
            String reviewerId,
            String revieweeId,
            ReviewRole revieweeRole,
            int rating,
            String comment
    ) {

        this.bookingId = bookingId;
        this.reviewerId = reviewerId;
        this.revieweeId = revieweeId;
        this.revieweeRole = revieweeRole;

        this.reviewId = bookingId + "_" + reviewerId;

        this.rating = rating;
        this.comment = comment;

        this.createdAt = System.currentTimeMillis();
    }


    //Constructor Overloading to avoid changing createdAt after reading by user
    public Review(
            String bookingId,
            String reviewerId,
            String revieweeId,
            ReviewRole revieweeRole,
            int rating,
            String comment,
            long createdAt
    ) {
        this.bookingId = bookingId;
        this.reviewerId = reviewerId;
        this.revieweeId = revieweeId;
        this.revieweeRole = revieweeRole;
        this.reviewId = bookingId + "_" + reviewerId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public String getReviewId() {
        return reviewId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public String getRevieweeId() {
        return revieweeId;
    }

    public ReviewRole getRevieweeRole() {
        return revieweeRole;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}