package com.example.sharely;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.sharely.model.AvailabilitySlot;
import com.example.sharely.model.Booking;
import com.example.sharely.service.BookingService;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class BookingServiceTest {

    @Test
    public void overlappingAcceptedBooking_cannotBeAccepted() {

        Booking existing = new Booking(
                "B001",
                "U001",
                "ITEM1",
                new AvailabilitySlot(1000, 5000)
        );

        existing.acceptBooking();

        Booking newBooking = new Booking(
                "B002",
                "U002",
                "ITEM1",
                new AvailabilitySlot(3000, 7000)
        );

        List<Booking> bookings = new ArrayList<>();
        bookings.add(existing);

        BookingService service = new BookingService();

        assertFalse(
                service.canAcceptBooking(newBooking, bookings)
        );
    }

    @Test
    public void separateAcceptedBooking_canBeAccepted() {

        Booking existing = new Booking(
                "B001",
                "U001",
                "ITEM1",
                new AvailabilitySlot(1000, 5000)
        );

        existing.acceptBooking();

        Booking newBooking = new Booking(
                "B002",
                "U002",
                "ITEM1",
                new AvailabilitySlot(6000, 9000)
        );

        List<Booking> bookings = new ArrayList<>();
        bookings.add(existing);

        BookingService service = new BookingService();

        assertTrue(
                service.canAcceptBooking(newBooking, bookings)
        );
    }

    @Test
    public void overlappingPendingBooking_canBeAccepted() {

        Booking existing = new Booking(
                "B001",
                "U001",
                "ITEM1",
                new AvailabilitySlot(1000, 5000)
        );

        Booking newBooking = new Booking(
                "B002",
                "U002",
                "ITEM1",
                new AvailabilitySlot(3000, 7000)
        );

        List<Booking> bookings = new ArrayList<>();
        bookings.add(existing);

        BookingService service = new BookingService();

        assertTrue(
                service.canAcceptBooking(newBooking, bookings)
        );
    }
}