package com.example.sharely.model;

import com.example.sharely.enums.BookingStatus;

public class Booking {

    private String bookingId;
    private String borrowerId;
    private String itemId;
    private AvailabilitySlot bookingSlot;
    private BookingStatus status;

    public Booking(
            String bookingId,
            String borrowerId,
            String itemId,
            AvailabilitySlot bookingSlot
    ) {
        this.bookingId = bookingId;
        this.borrowerId = borrowerId;
        this.itemId = itemId;
        this.bookingSlot = bookingSlot;
        this.status = BookingStatus.PENDING;
    }

    public void acceptBooking() {
        if (status == BookingStatus.PENDING) {
            status = BookingStatus.ACCEPTED;
        }
    }

    public void rejectBooking() {
        if (status == BookingStatus.PENDING) {
            status = BookingStatus.REJECTED;
        }
    }

    public BookingStatus getStatus() {
        return status;
    }
}