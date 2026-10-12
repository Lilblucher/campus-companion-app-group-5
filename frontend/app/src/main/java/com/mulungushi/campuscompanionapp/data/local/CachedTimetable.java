package com.mulungushi.campuscompanionapp.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_timetable")
public class CachedTimetable {
    @PrimaryKey
    public long session_id;
    public String course_code;
    public String title;
    public String group_name;
    public int day_of_week;
    public String start_time;
    public String end_time;
    public String venue;
    public String room;
    public long cached_at;
}
