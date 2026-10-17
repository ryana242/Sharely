
package com.example.sharely;

import com.example.sharely.model.Item;
import com.example.sharely.model.AvailabilitySlot;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class ItemAvailabilityTest {

    private Item createItem() {
        return new Item(
                "ITEM1",
                "OWNER1",
                "Drill",
                "Electric drill",
                "Tools",
                10.0,
                "Newtown",
                new ArrayList<>()
        );
    }

    @Test
    public void validAvailability_canBeAssigned() {

        Item item = createItem();

        AvailabilitySlot slot =
                new AvailabilitySlot(1000, 5000);

        item.setAvailability(slot);

        assertEquals(
                1000,
                item.getAvailability().getStartTime()
        );

        assertEquals(
                5000,
                item.getAvailability().getEndTime()
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidAvailability_isRejected() {

        Item item = createItem();

        item.setAvailability(
                new AvailabilitySlot(5000, 1000)
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullAvailability_isRejected() {

        Item item = createItem();

        item.setAvailability(null);
    }
}
