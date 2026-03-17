package com.manish.smartcart.service.notifications;

import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.service.EmailService;
import com.manish.smartcart.service.InvoiceService;
import com.manish.smartcart.service.email.EmailTemplateBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNotificationService {

    private final EmailService emailService;
    private final EmailTemplateBuilder emailTemplateBuilder;
    private final InvoiceService invoiceService;


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

    // 4️⃣ Send Invoice Email — HTML body + PDF attachment
    // InvoiceService generates the bytes → EmailService attaches it and sends
    @Async
    public void sendInvoiceEmail(OrderResponse order, byte[] invoicePdf){
        try{
            // Reuse the existing order-confirmation Thymeleaf template as the email body
            String body     = emailTemplateBuilder.buildOrderConfirmation(order);
            String subject  = "🧾 Your Invoice — Order #" + order.getOrderId() + " | CognitoCart";
            String filename = "CognitoCart-Invoice-" + order.getOrderId() + ".pdf";

            // Uses the new sendMailWithAttachment method we just added to EmailService
            emailService.sendMailWithAttachment(
                    order.getEmail(), subject, body, "CognitoCart Billing", invoicePdf, filename);

            log.info("Invoice email dispatched for Order #{}", order.getOrderId());


        } catch (Exception e) {
            log.warn("Failed to send invoice email for Order #{}: {}", order.getOrderId(), e.getMessage());
        }
    }
}
