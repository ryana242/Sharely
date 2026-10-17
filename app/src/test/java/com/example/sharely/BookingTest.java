package com.example.sharely;

import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertFalse;

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

    @Test
    public void acceptedBooking_blocksAvailability() {
        Booking booking = new Booking(
                "B001",
                "U001",
                "ITEM1",
                new AvailabilitySlot(1000, 5000)
        );

        booking.acceptBooking();

        assertEquals(true, booking.blocksAvailability());
    }

    @Test
    public void pendingBooking_doesNotBlockAvailability() {
        Booking booking = new Booking(
                "B001",
                "U001",
                "ITEM1",
                new AvailabilitySlot(1000, 5000)
        );

        assertEquals(false, booking.blocksAvailability());
    }

    @Test
    public void cancelledBooking_doesNotBlockAvailability() {
        Booking booking = new Booking(
                "B001",
                "U001",
                "ITEM1",
                new AvailabilitySlot(1000, 5000)
        );

        booking.acceptBooking();
        booking.cancelBooking();

        assertEquals(false, booking.blocksAvailability());
    }

    @Test
    public void acceptedBooking_canBecomeActive() {

        AvailabilitySlot slot =
                new AvailabilitySlot(
                        1000,
                        5000
                );

        Booking booking =
                new Booking(
                        "B001",
                        "USER1",
                        "ITEM1",
                        slot
                );

        booking.acceptBooking();
        booking.startBooking();

        assertEquals(
                BookingStatus.ACTIVE,
                booking.getStatus()
        );
    }

    @Test
    public void activeBooking_canBeCompleted() {

        AvailabilitySlot slot =
                new AvailabilitySlot(
                        1000,
                        5000
                );

        Booking booking =
                new Booking(
                        "B001",
                        "USER1",
                        "ITEM1",
                        slot
                );

        booking.acceptBooking();
        booking.startBooking();
        booking.completeBooking();

        assertEquals(
                BookingStatus.COMPLETED,
                booking.getStatus()
        );
    }

    @Test
    public void pendingBooking_cannotBecomeActiveDirectly() {

        AvailabilitySlot slot =
                new AvailabilitySlot(
                        1000,
                        5000
                );

        Booking booking =
                new Booking(
                        "B001",
                        "USER1",
                        "ITEM1",
                        slot
                );

        booking.startBooking();

        assertEquals(
                BookingStatus.PENDING,
                booking.getStatus()
        );
    }

    @Test
    public void acceptedBooking_cannotCompleteWithoutStarting() {

        AvailabilitySlot slot =
                new AvailabilitySlot(
                        1000,
                        5000
                );

        Booking booking =
                new Booking(
                        "B001",
                        "USER1",
                        "ITEM1",
                        slot
                );

        booking.acceptBooking();
        booking.completeBooking();

        assertEquals(
                BookingStatus.ACCEPTED,
                booking.getStatus()
        );
    }

    @Test
    public void nullOtherBooking_hasNoConflict() {

        Booking booking = new Booking(
                "B001", "USER1", "ITEM1",
                new AvailabilitySlot(1000, 5000)
        );

        assertFalse(booking.conflictsWith(null));
    }


    @Test
    public void nullItemId_hasNoConflict() {

        Booking first = new Booking(
                "B001", "USER1", null,
                new AvailabilitySlot(1000, 5000)
        );

        Booking second = new Booking(
                "B002", "USER2", "ITEM1",
                new AvailabilitySlot(2000, 4000)
        );

        assertFalse(first.conflictsWith(second));
    }


    @Test
    public void emptyItemId_hasNoConflict() {

        Booking first = new Booking(
                "B001", "USER1", "ITEM1",
                new AvailabilitySlot(1000, 5000)
        );

        Booking second = new Booking(
                "B002", "USER2", "",
                new AvailabilitySlot(2000, 4000)
        );

        assertFalse(first.conflictsWith(second));
    }


    @Test
    public void nullBookingSlot_hasNoConflict() {

        Booking first = new Booking(
                "B001", "USER1", "ITEM1",
                null
        );

        Booking second = new Booking(
                "B002", "USER2", "ITEM1",
                new AvailabilitySlot(2000, 4000)
        );

        assertFalse(first.conflictsWith(second));
    }


    @Test
    public void invalidBookingSlot_hasNoConflict() {

        Booking first = new Booking(
                "B001", "USER1", "ITEM1",
                new AvailabilitySlot(5000, 1000)
        );

        Booking second = new Booking(
                "B002", "USER2", "ITEM1",
                new AvailabilitySlot(2000, 4000)
        );

        assertFalse(first.conflictsWith(second));
    }
}