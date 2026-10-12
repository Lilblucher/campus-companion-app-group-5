package com.mulungushi.campuscompanionapp.network;

/** GET /api/timetable/next */
public class NextSessionResponse {
    public TimetableResponse.Session next; // null when the timetable is empty
}
