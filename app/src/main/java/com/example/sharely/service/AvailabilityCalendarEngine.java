package com.example.sharely.service;

import com.example.sharely.model.AvailabilitySlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Pure booking-calendar calculations. All periods are half-open [start, end),
 * so a new borrowing may begin exactly when another one ends.
 */
public final class AvailabilityCalendarEngine {

    private AvailabilityCalendarEngine() { }

    public static final long MINUTE = 60_000L;

    /** Return the usable (unreserved) windows within the requested day. */
    public static List<AvailabilitySlot> freeWindows(
            AvailabilitySlot owner,
            List<AvailabilitySlot> reserved,
            long dayStart,
            long nextDayStart,
            long earliestStart) {

        List<AvailabilitySlot> result = new ArrayList<>();
        if (owner == null || !owner.isValid() || dayStart >= nextDayStart) {
            return result;
        }

        long begin = Math.max(Math.max(owner.getStartTime(), dayStart), earliestStart);
        long finish = Math.min(owner.getEndTime(), nextDayStart);
        if (begin >= finish) return result;

        List<AvailabilitySlot> sorted = new ArrayList<>();
        if (reserved != null) {
            for (AvailabilitySlot slot : reserved) {
                if (slot != null && slot.isValid()
                        && slot.getEndTime() > begin
                        && slot.getStartTime() < finish) {
                    sorted.add(slot);
                }
            }
        }
        Collections.sort(sorted, Comparator.comparingLong(AvailabilitySlot::getStartTime));

        long cursor = begin;
        for (AvailabilitySlot taken : sorted) {
            long bookedStart = Math.max(taken.getStartTime(), begin);
            long bookedEnd = Math.min(taken.getEndTime(), finish);
            if (bookedStart > cursor) {
                result.add(new AvailabilitySlot(cursor, bookedStart));
            }
            cursor = Math.max(cursor, bookedEnd);
            if (cursor >= finish) break;
        }
        if (cursor < finish) result.add(new AvailabilitySlot(cursor, finish));
        return result;
    }

    /** Full continuous free period after a chosen start, possibly across days. */
    public static long freeUntil(
            long start,
            AvailabilitySlot owner,
            List<AvailabilitySlot> reserved) {
        if (owner == null || !owner.isValid()
                || start < owner.getStartTime()
                || start >= owner.getEndTime()) return start;

        long until = owner.getEndTime();
        if (reserved != null) {
            for (AvailabilitySlot taken : reserved) {
                if (taken == null || !taken.isValid()) continue;
                if (taken.getStartTime() <= start && taken.getEndTime() > start) {
                    return start;
                }
                if (taken.getStartTime() > start) {
                    until = Math.min(until, taken.getStartTime());
                }
            }
        }
        return until;
    }

    public static boolean isSelectable(
            long start,
            long end,
            AvailabilitySlot owner,
            List<AvailabilitySlot> reserved,
            long now) {
        if (owner == null || !owner.isValid()
                || start <= now || end <= start
                || start < owner.getStartTime()
                || end > owner.getEndTime()) return false;
        return freeUntil(start, owner, reserved) >= end;
    }
}
