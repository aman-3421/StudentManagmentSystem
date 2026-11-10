package com.example.student.view;

import com.example.student.dao.AcademicDAO;
import com.example.student.dao.ComplaintDAO;
import com.example.student.dao.StudentDAO;
import com.example.student.model.AttendanceSummary;
import com.example.student.model.BunkRecoveryStats;
import com.example.student.model.Complaint;
import com.example.student.model.Course;
import com.example.student.model.EnrolledCourseGrade;
import com.example.student.model.GradeRecord;
import com.example.student.model.StudentProfile;
import com.example.student.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class StudentDashboard extends JFrame {
    private final User user;
    private final StudentDAO studentDAO = new StudentDAO();
    private final AcademicDAO academicDAO = new AcademicDAO();
    private final ComplaintDAO complaintDAO = new ComplaintDAO();
    private final JComboBox<Course> complaintCourse = new JComboBox<>();
    private final DefaultTableModel complaintModel = new DefaultTableModel(
            new Object[]{"ID", "Course", "Category", "Subject", "Status", "Faculty Feedback", "Submitted"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel attendanceModel = new DefaultTableModel(
            new Object[]{"Course", "Attendance", "Safe to Miss", "Classes to Recover", "Attendance %"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable attendanceTable = new JTable(attendanceModel);
    private final JTable complaintTable = new JTable(complaintModel);
    private final JComboBox<String> complaintCategory =
            new JComboBox<>(new String[]{"MARKS", "ATTENDANCE", "OTHER"});
    private final JTextField complaintSubject = new JTextField();
    private final JTextArea complaintDescription = new JTextArea(3, 35);
    private final DefaultTableModel gradeModel = new DefaultTableModel(
            new Object[]{"Course", "Semester", "Internal", "Mid-sem", "End-sem", "Total",
                    "Letter Grade", "Locked"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public StudentDashboard(User user) {
        this.user = user;
        setTitle("Student Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        attendanceTable.setDefaultRenderer(Object.class, new AttendanceRenderer());
        loadData();
    }

    private JPanel buildContent() {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setBorder(new EmptyBorder(18, 22, 22, 22));
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Student Dashboard");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        header.add(title, BorderLayout.WEST);
        header.add(new JLabel("Welcome, " + user.getUsername()), BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 3, 14, 0));
        cards.add(summaryCard("Roll Number", "Loading...", new JLabel()));
        cards.add(summaryCard("Semester", "Loading...", new JLabel()));
        cards.add(summaryCard("CGPA", "Loading...", new JLabel()));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Attendance", new JScrollPane(attendanceTable));
        tabs.addTab("Grade Sheet", new JScrollPane(new JTable(gradeModel)));
        tabs.addTab("Grievances", complaintsPanel());
        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.add(cards, BorderLayout.NORTH);
        center.add(tabs, BorderLayout.CENTER);
        page.add(center, BorderLayout.CENTER);
        attendanceTable.getColumnModel().getColumn(4).setMinWidth(0);
        attendanceTable.getColumnModel().getColumn(4).setMaxWidth(0);
        attendanceTable.getColumnModel().getColumn(4).setWidth(0);
        return page;
    }

    private JPanel complaintsPanel() {
        JPanel page = new JPanel(new BorderLayout(0, 10));
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(10, 4, 10, 4));
        complaintDescription.setLineWrap(true);
        complaintDescription.setWrapStyleWord(true);
        addComplaintField(form, 0, "Course", complaintCourse);
        addComplaintField(form, 1, "Category", complaintCategory);
        addComplaintField(form, 2, "Subject", complaintSubject);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        c.gridy = 3;
        form.add(new JLabel("Description"), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        form.add(new JScrollPane(complaintDescription), c);
        JButton submit = new JButton("Submit Grievance");
        c.gridy = 4;
        c.gridx = 1;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        form.add(submit, c);
        submit.addActionListener(event -> submitComplaint(submit));

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        JButton refresh = new JButton("Refresh grievances");
        refresh.addActionListener(event -> loadComplaints());
        top.add(refresh, BorderLayout.SOUTH);
        page.add(top, BorderLayout.NORTH);
        complaintTable.setAutoCreateRowSorter(true);
        complaintTable.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());
        page.add(new JScrollPane(complaintTable), BorderLayout.CENTER);
        return page;
    }

    private void addComplaintField(JPanel form, int row, String label, JComponent input) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.gridy = row;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(input, c);
    }

    private void submitComplaint(JButton submit) {
        Course course = (Course) complaintCourse.getSelectedItem();
        if (course == null) {
            showError("There are no enrolled courses with an assigned faculty member.");
            return;
        }
        String subject = complaintSubject.getText().trim();
        String description = complaintDescription.getText().trim();
        if (subject.isEmpty() || description.isEmpty()) {
            showError("Subject and description are required.");
            return;
        }
        Complaint complaint = new Complaint();
        complaint.setCourseId(Math.toIntExact(course.getCourseId()));
        complaint.setCategory(complaintCategory.getSelectedItem().toString());
        complaint.setSubject(subject);
        complaint.setDescription(description);
        submit.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                complaint.setStudentId(Math.toIntExact(studentDAO.getStudentIdForUser(user.getUserId())));
                complaintDAO.raiseComplaint(complaint);
                return null;
            }

            @Override
            protected void done() {
                submit.setEnabled(true);
                try {
                    get();
                    complaintSubject.setText("");
                    complaintDescription.setText("");
                    loadComplaints();
                    JOptionPane.showMessageDialog(StudentDashboard.this, "Your grievance was submitted.",
                            "Grievance submitted", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    showError("Could not submit grievance: " + rootMessage(e));
                }
            }
        }.execute();
    }

    private void loadComplaints() {
        new SwingWorker<List<Complaint>, Void>() {
            @Override
            protected List<Complaint> doInBackground() throws Exception {
                return complaintDAO.getComplaintsForStudent(user.getUserId());
            }

            @Override
            protected void done() {
                try {
                    complaintModel.setRowCount(0);
                    for (Complaint complaint : get()) {
                        complaintModel.addRow(new Object[]{
                                complaint.complaintId(), complaint.courseCode(), complaint.category(),
                                complaint.subject(), complaint.status(), complaint.facultyRemark(),
                                complaint.createdAt()
                        });
                    }
                } catch (Exception e) {
                    showError("Could not load grievances: " + rootMessage(e));
                }
            }
        }.execute();
    }

    private String rootMessage(Exception exception) {
        Throwable cause = exception.getCause();
        return cause == null ? exception.getMessage() : cause.getMessage();
    }

    private class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            Component component = super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            if (!selected) {
                setForeground(switch (String.valueOf(value)) {
                    case "PENDING" -> new Color(190, 110, 0);
                    case "RESOLVED" -> new Color(35, 130, 70);
                    case "REJECTED" -> new Color(180, 45, 45);
                    default -> table.getForeground();
                });
            }
            return component;
        }
    }

    private JPanel summaryCard(String title, String value, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 233)),
                new EmptyBorder(15, 18, 15, 18)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(new Color(100, 110, 125));
        valueLabel.setText(value);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 22f));
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.putClientProperty("valueLabel", valueLabel);
        return card;
    }

    private void loadData() {
        new SwingWorker<StudentDashboardData, Void>() {
            @Override
            protected StudentDashboardData doInBackground() throws Exception {
                StudentProfile profile = studentDAO.getProfileForUser(user.getUserId());
                if (profile == null) return null;
                int studentId = Math.toIntExact(studentDAO.getStudentIdForUser(user.getUserId()));
                List<AttendanceSummary> attendance = academicDAO.getStudentAttendanceSummary(studentId);
                List<BunkRecoveryStats> recovery = new java.util.ArrayList<>();
                for (AttendanceSummary summary : attendance) {
                    recovery.add(academicDAO.getBunkRecoveryStats(
                            studentId, Math.toIntExact(summary.courseId())));
                }
                return new StudentDashboardData(profile, attendance, recovery,
                        academicDAO.getEnrolledCoursesWithGrades(studentId),
                        complaintDAO.getEnrolledCoursesForUser(user.getUserId()));
            }

            @Override
            protected void done() {
                try {
                    StudentDashboardData data = get();
                    if (data == null) {
                        showError("No student profile is linked to this account.");
                        return;
                    }
                    JPanel center = (JPanel) ((BorderLayout) getContentPane().getLayout())
                            .getLayoutComponent(BorderLayout.CENTER);
                    JPanel cards = (JPanel) center.getComponent(0);
                    setCardValue(cards, 0, data.profile().rollNo());
                    setCardValue(cards, 1, Integer.toString(data.profile().semester()));
                    setCardValue(cards, 2, String.format("%.2f", data.profile().cgpa()));
                    attendanceModel.setRowCount(0);
                    for (int i = 0; i < data.attendance().size(); i++) {
                        AttendanceSummary summary = data.attendance().get(i);
                        BunkRecoveryStats stats = data.recovery().get(i);
                        attendanceModel.addRow(new Object[]{
                                summary.courseCode() + " - " + summary.courseName(),
                                String.format("%.2f%% (%d/%d)", summary.percentage(),
                                        summary.attended(), summary.total()),
                                stats.classesCanMiss() == 0 ? "-" : stats.classesCanMiss() + " classes",
                                stats.classesToAttend() == 0 ? "-" : stats.classesToAttend() + " classes",
                                summary.percentage()
                        });
                    }
                    gradeModel.setRowCount(0);
                    for (EnrolledCourseGrade course : data.grades()) {
                        GradeRecord grade = course.gradeRecord();
                        gradeModel.addRow(new Object[]{
                                course.courseCode() + " - " + course.courseName(), course.semester(),
                                grade == null ? "--" : grade.getInternal(),
                                grade == null ? "--" : grade.getMidSem(),
                                grade == null ? "--" : grade.getEndSem(),
                                grade == null ? "--" : grade.getTotal(),
                                grade == null ? "--" : grade.getLetterGrade(),
                                grade != null && grade.isLocked() ? "Yes" : "No"
                        });
                    }
                    complaintCourse.removeAllItems();
                    for (Course course : data.courses()) complaintCourse.addItem(course);
                    loadComplaints();
                } catch (Exception e) {
                    showError("Could not load student information: " + rootMessage(e));
                }
            }
        }.execute();
    }

    private record StudentDashboardData(StudentProfile profile, List<AttendanceSummary> attendance,
                                        List<BunkRecoveryStats> recovery,
                                        List<EnrolledCourseGrade> grades, List<Course> courses) {
    }

    private void setCardValue(JPanel cards, int index, String value) {
        JPanel card = (JPanel) cards.getComponent(index);
        JLabel label = (JLabel) card.getClientProperty("valueLabel");
        label.setText(value);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Student Dashboard", JOptionPane.ERROR_MESSAGE);
    }

    private class AttendanceRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean selected, boolean focused,
                                                       int row, int column) {
            Component component = super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            double percentage = ((Number) attendanceModel.getValueAt(row, 4)).doubleValue();
            if (!selected) {
                component.setBackground(percentage < 75 ? new Color(255, 225, 225) : table.getBackground());
                component.setForeground(table.getForeground());
            }
            return component;
        }
    }
}
