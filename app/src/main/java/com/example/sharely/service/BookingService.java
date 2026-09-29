package com.example.sharely.service;

import com.example.sharely.model.Booking;

import com.example.sharely.model.AvailabilitySlot;

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

    public boolean canRequestBooking(
            Booking newBooking,
            List<AvailabilitySlot> ownerAvailability,
            List<Booking> existingBookings
    ) {

        if (newBooking == null
                || !newBooking.hasValidTimeSlot()) {

            return false;
        }


        if (ownerAvailability == null
                || ownerAvailability.isEmpty()) {

            return false;
        }


        boolean insideAvailableSlot = false;


        for (AvailabilitySlot availableSlot : ownerAvailability) {

            if (availableSlot != null
                    && availableSlot.contains(
                    newBooking.getBookingSlot()
            )) {

                insideAvailableSlot = true;
                break;
            }
        }


        if (!insideAvailableSlot) {

            return false;
        }


        if (existingBookings != null) {

            for (Booking existingBooking : existingBookings) {

                if (existingBooking != null
                        && existingBooking.blocksAvailability()
                        && newBooking.conflictsWith(
                        existingBooking
                )) {

                    return false;
                }
            }
        }


        return true;
    }

}