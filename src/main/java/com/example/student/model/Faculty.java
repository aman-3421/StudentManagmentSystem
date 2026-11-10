package com.example.student.model;

public class Faculty {
    private long facultyId;
    private long userId;
    private String empId;
    private String name;
    private String department;

    public Faculty() {
    }

    public Faculty(long facultyId, long userId, String empId, String name, String department) {
        this.facultyId = facultyId;
        this.userId = userId;
        this.empId = empId;
        this.name = name;
        this.department = department;
    }

    public long getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(long facultyId) {
        this.facultyId = facultyId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
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
}
