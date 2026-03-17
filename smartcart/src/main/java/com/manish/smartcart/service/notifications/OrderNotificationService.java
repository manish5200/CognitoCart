package com.manish.smartcart.service.notifications;

import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.service.EmailService;
import com.manish.smartcart.service.email.EmailTemplateBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderNotificationService {

    private final EmailService emailService;
    private final EmailTemplateBuilder emailTemplateBuilder;

    public OrderNotificationService(EmailService emailService, EmailTemplateBuilder emailTemplateBuilder) {
        this.emailService = emailService;
        this.emailTemplateBuilder = emailTemplateBuilder;
    }

    // 1️⃣ Send Order Confirmation Email
    public void sendEmailNotification(OrderResponse orderResponse) {
        try {
            String body = emailTemplateBuilder.buildOrderConfirmation(orderResponse);
            String subject = "✅ Order Confirmed! Order #" + orderResponse.getOrderId();
            emailService.sendMail(orderResponse.getEmail(), subject, body, "CognitoCart");
        } catch (Exception e) {
            log.warn("Error in sending order confirmation message {}", e.getMessage());
        }
    }

    // 2️⃣ Send Order Status Update Email
    public void sendStatusUpdateEmail(OrderResponse orderResponse) {
        try {
            String body = emailTemplateBuilder.buildOrderStatusUpdate(orderResponse);
            String subject = "📦 Order Update: #" + orderResponse.getOrderId() + " is now " + orderResponse.getStatus();
            emailService.sendMail(orderResponse.getEmail(), subject, body, "CognitoCart");
        } catch (Exception e) {
            log.warn("Error in sending order status update message {}", e.getMessage());
        }
    }

    // 3️⃣ Send Refund Confirmation Email
    public void sendRefundEmail(OrderResponse orderResponse, String refundId) {
        try {
            String body = emailTemplateBuilder.buildRefundEmail(orderResponse, refundId);
            String subject = "💰 Refund Processed: Order #" + orderResponse.getOrderId();
            emailService.sendMail(orderResponse.getEmail(), subject, body, "CognitoCart");
        } catch (Exception e) {
            log.warn("Error in sending refund email {}", e.getMessage());
        }
    }
}
