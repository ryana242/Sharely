package com.example.sharely.service;

import com.example.sharely.model.Booking;

import java.util.List;

public class BookingService {

    public boolean canAcceptBooking(
            Booking newBooking,
            List<Booking> existingBookings
    ) {

        if (!newBooking.hasValidTimeSlot()) {
            return false;
        }

        for (Booking existingBooking : existingBookings) {

            if (existingBooking.blocksAvailability()
                    && newBooking.conflictsWith(existingBooking)) {

                return false;
            }
        }

        return true;
    }

    public boolean acceptBooking(
            Booking newBooking,
            List<Booking> existingBookings
    ) {

        if (!canAcceptBooking(newBooking, existingBookings)) {
            return false;
        }

        newBooking.acceptBooking();
        return true;
    }
}