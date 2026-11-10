package com.example.student.model;

import java.sql.Timestamp;

public class Complaint {
    private int complaintId;
    private int studentId;
    private String studentRollNo;
    private String studentName;
    private int courseId;
    private String courseCode;
    private String courseName;
    private int facultyId;
    private String facultyName;
    private String category;
    private String subject;
    private String description;
    private String status;
    private String facultyRemark;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Complaint() {
    }

    public Complaint(int complaintId, int studentId, String studentRollNo, String studentName,
                     int courseId, String courseCode, String courseName, int facultyId,
                     String category, String subject, String description, String status,
                     String facultyRemark, Timestamp createdAt, Timestamp updatedAt) {
        this(complaintId, studentId, studentRollNo, studentName, courseId, courseCode,
                courseName, facultyId, null, category, subject, description, status,
                facultyRemark, createdAt, updatedAt);
    }

    public Complaint(int complaintId, int studentId, String studentRollNo, String studentName,
                     int courseId, String courseCode, String courseName, int facultyId,
                     String facultyName, String category, String subject, String description,
                     String status, String facultyRemark, Timestamp createdAt, Timestamp updatedAt) {
        this.complaintId = complaintId;
        this.studentId = studentId;
        this.studentRollNo = studentRollNo;
        this.studentName = studentName;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.facultyId = facultyId;
        this.facultyName = facultyName;
        this.category = category;
        this.subject = subject;
        this.description = description;
        this.status = status;
        this.facultyRemark = facultyRemark;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getComplaintId() { return complaintId; }
    public void setComplaintId(int complaintId) { this.complaintId = complaintId; }
    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public String getStudentRollNo() { return studentRollNo; }
    public void setStudentRollNo(String studentRollNo) { this.studentRollNo = studentRollNo; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public int getFacultyId() { return facultyId; }
    public void setFacultyId(int facultyId) { this.facultyId = facultyId; }
    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFacultyRemark() { return facultyRemark; }
    public void setFacultyRemark(String facultyRemark) { this.facultyRemark = facultyRemark; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public int complaintId() { return complaintId; }
    public int studentId() { return studentId; }
    public int courseId() { return courseId; }
    public int facultyId() { return facultyId; }
    public String rollNo() { return studentRollNo; }
    public String courseCode() { return courseCode; }
    public String category() { return category; }
    public String subject() { return subject; }
    public String description() { return description; }
    public String status() { return status; }
    public String facultyRemark() { return facultyRemark; }
    public Timestamp createdAt() { return createdAt; }

    @Override
    public String toString() {
        return "#" + complaintId + " " + category + " - " + subject + " (" + status + ")";
    }
}
