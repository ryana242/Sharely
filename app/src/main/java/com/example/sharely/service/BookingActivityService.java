package com.example.sharely.service;

import com.example.sharely.enums.BookingStatus;
import com.example.sharely.model.Booking;

import java.util.ArrayList;
import java.util.List;

public class BookingActivityService {

    public List<Booking> getOngoingBookings(
            List<Booking> bookings
    ) {

        List<Booking> result = new ArrayList<>();

        if (bookings == null) {
            return result;
        }

        for (Booking booking : bookings) {

            if (booking.getStatus() == BookingStatus.ACTIVE) {
                result.add(booking);
            }
        }

        return result;
    }


    public List<Booking> getUpcomingBookings(
            List<Booking> bookings
    ) {

        List<Booking> result = new ArrayList<>();

        if (bookings == null) {
            return result;
        }

        for (Booking booking : bookings) {

            BookingStatus status =
                    booking.getStatus();

            if (status == BookingStatus.PENDING
                    || status == BookingStatus.ACCEPTED) {

                result.add(booking);
            }
        }

        return result;
    }


    public List<Booking> getHistoryBookings(
            List<Booking> bookings
    ) {

        List<Booking> result = new ArrayList<>();

        if (bookings == null) {
            return result;
        }

        for (Booking booking : bookings) {

            BookingStatus status =
                    booking.getStatus();

            if (status == BookingStatus.COMPLETED
                    || status == BookingStatus.CANCELLED
                    || status == BookingStatus.REJECTED) {

                result.add(booking);
            }
        }

        return result;
    }
}