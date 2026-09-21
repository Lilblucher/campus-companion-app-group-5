package com.mulungushi.campuscompanionapp.network;

public class RegisterRequest {
    private String student_name;
    private String student_number;
    private String program_of_study;
    private String claim_code;
    private String password;

    public RegisterRequest(String student_name, String student_number,
                           String program_of_study, String claim_code, String password) {
        this.student_name = student_name;
        this.student_number = student_number;
        this.program_of_study = program_of_study;
        this.claim_code = claim_code;
        this.password = password;
    }
}