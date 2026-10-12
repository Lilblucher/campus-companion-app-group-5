package com.mulungushi.campuscompanionapp.network;

import java.util.List;

/** GET /api/lecturer/students */
public class RosterResponse {
    public List<Student> students;
    public int total;
    public int limit;
    public int offset;

    public static class Student {
        public long student_id;
        public String student_number;
        public String name;
        public String programme_code;
        public String email;
        public String phone;
        public String lab_group;   // group name or null
        public String status;
        public int is_group_leader;
    }
}
