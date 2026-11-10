package com.example.student.model;

import java.time.LocalDate;

public class AttendanceRecord {
    private long attendanceId;
    private long enrollmentId;
    private LocalDate date;
    private String status;

    public AttendanceRecord() {
    }

    public AttendanceRecord(long attendanceId, long enrollmentId, LocalDate date, String status) {
        this.attendanceId = attendanceId;
        this.enrollmentId = enrollmentId;
        this.date = date;
        this.status = status;
    }

    public long getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(long attendanceId) {
        this.attendanceId = attendanceId;
    }

    public long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
