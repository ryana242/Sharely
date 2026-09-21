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

    @Test
    public void pendingBooking_canBeCancelled() {
        AvailabilitySlot slot = new AvailabilitySlot(1000, 5000);

        Booking booking =
                new Booking("B001", "U001", "I001", slot);

        booking.cancelBooking();

        assertEquals(
                BookingStatus.CANCELLED,
                booking.getStatus()
        );
    }

    @Test
    public void acceptedBooking_canBeCancelled() {
        AvailabilitySlot slot = new AvailabilitySlot(1000, 5000);

        Booking booking =
                new Booking("B001", "U001", "I001", slot);

        booking.acceptBooking();
        booking.cancelBooking();

        assertEquals(
                BookingStatus.CANCELLED,
                booking.getStatus()
        );
    }

    @Test
    public void sameItemAndOverlappingTime_hasConflict() {
        Booking first = new Booking(
                "B001",
                "U001",
                "ITEM1",
                new AvailabilitySlot(1000, 5000)
        );

        Booking second = new Booking(
                "B002",
                "U002",
                "ITEM1",
                new AvailabilitySlot(3000, 7000)
        );

        assertEquals(true, first.conflictsWith(second));
    }

    @Test
    public void sameItemAndSeparateTime_hasNoConflict() {
        Booking first = new Booking(
                "B001",
                "U001",
                "ITEM1",
                new AvailabilitySlot(1000, 5000)
        );

        Booking second = new Booking(
                "B002",
                "U002",
                "ITEM1",
                new AvailabilitySlot(6000, 9000)
        );

        assertEquals(false, first.conflictsWith(second));
    }

    @Test
    public void differentItemsAndOverlappingTime_hasNoConflict() {
        Booking first = new Booking(
                "B001",
                "U001",
                "ITEM1",
                new AvailabilitySlot(1000, 5000)
        );

        Booking second = new Booking(
                "B002",
                "U002",
                "ITEM2",
                new AvailabilitySlot(3000, 7000)
        );

        assertEquals(false, first.conflictsWith(second));
    }
}