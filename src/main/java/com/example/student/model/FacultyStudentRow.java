package com.example.student.model;

public record FacultyStudentRow(
        long enrollmentId,
        String rollNo,
        String name,
        boolean presentToday,
        double internal,
        double midSem,
        double endSem,
        boolean marksLocked) {
}
