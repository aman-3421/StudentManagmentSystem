package com.example.student.dao;

import com.example.student.DBConnection;
import com.example.student.model.FacultyCourse;
import com.example.student.model.FacultyStudentRow;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FacultyDAO {
    public List<FacultyCourse> getCoursesForUser(long userId) throws SQLException {
        String sql = """
                SELECT c.course_id, c.course_code, c.course_name
                FROM faculty f JOIN courses c ON c.faculty_id = f.faculty_id
                WHERE f.user_id = ? ORDER BY c.course_code
                """;
        List<FacultyCourse> courses = new ArrayList<>();
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(new FacultyCourse(resultSet.getLong("course_id"),
                            resultSet.getString("course_code"), resultSet.getString("course_name")));
                }
            }
        }
        return courses;
    }

    public List<FacultyStudentRow> getStudentsForCourse(long courseId) throws SQLException {
        String sql = """
                SELECT e.enrollment_id, s.roll_no, s.name,
                       COALESCE(a.status IN ('PRESENT', 'LATE'), FALSE) AS present_today,
                       m.internal, m.mid_sem, m.end_sem, m.is_locked
                FROM enrollments e
                JOIN students s ON s.student_id = e.student_id
                LEFT JOIN attendance a ON a.enrollment_id = e.enrollment_id AND a.`date` = ?
                LEFT JOIN marks m ON m.enrollment_id = e.enrollment_id
                WHERE e.course_id = ?
                ORDER BY s.roll_no
                """;
        List<FacultyStudentRow> students = new ArrayList<>();
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(LocalDate.now()));
            statement.setLong(2, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    students.add(new FacultyStudentRow(
                            resultSet.getLong("enrollment_id"),
                            resultSet.getString("roll_no"),
                            resultSet.getString("name"),
                            resultSet.getBoolean("present_today"),
                            resultSet.getBigDecimal("internal") == null ? 0
                                    : resultSet.getBigDecimal("internal").doubleValue(),
                            resultSet.getBigDecimal("mid_sem") == null ? 0
                                    : resultSet.getBigDecimal("mid_sem").doubleValue(),
                            resultSet.getBigDecimal("end_sem") == null ? 0
                                    : resultSet.getBigDecimal("end_sem").doubleValue(),
                            resultSet.getBoolean("is_locked")));
                }
            }
        }
        return students;
    }

    public void saveAttendance(long facultyUserId, long courseId, Map<Long, Boolean> attendance)
            throws SQLException {
        String ownerSql = """
                SELECT 1
                FROM enrollments e
                JOIN courses c ON c.course_id = e.course_id
                JOIN faculty f ON f.faculty_id = c.faculty_id
                WHERE e.enrollment_id = ? AND e.course_id = ? AND f.user_id = ?
                """;
        String attendanceSql = """
                INSERT INTO attendance (enrollment_id, `date`, status) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status)
                """;
        try (Connection connection = DBConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement ownerStatement = connection.prepareStatement(ownerSql);
                 PreparedStatement attendanceStatement = connection.prepareStatement(attendanceSql)) {
                for (Map.Entry<Long, Boolean> entry : attendance.entrySet()) {
                    ownerStatement.setLong(1, entry.getKey());
                    ownerStatement.setLong(2, courseId);
                    ownerStatement.setLong(3, facultyUserId);
                    try (ResultSet resultSet = ownerStatement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("Faculty account is not authorized for enrollment "
                                    + entry.getKey() + ".");
                        }
                    }

                    attendanceStatement.setLong(1, entry.getKey());
                    attendanceStatement.setDate(2, Date.valueOf(LocalDate.now()));
                    attendanceStatement.setString(3, entry.getValue() ? "PRESENT" : "ABSENT");
                    attendanceStatement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        }
    }

}
