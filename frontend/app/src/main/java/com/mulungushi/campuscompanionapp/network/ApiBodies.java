package com.mulungushi.campuscompanionapp.network;

/** Request bodies for the lecturer / group-leader / reminder / timetable APIs.
 *  Gson omits null fields, so partial bodies (e.g. PATCH) only send what is set. */
public class ApiBodies {

    public static class CreateStudent {
        public String name, student_number, programme_code, email, phone;
        public CreateStudent(String name, String sn, String prog, String email, String phone) {
            this.name = name; this.student_number = sn; this.programme_code = prog; this.email = email; this.phone = phone;
        }
    }

    public static class EditStudent {
        public String name, programme_code, student_number;
    }

    public static class Assign {
        public Integer group_id; // null = unassign
        public Assign(Integer g) { this.group_id = g; }
    }

    public static class AddMember {
        public String student_number;
        public AddMember(String sn) { this.student_number = sn; }
    }

    public static class GroupChangeRequest {
        public Integer requested_group_id;
        public String reason;
        public GroupChangeRequest(Integer gid, String reason) {
            this.requested_group_id = gid; this.reason = reason;
        }
    }

    public static class UpdateSelf {
        public String name, email, phone;
        public UpdateSelf(String name, String email, String phone) {
            this.name = name; this.email = email; this.phone = phone;
        }
    }

    public static class NumberCorrection {
        public String correct_number, reason;
        public NumberCorrection(String correctNumber, String reason) {
            this.correct_number = correctNumber; this.reason = reason;
        }
    }

    public static class CreateReminder {
        public String title, body, scope, target_programme, remind_at;
        public Integer target_group_id;
    }

    public static class Session {
        public String course_code, title, start_time, end_time, venue, room;
        public Integer group_id, day_of_week;
    }
}
