package com.mulungushi.campuscompanionapp.network;

import java.util.List;

/** GET /api/timetable (and TimetableResponse.Session is reused by /next). */
public class TimetableResponse {
    public List<Session> sessions;

    public static class Session {
        public long session_id;
        public String course_code;
        public String title;
        public Integer group_id;     // null = all groups
        public String group_name;
        public int day_of_week;      // 0=Sun .. 6=Sat
        public String start_time;    // "HH:MM:SS"
        public String end_time;
        public String venue;
        public String room;
    }
}
