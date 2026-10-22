
package com.example.sharely;

import com.example.sharely.model.Item;
import com.example.sharely.enums.ItemStatus;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class ItemStatusTest {

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
    public void newItem_isActive() {
        Item item = createItem();

        assertEquals(ItemStatus.ACTIVE, item.getStatus());
    }

    @Test
    public void activeItem_canBecomeUnavailable() {
        Item item = createItem();

        item.setUnavailable();

        assertEquals(ItemStatus.UNAVAILABLE, item.getStatus());
    }

    @Test
    public void unavailableItem_canBeReactivated() {
        Item item = createItem();

        item.setUnavailable();
        item.setActive();

        assertEquals(ItemStatus.ACTIVE, item.getStatus());
    }

    @Test
    public void item_canBePermanentlyTakenDown() {
        Item item = createItem();

        item.setPast();

        assertEquals(ItemStatus.PAST, item.getStatus());
    }

    @Test(expected = IllegalStateException.class)
    public void pastItem_cannotBeReactivated() {
        Item item = createItem();

        item.setPast();
        item.setActive();
    }

    @Test(expected = IllegalStateException.class)
    public void pastItem_cannotBecomeUnavailable() {
        Item item = createItem();

        item.setPast();
        item.setUnavailable();
    }
}
