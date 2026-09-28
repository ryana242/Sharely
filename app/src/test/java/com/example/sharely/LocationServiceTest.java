package com.example.sharely;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.sharely.service.LocationService;

import org.junit.Test;

public class LocationServiceTest {


    @Test
    public void sameLocation_returnsZeroDistance() {

        LocationService service =
                new LocationService();

        double distance =
                service.calculateDistanceKm(
                        -33.8688,
                        151.2093,
                        -33.8688,
                        151.2093
                );

        assertEquals(
                0.0,
                distance,
                0.001
        );
    }


    @Test
    public void nearbyLocations_returnPositiveDistance() {

        LocationService service =
                new LocationService();

        double distance =
                service.calculateDistanceKm(
                        -33.8688,
                        151.2093,
                        -33.8731,
                        151.2065
                );

        assertTrue(
                distance > 0
        );
    }


    @Test
    public void reversedLocations_returnSameDistance() {

        LocationService service =
                new LocationService();

        double first =
                service.calculateDistanceKm(
                        -33.8688,
                        151.2093,
                        -33.8731,
                        151.2065
                );

        double second =
                service.calculateDistanceKm(
                        -33.8731,
                        151.2065,
                        -33.8688,
                        151.2093
                );

        assertEquals(
                first,
                second,
                0.001
        );
    }
}