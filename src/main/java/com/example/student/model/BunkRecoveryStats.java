package com.example.student.model;

public record BunkRecoveryStats(
        long attended,
        long total,
        double percentage,
        int classesToAttend,
        int classesCanMiss) {
}
