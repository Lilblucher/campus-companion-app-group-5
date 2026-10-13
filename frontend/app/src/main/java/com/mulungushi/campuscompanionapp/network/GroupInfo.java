package com.mulungushi.campuscompanionapp.network;

import java.util.List;

/**
 * GET /api/students/me/group returns either:
 *   { "group": null, "message": "Not yet assigned to a group" }   (unassigned)
 * or:
 *   { group_id, group_name, max_members, active_members,
 *     members: [ { name, student_number, programme_code }, ... ] }
 */
public class GroupInfo {
    private Integer group_id;
    private String group_name;
    private Integer max_members;
    private Integer active_members;
    private String message;        // present only when unassigned
    private List<Member> members;

    public Integer getGroupId() { return group_id; }
    public String getGroupName() { return group_name; }
    public Integer getMaxMembers() { return max_members; }
    public Integer getActiveMembers() { return active_members; }
    public String getMessage() { return message; }
    public List<Member> getMembers() { return members; }

    /** True when the student has no group yet (no group_id / no members). */
    public boolean isAssigned() { return group_id != null && group_name != null; }

    private Integer places_left;
    public Integer getPlacesLeft() { return places_left; }

    public static class Member {
        private long student_id;
        private String name;
        private String student_number;
        private String programme_code;
        private String status;
        private int is_group_leader;

        public long getStudentId() { return student_id; }
        public String getName() { return name; }
        public String getStudentNumber() { return student_number; }
        public String getProgrammeCode() { return programme_code; }
        public String getStatus() { return status; }
        public boolean isGroupLeader() { return is_group_leader == 1; }
    }
}
