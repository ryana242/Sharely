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
}