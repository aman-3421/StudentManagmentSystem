package com.example.student.dao;

import com.example.student.DBConnection;
import com.example.student.model.Complaint;
import com.example.student.model.ComplaintRow;
import com.example.student.model.Course;
import com.example.student.model.AttendanceSummary;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ComplaintDAO {
    private static final String COMPLAINT_SELECT = """
            SELECT g.complaint_id, g.student_id, s.roll_no, s.name AS student_name,
                   g.course_id, c.course_code, c.course_name, g.faculty_id,
                   f.name AS faculty_name, g.category, g.subject, g.description,
                   g.status, g.faculty_remark, g.created_at, g.updated_at
            FROM complaints g
            JOIN students s ON s.student_id = g.student_id
            JOIN courses c ON c.course_id = g.course_id
            JOIN faculty f ON f.faculty_id = g.faculty_id
            """;

    public void raiseComplaint(Complaint complaint) throws SQLException {
        if (complaint == null) throw new IllegalArgumentException("Complaint is required.");
        requirePositive(complaint.getStudentId(), "studentId");
        requirePositive(complaint.getCourseId(), "courseId");
        validateCategory(complaint.getCategory());
        requireText(complaint.getSubject(), "Subject");
        requireText(complaint.getDescription(), "Description");

        try (Connection connection = DBConnection.getInstance().getConnection()) {
            long facultyId;
            try (PreparedStatement courseStatement = connection.prepareStatement(
                    "SELECT faculty_id FROM courses WHERE course_id = ? AND faculty_id IS NOT NULL")) {
                courseStatement.setInt(1, complaint.getCourseId());
                try (ResultSet resultSet = courseStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException("Course does not exist or has no assigned faculty.");
                    }
                    facultyId = resultSet.getLong("faculty_id");
                }
            }

            String insertSql = """
                    INSERT INTO complaints
                        (student_id, course_id, faculty_id, category, subject, description, status)
                    SELECT s.student_id, c.course_id, c.faculty_id, ?, ?, ?, 'PENDING'
                    FROM students s
                    JOIN courses c ON c.course_id = ?
                    JOIN enrollments e ON e.student_id = s.student_id
                        AND e.course_id = c.course_id AND e.semester = s.semester
                    WHERE s.student_id = ? AND c.faculty_id = ?
                    LIMIT 1
                    """;
            try (PreparedStatement statement = connection.prepareStatement(insertSql,
                    PreparedStatement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, complaint.getCategory());
                statement.setString(2, complaint.getSubject().trim());
                statement.setString(3, complaint.getDescription().trim());
                statement.setInt(4, complaint.getCourseId());
                statement.setInt(5, complaint.getStudentId());
                statement.setLong(6, facultyId);
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Student is not enrolled in this course for the current semester.");
                }
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) complaint.setComplaintId(keys.getInt(1));
                }
            }
        }
    }

    public List<Complaint> getComplaintsByStudent(int studentId) throws SQLException {
        requirePositive(studentId, "studentId");
        return queryComplaints(COMPLAINT_SELECT
                + " WHERE g.student_id = ? ORDER BY g.created_at DESC", studentId);
    }

    public List<Complaint> getComplaintsForFaculty(int facultyId) throws SQLException {
        requirePositive(facultyId, "facultyId");
        return queryComplaints(COMPLAINT_SELECT
                + " WHERE g.faculty_id = ? ORDER BY CASE g.status WHEN 'PENDING' THEN 0 ELSE 1 END, g.created_at DESC",
                facultyId);
    }

    public List<Complaint> getAllComplaints() throws SQLException {
        return queryComplaints(COMPLAINT_SELECT + " ORDER BY g.created_at DESC");
    }

    public void resolveComplaintWithAcademicUpdate(
            int complaintId, String status, String remark, Integer enrollmentId,
            Double internal, Double midSem, Double endSem, Integer addPresentDays)
            throws SQLException {
        resolveComplaint(complaintId, null, status, remark, enrollmentId,
                internal, midSem, endSem, addPresentDays, null, null);
    }

    public void resolveComplaintWithAcademicUpdate(
            long facultyUserId, int complaintId, String status, String remark, Integer enrollmentId,
            Double internal, Double midSem, Double endSem, Integer addPresentDays)
            throws SQLException {
        resolveComplaint(complaintId, facultyUserId, status, remark, enrollmentId,
                internal, midSem, endSem, addPresentDays, null, null);
    }

    public AttendanceSummary getAttendanceCountsForFacultyComplaint(
            int complaintId, long facultyUserId) throws SQLException {
        String sql = """
                SELECT c.course_id, c.course_code, c.course_name,
                       SUM(CASE WHEN a.status IN ('PRESENT', 'LATE') THEN 1 ELSE 0 END) AS attended,
                       COUNT(a.attendance_id) AS total
                FROM complaints g
                JOIN faculty f ON f.faculty_id = g.faculty_id AND f.user_id = ?
                JOIN students s ON s.student_id = g.student_id
                JOIN courses c ON c.course_id = g.course_id
                JOIN enrollments e ON e.student_id = s.student_id
                    AND e.course_id = c.course_id AND e.semester = s.semester
                LEFT JOIN attendance a ON a.enrollment_id = e.enrollment_id
                WHERE g.complaint_id = ? AND g.category = 'ATTENDANCE'
                GROUP BY c.course_id, c.course_code, c.course_name
                """;
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, facultyUserId);
            statement.setInt(2, complaintId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Attendance complaint not found or not assigned to this faculty member.");
                }
                long attended = resultSet.getLong("attended");
                long total = resultSet.getLong("total");
                return new AttendanceSummary(resultSet.getLong("course_id"),
                        resultSet.getString("course_code"), resultSet.getString("course_name"),
                        attended, total, total == 0 ? 0 : attended * 100.0 / total);
            }
        }
    }

    public void resolveComplaintWithAttendanceDates(
            long facultyUserId, int complaintId, String remark, int desiredTotal,
            int desiredPresent, List<LocalDate> addDates, List<LocalDate> removeDates)
            throws SQLException {
            requirePositive(complaintId, "complaintId");
            requireText(remark, "Resolution feedback");
            if (addDates == null || removeDates == null) {
                throw new IllegalArgumentException("Provide both attendance date selections, even when empty.");
            }
            validateDates(addDates);
            validateDates(removeDates);
            if (addDates.stream().anyMatch(removeDates::contains)) {
                throw new IllegalArgumentException("A date cannot be both added and removed.");
            }
            try (Connection connection = DBConnection.getInstance().getConnection()) {
                connection.setAutoCommit(false);
                try {
                    Complaint complaint = lockComplaint(connection, complaintId, facultyUserId);
                    if (!"PENDING".equals(complaint.getStatus())) {
                        throw new SQLException("Only pending complaints can be resolved.");
                    }
                    if (!"ATTENDANCE".equals(complaint.getCategory())) {
                        throw new IllegalArgumentException("Only an attendance grievance can include attendance corrections.");
                    }
                    int enrollmentId = findCurrentEnrollment(connection, complaint);
                    requireComplaintEnrollment(connection, complaint, enrollmentId);
                    for (LocalDate date : removeDates) {
                        try (PreparedStatement statement = connection.prepareStatement(
                                "DELETE FROM attendance WHERE enrollment_id = ? AND `date` = ?")) {
                            statement.setInt(1, enrollmentId);
                            statement.setDate(2, Date.valueOf(date));
                            if (statement.executeUpdate() != 1) {
                                throw new SQLException("Attendance date " + date + " no longer exists.");
                            }
                        }
                    }
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO attendance (enrollment_id, `date`, status) VALUES (?, ?, 'ABSENT')")) {
                        for (LocalDate date : addDates) {
                            statement.setInt(1, enrollmentId);
                            statement.setDate(2, Date.valueOf(date));
                            statement.addBatch();
                        }
                        statement.executeBatch();
                    }
                    reconcilePresentCount(connection, enrollmentId, desiredTotal, desiredPresent);
                    try (PreparedStatement statement = connection.prepareStatement("""
                            UPDATE complaints SET status = 'RESOLVED', faculty_remark = ?
                            WHERE complaint_id = ? AND status = 'PENDING'
                            """)) {
                        statement.setString(1, remark.trim());
                        statement.setInt(2, complaintId);
                        if (statement.executeUpdate() != 1) {
                            throw new SQLException("Complaint was changed by another user; reload and retry.");
                        }
                    }
                    connection.commit();
                } catch (SQLException | RuntimeException e) {
                    rollback(connection, e);
                    throw e;
                }
            }
        }

        private void validateDates(List<LocalDate> dates) {
            if (dates == null || dates.stream().anyMatch(date -> date == null)
                    || dates.size() != dates.stream().distinct().count()) {
                throw new IllegalArgumentException("Attendance date selections must be valid and unique.");
            }
        }

        private void reconcilePresentCount(Connection connection, int enrollmentId,
                                           int desiredTotal, int desiredPresent) throws SQLException {
            List<Integer> presentIds = new ArrayList<>();
            List<Integer> absentIds = new ArrayList<>();
            int total = 0;
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT attendance_id, status FROM attendance
                    WHERE enrollment_id = ? ORDER BY `date` DESC, attendance_id DESC FOR UPDATE
                    """)) {
                statement.setInt(1, enrollmentId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        total++;
                        String status = resultSet.getString("status");
                        if ("PRESENT".equals(status) || "LATE".equals(status)) {
                            presentIds.add(resultSet.getInt("attendance_id"));
                        } else if ("ABSENT".equals(status)) {
                            absentIds.add(resultSet.getInt("attendance_id"));
                        }
                    }
                }
            }
            if (total != desiredTotal) {
                throw new IllegalArgumentException("Selected dates produce " + total
                        + " total classes, not the requested " + desiredTotal + ".");
            }
            if (desiredPresent < 0 || desiredPresent > total) {
                throw new IllegalArgumentException("Present classes must be between 0 and the total.");
            }
            int delta = desiredPresent - presentIds.size();
            List<Integer> changedIds = delta > 0
                    ? absentIds.subList(0, Math.min(delta, absentIds.size()))
                    : presentIds.subList(0, Math.min(-delta, presentIds.size()));
            if (changedIds.size() != Math.abs(delta)) {
                throw new IllegalArgumentException("The selected attendance records cannot produce that present count.");
            }
            String oldStatus = delta > 0 ? "ABSENT" : "PRESENT";
            String newStatus = delta > 0 ? "PRESENT" : "ABSENT";
            String updateSql = delta > 0
                    ? "UPDATE attendance SET status = ? WHERE attendance_id = ? AND status = ?"
                    : "UPDATE attendance SET status = ? WHERE attendance_id = ? AND status IN (?, 'LATE')";
            try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                for (int id : changedIds) {
                    statement.setString(1, newStatus);
                    statement.setInt(2, id);
                    statement.setString(3, oldStatus);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        }

    public void deleteComplaint(int complaintId) throws SQLException {
        requirePositive(complaintId, "complaintId");
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM complaints WHERE complaint_id = ?")) {
            statement.setInt(1, complaintId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Complaint not found: " + complaintId);
            }
        }
    }

    public void deleteComplaint(int complaintId, long adminUserId) throws SQLException {
        try (Connection connection = DBConnection.getInstance().getConnection()) {
            requireAdmin(connection, adminUserId);
            deleteComplaint(connection, complaintId);
        }
    }

    public void submitComplaint(long userId, long courseId, String category,
                                String subject, String description) throws SQLException {
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT student_id FROM students WHERE user_id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new SQLException("No student profile is linked to this account.");
                Complaint complaint = new Complaint();
                complaint.setStudentId(resultSet.getInt("student_id"));
                complaint.setCourseId(Math.toIntExact(courseId));
                complaint.setCategory(category);
                complaint.setSubject(subject);
                complaint.setDescription(description);
                raiseComplaint(complaint);
            }
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Course ID is outside the supported range.", e);
        }
    }

    public List<Course> getEnrolledCoursesForUser(long userId) throws SQLException {
        String sql = """
                SELECT DISTINCT c.course_id, c.course_code, c.course_name
                FROM students s
                JOIN enrollments e ON e.student_id = s.student_id AND e.semester = s.semester
                JOIN courses c ON c.course_id = e.course_id
                WHERE s.user_id = ? AND c.faculty_id IS NOT NULL
                ORDER BY c.course_code
                """;
        List<Course> courses = new ArrayList<>();
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(new Course(resultSet.getLong("course_id"),
                            resultSet.getString("course_code"), resultSet.getString("course_name"),
                            0, null));
                }
            }
        }
        return courses;
    }

    public List<Complaint> getComplaintsForStudent(long userId) throws SQLException {
        return queryComplaints(COMPLAINT_SELECT + """
                 WHERE g.student_id = (SELECT student_id FROM students WHERE user_id = ?)
                 ORDER BY g.created_at DESC
                """, userId);
    }

    public List<ComplaintRow> getComplaintsForFaculty(long userId) throws SQLException {
        List<Complaint> complaints = queryComplaints(COMPLAINT_SELECT + """
                 WHERE g.faculty_id = (SELECT faculty_id FROM faculty WHERE user_id = ?)
                 ORDER BY CASE g.status WHEN 'PENDING' THEN 0 ELSE 1 END, g.created_at DESC
                """, userId);
        return complaints.stream().map(this::toComplaintRow).toList();
    }

    public List<Complaint> getAllComplaints(long adminUserId) throws SQLException {
        try (Connection connection = DBConnection.getInstance().getConnection()) {
            requireAdmin(connection, adminUserId);
            return queryComplaints(connection, COMPLAINT_SELECT + " ORDER BY g.created_at DESC");
        }
    }

    public void resolveComplaint(long complaintId, long facultyUserId, String status, String remark,
                                 BigDecimal internal, BigDecimal midSem, BigDecimal endSem,
                                 LocalDate attendanceDate, String attendanceStatus) throws SQLException {
        int id = Math.toIntExact(complaintId);
        Integer enrollmentId = null;
        if (internal != null || midSem != null || endSem != null) {
            if (internal == null || midSem == null || endSem == null) {
                throw new IllegalArgumentException("All three marks must be supplied together.");
            }
            enrollmentId = findEnrollmentForFacultyComplaint(id, facultyUserId);
        }
        if (attendanceDate != null && enrollmentId == null) {
            enrollmentId = findEnrollmentForFacultyComplaint(id, facultyUserId);
        }

        if (attendanceDate != null) {
            if (!List.of("PRESENT", "ABSENT", "LATE", "EXCUSED").contains(attendanceStatus)) {
                throw new IllegalArgumentException("Choose a valid attendance status.");
            }
            resolveComplaint(id, facultyUserId, status, remark, enrollmentId,
                    toDouble(internal), toDouble(midSem), toDouble(endSem), null,
                    attendanceDate, attendanceStatus);
            return;
        }

        resolveComplaint(id, facultyUserId, status, remark,
                enrollmentId, toDouble(internal), toDouble(midSem), toDouble(endSem),
                null, null, null);
    }

    public void adminOverrideComplaint(long adminUserId, long complaintId, String status, String remark)
            throws SQLException {
        requireStatus(status);
        requireText(remark, "Administrative remark");
        try (Connection connection = DBConnection.getInstance().getConnection()) {
            requireAdmin(connection, adminUserId);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE complaints SET status = ?, faculty_remark = ? WHERE complaint_id = ?")) {
                statement.setString(1, status);
                statement.setString(2, remark);
                statement.setLong(3, complaintId);
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Complaint not found: " + complaintId);
                }
            }
        }
    }

    public void purgeComplaint(long adminUserId, long complaintId) throws SQLException {
        try (Connection connection = DBConnection.getInstance().getConnection()) {
            requireAdmin(connection, adminUserId);
            deleteComplaint(connection, Math.toIntExact(complaintId));
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Complaint ID is outside the supported range.", e);
        }
    }

    private void resolveComplaint(int complaintId, Long facultyUserId, String status, String remark,
                                  Integer enrollmentId, Double internal, Double midSem, Double endSem,
                                  Integer addPresentDays, LocalDate attendanceDate, String attendanceStatus)
            throws SQLException {
        requirePositive(complaintId, "complaintId");
        requireFinalStatus(status);
        requireText(remark, "Resolution feedback");
        validateOptionalMarks(internal, midSem, endSem);
        if ((internal != null || midSem != null || endSem != null) && enrollmentId == null) {
            throw new IllegalArgumentException("Enrollment ID is required to correct marks.");
        }
        try (Connection connection = DBConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                Complaint complaint = lockComplaint(connection, complaintId, facultyUserId);
                if (!"PENDING".equals(complaint.getStatus())) {
                    throw new SQLException("Only pending complaints can be resolved.");
                }
                if (!"RESOLVED".equals(status)
                        && (internal != null || (addPresentDays != null && addPresentDays != 0)
                        || attendanceDate != null)) {
                    throw new IllegalArgumentException("Academic corrections can only be committed with RESOLVED status.");
                }
                Integer correctionEnrollmentId = enrollmentId;
                if (internal != null && !"MARKS".equals(complaint.getCategory())) {
                    throw new IllegalArgumentException("Only a marks complaint can include a marks correction.");
                }
                if ((addPresentDays != null || attendanceDate != null)
                        && !"ATTENDANCE".equals(complaint.getCategory())) {
                    throw new IllegalArgumentException("Only an attendance complaint can include attendance corrections.");
                }
                if (internal != null || (addPresentDays != null && addPresentDays != 0)
                        || attendanceDate != null) {
                    if (correctionEnrollmentId == null) {
                        correctionEnrollmentId = findCurrentEnrollment(connection, complaint);
                    }
                    requireComplaintEnrollment(connection, complaint, correctionEnrollmentId);
                }

                if (internal != null) {
                    updateMarks(connection, correctionEnrollmentId, internal, midSem, endSem);
                }
                if (addPresentDays != null && addPresentDays != 0) {
                    addPresentDays(connection, correctionEnrollmentId, addPresentDays);
                }
                if (attendanceDate != null) {
                    if (!List.of("PRESENT", "ABSENT", "LATE", "EXCUSED").contains(attendanceStatus)) {
                        throw new IllegalArgumentException("Choose a valid attendance status.");
                    }
                    markAttendanceDate(connection, correctionEnrollmentId,
                            attendanceDate, attendanceStatus);
                }

                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE complaints SET status = ?, faculty_remark = ?
                        WHERE complaint_id = ? AND status = 'PENDING'
                        """)) {
                    statement.setString(1, status);
                    statement.setString(2, remark.trim());
                    statement.setInt(3, complaintId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Complaint was changed by another user; reload and retry.");
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                rollback(connection, e);
                throw e;
            }
        }
    }

    private List<Complaint> queryComplaints(String sql, Object... parameters) throws SQLException {
        try (Connection connection = DBConnection.getInstance().getConnection()) {
            return queryComplaints(connection, sql, parameters);
        }
    }

    private List<Complaint> queryComplaints(Connection connection, String sql, Object... parameters)
            throws SQLException {
        List<Complaint> complaints = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i] instanceof Long value) statement.setLong(i + 1, value);
                else statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) complaints.add(mapComplaint(resultSet));
            }
        }
        return complaints;
    }

    private Complaint lockComplaint(Connection connection, int complaintId, Long facultyUserId)
            throws SQLException {
        String sql = COMPLAINT_SELECT + " WHERE g.complaint_id = ?"
                + (facultyUserId == null ? "" : " AND f.user_id = ?") + " FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, complaintId);
            if (facultyUserId != null) statement.setLong(2, facultyUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Complaint not found or not assigned to this faculty account.");
                }
                return mapComplaint(resultSet);
            }
        }
    }

    private void requireComplaintEnrollment(Connection connection, Complaint complaint, int enrollmentId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM enrollments e
                JOIN students s ON s.student_id = e.student_id AND s.semester = e.semester
                WHERE e.enrollment_id = ? AND e.student_id = ? AND e.course_id = ?
                """)) {
            statement.setInt(1, enrollmentId);
            statement.setInt(2, complaint.getStudentId());
            statement.setInt(3, complaint.getCourseId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Enrollment does not belong to this complaint's student and course.");
                }
            }
        }
    }

    private int findCurrentEnrollment(Connection connection, Complaint complaint) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT e.enrollment_id FROM enrollments e
                JOIN students s ON s.student_id = e.student_id AND s.semester = e.semester
                WHERE e.student_id = ? AND e.course_id = ?
                """)) {
            statement.setInt(1, complaint.getStudentId());
            statement.setInt(2, complaint.getCourseId());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("No current enrollment exists for this complaint.");
                }
                return resultSet.getInt("enrollment_id");
            }
        }
    }

    private int findEnrollmentForFacultyComplaint(int complaintId, long facultyUserId) throws SQLException {
        String sql = """
                SELECT e.enrollment_id
                FROM complaints g
                JOIN faculty f ON f.faculty_id = g.faculty_id
                JOIN enrollments e ON e.student_id = g.student_id AND e.course_id = g.course_id
                JOIN students s ON s.student_id = e.student_id AND s.semester = e.semester
                WHERE g.complaint_id = ? AND f.user_id = ? AND g.status = 'PENDING'
                ORDER BY e.enrollment_id LIMIT 1
                """;
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, complaintId);
            statement.setLong(2, facultyUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new SQLException("Complaint not found or not assigned to this faculty.");
                return resultSet.getInt("enrollment_id");
            }
        }
    }

    private void updateMarks(Connection connection, int enrollmentId, double internal,
                             double midSem, double endSem) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO marks (enrollment_id, internal, mid_sem, end_sem, is_locked)
                VALUES (?, ?, ?, ?, FALSE)
                ON DUPLICATE KEY UPDATE internal = VALUES(internal),
                    mid_sem = VALUES(mid_sem), end_sem = VALUES(end_sem)
                """)) {
            statement.setInt(1, enrollmentId);
            statement.setDouble(2, internal);
            statement.setDouble(3, midSem);
            statement.setDouble(4, endSem);
            statement.executeUpdate();
        }
    }

    private void addPresentDays(Connection connection, int enrollmentId, int delta) throws SQLException {
        String oldStatus = delta > 0 ? "ABSENT" : "PRESENT";
        String newStatus = delta > 0 ? "PRESENT" : "ABSENT";
        int days = Math.abs(delta);
        List<Integer> attendanceIds = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT attendance_id FROM attendance
                WHERE enrollment_id = ? AND status = ?
                ORDER BY `date` DESC FOR UPDATE
                """)) {
            statement.setInt(1, enrollmentId);
            statement.setString(2, oldStatus);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next() && attendanceIds.size() < days) {
                    attendanceIds.add(resultSet.getInt("attendance_id"));
                }
            }
        }
        if (attendanceIds.size() < days) {
            throw new SQLException("Cannot adjust attendance by " + delta + " classes; only "
                    + attendanceIds.size() + " records can be changed.");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE attendance SET status = ? WHERE attendance_id = ?")) {
            for (int attendanceId : attendanceIds) {
                statement.setString(1, newStatus);
                statement.setInt(2, attendanceId);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void markAttendanceDate(Connection connection, int enrollmentId, LocalDate date, String status)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO attendance (enrollment_id, `date`, status) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status)
                """)) {
            statement.setInt(1, enrollmentId);
            statement.setDate(2, Date.valueOf(date));
            statement.setString(3, status);
            statement.executeUpdate();
        }
    }

    private Complaint mapComplaint(ResultSet resultSet) throws SQLException {
        return new Complaint(
                resultSet.getInt("complaint_id"), resultSet.getInt("student_id"),
                resultSet.getString("roll_no"), resultSet.getString("student_name"),
                resultSet.getInt("course_id"), resultSet.getString("course_code"),
                resultSet.getString("course_name"), resultSet.getInt("faculty_id"),
                resultSet.getString("faculty_name"), resultSet.getString("category"),
                resultSet.getString("subject"), resultSet.getString("description"),
                resultSet.getString("status"), resultSet.getString("faculty_remark"),
                resultSet.getTimestamp("created_at"), resultSet.getTimestamp("updated_at"));
    }

    private ComplaintRow toComplaintRow(Complaint complaint) {
        return new ComplaintRow(complaint.getComplaintId(), complaint.getStudentId(),
                complaint.getCourseId(), complaint.getFacultyId(), complaint.getStudentRollNo(),
                complaint.getCourseCode(), complaint.getCategory(), complaint.getSubject(),
                complaint.getDescription(), complaint.getStatus(), complaint.getFacultyRemark(),
                complaint.getCreatedAt() == null ? null : complaint.getCreatedAt().toLocalDateTime());
    }

    private void deleteComplaint(Connection connection, int complaintId) throws SQLException {
        requirePositive(complaintId, "complaintId");
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM complaints WHERE complaint_id = ?")) {
            statement.setInt(1, complaintId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Complaint not found: " + complaintId);
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

    private void validateOptionalMarks(Double internal, Double midSem, Double endSem) {
        if (internal == null && midSem == null && endSem == null) return;
        validateScore(internal, 20, "Internal");
        validateScore(midSem, 30, "Mid-semester");
        validateScore(endSem, 50, "End-semester");
    }

    private void validateScore(Double score, double maximum, String label) {
        if (score == null || !Double.isFinite(score) || score < 0 || score > maximum) {
            throw new IllegalArgumentException(label + " marks must be between 0 and " + maximum + ".");
        }
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private void validateCategory(String category) {
        if (!List.of("MARKS", "ATTENDANCE", "OTHER").contains(category)) {
            throw new IllegalArgumentException("Unsupported complaint category.");
        }
    }

    private void requireFinalStatus(String status) {
        if (!"RESOLVED".equals(status) && !"REJECTED".equals(status)) {
            throw new IllegalArgumentException("Status must be RESOLVED or REJECTED.");
        }
    }

    private void requireStatus(String status) {
        if (!List.of("PENDING", "RESOLVED", "REJECTED").contains(status)) {
            throw new IllegalArgumentException("Unsupported complaint status.");
        }
    }

    private void requirePositive(int value, String label) {
        if (value <= 0) throw new IllegalArgumentException(label + " must be positive.");
    }

    private void requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            original.addSuppressed(rollbackException);
        }
    }
}
