package com.mulungushi.campuscompanionapp.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CacheDao {
    // reminders
    @Query("SELECT * FROM cached_reminders ORDER BY COALESCE(remind_at, created_at) DESC")
    List<CachedReminder> getReminders();
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void putReminders(List<CachedReminder> items);
    @Query("DELETE FROM cached_reminders")
    void clearReminders();

    // timetable
    @Query("SELECT * FROM cached_timetable ORDER BY day_of_week, start_time")
    List<CachedTimetable> getTimetable();
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void putTimetable(List<CachedTimetable> items);
    @Query("DELETE FROM cached_timetable")
    void clearTimetable();

    // notifications
    @Query("SELECT * FROM cached_notifications ORDER BY created_at DESC")
    List<CachedNotification> getNotifications();
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void putNotifications(List<CachedNotification> items);
    @Query("DELETE FROM cached_notifications")
    void clearNotifications();
}
