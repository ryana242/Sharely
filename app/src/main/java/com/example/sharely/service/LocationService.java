package com.example.sharely.service;

public class LocationService {

    private static final double EARTH_RADIUS_KM = 6371.0;


    public double calculateDistanceKm(
            double latitude1,
            double longitude1,
            double latitude2,
            double longitude2
    ) {

        double latitudeDifference =
                Math.toRadians(
                        latitude2 - latitude1
                );

        double longitudeDifference =
                Math.toRadians(
                        longitude2 - longitude1
                );


        double firstLatitude =
                Math.toRadians(
                        latitude1
                );

        double secondLatitude =
                Math.toRadians(
                        latitude2
                );


        double a =
                Math.sin(latitudeDifference / 2)
                        * Math.sin(latitudeDifference / 2)

                        +

                        Math.cos(firstLatitude)
                                * Math.cos(secondLatitude)

                                *

                                Math.sin(longitudeDifference / 2)
                                * Math.sin(longitudeDifference / 2);


        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );


        return EARTH_RADIUS_KM * c;
    }
}