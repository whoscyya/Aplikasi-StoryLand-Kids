package com.astrantiabooks.models;

public class User {
    private String uid;
    private String email;
    private String username;
    private String role; // "user" atau "admin"
    private String profileImageUrl;

    // 1. PENTING: Constructor Kosong untuk Firebase Firestore
    public User() {
    }

    // 2. Constructor untuk membuat object baru
    public User(String uid, String email, String username, String role) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.role = role;
        this.profileImageUrl = ""; // Default kosong
    }

    // 3. Getter dan Setter (Wajib agar Firebase bisa membaca/menulis)
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
}