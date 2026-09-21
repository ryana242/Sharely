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

    public void cancelBooking() {
        if (status == BookingStatus.PENDING
                || status == BookingStatus.ACCEPTED) {
            status = BookingStatus.CANCELLED;
        }
    }

    public BookingStatus getStatus() {
        return status;
    }

    public boolean conflictsWith(Booking other) {
        boolean sameItem = itemId.equals(other.itemId);
        boolean overlappingTime = bookingSlot.overlaps(other.bookingSlot);

        return sameItem && overlappingTime;
    }

    public boolean blocksAvailability() {
        return status == BookingStatus.ACCEPTED
                || status == BookingStatus.ACTIVE;
    }
}