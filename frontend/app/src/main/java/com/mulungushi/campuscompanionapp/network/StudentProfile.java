package com.mulungushi.campuscompanionapp.network;

/**
 * Mirrors the JSON returned by GET /api/students/me:
 * { student_id, student_number, name, programme_code, email, phone,
 *   lab_group, status, record_version }
 * lab_group is the group name (e.g. "G01") or null when unassigned.
 */
public class StudentProfile {
    private long student_id;
    private String student_number;
    private String name;
    private String programme_code;
    private String email;
    private String phone;
    private String lab_group;
    private String status;
    private int record_version;
    private int is_group_leader;

    public long getStudentId() { return student_id; }
    public String getStudentNumber() { return student_number; }
    public String getName() { return name; }
    public String getProgrammeCode() { return programme_code; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getLabGroup() { return lab_group; }
    public String getStatus() { return status; }
    public int getRecordVersion() { return record_version; }
    public boolean isGroupLeader() { return is_group_leader == 1; }

    /** CS/IT/DS -> full programme name for display. */
    public String getProgrammeName() {
        if (programme_code == null) return "";
        switch (programme_code) {
            case "CS": return "Computer Science";
            case "IT": return "Information Technology";
            case "DS": return "Data Science";
            default:   return programme_code;
        }
    }
}
