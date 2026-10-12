

package com.mulungushi.campuscompanionapp.network;

public class AuthResponse {
    private String token;
    private String role;
    private String student_id;
    private String name;
    private int is_group_leader;

    public String getToken() { return token; }
    public String getRole() { return role; }
    public String getStudentId() { return student_id; }
    public String getName() { return name; }
    public boolean isGroupLeader() { return is_group_leader == 1; }
}
