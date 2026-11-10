package com.example.student.dao;

import com.example.student.DBConnection;
import com.example.student.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;

public class UserDAO {
    private static final String AUTHENTICATE_SQL =
            "SELECT user_id, username, password_hash, role FROM users WHERE username = ?";

    public User authenticate(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null) {
            throw new IllegalArgumentException("Username and password are required.");
        }

        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(AUTHENTICATE_SQL)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }

                byte[] storedHash;
                try {
                    storedHash = HexFormat.of().parseHex(resultSet.getString("password_hash"));
                } catch (IllegalArgumentException e) {
                    throw new SQLException("Stored password hash is not valid hexadecimal.", e);
                }

                if (!MessageDigest.isEqual(hashPassword(password), storedHash)) {
                    return null;
                }

                return new User(
                        resultSet.getLong("user_id"),
                        resultSet.getString("username"),
                        resultSet.getString("role"));
            }
        }
    }

    private byte[] hashPassword(String password) throws SQLException {
        try {
            byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
            return MessageDigest.getInstance("SHA-256").digest(passwordBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new SQLException("SHA-256 is not available in this Java runtime.", e);
        }
    }
}
