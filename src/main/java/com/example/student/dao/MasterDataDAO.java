package com.example.student.dao;

import com.example.student.DBConnection;
import com.example.student.model.MasterDataTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MasterDataDAO {
    public enum Entity {
        STUDENTS("SELECT student_id, roll_no, name, department, semester, contact FROM students ORDER BY roll_no"),
        FACULTY("SELECT faculty_id, emp_id, name, department FROM faculty ORDER BY emp_id"),
        COURSES("SELECT course_id, course_code, course_name, credits, faculty_id FROM courses ORDER BY course_code");

        private final String selectSql;

        Entity(String selectSql) {
            this.selectSql = selectSql;
        }
    }

    public MasterDataTable list(long adminUserId, Entity entity) throws SQLException {
        try (Connection connection = DBConnection.getInstance().getConnection()) {
            requireAdmin(connection, adminUserId);
            try (PreparedStatement statement = connection.prepareStatement(entity.selectSql);
                 ResultSet resultSet = statement.executeQuery()) {
                var metadata = resultSet.getMetaData();
                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    columns.add(metadata.getColumnLabel(i));
                }
                List<Object[]> rows = new ArrayList<>();
                while (resultSet.next()) {
                    Object[] row = new Object[columns.size()];
                    for (int i = 0; i < row.length; i++) row[i] = resultSet.getObject(i + 1);
                    rows.add(row);
                }
                return new MasterDataTable(columns, rows);
            }
        }
    }

    public void update(long adminUserId, Entity entity, Object[] row) throws SQLException {
        if (row == null) throw new IllegalArgumentException("Select a record to update.");
        String sql = switch (entity) {
            case STUDENTS -> "UPDATE students SET roll_no = ?, name = ?, department = ?, semester = ?, contact = ? WHERE student_id = ?";
            case FACULTY -> "UPDATE faculty SET emp_id = ?, name = ?, department = ? WHERE faculty_id = ?";
            case COURSES -> "UPDATE courses SET course_code = ?, course_name = ?, credits = ?, faculty_id = ? WHERE course_id = ?";
        };
        try (Connection connection = DBConnection.getInstance().getConnection()) {
            requireAdmin(connection, adminUserId);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                switch (entity) {
                    case STUDENTS -> {
                        statement.setString(1, text(row[1], "Roll number"));
                        statement.setString(2, text(row[2], "Student name"));
                        statement.setString(3, text(row[3], "Department"));
                        int semester = integer(row[4], "Semester");
                        if (semester < 1 || semester > 255) throw new IllegalArgumentException("Semester must be 1-255.");
                        statement.setInt(4, semester);
                        if (row[5] == null || row[5].toString().isBlank()) {
                            statement.setNull(5, java.sql.Types.VARCHAR);
                        } else {
                            statement.setString(5, row[5].toString().trim());
                        }
                        statement.setLong(6, id(row[0]));
                    }
                    case FACULTY -> {
                        statement.setString(1, text(row[1], "Employee ID"));
                        statement.setString(2, text(row[2], "Faculty name"));
                        statement.setString(3, text(row[3], "Department"));
                        statement.setLong(4, id(row[0]));
                    }
                    case COURSES -> {
                        statement.setString(1, text(row[1], "Course code"));
                        statement.setString(2, text(row[2], "Course name"));
                        int credits = integer(row[3], "Credits");
                        if (credits < 1 || credits > 255) throw new IllegalArgumentException("Credits must be 1-255.");
                        statement.setInt(3, credits);
                        if (row[4] == null || row[4].toString().isBlank()) {
                            statement.setNull(4, java.sql.Types.BIGINT);
                        } else {
                            statement.setLong(4, id(row[4]));
                        }
                        statement.setLong(5, id(row[0]));
                    }
                }
                if (statement.executeUpdate() != 1) throw new SQLException("Record not found or unchanged.");
            }
        }
    }

    public void delete(long adminUserId, Entity entity, long recordId) throws SQLException {
        try (Connection connection = DBConnection.getInstance().getConnection()) {
            requireAdmin(connection, adminUserId);
            connection.setAutoCommit(false);
            try {
                String sql = switch (entity) {
                    case STUDENTS -> """
                            DELETE FROM users WHERE user_id = (SELECT user_id FROM students WHERE student_id = ?)
                            """;
                    case FACULTY -> """
                            DELETE FROM users WHERE user_id = (SELECT user_id FROM faculty WHERE faculty_id = ?)
                            """;
                    case COURSES -> "DELETE FROM courses WHERE course_id = ?";
                };
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setLong(1, recordId);
                    if (statement.executeUpdate() != 1) throw new SQLException("Record not found: " + recordId);
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

    private void requireAdmin(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM users WHERE user_id = ? AND role = 'ADMIN'")) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new SQLException("Administrative privileges are required.");
            }
        }
    }

    private long id(Object value) {
        long id = Long.parseLong(String.valueOf(value));
        if (id <= 0) throw new IllegalArgumentException("Record ID must be positive.");
        return id;
    }

    private int integer(Object value, String label) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a whole number.", e);
        }
    }

    private String text(Object value, String label) {
        String text = value == null ? "" : value.toString().trim();
        if (text.isEmpty()) throw new IllegalArgumentException(label + " is required.");
        return text;
    }
}
