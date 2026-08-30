package com.manish.smartcart.infrastructure.email;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendMail(String to, String subject, String body, String senderName) throws Exception {
        try {
            // Create mime message instead of simple message
            // MIME = Multipurpose Internet Mail Extensions
            MimeMessage email = mailSender.createMimeMessage();

            // Use the MimeMessageHelper to build the message
            // The 'true' argument enables multipart messages (for attachments, etc.)
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(email, true);

            // Correct format: "CognitoCart <my-email@gmail.com>"
            mimeMessageHelper.setFrom(fromEmail, senderName);

            mimeMessageHelper.setTo(to);
            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setText(body, true);// true = HTML
            mailSender.send(email);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.warn("Failed to send email to {} (this is expected if SMTP port is blocked by Render): {}", to, e.getMessage());
        }
    }

    /**
     * Sends an HTML email WITH a PDF attachment.
     * CONCEPT: MimeMessageHelper(true) = multipart mode.
     * Multipart = one email can carry both HTML body + binary file attachment
     * together.
     * Same pattern as Amazon invoices, Flipkart receipts, bank e-statements.
     *
     * @param attachmentBytes Raw bytes of the PDF (from InvoiceService)
     * @param attachmentName  Filename shown in inbox e.g.
     *                        "CognitoCart-Invoice-105.pdf"
     */
    @Async
    public void sendMailWithAttachment(String to, String subject, String body,
            String senderName, byte[] attachmentBytes,
            String attachmentName) throws Exception {
        try {
            MimeMessage email = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(email, true); // true = multipart

            helper.setFrom(fromEmail, senderName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true = HTML

            // Wrap raw PDF bytes as a Spring Resource → attach with MIME type
            // "application/pdf"
            helper.addAttachment(attachmentName,
                    new org.springframework.core.io.ByteArrayResource(attachmentBytes),
                    "application/pdf");

            mailSender.send(email);
            log.info("Email with attachment '{}' sent to {}", attachmentName, to);

        } catch (Exception e) {
            log.warn("Failed to send email with attachment to {} (expected if SMTP blocked): {}", to, e.getMessage());
        }
    }
}
