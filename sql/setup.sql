CREATE DATABASE IF NOT EXISTS student_management;
USE student_management;

-- WARNING: This setup resets the schema and deletes all existing records.
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS complaints;
DROP TABLE IF EXISTS marks;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS faculty;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
    user_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash CHAR(64) NOT NULL,
    role ENUM('ADMIN', 'FACULTY', 'STUDENT') NOT NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_username (username)
) ENGINE = InnoDB;

CREATE TABLE students (
    student_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    roll_no VARCHAR(30) NOT NULL,
    name VARCHAR(120) NOT NULL,
    contact VARCHAR(40) NULL,
    department VARCHAR(100) NOT NULL,
    semester TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (student_id),
    UNIQUE KEY uq_students_user_id (user_id),
    UNIQUE KEY uq_students_roll_no (roll_no),
    CONSTRAINT chk_students_semester CHECK (semester > 0),
    CONSTRAINT fk_students_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE faculty (
    faculty_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    emp_id VARCHAR(30) NOT NULL,
    name VARCHAR(120) NOT NULL,
    department VARCHAR(100) NOT NULL,
    PRIMARY KEY (faculty_id),
    UNIQUE KEY uq_faculty_user_id (user_id),
    UNIQUE KEY uq_faculty_emp_id (emp_id),
    CONSTRAINT fk_faculty_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE courses (
    course_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    course_code VARCHAR(20) NOT NULL,
    course_name VARCHAR(120) NOT NULL,
    credits TINYINT UNSIGNED NOT NULL,
    faculty_id BIGINT UNSIGNED NULL,
    PRIMARY KEY (course_id),
    UNIQUE KEY uq_courses_course_code (course_code),
    CONSTRAINT chk_courses_credits CHECK (credits > 0),
    CONSTRAINT fk_courses_faculty
        FOREIGN KEY (faculty_id) REFERENCES faculty (faculty_id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE enrollments (
    enrollment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_id BIGINT UNSIGNED NOT NULL,
    course_id BIGINT UNSIGNED NOT NULL,
    semester TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (enrollment_id),
    UNIQUE KEY uq_enrollments_student_course_semester (student_id, course_id, semester),
    CONSTRAINT chk_enrollments_semester CHECK (semester > 0),
    CONSTRAINT fk_enrollments_student
        FOREIGN KEY (student_id) REFERENCES students (student_id) ON DELETE CASCADE,
    CONSTRAINT fk_enrollments_course
        FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE attendance (
    attendance_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT UNSIGNED NOT NULL,
    `date` DATE NOT NULL,
    status ENUM('PRESENT', 'ABSENT', 'LATE', 'EXCUSED') NOT NULL,
    PRIMARY KEY (attendance_id),
    UNIQUE KEY uq_attendance_enrollment_date (enrollment_id, `date`),
    CONSTRAINT fk_attendance_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments (enrollment_id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE marks (
    mark_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT UNSIGNED NOT NULL,
    internal DECIMAL(5,2) NOT NULL DEFAULT 0,
    mid_sem DECIMAL(5,2) NOT NULL DEFAULT 0,
    end_sem DECIMAL(5,2) NOT NULL DEFAULT 0,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (mark_id),
    UNIQUE KEY uq_marks_enrollment_id (enrollment_id),
    CONSTRAINT chk_marks_internal CHECK (internal >= 0 AND internal <= 100),
    CONSTRAINT chk_marks_mid_sem CHECK (mid_sem >= 0 AND mid_sem <= 100),
    CONSTRAINT chk_marks_end_sem CHECK (end_sem >= 0 AND end_sem <= 100),
    CONSTRAINT fk_marks_enrollment
        FOREIGN KEY (enrollment_id) REFERENCES enrollments (enrollment_id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS complaints (
    complaint_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_id BIGINT UNSIGNED NOT NULL,
    course_id BIGINT UNSIGNED NOT NULL,
    faculty_id BIGINT UNSIGNED NOT NULL,
    category ENUM('MARKS', 'ATTENDANCE', 'OTHER') NOT NULL,
    subject VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    status ENUM('PENDING', 'RESOLVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    faculty_remark TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (complaint_id),
    CONSTRAINT fk_complaints_student
        FOREIGN KEY (student_id) REFERENCES students (student_id) ON DELETE CASCADE,
    CONSTRAINT fk_complaints_course
        FOREIGN KEY (course_id) REFERENCES courses (course_id) ON DELETE CASCADE,
    CONSTRAINT fk_complaints_faculty
        FOREIGN KEY (faculty_id) REFERENCES faculty (faculty_id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- Demo credentials (change them after first use):
-- admin / Admin@123
-- faculty / Faculty@123
-- student / Student@123
-- These SHA-256 examples are for development only; production passwords should use
-- a password-hashing algorithm such as Argon2id or bcrypt.
INSERT INTO users (user_id, username, password_hash, role) VALUES
    (1, 'admin', SHA2('Admin@123', 256), 'ADMIN'),
    (2, 'faculty', SHA2('Faculty@123', 256), 'FACULTY'),
    (3, 'student', SHA2('Student@123', 256), 'STUDENT');

INSERT INTO faculty (faculty_id, user_id, emp_id, name, department) VALUES
    (1, 2, 'EMP001', 'Demo Faculty', 'Computer Science');

INSERT INTO students (student_id, user_id, roll_no, name, department, semester) VALUES
    (1, 3, 'STU001', 'Demo Student', 'Computer Science', 1);

INSERT INTO courses (course_id, course_code, course_name, credits, faculty_id) VALUES
    (1, 'CS101', 'Introduction to Computing', 3, 1);

INSERT INTO enrollments (enrollment_id, student_id, course_id, semester) VALUES
    (1, 1, 1, 1);

INSERT INTO attendance (enrollment_id, `date`, status) VALUES
    (1, CURRENT_DATE, 'PRESENT');

INSERT INTO marks (enrollment_id, internal, mid_sem, end_sem, is_locked) VALUES
    (1, 18.00, 25.00, 42.00, FALSE);

-- Seed test grievances.
INSERT INTO complaints (student_id, course_id, faculty_id, category, subject, description, status)
SELECT s.student_id, c.course_id, c.faculty_id, 'MARKS', 'Mid-Term Total Mismatch',
       'My mid-term marks show 18 instead of 24 as per evaluated script.', 'PENDING'
FROM students s
CROSS JOIN courses c
WHERE s.student_id = 1 AND c.course_id = 1 AND c.faculty_id IS NOT NULL
LIMIT 1;
