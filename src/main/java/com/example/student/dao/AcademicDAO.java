package com.example.student.dao;

import com.example.student.DBConnection;
import com.example.student.model.AttendanceSummary;
import com.example.student.model.BunkRecoveryStats;
import com.example.student.model.EnrolledCourseGrade;
import com.example.student.model.GradeRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AcademicDAO {
    private static final String ATTENDANCE_SUMMARY_SQL = """
            SELECT c.course_id, c.course_code, c.course_name,
                   COALESCE(SUM(CASE WHEN a.status IN ('PRESENT', 'LATE') THEN 1 ELSE 0 END), 0) AS attended,
                   COUNT(a.attendance_id) AS total
            FROM enrollments e
            JOIN courses c ON c.course_id = e.course_id
            LEFT JOIN attendance a ON a.enrollment_id = e.enrollment_id
            WHERE e.student_id = ?
            GROUP BY c.course_id, c.course_code, c.course_name
            ORDER BY c.course_code
            """;

    private static final String COURSE_ATTENDANCE_SQL = """
            SELECT COALESCE(SUM(CASE WHEN a.status IN ('PRESENT', 'LATE') THEN 1 ELSE 0 END), 0) AS attended,
                   COUNT(a.attendance_id) AS total
            FROM enrollments e
            LEFT JOIN attendance a ON a.enrollment_id = e.enrollment_id
            WHERE e.student_id = ? AND e.course_id = ?
            """;

    private static final String ENROLLMENT_EXISTS_SQL =
            "SELECT 1 FROM enrollments WHERE student_id = ? AND course_id = ? LIMIT 1";

    private static final String STUDENT_HAS_ENROLLMENTS_SQL =
            "SELECT 1 FROM enrollments WHERE student_id = ? LIMIT 1";

    private static final String UNDER_ATTENDANCE_COURSE_SQL = """
            SELECT 1
            FROM enrollments e
            LEFT JOIN attendance a ON a.enrollment_id = e.enrollment_id
            WHERE e.student_id = ?
            GROUP BY e.course_id
            HAVING COUNT(a.attendance_id) = 0
                OR 4 * SUM(CASE WHEN a.status IN ('PRESENT', 'LATE') THEN 1 ELSE 0 END)
                   < 3 * COUNT(a.attendance_id)
            LIMIT 1
            """;

    private static final String ENROLLED_COURSES_WITH_GRADES_SQL = """
            SELECT e.enrollment_id, e.course_id, c.course_code, c.course_name, c.credits, e.semester,
                   m.mark_id, m.internal, m.mid_sem, m.end_sem, m.is_locked
            FROM enrollments e
            JOIN courses c ON c.course_id = e.course_id
            LEFT JOIN marks m ON m.enrollment_id = e.enrollment_id
            WHERE e.student_id = ?
            ORDER BY e.semester, c.course_code
            """;

    private static final String SELECT_MARK_LOCK_SQL =
            "SELECT is_locked FROM marks WHERE enrollment_id = ? FOR UPDATE";

    private static final String INSERT_MARKS_SQL =
            "INSERT INTO marks (enrollment_id, internal, mid_sem, end_sem, is_locked) VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE_MARKS_SQL = """
            UPDATE marks
            SET internal = ?, mid_sem = ?, end_sem = ?, is_locked = ?
            WHERE enrollment_id = ? AND is_locked = FALSE
            """;

    public void adminUpdateStudent(int studentId, String name, String department,
                                   int semester, String contact) throws SQLException {
        requirePositiveId(studentId, "studentId");
        if (name == null || name.isBlank() || department == null || department.isBlank()) {
            throw new IllegalArgumentException("Student name and department are required.");
        }
        if (semester < 1 || semester > 255) {
            throw new IllegalArgumentException("Semester must be between 1 and 255.");
        }
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE students SET name = ?, department = ?, semester = ?, contact = ?
                     WHERE student_id = ?
                     """)) {
            statement.setString(1, name.trim());
            statement.setString(2, department.trim());
            statement.setInt(3, semester);
            if (contact == null || contact.isBlank()) {
                statement.setNull(4, java.sql.Types.VARCHAR);
            } else {
                statement.setString(4, contact.trim());
            }
            statement.setInt(5, studentId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Student not found: " + studentId);
            }
        }
    }

    public void adminDeleteStudent(int studentId) throws SQLException {
        requirePositiveId(studentId, "studentId");
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM users
                     WHERE user_id = (SELECT student_user_id FROM (
                         SELECT user_id AS student_user_id FROM students WHERE student_id = ?
                     ) AS student_to_delete)
                     """)) {
            statement.setInt(1, studentId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Student not found: " + studentId);
            }
        }
    }

    public void adminDeleteStudent(long adminUserId, int studentId) throws SQLException {
        requireAdminUser(adminUserId);
        adminDeleteStudent(studentId);
    }

    public void adminOverrideMarks(long adminUserId, int studentId, int courseId,
                                   double internal, double midSem, double endSem) throws SQLException {
        requireAdminUser(adminUserId);
        adminOverrideMarks(studentId, courseId, internal, midSem, endSem);
    }

    public void adminOverrideAttendance(long adminUserId, int studentId, int courseId,
                                        int totalClasses, int presentClasses) throws SQLException {
        requireAdminUser(adminUserId);
        adminOverrideAttendance(studentId, courseId, totalClasses, presentClasses);
    }

    public void adminOverrideMarks(int studentId, int courseId, double internal,
                                   double midSem, double endSem) throws SQLException {
        requirePositiveId(studentId, "studentId");
        requirePositiveId(courseId, "courseId");
        validateScore("internal", internal, 20);
        validateScore("midSem", midSem, 30);
        validateScore("endSem", endSem, 50);
        String sql = """
                INSERT INTO marks (enrollment_id, internal, mid_sem, end_sem, is_locked)
                SELECT e.enrollment_id, ?, ?, ?, FALSE
                FROM enrollments e
                JOIN students s ON s.student_id = e.student_id AND e.semester = s.semester
                WHERE e.student_id = ? AND e.course_id = ?
                ON DUPLICATE KEY UPDATE internal = VALUES(internal),
                    mid_sem = VALUES(mid_sem), end_sem = VALUES(end_sem)
                """;
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, internal);
            statement.setDouble(2, midSem);
            statement.setDouble(3, endSem);
            statement.setInt(4, studentId);
            statement.setInt(5, courseId);
            if (statement.executeUpdate() == 0) {
                throw new SQLException("No current enrollment exists for this student and course.");
            }
        }
    }

    public void adminOverrideAttendance(int studentId, int courseId, int totalClasses,
                                        int presentClasses) throws SQLException {
        requirePositiveId(studentId, "studentId");
        requirePositiveId(courseId, "courseId");
        if (totalClasses < 0 || presentClasses < 0 || presentClasses > totalClasses) {
            throw new IllegalArgumentException("Attendance counts must satisfy 0 <= present <= total.");
        }
        if (totalClasses > 10_000) {
            throw new IllegalArgumentException("Attendance overrides are limited to 10,000 class records.");
        }
        try (Connection connection = DBConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                int enrollmentId;
                try (PreparedStatement enrollment = connection.prepareStatement("""
                        SELECT e.enrollment_id FROM enrollments e
                        JOIN students s ON s.student_id = e.student_id AND e.semester = s.semester
                        WHERE e.student_id = ? AND e.course_id = ?
                        """)) {
                    enrollment.setInt(1, studentId);
                    enrollment.setInt(2, courseId);
                    try (ResultSet resultSet = enrollment.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("No current enrollment exists for this student and course.");
                        }
                        enrollmentId = resultSet.getInt("enrollment_id");
                    }
                }

                List<Integer> attendanceIds = new ArrayList<>();
                Set<LocalDate> attendanceDates = new HashSet<>();
                try (PreparedStatement select = connection.prepareStatement("""
                        SELECT attendance_id, `date` FROM attendance
                        WHERE enrollment_id = ? ORDER BY `date`, attendance_id FOR UPDATE
                        """)) {
                    select.setInt(1, enrollmentId);
                    try (ResultSet resultSet = select.executeQuery()) {
                        while (resultSet.next()) {
                            attendanceIds.add(resultSet.getInt("attendance_id"));
                            attendanceDates.add(resultSet.getDate("date").toLocalDate());
                        }
                    }
                }

                if (attendanceIds.size() > totalClasses) {
                    try (PreparedStatement delete = connection.prepareStatement(
                            "DELETE FROM attendance WHERE attendance_id = ?")) {
                        for (int index = totalClasses; index < attendanceIds.size(); index++) {
                            delete.setInt(1, attendanceIds.get(index));
                            delete.addBatch();
                        }
                        delete.executeBatch();
                    }
                    attendanceIds = new ArrayList<>(attendanceIds.subList(0, totalClasses));
                }

                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE attendance SET status = ? WHERE attendance_id = ?");
                     PreparedStatement insert = connection.prepareStatement(
                             "INSERT INTO attendance (enrollment_id, `date`, status) VALUES (?, ?, ?)")) {
                    for (int index = 0; index < attendanceIds.size(); index++) {
                        update.setString(1, index < presentClasses ? "PRESENT" : "ABSENT");
                        update.setInt(2, attendanceIds.get(index));
                        update.addBatch();
                    }
                    if (!attendanceIds.isEmpty()) update.executeBatch();

                    LocalDate candidate = LocalDate.now();
                    for (int index = attendanceIds.size(); index < totalClasses; index++) {
                        while (attendanceDates.contains(candidate)) candidate = candidate.minusDays(1);
                        attendanceDates.add(candidate);
                        insert.setInt(1, enrollmentId);
                        insert.setDate(2, Date.valueOf(candidate));
                        insert.setString(3, index < presentClasses ? "PRESENT" : "ABSENT");
                        insert.addBatch();
                        candidate = candidate.minusDays(1);
                    }
                    if (totalClasses > attendanceIds.size()) insert.executeBatch();
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

    public List<AttendanceSummary> getStudentAttendanceSummary(int studentId) throws SQLException {
        requirePositiveId(studentId, "studentId");
        List<AttendanceSummary> summaries = new ArrayList<>();

        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(ATTENDANCE_SUMMARY_SQL)) {
            statement.setInt(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long attended = resultSet.getLong("attended");
                    long total = resultSet.getLong("total");
                    summaries.add(new AttendanceSummary(
                            resultSet.getLong("course_id"),
                            resultSet.getString("course_code"),
                            resultSet.getString("course_name"),
                            attended,
                            total,
                            percentage(attended, total)));
                }
            }
        }
        return summaries;
    }

    public BunkRecoveryStats getBunkRecoveryStats(int studentId, int courseId) throws SQLException {
        requirePositiveId(studentId, "studentId");
        requirePositiveId(courseId, "courseId");

        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement existsStatement = connection.prepareStatement(ENROLLMENT_EXISTS_SQL)) {
            existsStatement.setInt(1, studentId);
            existsStatement.setInt(2, courseId);
            try (ResultSet existsResult = existsStatement.executeQuery()) {
                if (!existsResult.next()) {
                    throw new SQLException("Student is not enrolled in course " + courseId + ".");
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(COURSE_ATTENDANCE_SQL)) {
                statement.setInt(1, studentId);
                statement.setInt(2, courseId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    long attended = resultSet.getLong("attended");
                    long total = resultSet.getLong("total");
                    long difference = 4 * attended - 3 * total;
                    int classesToAttend = total == 0
                            ? 3
                            : difference < 0
                            ? Math.toIntExact(-difference)
                            : 0;
                    int classesCanMiss = difference > 0
                            ? Math.toIntExact(difference / 3)
                            : 0;
                    return new BunkRecoveryStats(
                            attended, total, percentage(attended, total),
                            classesToAttend, classesCanMiss);
                }
            }
        }
    }

    public boolean isEligibleForHallTicket(int studentId) throws SQLException {
        requirePositiveId(studentId, "studentId");

        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement enrollmentStatement =
                     connection.prepareStatement(STUDENT_HAS_ENROLLMENTS_SQL)) {
            enrollmentStatement.setInt(1, studentId);
            try (ResultSet enrollmentResult = enrollmentStatement.executeQuery()) {
                if (!enrollmentResult.next()) {
                    return false;
                }
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(UNDER_ATTENDANCE_COURSE_SQL)) {
                statement.setInt(1, studentId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return !resultSet.next();
                }
            }
        }
    }

    public void saveOrUpdateMarks(
            int enrollmentId, double internal, double midSem, double endSem, boolean lock)
            throws SQLException {
        requirePositiveId(enrollmentId, "enrollmentId");
        validateScore("internal", internal, 20);
        validateScore("midSem", midSem, 30);
        validateScore("endSem", endSem, 50);

        try (Connection connection = DBConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                boolean markExists;
                try (PreparedStatement selectStatement =
                             connection.prepareStatement(SELECT_MARK_LOCK_SQL)) {
                    selectStatement.setInt(1, enrollmentId);
                    try (ResultSet resultSet = selectStatement.executeQuery()) {
                        markExists = resultSet.next();
                        if (markExists && resultSet.getBoolean("is_locked")) {
                            throw new SQLException("Marks are locked for enrollment " + enrollmentId + ".");
                        }
                    }
                }

                if (markExists) {
                    try (PreparedStatement updateStatement =
                                 connection.prepareStatement(UPDATE_MARKS_SQL)) {
                        updateStatement.setDouble(1, internal);
                        updateStatement.setDouble(2, midSem);
                        updateStatement.setDouble(3, endSem);
                        updateStatement.setBoolean(4, lock);
                        updateStatement.setInt(5, enrollmentId);
                        if (updateStatement.executeUpdate() != 1) {
                            throw new SQLException("Marks could not be updated for enrollment " + enrollmentId + ".");
                        }
                    }
                } else {
                    try (PreparedStatement insertStatement =
                                 connection.prepareStatement(INSERT_MARKS_SQL)) {
                        insertStatement.setInt(1, enrollmentId);
                        insertStatement.setDouble(2, internal);
                        insertStatement.setDouble(3, midSem);
                        insertStatement.setDouble(4, endSem);
                        insertStatement.setBoolean(5, lock);
                        insertStatement.executeUpdate();
                    }
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

    public List<EnrolledCourseGrade> getEnrolledCoursesWithGrades(int studentId) throws SQLException {
        requirePositiveId(studentId, "studentId");
        List<EnrolledCourseGrade> courses = new ArrayList<>();

        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(ENROLLED_COURSES_WITH_GRADES_SQL)) {
            statement.setInt(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    GradeRecord gradeRecord = resultSet.getObject("mark_id") == null
                            ? null
                            : mapGradeRecord(resultSet);
                    courses.add(new EnrolledCourseGrade(
                            resultSet.getLong("enrollment_id"),
                            resultSet.getLong("course_id"),
                            resultSet.getString("course_code"),
                            resultSet.getString("course_name"),
                            resultSet.getInt("credits"),
                            resultSet.getInt("semester"),
                            gradeRecord));
                }
            }
        }
        return courses;
    }

    private GradeRecord mapGradeRecord(ResultSet resultSet) throws SQLException {
        BigDecimal internal = resultSet.getBigDecimal("internal");
        BigDecimal midSem = resultSet.getBigDecimal("mid_sem");
        BigDecimal endSem = resultSet.getBigDecimal("end_sem");
        BigDecimal total = internal.add(midSem).add(endSem);
        return new GradeRecord(
                resultSet.getLong("mark_id"),
                resultSet.getLong("enrollment_id"),
                internal,
                midSem,
                endSem,
                total,
                letterGrade(total),
                resultSet.getBoolean("is_locked"));
    }

    private String letterGrade(BigDecimal total) {
        if (total.compareTo(BigDecimal.valueOf(90)) >= 0) return "A+";
        if (total.compareTo(BigDecimal.valueOf(80)) >= 0) return "A";
        if (total.compareTo(BigDecimal.valueOf(70)) >= 0) return "B";
        if (total.compareTo(BigDecimal.valueOf(60)) >= 0) return "C";
        if (total.compareTo(BigDecimal.valueOf(40)) >= 0) return "D";
        return "F";
    }

    private double percentage(long attended, long total) {
        if (total == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(attended)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private void validateScore(String name, double score, double maximum) {
        if (!Double.isFinite(score) || score < 0 || score > maximum) {
            throw new IllegalArgumentException(name + " must be between 0 and " + maximum + ".");
        }
    }

    private void requirePositiveId(int id, String name) {
        if (id <= 0) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }

    private void requireAdminUser(long userId) throws SQLException {
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM users WHERE user_id = ? AND role = 'ADMIN'")) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Administrative privileges are required.");
                }
            }
        }
    }
}
