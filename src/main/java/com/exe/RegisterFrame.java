package com.exe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import javax.mail.*;
import javax.mail.internet.*;

public class RegisterFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField emailField;
    private JComboBox<String> courseComboBox;
    private JComboBox<String> durationComboBox; // Dropdown for duration (4 weeks or 6 weeks)
    private JTextField otpField;
    private JButton generateOtpButton;
    private JButton verifyOtpButton;
    private JButton registerButton;
    private Integer generatedOtp; // Store the OTP
    private List<String> allCourses; // List to store all available courses
    private String username, password, email, selectedCourse, selectedDuration;

    public RegisterFrame() {
        setTitle("Register");
        setSize(400, 450); // Adjusted size for new fields
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(8, 2, 10, 10)); // Adjusted layout for extra fields

        // Input fields for registration
        usernameField = new JTextField();
        passwordField = new JPasswordField();
        emailField = new JTextField();
        courseComboBox = new JComboBox<>();
        courseComboBox.setEditable(true); // Make the combo box editable
        loadCourses();

        // Add a document listener to filter courses dynamically
        JTextField editor = (JTextField) courseComboBox.getEditor().getEditorComponent();
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterCourses(editor.getText());
            }
        });

        // Dropdown for selecting course duration (4 weeks or 6 weeks)
        durationComboBox = new JComboBox<>();
        durationComboBox.addItem("4 weeks");
        durationComboBox.addItem("6 weeks");

        otpField = new JTextField();
        otpField.setEnabled(false); // Disable OTP field initially

        generateOtpButton = new JButton("Generate OTP");
        verifyOtpButton = new JButton("Verify OTP");
        verifyOtpButton.setEnabled(false); // Disable Verify OTP button initially
        registerButton = new JButton("Register");
        registerButton.setEnabled(false); // Disable Register button initially

        generateOtpButton.addActionListener(new GenerateOtpAction());
        verifyOtpButton.addActionListener(new VerifyOtpAction());
        registerButton.addActionListener(new RegisterAction());

        // Add components to the frame
        add(new JLabel("Username:"));
        add(usernameField);
        add(new JLabel("Password:"));
        add(passwordField);
        add(new JLabel("Email:"));
        add(emailField);
        add(new JLabel("Select Course:"));
        add(courseComboBox); // Combo box with search capability
        add(new JLabel("Select Duration:"));
        add(durationComboBox); // Duration dropdown
        add(new JLabel("Enter OTP:"));
        add(otpField);
        add(generateOtpButton);
        add(verifyOtpButton);
        add(new JLabel("")); // Empty label for spacing
        add(registerButton);
    }

    private void loadCourses() {
        allCourses = new ArrayList<>();
        try (Connection conn = Database.getConnection()) {
            String sql = "SELECT course_name FROM courses";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String courseName = rs.getString("course_name");
                allCourses.add(courseName);
                courseComboBox.addItem(courseName);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void filterCourses(String query) {
        courseComboBox.removeAllItems(); // Clear the combo box

        // Filter and add matching courses to the combo box
        for (String course : allCourses) {
            if (course.toLowerCase().contains(query.toLowerCase())) {
                courseComboBox.addItem(course);
            }
        }

        // Keep the current query in the editable field
        courseComboBox.getEditor().setItem(query);
        courseComboBox.hidePopup(); // To refresh the dropdown
        courseComboBox.showPopup();
    }

    // Inner class for OTP generation
    private class GenerateOtpAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            username = usernameField.getText();
            password = new String(passwordField.getPassword());
            email = emailField.getText();
            selectedCourse = (String) courseComboBox.getSelectedItem();
            selectedDuration = (String) durationComboBox.getSelectedItem();

            if (username.isEmpty() || password.isEmpty() || email.isEmpty() || selectedCourse == null || selectedDuration == null) {
                JOptionPane.showMessageDialog(null, "Please fill in all fields!");
                return;
            }

            // Generate a 4-digit OTP
            generatedOtp = new Random().nextInt(9000) + 1000;

            // Send the OTP to the user's email
            sendOtp(email, String.valueOf(generatedOtp));

            // Enable OTP field and verify button after sending OTP
            otpField.setEnabled(true);
            verifyOtpButton.setEnabled(true);

            JOptionPane.showMessageDialog(null, "OTP has been sent to your email.");
        }
    }

    // Method to send OTP via email
    private void sendOtp(String toEmail, String otp) {
        String host = "smtp.gmail.com"; // Example: Gmail's SMTP server
        final String user = "coursemanagement07@gmail.com"; // Your email address
        final String password = "wvag xlcl cwjz ceje"; // Your email password

        // Set the properties for the email session
        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true"); // Use TLS
        properties.put("mail.smtp.ssl.trust", host); // Enable SSL

        // Create an authenticated session with your email credentials
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });

        try {
            // Create the email message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(user));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Your OTP Code");

            // Email body with OTP
            message.setText("Your OTP code is: " + otp);

            // Send the email
            Transport.send(message);
            System.out.println("OTP email sent successfully to " + toEmail);
        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("Error sending OTP email.");
        }
    }

    // Inner class for OTP verification
    private class VerifyOtpAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (generatedOtp == null) {
                JOptionPane.showMessageDialog(null, "Please generate an OTP first!");
                return;
            }

            int enteredOtp;
            try {
                enteredOtp = Integer.parseInt(otpField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Invalid OTP format!");
                return;
            }

            if (enteredOtp == generatedOtp) {
                registerButton.setEnabled(true); // Enable the Register button once OTP is verified
                JOptionPane.showMessageDialog(null, "OTP Verified! You can now register.");
            } else {
                JOptionPane.showMessageDialog(null, "Incorrect OTP. Please try again.");
            }
        }
    }

    // Inner class for registration
    private class RegisterAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Validate the input fields
            if (username.isEmpty() || password.isEmpty() || email.isEmpty() || selectedCourse == null || selectedDuration == null) {
                JOptionPane.showMessageDialog(null, "Please fill in all fields!");
                return;
            }

            // Make sure OTP has been verified
            if (generatedOtp == null || !otpField.getText().equals(String.valueOf(generatedOtp))) {
                JOptionPane.showMessageDialog(null, "Please verify OTP first.");
                return;
            }

            try (Connection conn = Database.getConnection()) {
                // Insert user data into the users table, including registration time
                String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.setString(3, email);
                int rowsAffected = stmt.executeUpdate();

                if (rowsAffected > 0) {
                    // Retrieve the generated user ID
                    ResultSet rs = stmt.getGeneratedKeys();
                    if (rs.next()) {
                        int userId = rs.getInt(1); // Get the auto-generated user ID

                        // Now, insert into the user_courses table to register for the selected course
                        sql = "SELECT id FROM courses WHERE course_name = ?";
                        stmt = conn.prepareStatement(sql);
                        stmt.setString(1, selectedCourse);
                        rs = stmt.executeQuery();

                        if (rs.next()) {
                            int courseId = rs.getInt("id");

                            // Now, register the user for the selected course
                            sql = "INSERT INTO user_courses (user_id, course_id, duration) VALUES (?, ?, ?)";
                            stmt = conn.prepareStatement(sql);
                            stmt.setInt(1, userId);
                            stmt.setInt(2, courseId);
                            stmt.setString(3, selectedDuration);
                            stmt.executeUpdate();

                            JOptionPane.showMessageDialog(null, "Registration Successful!");
                            dispose(); // Close the RegisterFrame
                            new LoginFrame().setVisible(true); // Show the LoginFrame
                        } else {
                            JOptionPane.showMessageDialog(null, "Course not found.");
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Failed to register user.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Registration Failed!");
            }
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegisterFrame().setVisible(true));
    }
}