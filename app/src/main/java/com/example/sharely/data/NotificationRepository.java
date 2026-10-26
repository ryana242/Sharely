package com.example.sharely.data;

import com.example.sharely.enums.NotificationType;
import com.example.sharely.model.Notification;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationRepository {

    private final FirebaseFirestore db;

    public NotificationRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // Generate a new notification ID
    public String generateNotificationId() {
        return db.collection("notifications").document().getId();
    }

    // Convert Firestore document to Notification object
    private Notification documentToNotification(DocumentSnapshot document) {

        String notificationId = document.getString("notificationId");
        String recipientId = document.getString("recipientId");
        String senderId = document.getString("senderId");
        String typeString = document.getString("type");
        String title = document.getString("title");
        String message = document.getString("message");
        String relatedId = document.getString("relatedId");

        Boolean isRead = document.getBoolean("isRead");
        Long createdAt = document.getLong("createdAt");

        if (notificationId == null
                || recipientId == null
                || senderId == null
                || typeString == null
                || title == null
                || message == null
                || relatedId == null
                || isRead == null
                || createdAt == null) {
            return null;
        }

        NotificationType type;

        try {
            type = NotificationType.valueOf(typeString);
        } catch (IllegalArgumentException e) {
            return null;
        }

        return new Notification(
                notificationId,
                recipientId,
                senderId,
                type,
                title,
                message,
                relatedId,
                isRead,
                createdAt

        );
    }

    // Save a notification to Firestore
    public void addNotification(
            Notification notification,
            OnNotificationSavedListener listener
    ) {

        Map<String, Object> notificationData = new HashMap<>();

        notificationData.put(
                "notificationId",
                notification.getNotificationId()
        );

        notificationData.put(
                "recipientId",
                notification.getRecipientId()
        );

        notificationData.put(
                "senderId",
                notification.getSenderId()
        );

        notificationData.put(
                "type",
                notification.getType().name()
        );

        notificationData.put(
                "title",
                notification.getTitle()
        );

        notificationData.put(
                "message",
                notification.getMessage()
        );

        notificationData.put(
                "relatedId",
                notification.getRelatedId()
        );

        notificationData.put(
                "isRead",
                false
        );

        notificationData.put(
                "createdAt",
                notification.getCreatedAt()
        );

        db.collection("notifications")
                .document(notification.getNotificationId())
                .set(notificationData)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    // Get all notifications for a user
    public void getNotificationsByUser(
            String userId,
            OnNotificationsLoadedListener listener
    ) {

        db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    List<Notification> notifications = new ArrayList<>();

                    for (DocumentSnapshot document
                            : querySnapshot.getDocuments()) {

                        Notification notification =
                                documentToNotification(document);

                        if (notification != null) {
                            notifications.add(notification);
                        }
                    }

                    listener.onSuccess(notifications);
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Get unread notifications for a user
    public void getUnreadNotifications(
            String userId,
            OnNotificationsLoadedListener listener
    ) {

        db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    List<Notification> notifications = new ArrayList<>();

                    for (DocumentSnapshot document
                            : querySnapshot.getDocuments()) {

                        Notification notification =
                                documentToNotification(document);

                        if (notification != null) {
                            notifications.add(notification);
                        }
                    }

                    listener.onSuccess(notifications);
                })
                .addOnFailureListener(listener::onFailure);
    }

    // Mark notification as read
    public void markAsRead(
            String notificationId,
            OnNotificationUpdatedListener listener
    ) {

        db.collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    // Mark notification as unread
    public void markAsUnread(
            String notificationId,
            OnNotificationUpdatedListener listener
    ) {

        db.collection("notifications")
                .document(notificationId)
                .update("isRead", false)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }



    // Callbacks
    public interface OnNotificationSavedListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnNotificationsLoadedListener {
        void onSuccess(List<Notification> notifications);
        void onFailure(Exception e);
    }

    public interface OnNotificationUpdatedListener {
        void onSuccess();
        void onFailure(Exception e);
    }
}