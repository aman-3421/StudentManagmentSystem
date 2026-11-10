package com.example.student.view;

import com.example.student.dao.UserDAO;
import com.example.student.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

public class LoginFrame extends JFrame {
    private final JTextField usernameField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final UserDAO userDAO = new UserDAO();
    private JButton signInButton;

    public LoginFrame() {
        setTitle("Student Management System | Sign in");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(440, 390);
        setResizable(false);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        getRootPane().setDefaultButton(signInButton);
    }

    private JPanel buildContent() {
        JPanel page = new JPanel(new GridBagLayout());
        page.setBorder(new EmptyBorder(28, 32, 28, 32));
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(28, 30, 28, 30));

        JLabel title = new JLabel("Welcome back");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 25f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel subtitle = new JLabel("Sign in to your student portal");
        subtitle.setForeground(new Color(100, 110, 125));
        subtitle.setBorder(new EmptyBorder(5, 0, 20, 0));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        addField(card, "Username", usernameField);
        addField(card, "Password", passwordField);
        signInButton = loginButton();
        signInButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        signInButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        card.add(Box.createVerticalStrut(10));
        card.add(signInButton);
        card.add(Box.createVerticalStrut(12));
        JLabel demo = new JLabel("Demo: admin / Admin@123");
        demo.setForeground(new Color(115, 125, 140));
        demo.setFont(demo.getFont().deriveFont(11f));
        demo.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(title, 0);
        card.add(subtitle, 1);
        card.add(demo);
        page.add(card);
        return page;
    }

    private void addField(JPanel panel, String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(5));
        panel.add(field);
        panel.add(Box.createVerticalStrut(14));
    }

    private JButton loginButton() {
        JButton button = new JButton("Sign in");
        button.addActionListener(event -> authenticate());
        return button;
    }

    private void authenticate() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        try {
            User user = userDAO.authenticate(username, password);
            if (user == null) {
                JOptionPane.showMessageDialog(this, "Invalid username or password.",
                        "Sign in failed", JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
                return;
            }

            JFrame dashboard = switch (user.getRole()) {
                case "ADMIN" -> new AdminDashboard(user);
                case "FACULTY" -> new FacultyDashboard(user);
                case "STUDENT" -> new StudentDashboard(user);
                default -> throw new IllegalStateException("Unsupported account role: " + user.getRole());
            };
            dashboard.setVisible(true);
            dispose();
        } catch (SQLException | IllegalArgumentException | IllegalStateException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    "Unable to sign in", JOptionPane.ERROR_MESSAGE);
        }
    }
}
