package com.mulungushi.campuscompanionapp.network;

/** Generic response for simple write endpoints. Unused fields stay null. */
public class MessageResponse {
    public String message;
    public String error;
    public Long student_id;
    public Long reminder_id;
    public Long session_id;
    public Integer group_id;
    public String claim_code;
    public Integer recipients;
    public Integer members;
    public Integer lecturers_notified;
    public String status;
}
