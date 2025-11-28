package com.hotelmanagement.quanlikhachsan.services.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

/**
 * Email Service for sending verification emails
 * 
 * Handles email composition and delivery via Spring Mail
 * Uses HTML templates for better presentation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${email.from.address}")
    private String fromAddress;

    @Value("${email.from.name}")
    private String fromName;

    /**
     * Send verification email with OTP
     * 
     * @param toEmail           Recipient email
     * @param otp               Generated OTP
     * @param fullName          User's full name
     * @param expirationMinutes OTP expiration time
     */
    public void sendVerificationEmail(String toEmail, String otp, String fullName, long expirationMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject("🔐 Email Verification - Your OTP Code");

            String htmlContent = buildVerificationEmailHtml(otp, fullName, expirationMinutes);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email: " + e.getMessage());
        }
    }

    /**
     * Send welcome email after successful verification
     * 
     * @param toEmail  Recipient email
     * @param fullName User's full name
     */
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(toEmail);
            helper.setSubject("🎉 Welcome to Hotel Management System!");

            String htmlContent = buildWelcomeEmailHtml(fullName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
            // Don't throw exception - welcome email is not critical
        }
    }

    /**
     * Build HTML content for verification email
     */
    private String buildVerificationEmailHtml(String otp, String fullName, long expirationMinutes) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #ea9d66ff 0%%, #e1ddb5ff 100%%);">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background: linear-gradient(135deg, #ea9d66ff 0%%, #e1ddb5ff 100%%);">
                        <tr>
                            <td align="center" style="padding: 40px 20px;">
                                <table width="600" cellpadding="0" cellspacing="0" style="background: white; border-radius: 20px; box-shadow: 0 10px 30px rgba(0,0,0,0.2);">
                                    <!-- Header -->
                                    <tr>
                                        <td style="padding: 40px 40px 20px; text-align: center;">
                                            <h1 style="margin: 0; color: #ea9d66ff; font-size: 32px; font-weight: 700;">
                                                Hotel Management
                                            </h1>
                                            <p style="margin: 10px 0 0; color: #d4ddf0ff; font-size: 16px;">Email Verification</p>
                                        </td>
                                    </tr>

                                    <!-- Content -->
                                    <tr>
                                        <td style="padding: 20px 40px;">
                                            <p style="margin: 0 0 20px; color: #e9ebeeff; font-size: 16px; line-height: 1.6;">
                                                Xin chào <strong>%s</strong>,
                                            </p>
                                            <p style="margin: 0 0 30px; color: #d4ddf0ff; font-size: 15px; line-height: 1.6;">
                                                Cảm ơn bạn đã đăng ký tài khoản. Để hoàn tất quá trình đăng ký, vui lòng nhập mã OTP bên dưới:
                                            </p>

                                            <!-- OTP Box -->
                                            <table width="100%%" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td align="center" style="padding: 30px 0;">
                                                        <div style="display: inline-block; background: linear-gradient(135deg, #ea9d66ff 0%%, #e1ddb5ff 100%%); padding: 25px 60px; border-radius: 15px; box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);">
                                                            <span style="color: white; font-size: 42px; font-weight: 700; letter-spacing: 8px; font-family: 'Courier New', monospace;">
                                                                %s
                                                            </span>
                                                        </div>
                                                    </td>
                                                </tr>
                                            </table>

                                            <!-- Warning Box -->
                                            <table width="100%%" cellpadding="0" cellspacing="0" style="background: #fef3c7; border-left: 4px solid #f59e0b; border-radius: 8px; margin: 20px 0;">
                                                <tr>
                                                    <td style="padding: 15px 20px;">
                                                        <p style="margin: 0; color: #db9266ff; font-size: 14px;">
                                                             Mã OTP này sẽ hết hạn sau <strong>%d phút</strong>
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>

                                            <p style="margin: 20px 0 0; color: #d4ddf0ff; font-size: 14px; line-height: 1.6;">
                                                Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này.
                                            </p>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="padding: 30px 40px; background: #f9fafb; border-radius: 0 0 20px 20px;">
                                            <p style="margin: 0; color: #e4ebf7ff; font-size: 13px; text-align: center;">
                                                © 2024 Hotel Management System. All rights reserved.
                                            </p>
                                            <p style="margin: 10px 0 0; color: #e4ebf7ff; font-size: 12px; text-align: center;">
                                                Email này được gửi tự động, vui lòng không trả lời.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .formatted(fullName, otp, expirationMinutes);
    }

    /**
     * Build HTML content for welcome email
     */
    private String buildWelcomeEmailHtml(String fullName) {
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #ea9d66ff 0%%, #e1ddb5ff 100%%);">
                    <table width="100%%" cellpadding="0" cellspacing="0">
                        <tr>
                            <td align="center" style="padding: 40px 20px;">
                                <table width="600" cellpadding="0" cellspacing="0" style="background: white; border-radius: 20px; box-shadow: 0 10px 30px rgba(0,0,0,0.2);">
                                    <tr>
                                        <td style="padding: 40px; text-align: center;">
                                            <h1 style="margin: 0 0 20px; color: #ea9d66ff; font-size: 36px;">🎉</h1>
                                            <h2 style="margin: 0 0 20px; color: #c4cddbff; font-size: 28px;">Chào mừng, %s!</h2>
                                            <p style="margin: 0 0 30px; color: #c4cddbff; font-size: 16px; line-height: 1.6;">
                                                Email của bạn đã được xác thực thành công. Bây giờ bạn có thể trải nghiệm đầy đủ các tính năng của hệ thống quản lý khách sạn.
                                            </p>
                                            <a href="http://localhost:5173/login" style="display: inline-block; background: linear-gradient(135deg, #ea9d66ff 0%%, #e1ddb5ff 100%%); color: white; text-decoration: none; padding: 15px 40px; border-radius: 10px; font-size: 16px; font-weight: 600;">
                                                Đăng nhập ngay
                                            </a>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 30px; background: #f9fafb; border-radius: 0 0 20px 20px; text-align: center;">
                                            <p style="margin: 0; color: #f1cd97ff; font-size: 13px;">
                                                © 2024 Hotel Management System
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .formatted(fullName);
    }
}
