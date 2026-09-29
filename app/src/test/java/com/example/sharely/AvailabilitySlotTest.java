package com.example.sharely;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.sharely.model.AvailabilitySlot;

import org.junit.Test;

public class AvailabilitySlotTest {

    @Test
    public void validSlot_returnsTrue() {
        AvailabilitySlot slot = new AvailabilitySlot(1000, 5000);

        assertTrue(slot.isValid());
    }

    @Test
    public void invalidSlot_returnsFalse() {
        AvailabilitySlot slot = new AvailabilitySlot(5000, 1000);

        assertFalse(slot.isValid());
    }

    @Test
    public void overlappingSlots_returnsTrue() {
        AvailabilitySlot first = new AvailabilitySlot(1000, 5000);
        AvailabilitySlot second = new AvailabilitySlot(3000, 7000);

        assertTrue(first.overlaps(second));
    }

    @Test
    public void separateSlots_returnsFalse() {
        AvailabilitySlot first = new AvailabilitySlot(1000, 5000);
        AvailabilitySlot second = new AvailabilitySlot(6000, 9000);

        assertFalse(first.overlaps(second));
    }

    @Test
    public void touchingSlots_returnsFalse() {
        AvailabilitySlot first = new AvailabilitySlot(1000, 5000);
        AvailabilitySlot second = new AvailabilitySlot(5000, 9000);

        assertFalse(first.overlaps(second));
    }

    @Test
    public void contains_requestInsideAvailability_returnsTrue() {

        AvailabilitySlot available =
                new AvailabilitySlot(
                        1000,
                        5000
                );

        AvailabilitySlot requested =
                new AvailabilitySlot(
                        2000,
                        4000
                );


        assertTrue(
                available.contains(
                        requested
                )
        );
    }


    @Test
    public void contains_requestExactlyMatchesAvailability_returnsTrue() {

        AvailabilitySlot available =
                new AvailabilitySlot(
                        1000,
                        5000
                );

        AvailabilitySlot requested =
                new AvailabilitySlot(
                        1000,
                        5000
                );


        assertTrue(
                available.contains(
                        requested
                )
        );
    }


    @Test
    public void contains_requestExtendsPastAvailability_returnsFalse() {

        AvailabilitySlot available =
                new AvailabilitySlot(
                        1000,
                        5000
                );

        AvailabilitySlot requested =
                new AvailabilitySlot(
                        4000,
                        6000
                );


        assertFalse(
                available.contains(
                        requested
                )
        );
    }


    @Test
    public void contains_nullSlot_returnsFalse() {

        AvailabilitySlot available =
                new AvailabilitySlot(
                        1000,
                        5000
                );


        assertFalse(
                available.contains(
                        null
                )
        );
    }
}