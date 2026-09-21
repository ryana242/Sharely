package com.example.sharely;

import static org.junit.Assert.assertEquals;

import com.example.sharely.enums.BookingStatus;
import com.example.sharely.model.AvailabilitySlot;
import com.example.sharely.model.Booking;

import org.junit.Test;

public class BookingTest {

    @Test
    public void newBooking_startsPending() {
        AvailabilitySlot slot = new AvailabilitySlot(1000, 5000);

        Booking booking =
                new Booking("B001", "U001", "I001", slot);

        assertEquals(
                BookingStatus.PENDING,
                booking.getStatus()
        );
    }

    @Test
    public void pendingBooking_canBeAccepted() {
        AvailabilitySlot slot = new AvailabilitySlot(1000, 5000);

        Booking booking =
                new Booking("B001", "U001", "I001", slot);

        booking.acceptBooking();

        assertEquals(
                BookingStatus.ACCEPTED,
                booking.getStatus()
        );
    }

    @Test
    public void pendingBooking_canBeRejected() {
        AvailabilitySlot slot = new AvailabilitySlot(1000, 5000);

        Booking booking =
                new Booking("B001", "U001", "I001", slot);

        booking.rejectBooking();

        assertEquals(
                BookingStatus.REJECTED,
                booking.getStatus()
        );
    }
}