package com.manish.smartcart.infrastructure.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    private final RestClient restClient;

    @Value("${brevo.api-key}")
    private String apiKey;

    @Value("${brevo.from-email}")
    private String fromEmail;

    @Value("${brevo.from-name:CognitoCart}")
    private String fromName;

    public EmailService(){
        this.restClient = RestClient.create("https://api.brevo.com");
    }

    /**
     * Sends async HTML email via Brevo HTTP API.
     * No SMTP port needed — pure HTTPS on port 443.
     */
    @Async
    public void sendMail(String to, String subject, String body, String senderName) {
        try {

            Map<String, Object> payload = Map.of(
                    "sender", Map.of("name", senderName, "email", fromEmail),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", body
            );

            restClient.post()
                    .uri("/v3/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Email sent to {} via Brevo", to);

        } catch (Exception e) {
            log.error("Failed to send email to {} via Brevo: {}", to, e.getMessage());
        }
    }

    /**
     * Sends async HTML email with PDF attachment via Brevo HTTP API.
     * Attachment must be Base64 encoded per Brevo spec.
     */
    @Async
    public void sendMailWithAttachment(String to, String subject, String body,
                                       String senderName, byte[] attachmentBytes,
                                       String attachmentName) {
        try {

            String base64Content =  Base64.getEncoder().encodeToString(attachmentBytes);

            Map<String, Object> payload = Map.of(
                    "sender", Map.of("name", senderName, "email", fromEmail),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", body,
                    "attachment", List.of(Map.of(
                            "name", attachmentName,
                            "content", base64Content
                    ))
            );

            restClient.post()
                    .uri("/v3/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Email with attachment '{}' sent to {} via Brevo", attachmentName, to);

        } catch (Exception e) {
            log.error("Failed to send email with attachment to {}: {}", to, e.getMessage());
        }
    }
}
