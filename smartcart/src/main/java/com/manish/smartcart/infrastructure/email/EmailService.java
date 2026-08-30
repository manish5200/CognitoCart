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

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from-email}")
    private String fromEmail;

    public EmailService() {
        // Initialize Spring's modern RestClient for the Resend API
        this.restClient = RestClient.create("https://api.resend.com");
    }

    /**
     * Sends an HTML email via Resend's REST API.
     * Bypasses SMTP port blocking entirely.
     */
    @Async
    public void sendMail(String to, String subject, String body, String senderName) {
        try {
            // Construct the JSON payload requested by Resend
            Map<String, Object> payload = Map.of(
                    "from", fromEmail,
                    "to", List.of(to),
                    "subject", subject,
                    "html", body
            );

            // Execute the POST request
            restClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Email sent to {} via Resend REST API", to);
        } catch (Exception e) {
            log.error("Failed to send email to {} via Resend API: {}", to, e.getMessage());
        }
    }

    /**
     * Sends an HTML email WITH a PDF attachment via Resend's REST API.
     * Attachments must be Base64 encoded strings in the JSON payload.
     *
     * @param attachmentBytes Raw bytes of the PDF (from InvoiceService)
     * @param attachmentName  Filename shown in inbox e.g., "CognitoCart-Invoice-105.pdf"
     */
    @Async
    public void sendMailWithAttachment(String to, String subject, String body,
                                       String senderName, byte[] attachmentBytes,
                                       String attachmentName) {
        try {
            // Convert binary PDF bytes to a Base64 string for the JSON payload
            String base64Content = Base64.getEncoder().encodeToString(attachmentBytes);
            
            // Build the attachment map
            Map<String, Object> attachment = Map.of(
                    "filename", attachmentName,
                    "content", base64Content
            );

            // Build the full payload with the attachment array
            Map<String, Object> payload = Map.of(
                    "from", fromEmail,
                    "to", List.of(to),
                    "subject", subject,
                    "html", body,
                    "attachments", List.of(attachment)
            );

            // Execute the POST request
            restClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Email with attachment '{}' sent to {} via Resend REST API", attachmentName, to);
        } catch (Exception e) {
            log.error("Failed to send email with attachment to {} via Resend API: {}", to, e.getMessage());
        }
    }
}
