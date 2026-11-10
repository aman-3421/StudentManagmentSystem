package com.example.student.model;

public record EnrolledCourseGrade(
        long enrollmentId,
        long courseId,
        String courseCode,
        String courseName,
        int credits,
        int semester,
        GradeRecord gradeRecord) {
}
