package com.example.student.dao;

import com.example.student.DBConnection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AdminDAO {
    public void registerStudent(String username, String password, String rollNo, String name,
                                String department, int semester) throws SQLException {
        requireText(username, "Username");
        requireText(password, "Password");
        requireText(rollNo, "Roll number");
        requireText(name, "Name");
        requireText(department, "Department");
        if (semester < 1 || semester > 255) {
            throw new IllegalArgumentException("Semester must be between 1 and 255.");
        }

        inTransaction(connection -> {
            long userId = insertUser(connection, username, password, "STUDENT");
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO students (user_id, roll_no, name, department, semester) VALUES (?, ?, ?, ?, ?)")) {
                statement.setLong(1, userId);
                statement.setString(2, rollNo);
                statement.setString(3, name);
                statement.setString(4, department);
                statement.setInt(5, semester);
                statement.executeUpdate();
            }
        });
    }

    public void registerFaculty(String username, String password, String empId, String name,
                               String department) throws SQLException {
        requireText(username, "Username");
        requireText(password, "Password");
        requireText(empId, "Employee ID");
        requireText(name, "Name");
        requireText(department, "Department");

        inTransaction(connection -> {
            long userId = insertUser(connection, username, password, "FACULTY");
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO faculty (user_id, emp_id, name, department) VALUES (?, ?, ?, ?)")) {
                statement.setLong(1, userId);
                statement.setString(2, empId);
                statement.setString(3, name);
                statement.setString(4, department);
                statement.executeUpdate();
            }
        });
    }

    public void assignCourse(String courseCode, String courseName, int credits, Long facultyId)
            throws SQLException {
        requireText(courseCode, "Course code");
        requireText(courseName, "Course name");
        if (credits < 1 || credits > 255) {
            throw new IllegalArgumentException("Credits must be between 1 and 255.");
        }

        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO courses (course_code, course_name, credits, faculty_id) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, courseCode);
            statement.setString(2, courseName);
            statement.setInt(3, credits);
            if (facultyId == null) {
                statement.setNull(4, java.sql.Types.BIGINT);
            } else {
                statement.setLong(4, facultyId);
            }
            statement.executeUpdate();
        }
    }

    public Long findFacultyIdByEmployeeId(String empId) throws SQLException {
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT faculty_id FROM faculty WHERE emp_id = ?")) {
            statement.setString(1, empId);
            try (var resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong("faculty_id") : null;
            }
        }
    }

    public void enrollStudent(String rollNo, String courseCode, int semester) throws SQLException {
        requireText(rollNo, "Roll number");
        requireText(courseCode, "Course code");
        if (semester < 1 || semester > 255) {
            throw new IllegalArgumentException("Semester must be between 1 and 255.");
        }

        String sql = """
                INSERT INTO enrollments (student_id, course_id, semester)
                SELECT s.student_id, c.course_id, ?
                FROM students s CROSS JOIN courses c
                WHERE s.roll_no = ? AND c.course_code = ?
                """;
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, semester);
            statement.setString(2, rollNo);
            statement.setString(3, courseCode);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Student roll number or course code was not found.");
            }
        }
    }

    private long insertUser(Connection connection, String username, String password, String role)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, hashPassword(password));
            statement.setString(3, role);
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Could not retrieve the newly created user ID.");
                }
                return keys.getLong(1);
            }
        }
    }

    private String hashPassword(String password) throws SQLException {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(password.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new SQLException("SHA-256 is not available in this Java runtime.", e);
        }
    }

    private void inTransaction(SqlWork work) throws SQLException {
        try (Connection connection = DBConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                work.run(connection);
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

    private void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
    }

    @FunctionalInterface
    private interface SqlWork {
        void run(Connection connection) throws SQLException;
    }
}
