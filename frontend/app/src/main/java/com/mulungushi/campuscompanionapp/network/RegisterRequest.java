package com.mulungushi.campuscompanionapp.network;

public class RegisterRequest {
    private String name;
    private String student_number;
    private String programme_code;
    private String email;
    private String phone;
    private String password;
    private String claim_code;
    private int year_of_study;
    private String lab_group;

    public RegisterRequest(String name, String student_number, String programme_code,
                           String email, String phone, String password) {
        this(name, student_number, programme_code, email, phone, password, null, 0, null);
    }

    public RegisterRequest(String name, String student_number, String programme_code,
                           String email, String phone, String password, String claim_code) {
        this(name, student_number, programme_code, email, phone, password, claim_code, 0, null);
    }

    public RegisterRequest(String name, String student_number, String programme_code,
                           String email, String phone, String password, String claim_code,
                           int yearOfStudy, String labGroup) {
        this.name = name;
        this.student_number = student_number;
        this.programme_code = programme_code;
        this.email = email;
        this.phone = phone;
        this.password = password;
        // Blank/empty claim code must be sent as null so the server stores NULL
        // (it validates 3-20 chars when present).
        this.claim_code = (claim_code == null || claim_code.trim().isEmpty()) ? null : claim_code.trim();
        this.year_of_study = yearOfStudy;
        this.lab_group = (labGroup == null || labGroup.trim().isEmpty()) ? null : labGroup.trim();
    }
}
