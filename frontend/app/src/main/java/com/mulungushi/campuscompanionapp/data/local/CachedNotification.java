package com.mulungushi.campuscompanionapp.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_notifications")
public class CachedNotification {
    @PrimaryKey
    public long notification_id;
    public String type;
    public String title;
    public String body;
    public boolean is_read;
    public String created_at;
    public long cached_at;
}
