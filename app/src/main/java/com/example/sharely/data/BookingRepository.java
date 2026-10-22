package com.example.sharely.data;

import com.example.sharely.model.Booking;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.example.sharely.enums.BookingStatus;
import com.google.firebase.firestore.DocumentSnapshot;

import com.example.sharely.model.AvailabilitySlot;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class BookingRepository {

    private final FirebaseFirestore db;


    public BookingRepository() {

        db = FirebaseFirestore.getInstance();
    }

    // CONVERT FIRESTORE DOCUMENT TO BOOKING
    private Booking documentToBooking(
            DocumentSnapshot document
    ) {

        String bookingId =
                document.getString("bookingId");

        String borrowerId =
                document.getString("borrowerId");

        String itemId =
                document.getString("itemId");

        Long startTime =
                document.getLong("startTime");

        Long endTime =
                document.getLong("endTime");

        String statusString =
                document.getString("status");


        if (bookingId == null
                || borrowerId == null
                || itemId == null
                || startTime == null
                || endTime == null
                || statusString == null) {

            return null;
        }


        AvailabilitySlot slot =
                new AvailabilitySlot(
                        startTime,
                        endTime
                );

        if (!slot.isValid()) {
            return null;
        }


        Booking booking =
                new Booking(
                        bookingId,
                        borrowerId,
                        itemId,
                        slot
                );


        BookingStatus status;

        try {
            status = BookingStatus.valueOf(statusString);
        } catch (IllegalArgumentException e) {
            return null;
        }


        switch (status) {

            case ACCEPTED:
                booking.acceptBooking();
                break;

            case ACTIVE:
                booking.acceptBooking();
                booking.startBooking();
                break;

            case COMPLETED:
                booking.acceptBooking();
                booking.startBooking();
                booking.completeBooking();
                break;

            case REJECTED:
                booking.rejectBooking();
                break;

            case CANCELLED:
                booking.cancelBooking();
                break;

            case PENDING:
            default:
                break;
        }

        return booking;
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
    // Get current user all bookings information
    // =========================================================
    public void getBookingsByBorrower(
            String borrowerId,
            OnBookingsLoadedListener listener
    ) {

        db.collection("bookings")
                .whereEqualTo("borrowerId", borrowerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    List<Booking> bookings =
                            new ArrayList<>();

                    for (DocumentSnapshot document
                            : querySnapshot.getDocuments()) {

                        Booking booking =
                                documentToBooking(document);

                        if (booking != null) {
                            bookings.add(booking);
                        }
                    }

                    listener.onSuccess(bookings);
                })
                .addOnFailureListener(
                        listener::onFailure
                );
    }


    // =========================================================
    // GET BOOKINGS BY ITEM
    // =========================================================
    public void getBookingsByItem(
            String itemId,
            OnBookingsLoadedListener listener
    ) {

        db.collection("bookings")
                .whereEqualTo("itemId", itemId)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    List<Booking> bookings =
                            new ArrayList<>();

                    for (DocumentSnapshot document
                            : querySnapshot.getDocuments()) {

                        Booking booking =
                                documentToBooking(document);

                        if (booking != null) {
                            bookings.add(booking);
                        }
                    }

                    listener.onSuccess(bookings);
                })
                .addOnFailureListener(
                        listener::onFailure
                );
    }
    // =========================================================
    // UPDATE BOOKING STATUS
    // =========================================================
    public void updateBookingStatus(
            String bookingId,
            BookingStatus status,
            OnBookingStatusUpdatedListener listener
    ) {

        db.collection("bookings")
                .document(bookingId)
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

    // =========================================================
    // CALLBACK
    // =========================================================

    public interface OnBookingSavedListener {

        void onSuccess();

        void onFailure(Exception e);
    }
    public interface OnBookingsLoadedListener {

        void onSuccess(

                List<Booking> bookings

        );

        void onFailure(

                Exception e

        );
    }

    public interface OnBookingStatusUpdatedListener {

        void onSuccess();

        void onFailure(Exception e);
    }
}