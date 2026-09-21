package com.example.sharely.model;

public class AvailabilitySlot {

    private long startTime;
    private long endTime;

    public AvailabilitySlot(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public boolean isValid() {
        return startTime < endTime;
    }
    public boolean overlaps(AvailabilitySlot other) {
        return startTime < other.endTime
                && endTime > other.startTime;
    }
}