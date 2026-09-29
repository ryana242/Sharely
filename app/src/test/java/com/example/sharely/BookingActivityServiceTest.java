package com.example.sharely;

import static org.junit.Assert.assertEquals;

import com.example.sharely.model.AvailabilitySlot;
import com.example.sharely.model.Booking;
import com.example.sharely.service.BookingActivityService;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class BookingActivityServiceTest {

    @Test
    public void activeBooking_goesToOngoing() {

        BookingActivityService service =
                new BookingActivityService();

        Booking booking =
                createBooking("B001");

        booking.acceptBooking();
        booking.startBooking();

        List<Booking> bookings =
                new ArrayList<>();

        bookings.add(booking);

        List<Booking> result =
                service.getOngoingBookings(bookings);

        assertEquals(
                1,
                result.size()
        );
    }


    @Test
    public void pendingAndAcceptedBookings_goToUpcoming() {

        BookingActivityService service =
                new BookingActivityService();

        Booking pendingBooking =
                createBooking("B001");

        Booking acceptedBooking =
                createBooking("B002");

        acceptedBooking.acceptBooking();

        List<Booking> bookings =
                new ArrayList<>();

        bookings.add(pendingBooking);
        bookings.add(acceptedBooking);

        List<Booking> result =
                service.getUpcomingBookings(bookings);

        assertEquals(
                2,
                result.size()
        );
    }


    @Test
    public void completedBooking_goesToHistory() {

        BookingActivityService service =
                new BookingActivityService();

        Booking booking =
                createBooking("B001");

        booking.acceptBooking();
        booking.startBooking();
        booking.completeBooking();

        List<Booking> bookings =
                new ArrayList<>();

        bookings.add(booking);

        List<Booking> result =
                service.getHistoryBookings(bookings);

        assertEquals(
                1,
                result.size()
        );
    }


    @Test
    public void cancelledAndRejectedBookings_goToHistory() {

        BookingActivityService service =
                new BookingActivityService();

        Booking cancelledBooking =
                createBooking("B001");

        cancelledBooking.cancelBooking();

        Booking rejectedBooking =
                createBooking("B002");

        rejectedBooking.rejectBooking();

        List<Booking> bookings =
                new ArrayList<>();

        bookings.add(cancelledBooking);
        bookings.add(rejectedBooking);

        List<Booking> result =
                service.getHistoryBookings(bookings);

        assertEquals(
                2,
                result.size()
        );
    }


    @Test
    public void nullList_returnsEmptyResults() {

        BookingActivityService service =
                new BookingActivityService();

        assertEquals(
                0,
                service.getOngoingBookings(null).size()
        );

        assertEquals(
                0,
                service.getUpcomingBookings(null).size()
        );

        assertEquals(
                0,
                service.getHistoryBookings(null).size()
        );
    }


    private Booking createBooking(
            String bookingId
    ) {

        AvailabilitySlot slot =
                new AvailabilitySlot(
                        1000,
                        5000
                );

        return new Booking(
                bookingId,
                "USER1",
                "ITEM1",
                slot
        );
    }
}