
package com.example.sharely.data;

import com.example.sharely.enums.BookingStatus;
import com.example.sharely.model.AvailabilitySlot;
import com.example.sharely.model.Booking;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookingRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private Booking documentToBooking(DocumentSnapshot document) {
        if (document == null || !document.exists()) return null;

        String id = document.getString("bookingId");
        String borrowerId = document.getString("borrowerId");
        String itemId = document.getString("itemId");
        Long start = document.getLong("startTime");
        Long end = document.getLong("endTime");
        String rawStatus = document.getString("status");

        if (id == null || !id.equals(document.getId())
                || borrowerId == null || borrowerId.trim().isEmpty()
                || itemId == null || itemId.trim().isEmpty()
                || start == null || end == null || rawStatus == null) {
            return null;
        }

        AvailabilitySlot slot = new AvailabilitySlot(start, end);
        if (!slot.isValid()) return null;

        Booking booking = new Booking(id, borrowerId, itemId, slot);
        BookingStatus status;

        try {
            status = BookingStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException e) {
            return null;
        }

        switch (status) {
            case ACCEPTED:
                booking.acceptBooking();
                break;
            case ACTIVE:
                booking.acceptBooking();
                booking.startBooking();
                break;
            case COMPLETED:
                booking.acceptBooking();
                booking.startBooking();
                booking.completeBooking();
                break;
            case REJECTED:
                booking.rejectBooking();
                break;
            case CANCELLED:
                booking.cancelBooking();
                break;
            case PENDING:
                break;
        }

        return booking;
    }

    public String generateBookingId() {
        return db.collection("bookings").document().getId();
    }

    public void addBooking(
            Booking booking,
            OnBookingSavedListener listener
    ) {
        if (booking == null || !booking.hasValidTimeSlot()
                || booking.getStatus() != BookingStatus.PENDING
                || booking.getBookingId() == null
                || booking.getBookingId().trim().isEmpty()) {
            listener.onFailure(
                    new IllegalArgumentException("Invalid booking request")
            );
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("bookingId", booking.getBookingId());
        data.put("borrowerId", booking.getBorrowerId());
        data.put("itemId", booking.getItemId());
        data.put("startTime", booking.getBookingSlot().getStartTime());
        data.put("endTime", booking.getBookingSlot().getEndTime());
        data.put("status", BookingStatus.PENDING.name());

        db.collection("bookings")
                .document(booking.getBookingId())
                .set(data)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    public void getBookingsByBorrower(
            String borrowerId,
            OnBookingsLoadedListener listener
    ) {
        db.collection("bookings")
                .whereEqualTo("borrowerId", borrowerId)
                .get()
                .addOnSuccessListener(snapshot ->
                        deliverBookings(snapshot.getDocuments(), listener))
                .addOnFailureListener(listener::onFailure);
    }

    public void getBookingsByItem(
            String itemId,
            OnBookingsLoadedListener listener
    ) {
        db.collection("bookings")
                .whereEqualTo("itemId", itemId)
                .get()
                .addOnSuccessListener(snapshot ->
                        deliverBookings(snapshot.getDocuments(), listener))
                .addOnFailureListener(listener::onFailure);
    }

    private void deliverBookings(
            List<DocumentSnapshot> documents,
            OnBookingsLoadedListener listener
    ) {
        List<Booking> bookings = new ArrayList<>();

        for (DocumentSnapshot document : documents) {
            Booking booking = documentToBooking(document);

            if (booking == null) {
                listener.onFailure(
                        new IllegalStateException("Malformed booking record")
                );
                return;
            }

            bookings.add(booking);
        }

        listener.onSuccess(bookings);
    }

    private String signedInUid(OnBookingStatusUpdatedListener listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            listener.onFailure(
                    new IllegalStateException("Please sign in")
            );
            return null;
        }

        return user.getUid();
    }

    // Parse and validate the item's accepted reservation ledger.

    private List<Map<String, Object>> readLedger(DocumentSnapshot item) {
        List<Map<String, Object>> result = new ArrayList<>();

        Object stored = item.get("acceptedReservations");

        if (stored == null) {
            return result;
        }

        if (!(stored instanceof List<?>)) {
            throw new IllegalStateException("Invalid reservation ledger");
        }

        for (Object entry : (List<?>) stored) {
            if (!(entry instanceof Map<?, ?>)) {
                throw new IllegalStateException("Invalid reservation record");
            }

            Map<?, ?> record = (Map<?, ?>) entry;

            Object id = record.get("bookingId");
            Object start = record.get("startTime");
            Object end = record.get("endTime");

            if (!(id instanceof String)
                    || ((String) id).trim().isEmpty()
                    || !(start instanceof Number)
                    || !(end instanceof Number)) {
                throw new IllegalStateException("Invalid reservation fields");
            }

            AvailabilitySlot slot = new AvailabilitySlot(
                    ((Number) start).longValue(),
                    ((Number) end).longValue()
            );

            if (!slot.isValid()) {
                throw new IllegalStateException("Invalid reserved dates");
            }

            Map<String, Object> clean = new HashMap<>();
            clean.put("bookingId", id);
            clean.put("startTime", slot.getStartTime());
            clean.put("endTime", slot.getEndTime());

            result.add(clean);
        }

        return result;
    }

    private void removeReservation(
            List<Map<String, Object>> ledger,
            Booking booking
    ) {
        for (int i = ledger.size() - 1; i >= 0; i--) {
            Map<String, Object> entry = ledger.get(i);

            if (booking.getBookingId().equals(entry.get("bookingId"))) {

                if (!Long.valueOf(
                        booking.getBookingSlot().getStartTime()
                ).equals(entry.get("startTime"))
                        || !Long.valueOf(
                        booking.getBookingSlot().getEndTime()
                ).equals(entry.get("endTime"))) {

                    throw new IllegalStateException(
                            "Reservation dates do not match booking"
                    );
                }

                ledger.remove(i);
            }
        }
    }

    // OWNER: Accept or reject a pending request.

    public void decideBooking(
            String bookingId,
            boolean accept,
            OnBookingStatusUpdatedListener listener
    ) {
        String uid = signedInUid(listener);
        if (uid == null) return;

        if (bookingId == null || bookingId.trim().isEmpty()) {
            listener.onFailure(
                    new IllegalArgumentException("Missing booking ID")
            );
            return;
        }

        DocumentReference bookingRef =
                db.collection("bookings").document(bookingId);

        bookingRef.get().addOnSuccessListener(snapshot -> {

            Booking selected = documentToBooking(snapshot);

            if (selected == null
                    || selected.getStatus() != BookingStatus.PENDING) {

                listener.onFailure(
                        new IllegalStateException(
                                "Request is missing or no longer pending"
                        )
                );
                return;
            }

            // Check legacy accepted bookings without a reservation ledger.

            getBookingsByItem(
                    selected.getItemId(),
                    new OnBookingsLoadedListener() {

                        @Override
                        public void onSuccess(List<Booking> existing) {

                            if (accept) {
                                for (Booking other : existing) {
                                    if (!bookingId.equals(other.getBookingId())
                                            && other.blocksAvailability()
                                            && selected.conflictsWith(other)) {

                                        listener.onFailure(
                                                new IllegalStateException(
                                                        "Dates already reserved"
                                                )
                                        );
                                        return;
                                    }
                                }
                            }

                            DocumentReference itemRef =
                                    db.collection("items")
                                            .document(selected.getItemId());

                            db.runTransaction(tx -> {

                                DocumentSnapshot item = tx.get(itemRef);
                                DocumentSnapshot bookingDoc = tx.get(bookingRef);

                                Booking current =
                                        documentToBooking(bookingDoc);

                                if (!item.exists()
                                        || current == null
                                        || !selected.getItemId().equals(
                                        current.getItemId())
                                        || current.getStatus()
                                        != BookingStatus.PENDING) {

                                    throw new IllegalStateException(
                                            "Request changed; refresh the list"
                                    );
                                }

                                if (!uid.equals(item.getString("ownerId"))) {
                                    throw new IllegalStateException(
                                            "Only the item owner can decide"
                                    );
                                }

                                if (accept) {

                                    if (!"ACTIVE".equals(
                                            item.getString("status"))) {
                                        throw new IllegalStateException(
                                                "Listing is not active"
                                        );
                                    }

                                    Long from =
                                            item.getLong("availabilityStart");

                                    Long to =
                                            item.getLong("availabilityEnd");

                                    if (from == null || to == null
                                            || !new AvailabilitySlot(
                                            from, to
                                    ).contains(
                                            current.getBookingSlot()
                                    )
                                            || current.getBookingSlot()
                                            .getStartTime()
                                            <= System.currentTimeMillis()) {

                                        throw new IllegalStateException(
                                                "Booking is outside future availability"
                                        );
                                    }

                                    List<Map<String, Object>> ledger =
                                            readLedger(item);

                                    for (Map<String, Object> entry : ledger) {

                                        AvailabilitySlot reserved =
                                                new AvailabilitySlot(
                                                        (Long) entry.get("startTime"),
                                                        (Long) entry.get("endTime")
                                                );

                                        if (reserved.overlaps(
                                                current.getBookingSlot())) {

                                            throw new IllegalStateException(
                                                    "Those dates are already reserved"
                                            );
                                        }
                                    }

                                    Map<String, Object> entry =
                                            new HashMap<>();

                                    entry.put("bookingId", bookingId);
                                    entry.put(
                                            "startTime",
                                            current.getBookingSlot().getStartTime()
                                    );
                                    entry.put(
                                            "endTime",
                                            current.getBookingSlot().getEndTime()
                                    );

                                    ledger.add(entry);

                                    tx.update(
                                            itemRef,
                                            "acceptedReservations",
                                            ledger
                                    );
                                }

                                tx.update(
                                        bookingRef,
                                        "status",
                                        accept
                                                ? BookingStatus.ACCEPTED.name()
                                                : BookingStatus.REJECTED.name()
                                );

                                return null;

                            }).addOnSuccessListener(
                                    unused -> listener.onSuccess()
                            ).addOnFailureListener(listener::onFailure);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            listener.onFailure(e);
                        }
                    }
            );

        }).addOnFailureListener(listener::onFailure);
    }

    // BORROWER: Cancel a pending booking, or an accepted one
    // before its scheduled pickup time.

    public void cancelBooking(
            String bookingId,
            OnBookingStatusUpdatedListener listener
    ) {
        String uid = signedInUid(listener);
        if (uid == null) return;

        if (bookingId == null || bookingId.trim().isEmpty()) {
            listener.onFailure(
                    new IllegalArgumentException("Missing booking ID")
            );
            return;
        }

        DocumentReference bookingRef =
                db.collection("bookings").document(bookingId);

        bookingRef.get().addOnSuccessListener(initial -> {

            Booking seen = documentToBooking(initial);

            if (seen == null) {
                listener.onFailure(
                        new IllegalStateException("Booking not found")
                );
                return;
            }

            DocumentReference itemRef =
                    db.collection("items").document(seen.getItemId());

            db.runTransaction(tx -> {

                DocumentSnapshot bookingDoc = tx.get(bookingRef);
                DocumentSnapshot item = tx.get(itemRef);

                Booking current = documentToBooking(bookingDoc);

                if (current == null
                        || !seen.getItemId().equals(current.getItemId())) {

                    throw new IllegalStateException(
                            "Booking changed; refresh and retry"
                    );
                }

                if (!uid.equals(current.getBorrowerId())) {
                    throw new IllegalStateException(
                            "Only the borrower can cancel"
                    );
                }

                BookingStatus status = current.getStatus();

                if (status != BookingStatus.PENDING
                        && status != BookingStatus.ACCEPTED) {

                    throw new IllegalStateException(
                            "This booking cannot be cancelled"
                    );
                }

                if (status == BookingStatus.ACCEPTED) {

                    if (System.currentTimeMillis()
                            >= current.getBookingSlot().getStartTime()) {

                        throw new IllegalStateException(
                                "Pickup time has passed; contact the owner"
                        );
                    }

                    if (!item.exists()) {
                        throw new IllegalStateException(
                                "Item missing; unable to release reservation"
                        );
                    }

                    List<Map<String, Object>> ledger =
                            readLedger(item);

                    removeReservation(ledger, current);

                    tx.update(
                            itemRef,
                            "acceptedReservations",
                            ledger
                    );
                }

                tx.update(
                        bookingRef,
                        "status",
                        BookingStatus.CANCELLED.name()
                );

                return null;

            }).addOnSuccessListener(
                    unused -> listener.onSuccess()
            ).addOnFailureListener(listener::onFailure);

        }).addOnFailureListener(listener::onFailure);
    }

    // OWNER: Confirm pickup or return of an item.

    public void advanceBooking(
            String bookingId,
            OnBookingStatusUpdatedListener listener
    ) {
        String uid = signedInUid(listener);
        if (uid == null) return;

        if (bookingId == null || bookingId.trim().isEmpty()) {
            listener.onFailure(
                    new IllegalArgumentException("Missing booking ID")
            );
            return;
        }

        DocumentReference bookingRef =
                db.collection("bookings").document(bookingId);

        bookingRef.get().addOnSuccessListener(initial -> {

            Booking seen = documentToBooking(initial);

            if (seen == null) {
                listener.onFailure(
                        new IllegalStateException("Booking not found")
                );
                return;
            }

            DocumentReference itemRef =
                    db.collection("items").document(seen.getItemId());

            db.runTransaction(tx -> {

                DocumentSnapshot bookingDoc = tx.get(bookingRef);
                DocumentSnapshot item = tx.get(itemRef);

                Booking current = documentToBooking(bookingDoc);

                if (!item.exists()
                        || current == null
                        || !seen.getItemId().equals(current.getItemId())) {

                    throw new IllegalStateException(
                            "Booking or item changed; refresh"
                    );
                }

                if (!uid.equals(item.getString("ownerId"))) {
                    throw new IllegalStateException(
                            "Only the item owner can update pickup/return"
                    );
                }

                long now = System.currentTimeMillis();

                if (current.getStatus() == BookingStatus.ACCEPTED) {

                    if (now < current.getBookingSlot().getStartTime()
                            || now >= current.getBookingSlot().getEndTime()) {

                        throw new IllegalStateException(
                                "Booking cannot start outside its dates"
                        );
                    }

                    tx.update(
                            bookingRef,
                            "status",
                            BookingStatus.ACTIVE.name()
                    );

                } else if (current.getStatus() == BookingStatus.ACTIVE) {

                    if (now < current.getBookingSlot().getStartTime()) {
                        throw new IllegalStateException(
                                "Pickup has not started"
                        );
                    }

                    List<Map<String, Object>> ledger =
                            readLedger(item);

                    removeReservation(ledger, current);

                    tx.update(
                            itemRef,
                            "acceptedReservations",
                            ledger
                    );

                    tx.update(
                            bookingRef,
                            "status",
                            BookingStatus.COMPLETED.name()
                    );

                } else {

                    throw new IllegalStateException(
                            "Only accepted or ongoing bookings can advance"
                    );
                }

                return null;

            }).addOnSuccessListener(
                    unused -> listener.onSuccess()
            ).addOnFailureListener(listener::onFailure);

        }).addOnFailureListener(listener::onFailure);
    }

    // Retain the old method for compatibility with existing callers.

    public void updateBookingStatus(
            String bookingId,
            BookingStatus status,
            OnBookingStatusUpdatedListener listener
    ) {
        if (status == BookingStatus.REJECTED) {
            decideBooking(bookingId, false, listener);
        } else {
            listener.onFailure(
                    new IllegalStateException(
                            "Use owner/borrower lifecycle methods"
                    )
            );
        }
    }

    public interface OnBookingSavedListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnBookingsLoadedListener {
        void onSuccess(List<Booking> bookings);
        void onFailure(Exception e);
    }

    public interface OnBookingStatusUpdatedListener {
        void onSuccess();
        void onFailure(Exception e);
    }
}
