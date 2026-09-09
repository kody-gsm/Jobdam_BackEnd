package com.example.kodyjobdam.user.dto;

public class UserProfileResponse {

    private String name;
    private String email;
    private String student_number;
    private String profileImageUrl;

    public UserProfileResponse(String name, String email, String student_number, String profileImageUrl) {
        this.name = name;
        this.email = email;
        this.student_number = student_number;
        this.profileImageUrl = profileImageUrl;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getStudent_number() { return student_number; }
    public String getProfileImageUrl() { return profileImageUrl; }
}
