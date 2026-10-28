package com.example.sharely.service;

import com.example.sharely.data.NotificationRepository;
import com.example.sharely.enums.NotificationType;
import com.example.sharely.model.Notification;

public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService() {
        notificationRepository = new NotificationRepository();
    }

    // Create and save a notification
    private void sendNotification(
            String recipientId,
            String senderId,
            NotificationType type,
            String title,
            String message,
            String relatedId,
            NotificationRepository.OnNotificationSavedListener listener
    ) {

        String notificationId =
                notificationRepository.generateNotificationId();

        Notification notification = new Notification(
                notificationId,
                recipientId,
                senderId,
                type,
                title,
                message,
                relatedId
        );

        notificationRepository.addNotification(notification, listener);
    }

    // Borrower sends a booking request to Owner
    public void notifyBookingRequested(
            String bookingId,
            String borrowerId,
            String ownerId,
            NotificationRepository.OnNotificationSavedListener listener
    ) {

        sendNotification(
                ownerId,
                borrowerId,
                NotificationType.BOOKING_REQUEST,
                "New Booking Request",
                "You have received a new booking request.",
                bookingId,
                listener
        );
    }

    // Owner accepts a booking request
    public void notifyBookingAccepted(
            String bookingId,
            String ownerId,
            String borrowerId,
            NotificationRepository.OnNotificationSavedListener listener
    ) {

        sendNotification(
                borrowerId,
                ownerId,
                NotificationType.BOOKING_ACCEPTED,
                "Booking Accepted",
                "Your booking request has been accepted.",
                bookingId,
                listener
        );
    }

    // Owner rejects a booking request
    public void notifyBookingRejected(
            String bookingId,
            String ownerId,
            String borrowerId,
            NotificationRepository.OnNotificationSavedListener listener
    ) {

        sendNotification(
                borrowerId,
                ownerId,
                NotificationType.BOOKING_REJECTED,
                "Booking Rejected",
                "Your booking request has been rejected.",
                bookingId,
                listener
        );
    }

    // Borrower cancels a booking
    public void notifyBookingCancelled(
            String bookingId,
            String borrowerId,
            String ownerId,
            NotificationRepository.OnNotificationSavedListener listener
    ) {

        sendNotification(
                ownerId,
                borrowerId,
                NotificationType.BOOKING_CANCELLED,
                "Booking Cancelled",
                "A borrower has cancelled their booking.",
                bookingId,
                listener
        );
    }

    // Notify a user about a new chat message
    public void notifyNewMessage(
            String conversationId,
            String senderId,
            String recipientId,
            NotificationRepository.OnNotificationSavedListener listener
    ) {

        sendNotification(
                recipientId,
                senderId,
                NotificationType.NEW_MESSAGE,
                "New Message",
                "You have received a new message.",
                conversationId,
                listener
        );
    }
}