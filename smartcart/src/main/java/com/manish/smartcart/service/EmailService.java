package com.manish.smartcart.service;


import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
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


    @Async
    public void sendEmail(String to, String subject, String body) {
        log.info("Sending email to {}", to);
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(to);
        mail.setSubject(subject);
        mail.setText(body);
        mailSender.send(mail);
        log.info("Email sent successfully to {}", to);
    }

    @Value("${spring.mail.username}")
    private String fromEmail;
    @Async
    public void sendMail(String to ,String subject,String body,String senderName) throws Exception {
        try {
            //Create mime message instead of simple message
            //MIME = Multipurpose Internet Mail Extensions
            MimeMessage email = mailSender.createMimeMessage();

            //Use the MimeMessageHelper to build the message
            // The 'true' argument enables multipart messages (for attachments, etc.)
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(email, true);

            // Correct format: "CognitoCart <my-email@gmail.com>"
            mimeMessageHelper.setFrom(fromEmail, senderName);

            mimeMessageHelper.setTo(to);
            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setText(body,true);// true = HTML
            mailSender.send(email);
            log.info("Email sent to {}", to);
        }catch (Exception e) {
            log.error("Failed to send mail", e);
            throw new Exception("Exception in sending mail to " + to);
        }
    }
}
