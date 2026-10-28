package com.example.sharely.service;

import com.example.sharely.model.AvailabilitySlot;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AvailabilityCalendarEngineTest {

    private final AvailabilitySlot owner = new AvailabilitySlot(100, 1000);
    private final List<AvailabilitySlot> taken = Arrays.asList(
            new AvailabilitySlot(300, 450),
            new AvailabilitySlot(450, 600),
            new AvailabilitySlot(800, 900)
    );

    @Test public void mergesAdjacentReservationsIntoFreeWindows() {
        List<AvailabilitySlot> windows = AvailabilityCalendarEngine.freeWindows(
                owner, taken, 0, 1100, 0);
        assertEquals(3, windows.size());
        assertEquals(100, windows.get(0).getStartTime());
        assertEquals(300, windows.get(0).getEndTime());
        assertEquals(600, windows.get(1).getStartTime());
        assertEquals(800, windows.get(1).getEndTime());
        assertEquals(900, windows.get(2).getStartTime());
    }

    @Test public void cutsOffEndChoiceAtNextReservation() {
        assertEquals(300, AvailabilityCalendarEngine.freeUntil(200, owner, taken));
        assertEquals(200, AvailabilityCalendarEngine.freeUntil(200,
                new AvailabilitySlot(100, 200), taken));
        assertEquals(800, AvailabilityCalendarEngine.freeUntil(700, owner, taken));
    }

    @Test public void unavailablePeriodsCannotBeSelected() {
        assertFalse(AvailabilityCalendarEngine.isSelectable(200, 500, owner, taken, 50));
        assertFalse(AvailabilityCalendarEngine.isSelectable(350, 400, owner, taken, 50));
        assertFalse(AvailabilityCalendarEngine.isSelectable(200, 250, owner, taken, 201));
        assertTrue(AvailabilityCalendarEngine.isSelectable(200, 300, owner, taken, 50));
        assertTrue(AvailabilityCalendarEngine.isSelectable(600, 800, owner, taken, 50));
    }

    @Test public void honorsOwnersExactTimeBoundaries() {
        assertFalse(AvailabilityCalendarEngine.isSelectable(99, 200, owner,
                Collections.emptyList(), 0));
        assertFalse(AvailabilityCalendarEngine.isSelectable(950, 1001, owner,
                Collections.emptyList(), 0));
        assertTrue(AvailabilityCalendarEngine.isSelectable(100, 1000, owner,
                Collections.emptyList(), 0));
    }
    @Test public void aFullyReservedDayIsNotSelectable() {
        AvailabilitySlot fullDay = new AvailabilitySlot(0, 86_400_000);
        List<AvailabilitySlot> windows = AvailabilityCalendarEngine.freeWindows(
                fullDay, Collections.singletonList(fullDay),
                0, 86_400_000, 0);
        assertTrue(windows.isEmpty());
    }

    @Test public void aPartiallyReservedDayStillHasFreePeriods() {
        AvailabilitySlot day = new AvailabilitySlot(0, 86_400_000);
        List<AvailabilitySlot> windows = AvailabilityCalendarEngine.freeWindows(
                day, Collections.singletonList(new AvailabilitySlot(12_000_000, 18_000_000)),
                0, 86_400_000, 0);
        assertEquals(2, windows.size());
        assertEquals(12_000_000, windows.get(0).getEndTime());
        assertEquals(18_000_000, windows.get(1).getStartTime());
    }

    @Test public void aNewBookingCanTouchAReservationBoundary() {
        assertTrue(AvailabilityCalendarEngine.isSelectable(200, 300, owner, taken, 0));
        assertTrue(AvailabilityCalendarEngine.isSelectable(600, 700, owner, taken, 0));
        assertFalse(AvailabilityCalendarEngine.isSelectable(299, 301, owner, taken, 0));
    }

}
