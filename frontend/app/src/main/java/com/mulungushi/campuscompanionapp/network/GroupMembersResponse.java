package com.mulungushi.campuscompanionapp.network;

import java.util.List;

public class GroupMembersResponse {
    public List<Member> members;

    public static class Member {
        public long student_id;
        public String student_number;
        public String name;
        public String programme_code;
        public boolean is_group_leader;
    }
}
