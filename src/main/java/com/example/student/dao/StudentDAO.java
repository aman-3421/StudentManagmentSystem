package com.example.student.dao;

import com.example.student.DBConnection;
import com.example.student.model.StudentProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StudentDAO {
    private static final String PROFILE_SQL = """
            SELECT s.roll_no, s.semester,
                   COALESCE(
                       SUM(CASE
                           WHEN m.enrollment_id IS NULL THEN NULL
                           WHEN m.internal + m.mid_sem + m.end_sem >= 90 THEN 10
                           WHEN m.internal + m.mid_sem + m.end_sem >= 80 THEN 9
                           WHEN m.internal + m.mid_sem + m.end_sem >= 70 THEN 8
                           WHEN m.internal + m.mid_sem + m.end_sem >= 60 THEN 7
                           WHEN m.internal + m.mid_sem + m.end_sem >= 40 THEN 6
                           ELSE 0
                       END * c.credits) / NULLIF(SUM(CASE WHEN m.enrollment_id IS NOT NULL THEN c.credits END), 0),
                       0) AS cgpa
            FROM students s
            LEFT JOIN enrollments e ON e.student_id = s.student_id
            LEFT JOIN courses c ON c.course_id = e.course_id
            LEFT JOIN marks m ON m.enrollment_id = e.enrollment_id
            WHERE s.user_id = ?
            GROUP BY s.student_id, s.roll_no, s.semester
            """;

    public StudentProfile getProfileForUser(long userId) throws SQLException {
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(PROFILE_SQL)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new StudentProfile(
                        resultSet.getString("roll_no"),
                        resultSet.getInt("semester"),
                        resultSet.getDouble("cgpa"));
            }
        }
    }

    public long getStudentIdForUser(long userId) throws SQLException {
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT student_id FROM students WHERE user_id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("No student record found for user ID " + userId + ".");
                }
                return resultSet.getLong("student_id");
            }
        }
    }
}
