package com.example.sharely.data;

import com.example.sharely.model.Booking;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class BookingRepository {

    private final FirebaseFirestore db;


    public BookingRepository() {

        db = FirebaseFirestore.getInstance();
    }


    // =========================================================
    // GENERATE BOOKING ID
    // =========================================================

    public String generateBookingId() {

        return db
                .collection("bookings")
                .document()
                .getId();
    }


    // =========================================================
    // ADD BOOKING
    // =========================================================

    public void addBooking(
            Booking booking,
            OnBookingSavedListener listener
    ) {

        Map<String, Object> bookingData =
                new HashMap<>();


        bookingData.put(
                "bookingId",
                booking.getBookingId()
        );

        bookingData.put(
                "borrowerId",
                booking.getBorrowerId()
        );

        bookingData.put(
                "itemId",
                booking.getItemId()
        );

        bookingData.put(
                "startTime",
                booking
                        .getBookingSlot()
                        .getStartTime()
        );

        bookingData.put(
                "endTime",
                booking
                        .getBookingSlot()
                        .getEndTime()
        );

        bookingData.put(
                "status",
                booking
                        .getStatus()
                        .name()
        );


        db.collection("bookings")
                .document(
                        booking.getBookingId()
                )
                .set(bookingData)

                .addOnSuccessListener(unused -> {
                    listener.onSuccess();
                })

                .addOnFailureListener(e -> {
                    listener.onFailure(e);
                });
    }

    // =========================================================
    // CALLBACK
    // =========================================================

    public interface OnBookingSavedListener {

        void onSuccess();

        void onFailure(Exception e);
    }
}