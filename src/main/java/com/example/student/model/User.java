package com.example.student.model;

public class User {
    private long userId;
    private String username;
    private String role;

    public User() {
    }

    public User(long userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
