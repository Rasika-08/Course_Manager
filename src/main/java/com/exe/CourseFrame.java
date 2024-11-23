package com.exe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CourseFrame extends JFrame {
    private JComboBox<String> courseComboBox;
    private int userId;
    private DefaultComboBoxModel<String> registeredCoursesModel;

    public CourseFrame(int userId) {
        this.userId = userId;
        setTitle("Course Management");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Heading label
        JLabel headingLabel = new JLabel("Manage Your Courses", JLabel.CENTER);
        headingLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headingLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(headingLabel, BorderLayout.NORTH);

        // Panel for course selection and modification buttons
        JPanel coursePanel = new JPanel(new GridLayout(3, 1, 10, 10));
        coursePanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));

        // ComboBox for user's registered courses
        registeredCoursesModel = new DefaultComboBoxModel<>();
        courseComboBox = new JComboBox<>(registeredCoursesModel);
        loadRegisteredCourses();

        coursePanel.add(new JLabel("Your Registered Courses:", JLabel.CENTER));
        coursePanel.add(courseComboBox);

        // Button panel for action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        JButton addCourseButton = new JButton("Add Course");
        JButton removeCourseButton = new JButton("Remove Course");
        JButton logoutButton = new JButton("Logout");

        // Button colors
        addCourseButton.setBackground(new Color(0, 120, 215)); // Blue color
        addCourseButton.setForeground(Color.WHITE);
        removeCourseButton.setBackground(new Color(220, 53, 69)); // Red color
        removeCourseButton.setForeground(Color.WHITE);
        logoutButton.setBackground(new Color(0, 150, 0)); // Green color
        logoutButton.setForeground(Color.WHITE);

        // Add action listeners for buttons
        addCourseButton.addActionListener(new AddCourseAction());
        removeCourseButton.addActionListener(new RemoveCourseAction());
        logoutButton.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        buttonPanel.add(addCourseButton);
        buttonPanel.add(removeCourseButton);
        buttonPanel.add(logoutButton);

        add(coursePanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadRegisteredCourses() {
        registeredCoursesModel.removeAllElements(); // Clear the previous courses
        try (Connection conn = Database.getConnection()) {
            String sql = "SELECT c.course_name, uc.duration FROM courses c " +
                         "JOIN user_courses uc ON c.id = uc.course_id " +
                         "WHERE uc.user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            boolean hasCourses = false;
            while (rs.next()) {
                String courseName = rs.getString("course_name");
                String duration = rs.getString("duration");
                registeredCoursesModel.addElement(courseName + " - Duration: " + duration);
                hasCourses = true;
            }

            if (!hasCourses) {
                registeredCoursesModel.addElement("No courses found"); // Placeholder text if no courses are registered
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading courses.");
        }
    }

    private class AddCourseAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Create a dialog for selecting the course and duration
            JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
            JComboBox<String> courseDropdown = new JComboBox<>();
            JComboBox<String> durationDropdown = new JComboBox<>(new String[]{"4 weeks", "6 weeks"});

            try (Connection conn = Database.getConnection()) {
                // Load courses into the dropdown
                String sql = "SELECT course_name FROM courses";
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    courseDropdown.addItem(rs.getString("course_name"));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            panel.add(new JLabel("Select Course:"));
            panel.add(courseDropdown);
            panel.add(new JLabel("Select Duration:"));
            panel.add(durationDropdown);

            int option = JOptionPane.showConfirmDialog(null, panel, "Add Course", JOptionPane.OK_CANCEL_OPTION);

            if (option == JOptionPane.OK_OPTION) {
                String selectedCourse = (String) courseDropdown.getSelectedItem();
                String selectedDuration = (String) durationDropdown.getSelectedItem();

                if (selectedCourse != null && selectedDuration != null) {
                    try (Connection conn = Database.getConnection()) {
                        // Get course ID
                        String sql = "SELECT id FROM courses WHERE course_name = ?";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, selectedCourse);
                        ResultSet rs = stmt.executeQuery();

                        int courseId = -1;
                        if (rs.next()) {
                            courseId = rs.getInt("id");
                        } else {
                            JOptionPane.showMessageDialog(null, "Course not found.");
                            return;
                        }

                        // Insert the course for the user in user_courses table
                        sql = "INSERT INTO user_courses (user_id, course_id, duration) VALUES (?, ?, ?)";
                        stmt = conn.prepareStatement(sql);
                        stmt.setInt(1, userId);
                        stmt.setInt(2, courseId);
                        stmt.setString(3, selectedDuration);
                        stmt.executeUpdate();

                        JOptionPane.showMessageDialog(null, "Course added successfully!");
                        loadRegisteredCourses();  // Refresh the course list
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Error adding course.");
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Please select both course and duration.");
                }
            }
        }
    }

    private class RemoveCourseAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Show a dialog box to select a course to remove
            JList<String> courseList = new JList<>(getRegisteredCoursesList());
            courseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            courseList.setVisibleRowCount(5); // Show 5 rows at a time

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JScrollPane(courseList), BorderLayout.CENTER);

            int option = JOptionPane.showConfirmDialog(null, panel, "Select Course to Remove", JOptionPane.OK_CANCEL_OPTION);

            if (option == JOptionPane.OK_OPTION) {
                String selectedCourse = courseList.getSelectedValue();

                if (selectedCourse != null) {
                    // Extract the course name (before the ' - Duration: <duration>' part)
                    String courseName = selectedCourse.split(" - ")[0];

                    try (Connection conn = Database.getConnection()) {
                        // Retrieve the course ID
                        String sql = "SELECT id FROM courses WHERE course_name = ?";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, courseName);
                        ResultSet rs = stmt.executeQuery();

                        int courseId = -1;
                        if (rs.next()) {
                            courseId = rs.getInt("id");
                        } else {
                            JOptionPane.showMessageDialog(null, "Course not found.");
                            return;
                        }

                        // Remove the course from user_courses
                        sql = "DELETE FROM user_courses WHERE user_id = ? AND course_id = ?";
                        stmt = conn.prepareStatement(sql);
                        stmt.setInt(1, userId);
                        stmt.setInt(2, courseId);
                        stmt.executeUpdate();

                        JOptionPane.showMessageDialog(null, "Course removed successfully!");
                        loadRegisteredCourses();  // Refresh the course list
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Error removing course.");
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "No course selected.");
                }
            }
        }

        // Helper method to get the list of registered courses in a String array
        private String[] getRegisteredCoursesList() {
            java.util.List<String> coursesList = new java.util.ArrayList<>();
            try (Connection conn = Database.getConnection()) {
                String sql = "SELECT c.course_name, uc.duration FROM courses c " +
                             "JOIN user_courses uc ON c.id = uc.course_id " +
                             "WHERE uc.user_id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    String courseName = rs.getString("course_name");
                    String duration = rs.getString("duration");
                    coursesList.add(courseName + " - Duration: " + duration);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return coursesList.toArray(new String[0]); // Convert the list to an array
        }
    }

    public static void main(String[] args) {
        // Example user ID for testing
        new CourseFrame(1).setVisible(true);
    }
}