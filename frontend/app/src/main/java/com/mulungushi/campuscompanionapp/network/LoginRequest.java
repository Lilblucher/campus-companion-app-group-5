package com.mulungushi.campuscompanionapp.network;

/**
 * Login body. The backend accepts EITHER student_number (student login) OR
 * username (lecturer login, which is the lecturer's email). Gson omits null
 * fields, so only the relevant one is sent.
 */
public class LoginRequest {
    private String student_number;
    private String username;
    private String password;

    public LoginRequest(String student_number, String password) {
        this.student_number = student_number;
        this.password = password;
    }

    private LoginRequest() {}

    public static LoginRequest forStudent(String studentNumber, String password) {
        LoginRequest r = new LoginRequest();
        r.student_number = studentNumber;
        r.password = password;
        return r;
    }

    public static LoginRequest forLecturer(String email, String password) {
        LoginRequest r = new LoginRequest();
        r.username = email;
        r.password = password;
        return r;
    }
}
