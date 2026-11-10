package com.example.student.model;

import java.time.LocalDateTime;

public record ComplaintRow(
        long complaintId,
        long studentId,
        long courseId,
        long facultyId,
        String rollNo,
        String courseCode,
        String category,
        String subject,
        String description,
        String status,
        String facultyRemark,
        LocalDateTime createdAt) {
}
