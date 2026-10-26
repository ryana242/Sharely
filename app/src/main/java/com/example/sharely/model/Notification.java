package com.example.sharely.model;

import com.example.sharely.enums.NotificationType;

public class Notification {

    private String notificationId;


    //ownerID
    private String recipientId;

    private String senderId;

    private NotificationType type;

    private String title;

    private String message;

    //Could be bookingId or Chat ID
    private String relatedId;

    private boolean isRead;

    private long createdAt;

    public Notification(
            String notificationId,
            String recipientId,
            String senderId,
            NotificationType type,
            String title,
            String message,
            String relatedId
    ) {

        this.notificationId = notificationId;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.relatedId = relatedId;

        this.isRead = false;

        this.createdAt = System.currentTimeMillis();
    }

    //Constructor Overloading to avoid changing createdAt after reading by user
    public Notification(
            String notificationId,
            String recipientId,
            String senderId,
            NotificationType type,
            String title,
            String message,
            String relatedId,
            boolean isRead,
            long createdAt
    ) {
        this.notificationId = notificationId;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.relatedId = relatedId;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }


    public String getNotificationId() {
        return notificationId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public String getSenderId() {
        return senderId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getRelatedId() {
        return relatedId;
    }

    public boolean isRead() {
        return isRead;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public void markAsUnread() {
        this.isRead = false;
    }
}