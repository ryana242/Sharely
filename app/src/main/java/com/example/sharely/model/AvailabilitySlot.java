package com.example.sharely.model;

public class AvailabilitySlot {

    private long startTime;
    private long endTime;


    public AvailabilitySlot(
            long startTime,
            long endTime
    ) {

        this.startTime = startTime;
        this.endTime = endTime;
    }


    public long getStartTime() {

        return startTime;
    }


    public long getEndTime() {

        return endTime;
    }


    public boolean isValid() {

        return startTime < endTime;
    }


    public boolean overlaps(
            AvailabilitySlot other
    ) {

        if (other == null) {

            return false;
        }


        return startTime < other.endTime
                &&
                endTime > other.startTime;
    }


    public boolean contains(
            AvailabilitySlot other
    ) {

        if (other == null) {

            return false;
        }


        if (!isValid()
                || !other.isValid()) {

            return false;
        }


        return other.startTime >= startTime
                &&
                other.endTime <= endTime;
    }
}