
package com.example.sharely;

import com.example.sharely.enums.BookingStatus;
import com.example.sharely.model.AvailabilitySlot;
import com.example.sharely.model.Booking;
import com.example.sharely.service.BookingService;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BookingServiceTest {

    private final BookingService service =
            new BookingService();

    private Booking booking(
            String id,
            String borrower,
            long start,
            long end
    ) {

        return new Booking(
                id,
                borrower,
                "ITEM1",
                new AvailabilitySlot(start, end)
        );
    }

    private List<AvailabilitySlot> availability() {

        return Collections.singletonList(
                new AvailabilitySlot(1000, 10000)
        );
    }

    private List<Booking> noBookings() {
        return new ArrayList<>();
    }

    @Test
    public void overlappingAcceptedBooking_cannotBeAccepted() {

        Booking existing = booking(
                "B001", "U001", 1000, 5000
        );

        existing.acceptBooking();

        Booking requested = booking(
                "B002", "U002", 3000, 7000
        );

        assertFalse(
                service.canAcceptBooking(
                        requested,
                        Collections.singletonList(existing)
                )
        );
    }

    @Test
    public void separateAcceptedBooking_canBeAccepted() {

        Booking existing = booking(
                "B001", "U001", 1000, 5000
        );

        existing.acceptBooking();

        Booking requested = booking(
                "B002", "U002", 6000, 9000
        );

        assertTrue(
                service.canAcceptBooking(
                        requested,
                        Collections.singletonList(existing)
                )
        );
    }

    @Test
    public void overlappingPendingBooking_canBeAccepted() {

        Booking existing = booking(
                "B001", "U001", 1000, 5000
        );

        Booking requested = booking(
                "B002", "U002", 3000, 7000
        );

        assertTrue(
                service.canAcceptBooking(
                        requested,
                        Collections.singletonList(existing)
                )
        );
    }

    @Test
    public void availableBooking_isAccepted() {

        Booking existing = booking(
                "B001", "U001", 1000, 5000
        );

        existing.acceptBooking();

        Booking requested = booking(
                "B002", "U002", 6000, 9000
        );

        assertTrue(
                service.acceptBooking(
                        requested,
                        Collections.singletonList(existing)
                )
        );

        assertEquals(
                BookingStatus.ACCEPTED,
                requested.getStatus()
        );
    }

    @Test
    public void conflictingBooking_staysPending() {

        Booking existing = booking(
                "B001", "U001", 1000, 5000
        );

        existing.acceptBooking();

        Booking requested = booking(
                "B002", "U002", 3000, 7000
        );

        assertFalse(
                service.acceptBooking(
                        requested,
                        Collections.singletonList(existing)
                )
        );

        assertEquals(
                BookingStatus.PENDING,
                requested.getStatus()
        );
    }

    @Test
    public void invalidTimeSlot_cannotBeAccepted() {

        Booking requested = booking(
                "B001", "U001", 5000, 1000
        );

        assertFalse(
                service.acceptBooking(
                        requested,
                        noBookings()
                )
        );
    }

    @Test
    public void requestInsideAvailabilityWithoutConflict_returnsTrue() {

        Booking requested = booking(
                "B001", "USER1", 2000, 4000
        );

        assertTrue(
                service.canRequestBooking(
                        requested,
                        availability(),
                        noBookings()
                )
        );
    }

    @Test
    public void requestOutsideAvailability_returnsFalse() {

        Booking requested = booking(
                "B001", "USER1", 4000, 11000
        );

        assertFalse(
                service.canRequestBooking(
                        requested,
                        availability(),
                        noBookings()
                )
        );
    }

    @Test
    public void requestConflictsWithAcceptedBooking_returnsFalse() {

        Booking existing = booking(
                "B001", "USER2", 3000, 6000
        );

        existing.acceptBooking();

        Booking requested = booking(
                "B002", "USER1", 4000, 5000
        );

        assertFalse(
                service.canRequestBooking(
                        requested,
                        availability(),
                        Collections.singletonList(existing)
                )
        );
    }

    @Test
    public void pendingBooking_doesNotBlockAnotherRequest() {

        Booking existing = booking(
                "B001", "USER2", 3000, 6000
        );

        Booking requested = booking(
                "B002", "USER1", 4000, 5000
        );

        assertTrue(
                service.canRequestBooking(
                        requested,
                        availability(),
                        Collections.singletonList(existing)
                )
        );
    }

    @Test
    public void cancelledBooking_cannotBeAccepted() {

        Booking requested = booking(
                "B001", "USER1", 1000, 5000
        );

        requested.cancelBooking();

        assertFalse(
                service.acceptBooking(
                        requested,
                        noBookings()
                )
        );

        assertEquals(
                BookingStatus.CANCELLED,
                requested.getStatus()
        );
    }

    @Test
    public void rejectedBooking_cannotBeAccepted() {

        Booking requested = booking(
                "B001", "USER1", 1000, 5000
        );

        requested.rejectBooking();

        assertFalse(
                service.acceptBooking(
                        requested,
                        noBookings()
                )
        );

        assertEquals(
                BookingStatus.REJECTED,
                requested.getStatus()
        );
    }

    @Test
    public void completedBooking_cannotBeAcceptedAgain() {

        Booking requested = booking(
                "B001", "USER1", 1000, 5000
        );

        requested.acceptBooking();
        requested.startBooking();
        requested.completeBooking();

        assertFalse(
                service.acceptBooking(
                        requested,
                        noBookings()
                )
        );

        assertEquals(
                BookingStatus.COMPLETED,
                requested.getStatus()
        );
    }

    @Test
    public void nullBooking_cannotBeAccepted() {

        assertFalse(
                service.acceptBooking(
                        null,
                        noBookings()
                )
        );
    }

    @Test
    public void nullExistingBookings_cannotBeAccepted() {

        Booking requested = booking(
                "B001", "USER1", 1000, 5000
        );

        assertFalse(
                service.acceptBooking(
                        requested,
                        null
                )
        );

        assertEquals(
                BookingStatus.PENDING,
                requested.getStatus()
        );
    }

    @Test
    public void nullBookingInList_doesNotCrash() {

        Booking requested = booking(
                "B001", "USER1", 1000, 5000
        );

        List<Booking> existing = new ArrayList<>();
        existing.add(null);

        assertTrue(
                service.acceptBooking(
                        requested,
                        existing
                )
        );

        assertEquals(
                BookingStatus.ACCEPTED,
                requested.getStatus()
        );
    }

    @Test
    public void sameBorrowerOverlappingPendingRequest_isRejected() {

        Booking existing = booking(
                "B001", "USER1", 2000, 5000
        );

        Booking requested = booking(
                "B002", "USER1", 3000, 4000
        );

        assertFalse(
                service.canRequestBooking(
                        requested,
                        availability(),
                        Collections.singletonList(existing)
                )
        );
    }

    @Test
    public void sameBorrowerNonoverlappingPendingRequest_isAllowed() {

        Booking existing = booking(
                "B001", "USER1", 2000, 3000
        );

        Booking requested = booking(
                "B002", "USER1", 3000, 4000
        );

        assertTrue(
                service.canRequestBooking(
                        requested,
                        availability(),
                        Collections.singletonList(existing)
                )
        );
    }

    @Test
    public void cancelledRequestDoesNotBlockSameBorrower() {

        Booking existing = booking(
                "B001", "USER1", 2000, 5000
        );

        existing.cancelBooking();

        Booking requested = booking(
                "B002", "USER1", 3000, 4000
        );

        assertTrue(
                service.canRequestBooking(
                        requested,
                        availability(),
                        Collections.singletonList(existing)
                )
        );
    }

    @Test
    public void blankBorrowerCannotRequestBooking() {

        Booking requested = booking(
                "B001", "  ", 2000, 4000
        );

        assertFalse(
                service.canRequestBooking(
                        requested,
                        availability(),
                        noBookings()
                )
        );
    }
}
