package com.mulungushi.campuscompanionapp.network;

public class RegisterRequest {
    private String name;
    private String student_number;
    private String programme_code;
    private String password;

    public RegisterRequest(String name, String student_number,
                           String programme_code, String password) {
        this.name = name;
        this.student_number = student_number;
        this.programme_code = programme_code;
        this.password = password;
    }
}