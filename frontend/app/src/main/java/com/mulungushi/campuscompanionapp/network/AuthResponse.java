

package com.mulungushi.campuscompanionapp.network;

public class AuthResponse {
    private String token;
    private String role;
    private String student_id;
    private String name;

    public String getToken() { return token; }
    public String getRole() { return role; }
    public String getStudentId() { return student_id; }
    public String getName() { return name; }
}
