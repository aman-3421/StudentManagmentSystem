package com.example.student.view;

import com.example.student.dao.AcademicDAO;
import com.example.student.dao.ComplaintDAO;
import com.example.student.dao.FacultyDAO;
import com.example.student.model.AttendanceSummary;
import com.example.student.model.ComplaintRow;
import com.example.student.model.FacultyCourse;
import com.example.student.model.FacultyStudentRow;
import com.example.student.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class FacultyDashboard extends JFrame {
    private final User user;
    private final FacultyDAO facultyDAO = new FacultyDAO();
    private final ComplaintDAO complaintDAO = new ComplaintDAO();
    private final AcademicDAO academicDAO = new AcademicDAO();
    private final JComboBox<FacultyCourse> courseSelector = new JComboBox<>();
    private final DefaultTableModel attendanceModel = new DefaultTableModel(
            new Object[]{"Enrollment ID", "Roll No", "Student", "Present"}, 0) {
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 3 ? Boolean.class : columnIndex == 0 ? Long.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 3;
        }
    };
    private final DefaultTableModel marksModel = new DefaultTableModel(
            new Object[]{"Enrollment ID", "Roll No", "Student", "Internal (0-20)", "Mid-sem (0-30)",
                    "End-sem (0-50)", "Locked"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column >= 3 && column <= 5 && !Boolean.TRUE.equals(getValueAt(row, 6));
        }
    };
    private final JTable attendanceTable = new JTable(attendanceModel);
    private final JTable marksTable = new JTable(marksModel);
    private final DefaultTableModel complaintModel = new DefaultTableModel(
            new Object[]{"ID", "Roll No", "Course", "Category", "Subject", "Description",
                    "Status", "Feedback", "Created"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable complaintTable = new JTable(complaintModel);
    private final Map<Long, ComplaintRow> complaintRows = new HashMap<>();
    private final JComboBox<String> complaintFilter =
            new JComboBox<>(new String[]{"All", "PENDING", "RESOLVED"});
    private boolean loadingCourses;

    public FacultyDashboard(User user) {
        this.user = user;
        setTitle("Faculty Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 720);
        setMinimumSize(new Dimension(850, 560));
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        hideIdColumn(attendanceTable);
        hideIdColumn(marksTable);
        courseSelector.addActionListener(event -> {
            if (!loadingCourses) refreshRoster();
        });
        loadCourses();
    }

    private JPanel buildContent() {
        JPanel page = new JPanel(new BorderLayout(0, 14));
        page.setBorder(new EmptyBorder(18, 22, 22, 22));
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Faculty Dashboard");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        header.add(title, BorderLayout.WEST);
        header.add(new JLabel("Signed in as " + user.getUsername()), BorderLayout.EAST);
        page.add(header, BorderLayout.NORTH);

        JPanel courseBar = new JPanel(new BorderLayout(12, 0));
        courseBar.add(new JLabel("Course"), BorderLayout.WEST);
        courseBar.add(courseSelector, BorderLayout.CENTER);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refreshRoster());
        courseBar.add(refresh, BorderLayout.EAST);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Daily Attendance", attendancePanel());
        tabs.addTab("Marks", marksPanel());
        tabs.addTab("Grievance Inbox", complaintsPanel());
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.add(courseBar, BorderLayout.NORTH);
        center.add(tabs, BorderLayout.CENTER);
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    private JPanel attendancePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(new JScrollPane(attendanceTable), BorderLayout.CENTER);
        JButton save = new JButton("Save today's attendance");
        save.addActionListener(event -> saveAttendance());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.add(save);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel marksPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.add(new JScrollPane(marksTable), BorderLayout.CENTER);
        JButton finalize = new JButton("Finalize & Lock");
        finalize.addActionListener(event -> finalizeMarks());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.add(finalize);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel complaintsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        complaintTable.setAutoCreateRowSorter(true);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(complaintModel);
        complaintTable.setRowSorter(sorter);
        complaintFilter.addActionListener(event -> {
            String status = complaintFilter.getSelectedItem().toString();
            sorter.setRowFilter("All".equals(status) ? null : RowFilter.regexFilter("^" + status + "$", 6));
        });
        panel.add(new JScrollPane(complaintTable), BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Status"));
        controls.add(complaintFilter);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refreshComplaints());
        controls.add(refresh);
        panel.add(controls, BorderLayout.NORTH);
        JButton review = new JButton("Review & Rectify");
        review.addActionListener(event -> reviewComplaint());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(review);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private void refreshComplaints() {
        new SwingWorker<List<ComplaintRow>, Void>() {
            @Override
            protected List<ComplaintRow> doInBackground() throws Exception {
                return complaintDAO.getComplaintsForFaculty(user.getUserId());
            }

            @Override
            protected void done() {
                try {
                    complaintModel.setRowCount(0);
                    complaintRows.clear();
                    for (ComplaintRow complaint : get()) {
                        complaintRows.put(complaint.complaintId(), complaint);
                        complaintModel.addRow(new Object[]{
                                complaint.complaintId(), complaint.rollNo(), complaint.courseCode(),
                                complaint.category(), complaint.subject(), complaint.description(),
                                complaint.status(), complaint.facultyRemark(), complaint.createdAt()
                        });
                    }
                } catch (Exception e) {
                    showMessage("Could not load grievances: " + rootMessage(e), false);
                }
            }
        }.execute();
    }

    private void reviewComplaint() {
        int selectedRow = complaintTable.getSelectedRow();
        if (selectedRow < 0) {
            showMessage("Select a complaint to review.", false);
            return;
        }
        int modelRow = complaintTable.convertRowIndexToModel(selectedRow);
        long complaintId = ((Number) complaintModel.getValueAt(modelRow, 0)).longValue();
        ComplaintRow complaint = complaintRows.get(complaintId);
        if (complaint == null) return;
        if (!"PENDING".equals(complaint.status())) {
            showMessage("Only pending grievances can be reviewed.", false);
            return;
        }
        if ("ATTENDANCE".equals(complaint.category())) {
            new SwingWorker<AttendanceSummary, Void>() {
                @Override
                protected AttendanceSummary doInBackground() throws Exception {
                    return complaintDAO.getAttendanceCountsForFacultyComplaint(
                            Math.toIntExact(complaint.complaintId()), user.getUserId());
                }
                @Override
                protected void done() {
                    try {
                        showReviewDialog(complaint, get());
                    } catch (Exception e) {
                        showMessage("Could not load attendance details: " + rootMessage(e), false);
                    }
                }
            }.execute();
        } else {
            showReviewDialog(complaint, null);
        }
    }

    private void showReviewDialog(ComplaintRow complaint, AttendanceSummary counts) {
        JDialog dialog = new JDialog(this, "Review grievance #" + complaint.complaintId(), true);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(10, 12, 10, 12));
        JTextArea details = new JTextArea(complaint.description(), 3, 36);
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        JTextArea remark = new JTextArea(3, 36);
        remark.setLineWrap(true);
        remark.setWrapStyleWord(true);
        int row = 0;
        addDialogRow(form, row++, "Student / course", complaint.rollNo() + " / " + complaint.courseCode());
        addDialogRow(form, row++, "Category / subject", complaint.category() + " / " + complaint.subject());
        addDialogRow(form, row++, "Description", new JScrollPane(details));
        JTextField internal = new JTextField();
        JTextField midSem = new JTextField();
        JTextField endSem = new JTextField();
        JSpinner total = null;
        JSpinner present = null;
        JTextArea addDates = null;
        JTextArea removeDates = null;
        if ("MARKS".equals(complaint.category())) {
            addDialogRow(form, row++, "Internal (0-20)", internal);
            addDialogRow(form, row++, "Mid-sem (0-30)", midSem);
            addDialogRow(form, row++, "End-sem (0-50)", endSem);
        } else if ("ATTENDANCE".equals(complaint.category())) {
            total = new JSpinner(new SpinnerNumberModel((int) counts.total(), 0, 10000, 1));
            present = new JSpinner(new SpinnerNumberModel((int) counts.attended(), 0, 10000, 1));
            addDialogRow(form, row++, "Current total / present",
                    new JLabel(counts.total() + " / " + counts.attended()));
            addDialogRow(form, row++, "Corrected total classes", total);
            addDialogRow(form, row++, "Corrected present classes", present);
            addDates = new JTextArea(2, 36);
            removeDates = new JTextArea(2, 36);
            addDates.setToolTipText("One ISO date (YYYY-MM-DD) per line");
            removeDates.setToolTipText("One ISO date (YYYY-MM-DD) per line");
            addDialogRow(form, row++, "Dates to add (one per line)", new JScrollPane(addDates));
            addDialogRow(form, row++, "Dates to remove (one per line)", new JScrollPane(removeDates));
        }
        addDialogRow(form, row++, "Faculty remark", new JScrollPane(remark));

        JButton resolve = new JButton("Resolve & Update");
        JButton reject = new JButton("Reject");
        JButton cancel = new JButton("Cancel");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(reject);
        buttons.add(cancel);
        buttons.add(resolve);
        reject.addActionListener(event -> processReview(dialog, complaint, "REJECTED", remark,
                null, null, null, null, null, null, null));
        JSpinner totalInput = total;
        JSpinner presentInput = present;
        JTextArea addDatesInput = addDates;
        JTextArea removeDatesInput = removeDates;
        resolve.addActionListener(event -> {
            try {
                Double i = null, m = null, e = null;
                Integer correctedTotal = null, correctedPresent = null;
                List<LocalDate> add = null, remove = null;
                if ("MARKS".equals(complaint.category())) {
                    i = parseScore(internal.getText(), "Internal");
                    m = parseScore(midSem.getText(), "Mid-sem");
                    e = parseScore(endSem.getText(), "End-sem");
                } else if ("ATTENDANCE".equals(complaint.category())) {
                    correctedTotal = (Integer) totalInput.getValue();
                    correctedPresent = (Integer) presentInput.getValue();
                    add = parseDates(addDatesInput.getText());
                    remove = parseDates(removeDatesInput.getText());
                    if (correctedTotal != counts.total() + add.size() - remove.size()) {
                        throw new IllegalArgumentException("The selected add/remove dates must match the corrected total.");
                    }
                }
                processReview(dialog, complaint, "RESOLVED", remark, i, m, e,
                        correctedTotal, correctedPresent, add, remove);
            } catch (IllegalArgumentException ex) {
                showMessage(ex.getMessage(), false);
            }
        });
        cancel.addActionListener(event -> dialog.dispose());
        dialog.add(new JScrollPane(form), BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(560, 480));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void processReview(JDialog dialog, ComplaintRow complaint, String status, JTextArea remark,
                               Double internal, Double midSem, Double endSem,
                               Integer total, Integer present, List<LocalDate> addDates,
                               List<LocalDate> removeDates) {
        String feedback = remark.getText().trim();
        if (feedback.isEmpty()) {
            showMessage("Faculty remark is required.", false);
            return;
        }
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                int id = Math.toIntExact(complaint.complaintId());
                if ("REJECTED".equals(status)) {
                    complaintDAO.resolveComplaintWithAcademicUpdate(user.getUserId(), id,
                            status, feedback, null, null, null, null, null);
                } else if ("ATTENDANCE".equals(complaint.category())) {
                    complaintDAO.resolveComplaintWithAttendanceDates(user.getUserId(), id,
                            feedback, total, present, addDates, removeDates);
                } else {
                    complaintDAO.resolveComplaintWithAcademicUpdate(user.getUserId(), id,
                            status, feedback, null, internal, midSem, endSem, null);
                }
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    dialog.dispose();
                    refreshComplaints();
                    refreshRoster();
                    showMessage("Grievance and its academic correction were committed atomically.", true);
                } catch (Exception e) {
                    showMessage("Could not process grievance: " + rootMessage(e), false);
                }
            }
        }.execute();
    }

    private List<LocalDate> parseDates(String text) {
        Set<LocalDate> dates = new LinkedHashSet<>();
        for (String line : text.split("\\R")) {
            if (!line.isBlank() && !dates.add(LocalDate.parse(line.trim()))) {
                throw new IllegalArgumentException("Attendance dates must not be repeated.");
            }
        }
        return new ArrayList<>(dates);
    }

    private void addDialogRow(JPanel form, int row, String label, JComponent input) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridy = row;
        c.gridx = 0;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(input, c);
    }

    private void addDialogRow(JPanel form, int row, String label, String value) {
        addDialogRow(form, row, label, new JLabel(value));
    }

    private void loadCourses() {
        new SwingWorker<List<FacultyCourse>, Void>() {
            @Override
            protected List<FacultyCourse> doInBackground() throws Exception {
                return facultyDAO.getCoursesForUser(user.getUserId());
            }
            @Override
            protected void done() {
                try {
                    List<FacultyCourse> courses = get();
                    loadingCourses = true;
                    courseSelector.removeAllItems();
                    for (FacultyCourse course : courses) courseSelector.addItem(course);
                    loadingCourses = false;
                    if (courses.isEmpty()) {
                        showMessage("No courses are assigned to your faculty account.", false);
                    }
                    refreshRoster();
                    refreshComplaints();
                } catch (Exception e) {
                    loadingCourses = false;
                    showMessage("Could not load assigned courses: " + rootMessage(e), false);
                }
            }
        }.execute();
    }

    private void refreshRoster() {
        FacultyCourse course = (FacultyCourse) courseSelector.getSelectedItem();
        attendanceModel.setRowCount(0);
        marksModel.setRowCount(0);
        if (course == null) {
            return;
        }
        long courseId = course.courseId();
        new SwingWorker<List<FacultyStudentRow>, Void>() {
            @Override
            protected List<FacultyStudentRow> doInBackground() throws Exception {
                return facultyDAO.getStudentsForCourse(courseId);
            }
            @Override
            protected void done() {
                FacultyCourse selected = (FacultyCourse) courseSelector.getSelectedItem();
                if (selected == null || selected.courseId() != courseId) return;
                try {
                    for (FacultyStudentRow row : get()) {
                        attendanceModel.addRow(new Object[]{
                                row.enrollmentId(), row.rollNo(), row.name(), row.presentToday()
                        });
                        marksModel.addRow(new Object[]{
                                row.enrollmentId(), row.rollNo(), row.name(),
                                row.internal(), row.midSem(), row.endSem(), row.marksLocked()
                        });
                    }
                } catch (Exception e) {
                    showMessage("Could not load course roster: " + rootMessage(e), false);
                }
            }
        }.execute();
    }

    private void saveAttendance() {
        FacultyCourse course = (FacultyCourse) courseSelector.getSelectedItem();
        if (course == null) return;
        if (attendanceTable.isEditing()) attendanceTable.getCellEditor().stopCellEditing();

        Map<Long, Boolean> attendance = new LinkedHashMap<>();
        for (int row = 0; row < attendanceModel.getRowCount(); row++) {
            attendance.put((Long) attendanceModel.getValueAt(row, 0),
                    Boolean.TRUE.equals(attendanceModel.getValueAt(row, 3)));
        }
        try {
            facultyDAO.saveAttendance(user.getUserId(), course.courseId(), attendance);
            showMessage("Today's attendance was saved.", true);
        } catch (SQLException e) {
            showMessage("Could not save attendance: " + e.getMessage(), false);
        }
    }

    private void finalizeMarks() {
        FacultyCourse course = (FacultyCourse) courseSelector.getSelectedItem();
        if (course == null) return;
        if (marksTable.isEditing() && !marksTable.getCellEditor().stopCellEditing()) return;

        try {
            for (int row = 0; row < marksModel.getRowCount(); row++) {
                if (Boolean.TRUE.equals(marksModel.getValueAt(row, 6))) continue;
                long enrollmentId = (Long) marksModel.getValueAt(row, 0);
                double internal = parseScore(marksModel.getValueAt(row, 3), "Internal");
                double midSem = parseScore(marksModel.getValueAt(row, 4), "Mid-sem");
                double endSem = parseScore(marksModel.getValueAt(row, 5), "End-sem");
                academicDAO.saveOrUpdateMarks(Math.toIntExact(enrollmentId),
                        internal, midSem, endSem, true);
            }
            refreshRoster();
            showMessage("Marks finalized and locked.", true);
        } catch (SQLException | IllegalArgumentException e) {
            showMessage("Could not finalize marks: " + e.getMessage(), false);
        }
    }

    private double parseScore(Object value, String label) {
        try {
            double score = Double.parseDouble(String.valueOf(value));
            double maximum = switch (label) {
                case "Internal" -> 20;
                case "Mid-sem" -> 30;
                default -> 50;
            };
            if (!Double.isFinite(score) || score < 0 || score > maximum) {
                throw new IllegalArgumentException(label + " score must be between 0 and " + maximum + ".");
            }
            return score;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " score must be numeric.", e);
        }
    }

    private void hideIdColumn(JTable table) {
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);
    }

    private void showMessage(String message, boolean success) {
        JOptionPane.showMessageDialog(this, message, success ? "Complete" : "Faculty Dashboard",
                success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    private String rootMessage(Exception exception) {
        Throwable cause = exception.getCause();
        return cause == null ? exception.getMessage() : cause.getMessage();
    }
}
