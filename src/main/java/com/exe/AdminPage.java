package com.exe;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileOutputStream;
import java.sql.*;
import com.itextpdf.text.Font; // Correct Font for iText
import com.itextpdf.text.BaseColor; // BaseColor for styling


public class AdminPage extends JFrame {
    private JButton exportPdfButton, addCourseButton;
    private JTextField newCourseField;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/course_management";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";

    public AdminPage() {
        setTitle("Admin Page");
        setSize(425, 132);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        exportPdfButton = new JButton("Export Data to PDF");
        exportPdfButton.setBounds(41, 48, 156, 21);
        addCourseButton = new JButton("Add New Course");
        addCourseButton.setBounds(225, 48, 133, 21);
        newCourseField = new JTextField(20);
        newCourseField.setBounds(83, 6, 166, 19);

        exportPdfButton.addActionListener(new ExportPdfAction());
        addCourseButton.addActionListener(new AddCourseAction());
        getContentPane().setLayout(null);

        JLabel label = new JLabel("Add Course:");
        label.setBounds(5, 9, 90, 13);
        getContentPane().add(label);
        getContentPane().add(newCourseField);
        getContentPane().add(addCourseButton);
        getContentPane().add(exportPdfButton);
    }

    private class ExportPdfAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream("ExportedData.pdf"));
                document.open();

                // Title Section
                Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
                Paragraph title = new Paragraph("Admin Report: Registered Users and Courses", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(20);
                document.add(title);

                // Users Section
                Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
                Font contentFont = new Font(Font.FontFamily.HELVETICA, 10);
                Font courseFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
                BaseColor headerColor = new BaseColor(0, 102, 204); // Light Blue

                // Fetch and display all registered users
                Statement stmt = conn.createStatement();
                ResultSet users = stmt.executeQuery("SELECT * FROM users");

                // Adding section for users
                Paragraph userSection = new Paragraph("Registered Users (with details):", headerFont);
                userSection.setSpacingAfter(10);
                document.add(userSection);

                PdfPTable userTable = new PdfPTable(5); // Updated to 5 columns (ID, username, email, registration time, course count)
                userTable.setWidthPercentage(100);
                userTable.setSpacingBefore(10);

                // Add table headers with background color
                PdfPCell headerUsername = new PdfPCell(new Phrase("Username", courseFont));
                headerUsername.setBackgroundColor(headerColor);
                PdfPCell headerEmail = new PdfPCell(new Phrase("Email", courseFont));
                headerEmail.setBackgroundColor(headerColor);
                PdfPCell headerRegTime = new PdfPCell(new Phrase("Registration Time", courseFont));
                headerRegTime.setBackgroundColor(headerColor);
                PdfPCell headerUserId = new PdfPCell(new Phrase("User ID", courseFont));
                headerUserId.setBackgroundColor(headerColor);
                PdfPCell headerCourseCount = new PdfPCell(new Phrase("Course Count", courseFont));
                headerCourseCount.setBackgroundColor(headerColor);

                userTable.addCell(headerUsername);
                userTable.addCell(headerEmail);
                userTable.addCell(headerRegTime);
                userTable.addCell(headerUserId);
                userTable.addCell(headerCourseCount);

                // Add users to the table
                while (users.next()) {
                    String username = users.getString("username");
                    String email = users.getString("email");
                    String registrationTime = users.getString("registration_time");
                    int userId = users.getInt("id");

                    // Query to count courses for this user
                    String courseCountQuery = "SELECT COUNT(*) FROM user_courses WHERE user_id = ?";
                    try (PreparedStatement courseCountStmt = conn.prepareStatement(courseCountQuery)) {
                        courseCountStmt.setInt(1, userId);
                        ResultSet courseCountResult = courseCountStmt.executeQuery();
                        int courseCount = 0;
                        if (courseCountResult.next()) {
                            courseCount = courseCountResult.getInt(1);
                        }

                        // Add row for each user with their registration time and course count
                        userTable.addCell(new PdfPCell(new Phrase(username, contentFont)));
                        userTable.addCell(new PdfPCell(new Phrase(email, contentFont)));
                        userTable.addCell(new PdfPCell(new Phrase(registrationTime, contentFont)));
                        userTable.addCell(new PdfPCell(new Phrase(String.valueOf(userId), contentFont)));
                        userTable.addCell(new PdfPCell(new Phrase(String.valueOf(courseCount), contentFont)));
                    }
                }

                // Add the user table to the document
                document.add(userTable);

                // Courses Section
                Paragraph courseSection = new Paragraph("\nCourses and Registered Users:", headerFont);
                courseSection.setSpacingBefore(20);
                document.add(courseSection);

                // Fetch and display all courses
                String courseQuery = "SELECT courses.course_name, user_courses.duration, courses.id " +
                                     "FROM courses JOIN user_courses ON courses.id = user_courses.course_id";
                ResultSet courses = stmt.executeQuery(courseQuery);

                while (courses.next()) {
                    String courseName = courses.getString("course_name");
                    String duration = courses.getString("duration");  // Fetch the duration from user_courses
                    int courseId = courses.getInt("id");

                    // Add course title to PDF with styling
                    Font courseTitleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
                    Paragraph courseTitle = new Paragraph("Course: " + courseName + " (" + duration + ")", courseTitleFont);
                    courseTitle.setSpacingBefore(15);
                    courseTitle.setSpacingAfter(5);
                    document.add(courseTitle);

                    // Query users registered for this course
                    String userQuery = "SELECT users.username, users.email FROM users " +
                                        "JOIN user_courses ON users.id = user_courses.user_id " +
                                        "WHERE user_courses.course_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(userQuery)) {
                        pstmt.setInt(1, courseId);
                        ResultSet enrolledUsers = pstmt.executeQuery();

                        // Create a table to display users enrolled in the course
                        PdfPTable enrolledUsersTable = new PdfPTable(2); // Columns for username and email
                        enrolledUsersTable.setWidthPercentage(100);
                        enrolledUsersTable.setSpacingBefore(10);

                        // Add table headers for enrolled users
                        PdfPCell enrolledHeaderUsername = new PdfPCell(new Phrase("Username", courseFont));
                        enrolledHeaderUsername.setBackgroundColor(headerColor);
                        PdfPCell enrolledHeaderEmail = new PdfPCell(new Phrase("Email", courseFont));
                        enrolledHeaderEmail.setBackgroundColor(headerColor);

                        enrolledUsersTable.addCell(enrolledHeaderUsername);
                        enrolledUsersTable.addCell(enrolledHeaderEmail);

                        // Add enrolled users to the table
                        while (enrolledUsers.next()) {
                            enrolledUsersTable.addCell(new PdfPCell(new Phrase(enrolledUsers.getString("username"), contentFont)));
                            enrolledUsersTable.addCell(new PdfPCell(new Phrase(enrolledUsers.getString("email"), contentFont)));
                        }

                        document.add(enrolledUsersTable);
                    }
                }

                // Close the document and display the success message
                document.close();
                JOptionPane.showMessageDialog(null, "Data exported to PDF successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
            }
        }
    }

    private class AddCourseAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String newCourse = newCourseField.getText();
            if (newCourse.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please enter a course name.");
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                String query = "INSERT INTO courses (course_name) VALUES (?)";
                PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setString(1, newCourse);
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(null, "New course added successfully!");
                newCourseField.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AdminPage().setVisible(true);
        });
    }
}