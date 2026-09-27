package com.example.sharely;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.sharely.service.ItemSearchService;

import org.junit.Test;

public class ItemSearchServiceTest {


    @Test
    public void matchingName_returnsTrue() {

        ItemSearchService service =
                new ItemSearchService();

        assertTrue(
                service.matchesKeyword(
                        "Cordless Drill",
                        "drill"
                )
        );
    }


    @Test
    public void matchingKeyword_isCaseInsensitive() {

        ItemSearchService service =
                new ItemSearchService();

        assertTrue(
                service.matchesKeyword(
                        "Cordless Drill",
                        "DRILL"
                )
        );
    }


    @Test
    public void unrelatedKeyword_returnsFalse() {

        ItemSearchService service =
                new ItemSearchService();

        assertFalse(
                service.matchesKeyword(
                        "Cordless Drill",
                        "tent"
                )
        );
    }


    @Test
    public void emptyKeyword_returnsTrue() {

        ItemSearchService service =
                new ItemSearchService();

        assertTrue(
                service.matchesKeyword(
                        "Cordless Drill",
                        ""
                )
        );
    }


    @Test
    public void matchingCategory_returnsTrue() {

        ItemSearchService service =
                new ItemSearchService();

        assertTrue(
                service.matchesCategory(
                        "Tools",
                        "Tools"
                )
        );
    }


    @Test
    public void matchingCategory_isCaseInsensitive() {

        ItemSearchService service =
                new ItemSearchService();

        assertTrue(
                service.matchesCategory(
                        "Tools",
                        "tools"
                )
        );
    }


    @Test
    public void differentCategory_returnsFalse() {

        ItemSearchService service =
                new ItemSearchService();

        assertFalse(
                service.matchesCategory(
                        "Tools",
                        "Camping"
                )
        );
    }


    @Test
    public void noCategoryFilter_returnsTrue() {

        ItemSearchService service =
                new ItemSearchService();

        assertTrue(
                service.matchesCategory(
                        "Tools",
                        null
                )
        );
    }


    @Test
    public void itemWithinPriceLimit_returnsTrue() {

        ItemSearchService service =
                new ItemSearchService();

        assertTrue(
                service.matchesMaxPrice(
                        5.0,
                        10.0
                )
        );
    }


    @Test
    public void itemAbovePriceLimit_returnsFalse() {

        ItemSearchService service =
                new ItemSearchService();

        assertFalse(
                service.matchesMaxPrice(
                        15.0,
                        10.0
                )
        );
    }


    @Test
    public void noPriceFilter_returnsTrue() {

        ItemSearchService service =
                new ItemSearchService();

        assertTrue(
                service.matchesMaxPrice(
                        15.0,
                        null
                )
        );
    }


    @Test
    public void itemWithinDistanceLimit_returnsTrue() {

        ItemSearchService service =
                new ItemSearchService();

        assertTrue(
                service.matchesMaxDistance(
                        2.4,
                        5.0
                )
        );
    }


    @Test
    public void itemBeyondDistanceLimit_returnsFalse() {

        ItemSearchService service =
                new ItemSearchService();

        assertFalse(
                service.matchesMaxDistance(
                        7.5,
                        5.0
                )
        );
    }


    @Test
    public void noDistanceFilter_returnsTrue() {

        ItemSearchService service =
                new ItemSearchService();

        assertTrue(
                service.matchesMaxDistance(
                        7.5,
                        null
                )
        );
    }
}