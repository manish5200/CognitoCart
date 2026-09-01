package com.manish.smartcart.infrastructure.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    // Autoconfigured by spring-boot-starter-mail using spring.mail.* properties
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Sends an async HTML email via Gmail SMTP (port 587 / TLS).
     * senderName is used as the display name in the inbox.
     */
    @Async
    public void sendMail(String to, String subject, String body, String senderName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // false = not multipart (no attachments)
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromEmail, senderName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML

            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            // Log and swallow — email failure must never crash the main request
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Sends an async HTML email with a PDF attachment via Gmail SMTP.
     * Used for invoice delivery after order placement.
     */
    @Async
    public void sendMailWithAttachment(String to, String subject, String body,
                                       String senderName, byte[] attachmentBytes,
                                       String attachmentName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true = multipart (required for attachments)
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, senderName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML

            // Attach raw bytes with filename (e.g., "CognitoCart-Invoice-105.pdf")
            helper.addAttachment(attachmentName,
                    new org.springframework.core.io.ByteArrayResource(attachmentBytes));

            mailSender.send(message);
            log.info("Email with attachment '{}' sent to {}", attachmentName, to);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email with attachment to {}: {}", to, e.getMessage());
        }
    }
}
