package com.mulungushi.campuscompanionapp.network;

import java.util.List;

/**
 * GET /api/notifications →
 *   { notifications: [ { notification_id, type, title, body, is_read, created_at } ],
 *     unread_count }
 */
public class NotificationsResponse {
    private List<Item> notifications;
    private int unread_count;

    public List<Item> getNotifications() { return notifications; }
    public int getUnreadCount() { return unread_count; }

    public static class Item {
        private long notification_id;
        private String type;
        private String title;
        private String body;
        private boolean is_read;
        private String created_at;

        public long getNotificationId() { return notification_id; }
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getBody() { return body; }
        public boolean isRead() { return is_read; }
        public String getCreatedAt() { return created_at; }
    }
}
