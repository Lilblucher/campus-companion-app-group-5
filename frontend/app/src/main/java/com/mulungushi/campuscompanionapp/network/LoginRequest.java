package com.mulungushi.campuscompanionapp.network;

public class LoginRequest {
    private String student_number;
    private String password;

    public LoginRequest(String student_number, String password) {
        this.student_number = student_number;
        this.password = password;
    }
}