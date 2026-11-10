package com.example.student.model;

public record AttendanceSummary(
        long courseId,
        String courseCode,
        String courseName,
        long attended,
        long total,
        double percentage) {
}
