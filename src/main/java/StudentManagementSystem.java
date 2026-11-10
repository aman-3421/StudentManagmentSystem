import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import com.example.student.view.LoginFrame;
import com.formdev.flatlaf.FlatLightLaf;
import com.example.student.DatabaseConfig;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Locale;

/*
 * Student Management System
 * Frontend: Java Swing
 * Storage: MySQL database using JDBC
 * Features: Registration, marks, attendance, result calculation, search, and records table
 */

class Student {

    int rollNo;
    String name;
    String course;
    int age;
    double[] marks = new double[3];
    int totalClasses;
    int presentClasses;
    boolean marksEntered;

    Student(int rollNo, String name, String course, int age) {
        this.rollNo = rollNo;
        this.name = name;
        this.course = course;
        this.age = age;
    }

    double getTotalMarks() {
        return marks[0] + marks[1] + marks[2];
    }

    double getPercentage() {
        return getTotalMarks() / 3.0;
    }

    String getGrade() {
        if (!marksEntered) return "--";
        double percentage = getPercentage();
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 40) return "D";
        return "F";
    }

    double getAttendancePercentage() {
        if (totalClasses <= 0) return 0.0;
        return presentClasses * 100.0 / totalClasses;
    }
}

public class StudentManagementSystem extends JFrame {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/student_management?serverTimezone=UTC";
    private static final String DB_USER = DatabaseConfig.get("SMS_DB_USER", "root");
    private static final String DB_PASSWORD = DatabaseConfig.get("SMS_DB_PASSWORD", "");

    private final ArrayList<Student> students = new ArrayList<>();

    private final Color navy = new Color(27, 45, 75);
    private final Color blue = new Color(45, 105, 185);
    private final Color lightBackground = new Color(244, 247, 251);
    private final Color green = new Color(35, 139, 105);

    // Dashboard labels
    private JLabel totalStudentsLabel;
    private JLabel marksEnteredLabel;
    private JLabel attendanceLabel;

    // Registration fields
    private JTextField regRollField;
    private JTextField regNameField;
    private JTextField regCourseField;
    private JTextField regAgeField;

    // Marks fields
    private JTextField marksRollField;
    private JTextField subject1Field;
    private JTextField subject2Field;
    private JTextField subject3Field;

    // Attendance fields
    private JTextField attendanceRollField;
    private JTextField totalClassesField;
    private JTextField presentClassesField;

    // Result/search fields
    private JTextField resultRollField;
    private JTextArea resultArea;

    // Records table and filter
    private JTextField recordsSearchField;
    private DefaultTableModel tableModel;
    private JTable recordsTable;

    public StudentManagementSystem() {
        loadRecords();
        repairLoadedData();

        setTitle("Student Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(950, 650));
        setSize(1100, 740);
        setLocationRelativeTo(null);
        setContentPane(buildMainPanel());
        refreshAllViews();
    }

    private JPanel buildMainPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(lightBackground);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(navy);
        header.setBorder(new EmptyBorder(18, 24, 18, 24));

        JLabel title = new JLabel("STUDENT MANAGEMENT SYSTEM");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 23));

        JLabel subtitle = new JLabel("Registration  |  Marks  |  Attendance  |  Results");
        subtitle.setForeground(new Color(220, 229, 241));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JPanel titleStack = new JPanel();
        titleStack.setOpaque(false);
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.add(title);
        titleStack.add(Box.createVerticalStrut(5));
        titleStack.add(subtitle);
        header.add(titleStack, BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.addTab("Dashboard", buildDashboardPanel());
        tabs.addTab("Student Registration", buildRegistrationPanel());
        tabs.addTab("Marks Management", buildMarksPanel());
        tabs.addTab("Attendance", buildAttendancePanel());
        tabs.addTab("Results / Search", buildResultsPanel());
        tabs.addTab("All Records", buildRecordsPanel());
        tabs.setBorder(new EmptyBorder(10, 12, 12, 12));

        root.add(header, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 20));
        panel.setBackground(lightBackground);
        panel.setBorder(new EmptyBorder(22, 18, 22, 18));

        JLabel heading = new JLabel("Overview");
        heading.setFont(new Font("SansSerif", Font.BOLD, 22));
        heading.setForeground(navy);
        panel.add(heading, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 3, 16, 16));
        cards.setOpaque(false);
        totalStudentsLabel = new JLabel("0", SwingConstants.CENTER);
        marksEnteredLabel = new JLabel("0", SwingConstants.CENTER);
        attendanceLabel = new JLabel("0%", SwingConstants.CENTER);
        cards.add(makeSummaryCard("Registered Students", totalStudentsLabel, blue));
        cards.add(makeSummaryCard("Students With Marks", marksEnteredLabel, green));
        cards.add(makeSummaryCard("Average Attendance", attendanceLabel, new Color(155, 105, 35)));

        JPanel center = new JPanel(new BorderLayout(0, 18));
        center.setOpaque(false);
        center.add(cards, BorderLayout.NORTH);

        JTextArea information = new JTextArea(
            "WELCOME\n\n" +
            "Use the tabs above to manage student information.\n\n" +
            "1. Student Registration: add a student using a unique roll number.\n" +
            "2. Marks Management: enter or update marks for three subjects.\n" +
            "3. Attendance: record total classes and classes attended.\n" +
            "4. Results / Search: search by roll number and calculate the result.\n" +
            "5. All Records: view and filter the registered student list.\n\n" +
            "Records are stored in the MySQL database: student_management."
        );
        information.setEditable(false);
        information.setFocusable(false);
        information.setLineWrap(true);
        information.setWrapStyleWord(true);
        information.setFont(new Font("SansSerif", Font.PLAIN, 15));
        information.setForeground(new Color(55, 65, 80));
        information.setBackground(Color.WHITE);
        information.setBorder(new EmptyBorder(18, 20, 18, 20));
        center.add(information, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private JPanel makeSummaryCard(String heading, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(6, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 5, 0, 0, accent),
            new EmptyBorder(18, 15, 18, 15)
        ));
        JLabel label = new JLabel(heading, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        label.setForeground(new Color(85, 95, 110));
        value.setFont(new Font("SansSerif", Font.BOLD, 30));
        value.setForeground(accent);
        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildRegistrationPanel() {
        JPanel page = basePage("Register a Student", "Enter the student's basic details below.");
        JPanel form = new JPanel(new GridLayout(0, 2, 12, 12));
        form.setOpaque(false);

        regRollField = new JTextField();
        regNameField = new JTextField();
        regCourseField = new JTextField();
        regAgeField = new JTextField();

        addFormRow(form, "Roll Number", regRollField);
        addFormRow(form, "Student Name", regNameField);
        addFormRow(form, "Course / Class", regCourseField);
        addFormRow(form, "Age", regAgeField);

        JButton registerButton = makeButton("Register Student", blue);
        JButton clearButton = makeButton("Clear Form", new Color(105, 115, 130));
        registerButton.addActionListener(e -> registerStudent());
        clearButton.addActionListener(e -> clearRegistrationForm());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(registerButton);
        buttons.add(clearButton);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        content.add(form, BorderLayout.NORTH);
        content.add(buttons, BorderLayout.CENTER);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildMarksPanel() {
        JPanel page = basePage("Marks Management", "Enter marks between 0 and 100 for each subject.");
        JPanel form = new JPanel(new GridLayout(0, 2, 12, 12));
        form.setOpaque(false);

        marksRollField = new JTextField();
        subject1Field = new JTextField();
        subject2Field = new JTextField();
        subject3Field = new JTextField();
        addFormRow(form, "Student Roll Number", marksRollField);
        addFormRow(form, "Subject 1 Marks / 100", subject1Field);
        addFormRow(form, "Subject 2 Marks / 100", subject2Field);
        addFormRow(form, "Subject 3 Marks / 100", subject3Field);

        JButton loadButton = makeButton("Load Student Marks", new Color(105, 115, 130));
        JButton saveButton = makeButton("Save / Update Marks", blue);
        loadButton.addActionListener(e -> loadMarksIntoForm());
        saveButton.addActionListener(e -> saveMarks());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(loadButton);
        buttons.add(saveButton);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        content.add(form, BorderLayout.NORTH);
        content.add(buttons, BorderLayout.CENTER);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildAttendancePanel() {
        JPanel page = basePage("Attendance Management", "Record the total classes held and classes attended.");
        JPanel form = new JPanel(new GridLayout(0, 2, 12, 12));
        form.setOpaque(false);

        attendanceRollField = new JTextField();
        totalClassesField = new JTextField();
        presentClassesField = new JTextField();
        addFormRow(form, "Student Roll Number", attendanceRollField);
        addFormRow(form, "Total Classes Held", totalClassesField);
        addFormRow(form, "Classes Attended", presentClassesField);

        JButton loadButton = makeButton("Load Attendance", new Color(105, 115, 130));
        JButton saveButton = makeButton("Save / Update Attendance", green);
        loadButton.addActionListener(e -> loadAttendanceIntoForm());
        saveButton.addActionListener(e -> saveAttendance());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(loadButton);
        buttons.add(saveButton);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        content.add(form, BorderLayout.NORTH);
        content.add(buttons, BorderLayout.CENTER);

        JLabel note = new JLabel("Attendance % = (Classes Attended / Total Classes) × 100");
        note.setForeground(new Color(75, 85, 100));
        content.add(note, BorderLayout.SOUTH);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildResultsPanel() {
        JPanel page = basePage("Results and Student Search", "Search using a student's roll number to view their record and result.");
        JPanel searchRow = new JPanel(new BorderLayout(10, 0));
        searchRow.setOpaque(false);
        resultRollField = new JTextField();
        JButton searchButton = makeButton("Search / Calculate Result", blue);
        searchButton.addActionListener(e -> searchAndShowResult());
        searchRow.add(new JLabel("Roll Number:"), BorderLayout.WEST);
        searchRow.add(resultRollField, BorderLayout.CENTER);
        searchRow.add(searchButton, BorderLayout.EAST);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        resultArea.setBackground(Color.WHITE);
        resultArea.setBorder(new EmptyBorder(14, 14, 14, 14));
        resultArea.setText("Enter a roll number above and click Search / Calculate Result.");

        JPanel content = new JPanel(new BorderLayout(0, 15));
        content.setOpaque(false);
        content.add(searchRow, BorderLayout.NORTH);
        content.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildRecordsPanel() {
        JPanel page = basePage("All Student Records", "Use the search box to filter by roll number, name, or course.");
        JPanel searchRow = new JPanel(new BorderLayout(10, 0));
        searchRow.setOpaque(false);
        recordsSearchField = new JTextField();
        JButton searchButton = makeButton("Search", blue);
        JButton showAllButton = makeButton("Show All", new Color(105, 115, 130));
        searchButton.addActionListener(e -> refreshTable());
        showAllButton.addActionListener(e -> {
            recordsSearchField.setText("");
            refreshTable();
        });
        JPanel searchButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchButtons.setOpaque(false);
        searchButtons.add(searchButton);
        searchButtons.add(showAllButton);
        searchRow.add(recordsSearchField, BorderLayout.CENTER);
        searchRow.add(searchButtons, BorderLayout.EAST);

        String[] columns = {"Roll No.", "Name", "Course", "Age", "Sub 1", "Sub 2", "Sub 3", "Total / 300", "Percentage", "Grade", "Attendance"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        recordsTable = new JTable(tableModel);
        recordsTable.setRowHeight(26);
        recordsTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        recordsTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        recordsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        recordsTable.setFillsViewportHeight(true);

        JScrollPane tableScroll = new JScrollPane(recordsTable);
        tableScroll.setPreferredSize(new Dimension(850, 380));

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(searchRow, BorderLayout.NORTH);
        content.add(tableScroll, BorderLayout.CENTER);
        page.add(content, BorderLayout.CENTER);
        return page;
    }

    private JPanel basePage(String titleText, String subtitleText) {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(lightBackground);
        page.setBorder(new EmptyBorder(22, 22, 22, 22));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(titleText);
        title.setFont(new Font("SansSerif", Font.BOLD, 21));
        title.setForeground(navy);
        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(new Color(85, 95, 110));
        titles.add(title);
        titles.add(Box.createVerticalStrut(5));
        titles.add(subtitle);
        page.add(titles, BorderLayout.NORTH);
        return page;
    }

    private void addFormRow(JPanel form, String labelText, JTextField field) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(250, 34));
        form.add(label);
        form.add(field);
    }

    private JButton makeButton(String text, Color background) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(background);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        return button;
    }

    private void registerStudent() {
        try {
            int rollNo = Integer.parseInt(regRollField.getText().trim());
            int age = Integer.parseInt(regAgeField.getText().trim());
            String name = regNameField.getText().trim();
            String course = regCourseField.getText().trim();

            if (rollNo <= 0 || age <= 0) {
                showError("Roll number and age must be positive numbers.");
                return;
            }
            if (name.isEmpty() || course.isEmpty()) {
                showError("Please enter both the student name and course.");
                return;
            }
            if (findStudent(rollNo) != null) {
                showError("That roll number already exists. Use a different roll number.");
                return;
            }

            students.add(new Student(rollNo, name, course, age));
            if (saveRecords()) {
                refreshAllViews();
                clearRegistrationForm();
                showInfo("Student registered successfully.");
            }
        } catch (NumberFormatException ex) {
            showError("Roll number and age must be whole numbers.");
        }
    }

    private void clearRegistrationForm() {
        regRollField.setText("");
        regNameField.setText("");
        regCourseField.setText("");
        regAgeField.setText("");
        regRollField.requestFocusInWindow();
    }

    private void loadMarksIntoForm() {
        Student student = findStudentFromField(marksRollField);
        if (student == null) return;
        subject1Field.setText(formatMark(student.marks[0]));
        subject2Field.setText(formatMark(student.marks[1]));
        subject3Field.setText(formatMark(student.marks[2]));
        showInfo("Student found. You can now review or update the marks.");
    }

    private void saveMarks() {
        Student student = findStudentFromField(marksRollField);
        if (student == null) return;

        try {
            double mark1 = Double.parseDouble(subject1Field.getText().trim());
            double mark2 = Double.parseDouble(subject2Field.getText().trim());
            double mark3 = Double.parseDouble(subject3Field.getText().trim());
            if (!validMark(mark1) || !validMark(mark2) || !validMark(mark3)) {
                showError("Each mark must be between 0 and 100.");
                return;
            }

            student.marks[0] = mark1;
            student.marks[1] = mark2;
            student.marks[2] = mark3;
            student.marksEntered = true;
            if (saveRecords()) {
                refreshAllViews();
                showInfo("Marks saved successfully.");
            }
        } catch (NumberFormatException ex) {
            showError("Enter valid numeric marks for all three subjects.");
        }
    }

    private boolean validMark(double mark) {
        return mark >= 0 && mark <= 100 && Double.isFinite(mark);
    }

    private String formatMark(double mark) {
        if (mark == Math.rint(mark)) return String.valueOf((int) mark);
        return String.format(Locale.US, "%.2f", mark);
    }

    private void loadAttendanceIntoForm() {
        Student student = findStudentFromField(attendanceRollField);
        if (student == null) return;
        totalClassesField.setText(String.valueOf(student.totalClasses));
        presentClassesField.setText(String.valueOf(student.presentClasses));
        showInfo("Attendance loaded. You can now review or update it.");
    }

    private void saveAttendance() {
        Student student = findStudentFromField(attendanceRollField);
        if (student == null) return;

        try {
            int total = Integer.parseInt(totalClassesField.getText().trim());
            int present = Integer.parseInt(presentClassesField.getText().trim());
            if (total < 0) {
                showError("Total classes cannot be negative.");
                return;
            }
            if (present < 0 || present > total) {
                showError("Classes attended must be between 0 and total classes held.");
                return;
            }

            student.totalClasses = total;
            student.presentClasses = present;
            if (saveRecords()) {
                refreshAllViews();
                showInfo("Attendance saved successfully. Attendance: " +
                    String.format(Locale.US, "%.2f%%", student.getAttendancePercentage()));
            }
        } catch (NumberFormatException ex) {
            showError("Total classes and classes attended must be whole numbers.");
        }
    }

    private void searchAndShowResult() {
        Student student = findStudentFromField(resultRollField);
        if (student == null) {
            resultArea.setText("No student record was found for that roll number.");
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append("STUDENT DETAILS\n");
        text.append("-----------------------------------------------\n");
        text.append("Roll Number : ").append(student.rollNo).append('\n');
        text.append("Name        : ").append(student.name).append('\n');
        text.append("Course      : ").append(student.course).append('\n');
        text.append("Age         : ").append(student.age).append("\n\n");
        text.append("MARKS AND RESULT\n");
        text.append("-----------------------------------------------\n");
        if (student.marksEntered) {
            text.append(String.format(Locale.US, "Subject 1   : %.2f / 100%n", student.marks[0]));
            text.append(String.format(Locale.US, "Subject 2   : %.2f / 100%n", student.marks[1]));
            text.append(String.format(Locale.US, "Subject 3   : %.2f / 100%n", student.marks[2]));
            text.append(String.format(Locale.US, "Total Marks : %.2f / 300%n", student.getTotalMarks()));
            text.append(String.format(Locale.US, "Percentage  : %.2f%%%n", student.getPercentage()));
            text.append("Grade       : ").append(student.getGrade()).append('\n');
        } else {
            text.append("Marks have not been entered for this student yet.\n");
        }
        text.append("\nATTENDANCE\n");
        text.append("-----------------------------------------------\n");
        text.append("Total Classes   : ").append(student.totalClasses).append('\n');
        text.append("Classes Attended: ").append(student.presentClasses).append('\n');
        text.append(String.format(Locale.US, "Attendance      : %.2f%%%n", student.getAttendancePercentage()));
        text.append("\nNote: Grade is based on the average percentage of the three subjects.\n");
        resultArea.setText(text.toString());
        resultArea.setCaretPosition(0);
    }

    private Student findStudentFromField(JTextField field) {
        try {
            int rollNo = Integer.parseInt(field.getText().trim());
            if (rollNo <= 0) {
                showError("Enter a positive roll number.");
                return null;
            }
            Student student = findStudent(rollNo);
            if (student == null) showError("No student found with roll number " + rollNo + ".");
            return student;
        } catch (NumberFormatException ex) {
            showError("Enter a valid roll number.");
            return null;
        }
    }

    private Student findStudent(int rollNo) {
        for (Student student : students) {
            if (student.rollNo == rollNo) return student;
        }
        return null;
    }

    private void refreshAllViews() {
        refreshDashboard();
        refreshTable();
    }

    private void refreshDashboard() {
        if (totalStudentsLabel == null) return;
        totalStudentsLabel.setText(String.valueOf(students.size()));

        int withMarks = 0;
        double attendanceSum = 0.0;
        int withAttendance = 0;
        for (Student student : students) {
            if (student.marksEntered) withMarks++;
            if (student.totalClasses > 0) {
                attendanceSum += student.getAttendancePercentage();
                withAttendance++;
            }
        }
        marksEnteredLabel.setText(String.valueOf(withMarks));
        attendanceLabel.setText(withAttendance == 0 ? "0%" :
            String.format(Locale.US, "%.1f%%", attendanceSum / withAttendance));
    }

    private void refreshTable() {
        if (tableModel == null) return;
        tableModel.setRowCount(0);
        String query = recordsSearchField == null ? "" : recordsSearchField.getText().trim().toLowerCase(Locale.ROOT);

        for (Student student : students) {
            String searchable = (student.rollNo + " " + student.name + " " + student.course).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !searchable.contains(query)) continue;

            Object mark1 = student.marksEntered ? formatMark(student.marks[0]) : "--";
            Object mark2 = student.marksEntered ? formatMark(student.marks[1]) : "--";
            Object mark3 = student.marksEntered ? formatMark(student.marks[2]) : "--";
            Object total = student.marksEntered ? formatMark(student.getTotalMarks()) : "--";
            Object percentage = student.marksEntered ? String.format(Locale.US, "%.2f%%", student.getPercentage()) : "--";
            Object grade = student.getGrade();
            Object attendance = student.totalClasses > 0 ?
                String.format(Locale.US, "%.2f%%", student.getAttendancePercentage()) : "--";

            tableModel.addRow(new Object[]{student.rollNo, student.name, student.course, student.age,
                mark1, mark2, mark3, total, percentage, grade, attendance});
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private boolean saveRecords() {
        String sql = "INSERT INTO students " +
            "(roll_no, name, course, age, subject1, subject2, subject3, marks_entered, total_classes, present_classes) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE name=VALUES(name), course=VALUES(course), age=VALUES(age), " +
            "subject1=VALUES(subject1), subject2=VALUES(subject2), subject3=VALUES(subject3), " +
            "marks_entered=VALUES(marks_entered), total_classes=VALUES(total_classes), present_classes=VALUES(present_classes)";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Student student : students) {
                statement.setInt(1, student.rollNo);
                statement.setString(2, student.name);
                statement.setString(3, student.course);
                statement.setInt(4, student.age);
                statement.setDouble(5, student.marks[0]);
                statement.setDouble(6, student.marks[1]);
                statement.setDouble(7, student.marks[2]);
                statement.setBoolean(8, student.marksEntered);
                statement.setInt(9, student.totalClasses);
                statement.setInt(10, student.presentClasses);
                statement.addBatch();
            }
            statement.executeBatch();
            return true;
        } catch (SQLException ex) {
            showError("Could not save to MySQL. Check that MySQL is running, the database exists, and credentials are correct.\n" + ex.getMessage());
            return false;
        }
    }

    private void loadRecords() {
        String sql = "SELECT roll_no, name, course, age, subject1, subject2, subject3, marks_entered, total_classes, present_classes FROM students ORDER BY roll_no";
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                Student student = new Student(
                    rs.getInt("roll_no"), rs.getString("name"),
                    rs.getString("course"), rs.getInt("age")
                );
                student.marks[0] = rs.getDouble("subject1");
                student.marks[1] = rs.getDouble("subject2");
                student.marks[2] = rs.getDouble("subject3");
                student.marksEntered = rs.getBoolean("marks_entered");
                student.totalClasses = rs.getInt("total_classes");
                student.presentClasses = rs.getInt("present_classes");
                students.add(student);
            }
        } catch (SQLException ex) {
            System.err.println("MySQL connection failed: " + ex.getMessage());
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                this,
                "Cannot connect to MySQL.\n\n1. Start MySQL in XAMPP/MySQL service.\n" +
                "2. Run sql/setup.sql in MySQL Workbench/phpMyAdmin.\n" +
                "3. Check DB_USER and DB_PASSWORD in the environment variables.\n\nDetails: " + ex.getMessage(),
                "Database Connection Error",
                JOptionPane.ERROR_MESSAGE
            ));
        }
    }

    private void repairLoadedData() {
        // Backward compatibility with the earlier console version, which did not store marksEntered.
        for (Student student : students) {
            if (student.marks != null && student.marks.length >= 3) {
                if (student.marks[0] != 0 || student.marks[1] != 0 || student.marks[2] != 0) {
                    student.marksEntered = true;
                }
            } else {
                student.marks = new double[3];
            }
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Check Input", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Student Management System", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            new LoginFrame().setVisible(true);
        });
    }
}
