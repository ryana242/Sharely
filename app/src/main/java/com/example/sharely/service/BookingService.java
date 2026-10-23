
package com.example.sharely.service;

import com.example.sharely.enums.BookingStatus;
import com.example.sharely.model.AvailabilitySlot;
import com.example.sharely.model.Booking;

import java.util.List;

public class BookingService {

    // Validate a booking that is waiting for approval.
    private boolean isValidPendingBooking(Booking booking) {

        return booking != null
                && booking.getStatus() == BookingStatus.PENDING
                && booking.getItemId() != null
                && !booking.getItemId().trim().isEmpty()
                && booking.getBorrowerId() != null
                && !booking.getBorrowerId().trim().isEmpty()
                && booking.hasValidTimeSlot();
    }

    // Check whether an owner can accept a booking.
    public boolean canAcceptBooking(
            Booking newBooking,
            List<Booking> existingBookings
    ) {

        if (!isValidPendingBooking(newBooking)
                || existingBookings == null) {
            return false;
        }

        for (Booking existingBooking : existingBookings) {

            if (existingBooking != null
                    && existingBooking.blocksAvailability()
                    && newBooking.conflictsWith(existingBooking)) {
                return false;
            }
        }

        return true;
    }

    // Accept a booking only if valid.
    public boolean acceptBooking(
            Booking newBooking,
            List<Booking> existingBookings
    ) {

        if (!canAcceptBooking(newBooking, existingBookings)) {
            return false;
        }

        newBooking.acceptBooking();

        return newBooking.getStatus()
                == BookingStatus.ACCEPTED;
    }

    // Validate a borrower's requested booking period.
    public boolean canRequestBooking(
            Booking newBooking,
            List<AvailabilitySlot> ownerAvailability,
            List<Booking> existingBookings
    ) {

        if (!isValidPendingBooking(newBooking)
                || ownerAvailability == null
                || ownerAvailability.isEmpty()
                || existingBookings == null) {
            return false;
        }

        boolean insideAvailableSlot = false;

        for (AvailabilitySlot availableSlot : ownerAvailability) {

            if (availableSlot != null
                    && availableSlot.contains(
                    newBooking.getBookingSlot())) {

                insideAvailableSlot = true;
                break;
            }
        }

        if (!insideAvailableSlot) {
            return false;
        }

        for (Booking existingBooking : existingBookings) {

            if (existingBooking == null) {
                continue;
            }

            // Accepted and active bookings block availability.
            if (existingBooking.blocksAvailability()
                    && newBooking.conflictsWith(existingBooking)) {
                return false;
            }

            // Prevent the same borrower from repeatedly sending
            // overlapping pending requests for the same item.
            if (existingBooking.getStatus() == BookingStatus.PENDING
                    && newBooking.getBorrowerId().equals(
                    existingBooking.getBorrowerId())
                    && newBooking.conflictsWith(existingBooking)) {
                return false;
            }
        }

        return true;
    }
}
