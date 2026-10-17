package com.example.sharely;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.sharely.model.AvailabilitySlot;
import com.example.sharely.model.Booking;
import com.example.sharely.service.BookingService;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import com.example.sharely.enums.BookingStatus;

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

    @Test
    public void availableBooking_isAccepted() {

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

        boolean result =
                service.acceptBooking(newBooking, bookings);

        assertTrue(result);

        assertEquals(
                BookingStatus.ACCEPTED,
                newBooking.getStatus()
        );
    }

    @Test
    public void conflictingBooking_staysPending() {

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

        boolean result =
                service.acceptBooking(newBooking, bookings);

        assertFalse(result);

        assertEquals(
                BookingStatus.PENDING,
                newBooking.getStatus()
        );
    }

    @Test
    public void invalidTimeSlot_cannotBeAccepted() {

        Booking newBooking = new Booking(
                "B001",
                "U001",
                "ITEM1",
                new AvailabilitySlot(5000, 1000)
        );

        List<Booking> bookings = new ArrayList<>();

        BookingService service = new BookingService();

        boolean result =
                service.acceptBooking(newBooking, bookings);

        assertFalse(result);

        assertEquals(
                BookingStatus.PENDING,
                newBooking.getStatus()
        );
    }

    @Test
    public void requestInsideAvailabilityWithoutConflict_returnsTrue() {

        BookingService service =
                new BookingService();

        List<AvailabilitySlot> availability =
                new ArrayList<>();

        availability.add(
                new AvailabilitySlot(
                        1000,
                        10000
                )
        );


        Booking request =
                new Booking(
                        "B001",
                        "USER1",
                        "ITEM1",
                        new AvailabilitySlot(
                                2000,
                                4000
                        )
                );


        List<Booking> existingBookings =
                new ArrayList<>();


        assertTrue(
                service.canRequestBooking(
                        request,
                        availability,
                        existingBookings
                )
        );
    }

    @Test
    public void requestOutsideAvailability_returnsFalse() {

        BookingService service =
                new BookingService();

        List<AvailabilitySlot> availability =
                new ArrayList<>();

        availability.add(
                new AvailabilitySlot(
                        1000,
                        5000
                )
        );


        Booking request =
                new Booking(
                        "B001",
                        "USER1",
                        "ITEM1",
                        new AvailabilitySlot(
                                4000,
                                7000
                        )
                );


        assertFalse(
                service.canRequestBooking(
                        request,
                        availability,
                        new ArrayList<>()
                )
        );
    }

    @Test
    public void requestConflictsWithAcceptedBooking_returnsFalse() {

        BookingService service =
                new BookingService();

        List<AvailabilitySlot> availability =
                new ArrayList<>();

        availability.add(
                new AvailabilitySlot(
                        1000,
                        10000
                )
        );


        Booking existing =
                new Booking(
                        "B001",
                        "USER2",
                        "ITEM1",
                        new AvailabilitySlot(
                                3000,
                                6000
                        )
                );

        existing.acceptBooking();


        List<Booking> existingBookings =
                new ArrayList<>();

        existingBookings.add(
                existing
        );


        Booking request =
                new Booking(
                        "B002",
                        "USER1",
                        "ITEM1",
                        new AvailabilitySlot(
                                4000,
                                5000
                        )
                );


        assertFalse(
                service.canRequestBooking(
                        request,
                        availability,
                        existingBookings
                )
        );
    }

    @Test
    public void pendingBooking_doesNotBlockAnotherRequest() {

        BookingService service =
                new BookingService();

        List<AvailabilitySlot> availability =
                new ArrayList<>();

        availability.add(
                new AvailabilitySlot(
                        1000,
                        10000
                )
        );


        Booking pending =
                new Booking(
                        "B001",
                        "USER2",
                        "ITEM1",
                        new AvailabilitySlot(
                                3000,
                                6000
                        )
                );


        List<Booking> existingBookings =
                new ArrayList<>();

        existingBookings.add(
                pending
        );


        Booking request =
                new Booking(
                        "B002",
                        "USER1",
                        "ITEM1",
                        new AvailabilitySlot(
                                4000,
                                5000
                        )
                );


        assertTrue(
                service.canRequestBooking(
                        request,
                        availability,
                        existingBookings
                )
        );
    }

    @Test
    public void cancelledBooking_cannotBeAccepted() {

        Booking booking = new Booking(
                "B001", "USER1", "ITEM1",
                new AvailabilitySlot(1000, 5000)
        );

        booking.cancelBooking();

        BookingService service = new BookingService();

        assertFalse(
                service.acceptBooking(booking, new ArrayList<>())
        );

        assertEquals(
                BookingStatus.CANCELLED,
                booking.getStatus()
        );
    }


    @Test
    public void rejectedBooking_cannotBeAccepted() {

        Booking booking = new Booking(
                "B001", "USER1", "ITEM1",
                new AvailabilitySlot(1000, 5000)
        );

        booking.rejectBooking();

        BookingService service = new BookingService();

        assertFalse(
                service.acceptBooking(booking, new ArrayList<>())
        );

        assertEquals(
                BookingStatus.REJECTED,
                booking.getStatus()
        );
    }


    @Test
    public void completedBooking_cannotBeAcceptedAgain() {

        Booking booking = new Booking(
                "B001", "USER1", "ITEM1",
                new AvailabilitySlot(1000, 5000)
        );

        booking.acceptBooking();
        booking.startBooking();
        booking.completeBooking();

        BookingService service = new BookingService();

        assertFalse(
                service.acceptBooking(booking, new ArrayList<>())
        );

        assertEquals(
                BookingStatus.COMPLETED,
                booking.getStatus()
        );
    }


    @Test
    public void nullBooking_cannotBeAccepted() {

        BookingService service = new BookingService();

        assertFalse(
                service.acceptBooking(null, new ArrayList<>())
        );
    }


    @Test
    public void nullExistingBookings_cannotBeAccepted() {

        Booking booking = new Booking(
                "B001", "USER1", "ITEM1",
                new AvailabilitySlot(1000, 5000)
        );

        BookingService service = new BookingService();

        assertFalse(
                service.acceptBooking(booking, null)
        );

        assertEquals(
                BookingStatus.PENDING,
                booking.getStatus()
        );
    }


    @Test
    public void nullBookingInList_doesNotCrash() {

        Booking booking = new Booking(
                "B001", "USER1", "ITEM1",
                new AvailabilitySlot(1000, 5000)
        );

        List<Booking> existingBookings = new ArrayList<>();
        existingBookings.add(null);

        BookingService service = new BookingService();

        assertTrue(
                service.acceptBooking(booking, existingBookings)
        );

        assertEquals(
                BookingStatus.ACCEPTED,
                booking.getStatus()
        );
    }
}