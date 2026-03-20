package com.manish.smartcart.service;

import com.manish.smartcart.enums.DlqStatus;
import com.manish.smartcart.model.admin.FailedWebhookEvent;
import com.manish.smartcart.repository.FailedWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDlqService {

    private final FailedWebhookEventRepository dlqRepository;
    /**
     * MUST run in its own fresh transaction (REQUIRES_NEW).
     * If the parent webhook transaction failed and rolled back (e.g. ConstraintViolation),
     * we do NOT want this save operation to roll back with it!
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedWebhook(String payload, String signature,
                                  String eventType, String errorMessage){
        try{
            FailedWebhookEvent event = FailedWebhookEvent.builder()
                    .payload(payload)
                    .signature(signature)
                    .eventType(eventType)
                    .errorMessage(errorMessage)
                    .status(DlqStatus.PENDING)
                    .build();

            dlqRepository.save(event);
            log.warn("🚨 Saved failed webhook to DLQ (Time: {})", event.getCreatedAt());
        }catch(Exception e){
            // If the database itself is dead, this will fail. We must at least log the raw payload
            // to standard output so DevOps can recover it from DataDog/CloudWatch logs.
            log.error("CRITICAL: Failed to save to DLQ. Raw Payload: {}", payload, e);
        }
    }

    /**
     * Finds all webhooks that are currently broken and waiting for Admin review
     */
    public java.util.List<FailedWebhookEvent> getPendingFailures() {
        return dlqRepository.findByStatusOrderByCreatedAtDesc(DlqStatus.PENDING);
    }

    /**
     * The Magic Method: The Admin clicks "Retry" and this acts like Razorpay
     * shooting the exact same JSON payload back at our server.
     */
    @Transactional
    public String replayFailedWebhook(Long eventId){
        FailedWebhookEvent event = dlqRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("DLQ Event Not Found"));

        if(event.getStatus() == DlqStatus.RESOLVED){
            return "This event was already resolved successfully!";
        }

        try{
            // 1. Pretend to be Razorpay and make a real HTTP request to our own server
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/v1/payments/webhook"))
                    .header("Content-Type", "application/json")
                    // Inject the exact cryptographic signature we saved when it failed
                    .header("X-Razorpay-Signature", event.getSignature())
                    // Inject the exact original JSON payload
                    .POST(HttpRequest.BodyPublishers.ofString(event.getPayload()))
                    .build();

            // 2. Fire the webhook!
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            // 3. Did it work this time?
            if(response.statusCode() == 200){
                event.setStatus(DlqStatus.RESOLVED);
                dlqRepository.save(event);
                log.info("✅ DLQ Event {} successfully replayed and resolved!", eventId);
                return "Successfully replayed and resolved the webhook!";
            }else{
                return "Replay failed again. Status Code: "
                        + response.statusCode() + ". Body: " + response.body();
            }
        } catch (Exception e) {
            log.error("Failed to execute HTTP replay for DLQ Event {}", eventId, e);
            throw new RuntimeException("Critical failure while replaying webhook: " + e.getMessage());
        }
    }
}
