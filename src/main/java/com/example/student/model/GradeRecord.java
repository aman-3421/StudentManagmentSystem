package com.example.student.model;

import java.math.BigDecimal;

public class GradeRecord {
    private long markId;
    private long enrollmentId;
    private BigDecimal internal;
    private BigDecimal midSem;
    private BigDecimal endSem;
    private BigDecimal total;
    private String letterGrade;
    private boolean isLocked;

    public GradeRecord() {
    }

    public GradeRecord(long markId, long enrollmentId, BigDecimal internal, BigDecimal midSem,
                       BigDecimal endSem, BigDecimal total, String letterGrade, boolean locked) {
        this.markId = markId;
        this.enrollmentId = enrollmentId;
        this.internal = internal;
        this.midSem = midSem;
        this.endSem = endSem;
        this.total = total;
        this.letterGrade = letterGrade;
        this.isLocked = locked;
    }

    public long getMarkId() {
        return markId;
    }

    public void setMarkId(long markId) {
        this.markId = markId;
    }

    public long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public BigDecimal getInternal() {
        return internal;
    }

    public void setInternal(BigDecimal internal) {
        this.internal = internal;
    }

    public BigDecimal getMidSem() {
        return midSem;
    }

    public void setMidSem(BigDecimal midSem) {
        this.midSem = midSem;
    }

    public BigDecimal getEndSem() {
        return endSem;
    }

    public void setEndSem(BigDecimal endSem) {
        this.endSem = endSem;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getLetterGrade() {
        return letterGrade;
    }

    public void setLetterGrade(String letterGrade) {
        this.letterGrade = letterGrade;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        this.isLocked = locked;
    }
}
