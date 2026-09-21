package com.example.sharely.service;

import com.example.sharely.model.Booking;

import java.util.List;

public class BookingService {

    public boolean canAcceptBooking(
            Booking newBooking,
            List<Booking> existingBookings
    ) {

        for (Booking existingBooking : existingBookings) {

            if (existingBooking.blocksAvailability()
                    && newBooking.conflictsWith(existingBooking)) {

                return false;
            }
        }

        return true;
    }
}