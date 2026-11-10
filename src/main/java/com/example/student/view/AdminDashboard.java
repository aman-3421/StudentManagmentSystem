package com.example.student.view;

import com.example.student.dao.AdminDAO;
import com.example.student.dao.AcademicDAO;
import com.example.student.dao.ComplaintDAO;
import com.example.student.dao.MasterDataDAO;
import com.example.student.model.Complaint;
import com.example.student.model.MasterDataTable;
import com.example.student.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;

public class AdminDashboard extends JFrame {
    private final AdminDAO adminDAO = new AdminDAO();
    private final ComplaintDAO complaintDAO = new ComplaintDAO();
    private final AcademicDAO academicDAO = new AcademicDAO();
    private final MasterDataDAO masterDataDAO = new MasterDataDAO();
    private final User user;
    private final JComboBox<MasterDataDAO.Entity> entitySelector =
            new JComboBox<>(MasterDataDAO.Entity.values());
    private final DefaultTableModel masterModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable masterTable = new JTable(masterModel);
    private final DefaultTableModel complaintModel = new DefaultTableModel(
            new Object[]{"ID", "Roll No", "Student", "Course", "Faculty", "Category", "Subject",
                    "Description", "Status", "Feedback", "Created"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable complaintTable = new JTable(complaintModel);

    public AdminDashboard(User user) {
        this.user = user;
        setTitle("Admin Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 700);
        setMinimumSize(new Dimension(760, 560));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBorder(new EmptyBorder(18, 22, 22, 22));
        root.add(header("Administrator", user.getUsername()), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Register Student", studentPanel());
        tabs.addTab("Register Faculty", facultyPanel());
        tabs.addTab("Assign Courses", coursesPanel());
        tabs.addTab("Grievances & Records Control", controlPanel());
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
        refreshMasterData();
        refreshComplaints();
    }

    private JPanel studentPanel() {
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JTextField rollNo = new JTextField();
        JTextField name = new JTextField();
        JTextField department = new JTextField();
        JSpinner semester = new JSpinner(new SpinnerNumberModel(1, 1, 255, 1));
        JPanel form = formPanel();
        addRow(form, "Username", username);
        addRow(form, "Temporary password", password);
        addRow(form, "Roll number", rollNo);
        addRow(form, "Full name", name);
        addRow(form, "Department", department);
        addRow(form, "Semester", semester);
        JButton submit = new JButton("Register student");
        submit.addActionListener(event -> runAction(() -> adminDAO.registerStudent(
                username.getText().trim(), new String(password.getPassword()),
                rollNo.getText().trim(), name.getText().trim(), department.getText().trim(),
                (Integer) semester.getValue()), "Student registered successfully."));
        form.add(submit);
        return wrapForm(form);
    }

    private JPanel facultyPanel() {
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JTextField empId = new JTextField();
        JTextField name = new JTextField();
        JTextField department = new JTextField();
        JPanel form = formPanel();
        addRow(form, "Username", username);
        addRow(form, "Temporary password", password);
        addRow(form, "Employee ID", empId);
        addRow(form, "Full name", name);
        addRow(form, "Department", department);
        JButton submit = new JButton("Register faculty");
        submit.addActionListener(event -> runAction(() -> adminDAO.registerFaculty(
                username.getText().trim(), new String(password.getPassword()),
                empId.getText().trim(), name.getText().trim(), department.getText().trim()),
                "Faculty member registered successfully."));
        form.add(submit);
        return wrapForm(form);
    }

    private JComponent coursesPanel() {
        JPanel page = new JPanel();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        JTextField code = new JTextField();
        JTextField courseName = new JTextField();
        JSpinner credits = new JSpinner(new SpinnerNumberModel(3, 1, 255, 1));
        JTextField facultyEmpId = new JTextField();
        JPanel courseForm = formPanel();
        addRow(courseForm, "Course code", code);
        addRow(courseForm, "Course name", courseName);
        addRow(courseForm, "Credits", credits);
        addRow(courseForm, "Faculty employee ID (optional)", facultyEmpId);
        JButton createCourse = new JButton("Create course");
        createCourse.addActionListener(event -> runAction(() -> {
            String empId = facultyEmpId.getText().trim();
            Long facultyId = empId.isEmpty() ? null : adminDAO.findFacultyIdByEmployeeId(empId);
            if (!empId.isEmpty() && facultyId == null) {
                throw new SQLException("No faculty member found for employee ID " + empId + ".");
            }
            adminDAO.assignCourse(code.getText().trim(), courseName.getText().trim(),
                    (Integer) credits.getValue(), facultyId);
        }, "Course created successfully."));
        courseForm.add(createCourse);

        JTextField rollNo = new JTextField();
        JTextField courseCode = new JTextField();
        JSpinner semester = new JSpinner(new SpinnerNumberModel(1, 1, 255, 1));
        JPanel enrollmentForm = formPanel();
        enrollmentForm.setBorder(BorderFactory.createTitledBorder("Enroll a student in a course"));
        addRow(enrollmentForm, "Student roll number", rollNo);
        addRow(enrollmentForm, "Course code", courseCode);
        addRow(enrollmentForm, "Semester", semester);
        JButton enroll = new JButton("Enroll student");
        enroll.addActionListener(event -> runAction(() -> adminDAO.enrollStudent(
                rollNo.getText().trim(), courseCode.getText().trim(), (Integer) semester.getValue()),
                "Student enrolled successfully."));
        enrollmentForm.add(enroll);

        page.add(courseForm);
        page.add(Box.createVerticalStrut(26));
        page.add(enrollmentForm);
        return new JScrollPane(page);
    }

    private void runAction(SqlAction action, String success) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                action.run();
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(AdminDashboard.this, success,
                            "Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    showError("Unable to complete action: " + rootMessage(e));
                }
            }
        }.execute();
    }

    private JPanel controlPanel() {
        JTabbedPane sections = new JTabbedPane();
        sections.addTab("Student, Faculty & Course Records", masterDataPanel());
        sections.addTab("Institutional Grievances", grievancePanel());
        JPanel page = new JPanel(new BorderLayout());
        page.add(sections, BorderLayout.CENTER);
        return page;
    }

    private JPanel masterDataPanel() {
            JPanel page = new JPanel(new BorderLayout(0, 10));
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
            controls.add(new JLabel("Records"));
            controls.add(entitySelector);
            JButton refresh = new JButton("Refresh");
            refresh.addActionListener(event -> refreshMasterData());
            JButton edit = new JButton("Edit Record");
            edit.addActionListener(event -> editMasterRecord());
            JButton delete = new JButton("Delete Record");
            delete.addActionListener(event -> deleteMasterRecord());
            controls.add(refresh);
            controls.add(edit);
            controls.add(delete);
            entitySelector.addActionListener(event -> refreshMasterData());
            masterTable.setAutoCreateRowSorter(true);
            page.add(controls, BorderLayout.NORTH);
            page.add(new JScrollPane(masterTable), BorderLayout.CENTER);
            JLabel warning = new JLabel("Edits and deletions are permanent. Student/faculty deletions cascade to dependent records.");
            warning.setBorder(new EmptyBorder(4, 4, 4, 4));
            page.add(warning, BorderLayout.SOUTH);
            return page;
        }

    private void refreshMasterData() {
            MasterDataDAO.Entity entity = (MasterDataDAO.Entity) entitySelector.getSelectedItem();
            if (entity == null || user == null) return;
            new SwingWorker<MasterDataTable, Void>() {
                @Override
                protected MasterDataTable doInBackground() throws Exception {
                    return masterDataDAO.list(user.getUserId(), entity);
                }
                @Override
                protected void done() {
                    try {
                        if (entity != entitySelector.getSelectedItem()) return;
                        MasterDataTable data = get();
                        masterModel.setDataVector(data.rows().toArray(Object[][]::new),
                                data.columns().toArray());
                        if (masterModel.getColumnCount() > 0) {
                            masterTable.getColumnModel().getColumn(0).setMinWidth(55);
                            masterTable.getColumnModel().getColumn(0).setMaxWidth(90);
                        }
                    } catch (Exception e) {
                        showError("Could not load master records: " + rootMessage(e));
                    }
                }
            }.execute();
        }

    private Object[] selectedMasterRow() {
            int selected = masterTable.getSelectedRow();
            if (selected < 0) {
                showError("Select a record first.");
                return null;
            }
            int row = masterTable.convertRowIndexToModel(selected);
            Object[] values = new Object[masterModel.getColumnCount()];
            for (int column = 0; column < values.length; column++) {
                values[column] = masterModel.getValueAt(row, column);
            }
            return values;
        }

    private void editMasterRecord() {
        Object[] values = selectedMasterRow();
        if (values == null) return;
        MasterDataDAO.Entity entity = (MasterDataDAO.Entity) entitySelector.getSelectedItem();
        JDialog dialog = new JDialog(this, "Edit " + entity.name().toLowerCase(), true);
        JPanel form = formPanel();
        JTextField[] fields = new JTextField[values.length];
        for (int column = 0; column < values.length; column++) {
            fields[column] = new JTextField(values[column] == null ? "" : values[column].toString());
            fields[column].setEditable(column != 0);
            String label = masterModel.getColumnName(column).replace('_', ' ');
            addRow(form, label, fields[column]);
        }
        JTextField courseId = new JTextField();
        JTextField internal = new JTextField();
        JTextField midSem = new JTextField();
        JTextField endSem = new JTextField();
        if (entity == MasterDataDAO.Entity.STUDENTS) {
            form.setBorder(BorderFactory.createTitledBorder("Student details"));
            addRow(form, "Grade course ID (optional)", courseId);
            addRow(form, "Internal marks (0-20)", internal);
            addRow(form, "Mid-sem marks (0-30)", midSem);
            addRow(form, "End-sem marks (0-50)", endSem);
        }
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton save = new JButton("Save Record");
        JButton gradeSave = new JButton("Save Grade Override");
        JButton cancel = new JButton("Cancel");
        save.addActionListener(event -> {
            try {
                Object[] updated = new Object[fields.length];
                for (int i = 0; i < fields.length; i++) updated[i] = fields[i].getText().trim();
                if (entity == MasterDataDAO.Entity.STUDENTS) {
                    updated[4] = parseInteger(fields[4].getText(), "Semester");
                } else if (entity == MasterDataDAO.Entity.COURSES) {
                    updated[3] = parseInteger(fields[3].getText(), "Credits");
                    updated[4] = fields[4].getText().isBlank() ? null : fields[4].getText().trim();
                }
                executeDialogAction(dialog, () -> masterDataDAO.update(user.getUserId(), entity, updated),
                        "Record updated successfully.");
            } catch (IllegalArgumentException ex) {
                showError(ex.getMessage());
            }
        });
        buttons.add(save);
        if (entity == MasterDataDAO.Entity.STUDENTS) {
            gradeSave.addActionListener(event -> {
                try {
                    int studentId = Math.toIntExact(((Number) values[0]).longValue());
                    int selectedCourseId = Integer.parseInt(courseId.getText().trim());
                    double i = Double.parseDouble(internal.getText().trim());
                    double m = Double.parseDouble(midSem.getText().trim());
                    double e = Double.parseDouble(endSem.getText().trim());
                    executeDialogAction(dialog, () -> academicDAO.adminOverrideMarks(
                            user.getUserId(), studentId, selectedCourseId, i, m, e),
                            "Grade override saved successfully.");
                } catch (NumberFormatException | ArithmeticException ex) {
                    showError("Enter a valid course ID and numeric marks.");
                }
            });
            buttons.add(gradeSave);
        }
        cancel.addActionListener(event -> dialog.dispose());
        buttons.add(cancel);
        dialog.add(new JScrollPane(form), BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(560, 440));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void executeDialogAction(JDialog dialog, SqlAction action, String success) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                action.run();
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    dialog.dispose();
                    refreshMasterData();
                    JOptionPane.showMessageDialog(AdminDashboard.this, success,
                            "Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception e) {
                    showError("Could not save record: " + rootMessage(e));
                }
            }
        }.execute();
    }

    private void deleteMasterRecord() {
            Object[] selected = selectedMasterRow();
            if (selected == null) return;
            MasterDataDAO.Entity entity = (MasterDataDAO.Entity) entitySelector.getSelectedItem();
            long id = ((Number) selected[0]).longValue();
            int confirmation = JOptionPane.showConfirmDialog(this,
                    "Delete this record? Related records may also be deleted.", "Confirm deletion",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirmation != JOptionPane.YES_OPTION) return;
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    if (entity == MasterDataDAO.Entity.STUDENTS) {
                        academicDAO.adminDeleteStudent(user.getUserId(), Math.toIntExact(id));
                    } else {
                        masterDataDAO.delete(user.getUserId(), entity, id);
                    }
                    return null;
                }
                @Override
                protected void done() {
                    try {
                        get();
                        refreshMasterData();
                        JOptionPane.showMessageDialog(AdminDashboard.this, "Record deleted successfully.",
                                "Complete", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        showError("Could not delete record: " + rootMessage(e));
                    }
                }
            }.execute();
        }

    private JPanel grievancePanel() {
            complaintTable.setAutoCreateRowSorter(true);
            JPanel page = new JPanel(new BorderLayout(0, 10));
            page.add(new JScrollPane(complaintTable), BorderLayout.CENTER);
            JButton refresh = new JButton("Refresh");
            refresh.addActionListener(event -> refreshComplaints());
            JButton override = new JButton("Override status / feedback");
            override.addActionListener(event -> overrideComplaint());
            JButton purge = new JButton("Delete Ticket");
            purge.addActionListener(event -> purgeComplaint());
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            actions.add(refresh);
            actions.add(override);
            actions.add(purge);
            page.add(actions, BorderLayout.SOUTH);
            return page;
        }

    private void refreshComplaints() {
            new SwingWorker<java.util.List<Complaint>, Void>() {
                @Override
                protected java.util.List<Complaint> doInBackground() throws Exception {
                    return complaintDAO.getAllComplaints(user.getUserId());
                }
                @Override
                protected void done() {
                    try {
                        complaintModel.setRowCount(0);
                        for (Complaint complaint : get()) {
                            complaintModel.addRow(new Object[]{
                                    complaint.getComplaintId(), complaint.getStudentRollNo(), complaint.getStudentName(),
                                    complaint.getCourseCode() + " - " + complaint.getCourseName(),
                                    complaint.getFacultyName(), complaint.getCategory(), complaint.getSubject(),
                                    complaint.getDescription(), complaint.getStatus(), complaint.getFacultyRemark(),
                                    complaint.getCreatedAt()
                            });
                        }
                    } catch (Exception e) {
                        showError("Could not load grievances: " + rootMessage(e));
                    }
                }
            }.execute();
        }

    private void overrideComplaint() {
            Long complaintId = selectedComplaintId();
            if (complaintId == null) return;
            JComboBox<String> status = new JComboBox<>(new String[]{"PENDING", "RESOLVED", "REJECTED"});
            JTextArea remark = new JTextArea(4, 32);
            remark.setLineWrap(true);
            remark.setWrapStyleWord(true);
            JPanel form = new JPanel(new BorderLayout(6, 6));
            form.add(status, BorderLayout.NORTH);
            form.add(new JScrollPane(remark), BorderLayout.CENTER);
            if (JOptionPane.showConfirmDialog(this, form,
                    "Override complaint #" + complaintId, JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }
            String selectedStatus = status.getSelectedItem().toString();
            String selectedRemark = remark.getText().trim();
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    complaintDAO.adminOverrideComplaint(user.getUserId(), complaintId,
                            selectedStatus, selectedRemark);
                    return null;
                }
                @Override
                protected void done() {
                    try {
                        get();
                        refreshComplaints();
                        JOptionPane.showMessageDialog(AdminDashboard.this, "Grievance status updated.",
                                "Complete", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        showError("Could not override grievance: " + rootMessage(e));
                    }
                }
            }.execute();
        }

    private void purgeComplaint() {
            Long complaintId = selectedComplaintId();
            if (complaintId == null) return;
            int confirmation = JOptionPane.showConfirmDialog(this,
                    "Permanently purge complaint #" + complaintId + "?", "Confirm purge",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirmation != JOptionPane.YES_OPTION) return;
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    complaintDAO.purgeComplaint(user.getUserId(), complaintId);
                    return null;
                }
                @Override
                protected void done() {
                    try {
                        get();
                        refreshComplaints();
                        JOptionPane.showMessageDialog(AdminDashboard.this, "Grievance ticket deleted.",
                                "Complete", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        showError("Could not delete grievance: " + rootMessage(e));
                    }
                }
            }.execute();
        }

    private Long selectedComplaintId() {
            int selected = complaintTable.getSelectedRow();
            if (selected < 0) {
                showError("Select a grievance first.");
                return null;
            }
            int modelRow = complaintTable.convertRowIndexToModel(selected);
            return ((Number) complaintModel.getValueAt(modelRow, 0)).longValue();
        }

    private void showError(String message) {
            JOptionPane.showMessageDialog(this, message, "Administrative action failed",
                    JOptionPane.ERROR_MESSAGE);
        }

    private String rootMessage(Exception exception) {
        Throwable cause = exception.getCause();
        return cause == null ? exception.getMessage() : cause.getMessage();
    }

    private int parseInteger(String value, String label) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a whole number.", e);
        }
    }
    private JPanel formPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(24, 28, 24, 28));
        return form;
    }

    private JPanel wrapForm(JPanel form) {
        JPanel page = new JPanel(new BorderLayout());
        page.add(form, BorderLayout.NORTH);
        return page;
    }

    private void addRow(JPanel form, String label, JComponent input) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = form.getComponentCount() / 2;
        c.weightx = 0.3;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 0.7;
        input.setPreferredSize(new Dimension(320, 34));
        form.add(input, c);
    }

    private JPanel header(String title, String username) {
        JPanel header = new JPanel(new BorderLayout());
        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 24f));
        header.add(heading, BorderLayout.WEST);
        header.add(new JLabel("Signed in as " + username), BorderLayout.EAST);
        return header;
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }
}
