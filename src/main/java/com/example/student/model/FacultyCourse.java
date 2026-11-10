package com.example.student.model;

public record FacultyCourse(long courseId, String courseCode, String courseName) {
    @Override
    public String toString() {
        return courseCode + " - " + courseName;
    }
}
