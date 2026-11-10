package com.example.student;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    private static final String URL =
            "jdbc:mysql://localhost:3306/student_management?serverTimezone=UTC";

    private DBConnection() {
    }

    private static class InstanceHolder {
        private static final DBConnection INSTANCE = new DBConnection();
    }

    public static DBConnection getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        String username = DatabaseConfig.get("SMS_DB_USER", "root");
        String password = DatabaseConfig.get("SMS_DB_PASSWORD", "");
        if (password.isEmpty()) {
            throw new SQLException("SMS_DB_PASSWORD must be configured in .env or the environment.");
        }
        return DriverManager.getConnection(URL, username, password);
    }
}
