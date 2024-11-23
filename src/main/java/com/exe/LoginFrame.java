package com.exe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Random;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Add header
        JLabel headerLabel = new JLabel("Welcome to Course Management", JLabel.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(headerLabel, BorderLayout.NORTH);

        // Center panel for form input
        JPanel centerPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));

        // Username and Password fields
        usernameField = new JTextField();
        passwordField = new JPasswordField();

        centerPanel.add(new JLabel("Username:"));
        centerPanel.add(usernameField);
        centerPanel.add(new JLabel("Password:"));
        centerPanel.add(passwordField);

        add(centerPanel, BorderLayout.CENTER);

        // Button panel for Login, Register, and Admin buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");
        JButton adminButton = new JButton("Admin");
        JButton forgotPasswordButton = new JButton("Forgot Password?");  // New button for forgot password

        // Set button styles
        loginButton.setBackground(new Color(0, 120, 215));  // Blue color
        loginButton.setForeground(Color.WHITE);
        registerButton.setBackground(new Color(34, 139, 34));  // Green color
        registerButton.setForeground(Color.WHITE);
        adminButton.setBackground(new Color(255, 69, 0));  // Red color
        adminButton.setForeground(Color.WHITE);
        forgotPasswordButton.setForeground(Color.RED);  // Red color for Forgot Password

        loginButton.addActionListener(new LoginAction());
        registerButton.addActionListener(e -> {
            dispose();
            new RegisterFrame().setVisible(true);  // Open Register Frame
        });
        adminButton.addActionListener(e -> {
            dispose();
            new AdminPage().setVisible(true);  // Open Admin Page
        });

        // Action for Forgot Password
        forgotPasswordButton.addActionListener(e -> {
            // Open Forgot Password dialog
            new ForgotPasswordDialog(this).setVisible(true);
        });

        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        buttonPanel.add(adminButton);
        buttonPanel.add(forgotPasswordButton);  // Add the forgot password button to the panel
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private class LoginAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            try (Connection conn = Database.getConnection()) {
                // Validate user credentials
                String sql = "SELECT id, password FROM users WHERE username = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String storedPassword = rs.getString("password");

                    // Direct password comparison if using plain text (replace with hashed comparison if needed)
                    if (password.equals(storedPassword)) {
                        int userId = rs.getInt("id");
                        dispose();  // Close Login Frame
                        new CourseFrame(userId).setVisible(true);  // Open CourseFrame for logged-in user
                    } else {
                        JOptionPane.showMessageDialog(null, "Incorrect Password!", "Login Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Username not found!", "Login Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ForgotPasswordDialog to handle OTP generation and validation
    private class ForgotPasswordDialog extends JDialog {
        private JTextField usernameField;
        private JButton submitButton;
        private JTextField otpField;
        private JButton verifyOtpButton;
        private String generatedOtp;

        public ForgotPasswordDialog(JFrame parent) {
            super(parent, "Forgot Password", true);
            setSize(400, 250);
            setLocationRelativeTo(parent);
            setLayout(new FlowLayout());

            // Add components to the dialog
            usernameField = new JTextField(20);
            submitButton = new JButton("Send OTP");
            otpField = new JTextField(6);
            verifyOtpButton = new JButton("Verify OTP");

            add(new JLabel("Enter your Username:"));
            add(usernameField);
            add(submitButton);
            add(new JLabel("Enter OTP:"));
            add(otpField);
            add(verifyOtpButton);

            // Add action listeners for buttons
            submitButton.addActionListener(new SendOtpAction());
            verifyOtpButton.addActionListener(new VerifyOtpAction());
        }

        // Action for sending OTP to the user
        private class SendOtpAction implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                if (username.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Please enter your username");
                    return;
                }

                // Generate OTP
                generatedOtp = generateOtp();

                // Send OTP to the user's email
                boolean otpSent = sendOtpToEmail(username, generatedOtp);

                if (otpSent) {
                    JOptionPane.showMessageDialog(null, "OTP sent to your email!");
                } else {
                    JOptionPane.showMessageDialog(null, "Error in sending OTP. Please try again.");
                }
            }

            private String generateOtp() {
                Random rand = new Random();
                int otp = 100000 + rand.nextInt(900000); // Generate a 6-digit OTP
                return String.valueOf(otp);
            }

            // Function to send OTP to email
            private boolean sendOtpToEmail(String username, String otp) {
                try (Connection conn = Database.getConnection()) {
                    // Fetch user email based on the username
                    String userEmailQuery = "SELECT email FROM users WHERE username = ?";
                    PreparedStatement stmt = conn.prepareStatement(userEmailQuery);
                    stmt.setString(1, username);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        String email = rs.getString("email");

                        // Send OTP to the user's email
                        final String senderEmail = "coursemanagement07@gmail.com"; // Replace with your email
                        final String password = "wvag xlcl cwjz ceje"; // Replace with your email password

                        Properties properties = new Properties();
                        properties.put("mail.smtp.host", "smtp.gmail.com");
                        properties.put("mail.smtp.port", "587");
                        properties.put("mail.smtp.auth", "true");
                        properties.put("mail.smtp.starttls.enable", "true");

                        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(senderEmail, password);
                            }
                        });

                        try {
                            Message message = new MimeMessage(session);
                            message.setFrom(new InternetAddress(senderEmail));
                            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
                            message.setSubject("OTP for Password Reset");
                            message.setText("Your OTP for password reset is: " + otp);

                            Transport.send(message);
                            return true;
                        } catch (MessagingException e) {
                            e.printStackTrace();
                            return false;
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Username not found.");
                        return false;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }

        // Action for verifying OTP
        private class VerifyOtpAction implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                String enteredOtp = otpField.getText();
                if (enteredOtp.equals(generatedOtp)) {
                    JOptionPane.showMessageDialog(null, "OTP Verified. Please reset your password.");
                    resetPassword();
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid OTP. Please try again.");
                }
            }

            private void resetPassword() {
                String newPassword = JOptionPane.showInputDialog(null, "Enter your new password:");
                if (newPassword != null && !newPassword.isEmpty()) {
                    String username = usernameField.getText();

                    try (Connection conn = Database.getConnection()) {
                        // Step 1: Update the password for the specific user using the username
                        String updatePasswordQuery = "UPDATE users SET password = ? WHERE username = ?";
                        PreparedStatement stmt = conn.prepareStatement(updatePasswordQuery);
                        stmt.setString(1, newPassword); // New password
                        stmt.setString(2, username); // Specific username
                        int rowsAffected = stmt.executeUpdate();

                        if (rowsAffected > 0) {
                            JOptionPane.showMessageDialog(null, "Password reset successfully!");
                            dispose();
                        } else {
                            JOptionPane.showMessageDialog(null, "Error updating password.");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Password cannot be empty.");
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}