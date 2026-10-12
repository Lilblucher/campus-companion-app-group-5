package com.mulungushi.campuscompanionapp.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_reminders")
public class CachedReminder {
    @PrimaryKey
    public long reminder_id;
    public String title;
    public String body;
    public String scope;
    public String creator_role;
    public String remind_at;
    public String created_at;
    public long cached_at;
}
