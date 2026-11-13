# Student Management System

A Java-based Student Management System developed using **Java Swing, JDBC, MySQL, and Maven**. The application provides a graphical interface for managing student information, marks, attendance, and results, with MySQL used to store records persistently.


---
## 👥 Project Contributors & Team Division

| Member Name | Role | Key Contributions |
| :--- | :--- | :--- |
| **Aman Kumar Sharma** | Full-Stack Architecture & Backend | Application core architecture, JDBC connection management, Swing GUI layout, MySQL transaction handling |
| **Nasreen Ansari** | Database & Business Logic | Relational schema design (`setup.sql`), attendance calculation algorithms, grievance workflow queries |
| **Pranisha Chettri** | Frontend Validation & Documentation | Input validation rules, search & result calculation modules, testing, and technical documentation |

---


## 📌 Project Overview

The Student Management System is designed to simplify student record management through a user-friendly desktop application. It allows users to register students, manage academic marks, record attendance, calculate results, and search student records.

## ✨ Features

* **Student Registration:** Add and manage student details, including roll number, name, course, and age.
* **Marks Management:** Enter and manage marks for three subjects.
* **Attendance Management:** Record total classes and classes attended.
* **Result Calculation:** Calculate student totals and percentages based on entered marks.
* **Record Search:** Search for student records.
* **All Records:** View stored student information in a table.
* **MySQL Database:** Store student information persistently.
* **Graphical User Interface:** Use a desktop interface built with Java Swing.
* **Academic Grievances:** Students can submit marks, attendance, and general complaints; assigned faculty can review, correct academic records, and provide resolution feedback.
* **Administrative Overrides:** Administrators can edit/delete student, faculty, and course records, and override or purge grievances.

The grievance workflow requires the `complaints` table and the `students.contact` column defined by `sql/setup.sql`. The setup script resets the schema and deletes existing data; back up the database before running it.

Faculty attendance grievance corrections operate on dated attendance records. To change the total class count, provide the exact dates to add or remove; added records start as absent, then the requested present count is reconciled in the same transaction. Dates selected for removal must already exist for that enrollment.

## 🛠️ Technologies Used

| Technology         | Purpose                                 |
| ------------------ | --------------------------------------- |
| Java               | Application logic and backend           |
| Java Swing         | Graphical user interface                |
| JDBC               | Communication between Java and MySQL    |
| MySQL              | Database and persistent storage         |
| Maven              | Project build and dependency management |
| MySQL Connector/J  | JDBC driver for MySQL                   |
| Visual Studio Code | Development environment                 |

## 📂 Project Structure

```text
StudentManagementSystemMySQL/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── example/
│                   └── student/
│                       ├── StudentManagementSystem.java
│                       └── DBConnection.java
├── sql/
│   └── setup.sql
├── pom.xml
├── README.md
└── .gitignore
```

## ⚙️ Prerequisites

Before running the application, install the following:

* Java JDK
* Apache Maven
* MySQL Server
* MySQL Workbench (optional, for database management)
* Visual Studio Code (recommended)

## 🗄️ Database Setup

1. Start your MySQL Server.
2. Open MySQL Workbench and connect to your MySQL instance.
3. Open the `sql/setup.sql` file from this project.
4. Execute the SQL script to create the required database and tables.
5. Verify that the database and tables have been created successfully.

**Warning:** `sql/setup.sql` drops and recreates the application tables, including grievances. Running it deletes all existing records.

## 🔌 Database Configuration

The application uses JDBC to connect Java to MySQL.

Configure your database connection details in a `.env` file in the project root:

```properties
SMS_DB_USER=root
SMS_DB_PASSWORD=your_mysql_password
```

Copy `.env.example` to `.env` and replace the placeholder password with your MySQL password. `.env` is ignored by Git; do not commit it. The application also accepts `SMS_DB_USER` and `SMS_DB_PASSWORD` environment variables, which take precedence over values in `.env`.

The database URL is `jdbc:mysql://localhost:3306/student_management?serverTimezone=UTC`.


## ▶️ How to Run the Project

### 1. Open the Project

Open the `StudentManagementSystemMySQL` folder in Visual Studio Code.

### 2. Configure MySQL

Make sure MySQL Server is running and that the database setup script has been executed.

### 3. Open the Terminal

In VS Code, select **Terminal → New Terminal** and navigate to the folder containing `pom.xml`.

### 4. Compile the Project

```bash
mvn clean compile
```

### 5. Run the Application

```bash
mvn exec:java
```

If Maven cannot identify the main class, verify the `exec-maven-plugin` configuration in `pom.xml` and ensure the main class is correctly specified.

## 🔄 How It Works

1. The user interacts with the application through the Java Swing interface.
2. Java handles user input and application operations.
3. JDBC establishes a connection between the Java application and MySQL.
4. SQL queries insert, retrieve, and update student records.
5. MySQL stores the records so they remain available after the application closes and reopens.

## 🎯 Learning Outcomes

This project demonstrates:

* Java application development using object-oriented programming.
* GUI development using Java Swing.
* Database connectivity using JDBC.
* MySQL database creation and record management.
* Maven project structure, dependencies, and build commands.
* Integration of a Java desktop application with a relational database.

## 🚀 Future Enhancements

* User login and role-based access.
* Export student records to CSV or PDF.
* Improved validation and error handling.
* Report generation for marks and attendance.
* Enhanced user interface and dashboard statistics.

## 👩‍💻 Project Information

**Project Name:** Student Management System
**Project Type:** Java Desktop Application
**Backend:** Java
**Frontend:** Java Swing
**Database:** MySQL
**Build Tool:** Maven

---

**Developed as an academic project to demonstrate Java, database connectivity, and student record management.**
