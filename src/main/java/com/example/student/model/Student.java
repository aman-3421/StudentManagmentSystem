package com.example.student.model;

public class Student {
    private long studentId;
    private long userId;
    private String rollNo;
    private String name;
    private String department;
    private int semester;

    public Student() {
    }

    public Student(long studentId, long userId, String rollNo, String name,
                   String department, int semester) {
        this.studentId = studentId;
        this.userId = userId;
        this.rollNo = rollNo;
        this.name = name;
        this.department = department;
        this.semester = semester;
    }

    public long getStudentId() {
        return studentId;
    }

    public void setStudentId(long studentId) {
        this.studentId = studentId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }
}
