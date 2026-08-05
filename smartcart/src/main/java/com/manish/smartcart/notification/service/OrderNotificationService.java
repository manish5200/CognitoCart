package com.manish.smartcart.notification.service;

import com.manish.smartcart.order.dto.OrderResponse;
import com.manish.smartcart.shared.enums.NotificationType;
import com.manish.smartcart.infrastructure.email.EmailService;
import com.manish.smartcart.infrastructure.invoice.InvoiceService;
import com.manish.smartcart.infrastructure.email.EmailTemplateBuilder;
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
    private final InAppNotificationService inAppNotificationService;
    private final SmsNotificationService smsNotificationService;


    // 1️⃣ Send Order Confirmation Email
    public void sendOrderConfirmationEmail(OrderResponse orderResponse) {
        try {
            String body = emailTemplateBuilder.buildOrderConfirmation(orderResponse);
            String subject = "✅ Order Confirmed! Order #" + orderResponse.getOrderPublicId();
            emailService.sendMail(orderResponse.getEmail(), subject, body, "CognitoCart");

            // In-App & SMS
            String msg = "Order #" + orderResponse.getOrderPublicId() + " confirmed successfully. Total: ₹" + orderResponse.getTotalAmount();
            inAppNotificationService.createNotification(
                    orderResponse.getCustomerPublicId(),
                    NotificationType.ORDER_STATUS_UPDATE,
                    "Order Confirmed",
                    msg);
            smsNotificationService.sendSms(orderResponse.getShippingPhone(), msg);

        } catch (Exception e) {
            log.warn("Failed to send confirmation notification for Order #{}: {}", orderResponse.getOrderPublicId(), e.getMessage());
        }
    }

    // 2️⃣ Send Order Status Update Email
    public void sendStatusUpdateEmail(OrderResponse orderResponse) {
        try {
            String body = emailTemplateBuilder.buildOrderStatusUpdate(orderResponse);
            String subject = "📦 Order Update: #" + orderResponse.getOrderPublicId() + " is now " + orderResponse.getStatus();
            emailService.sendMail(orderResponse.getEmail(), subject, body, "CognitoCart");

            // In-App & SMS
            String msg = "Your order #" + orderResponse.getOrderPublicId() + " status changed to " + orderResponse.getStatus();
            inAppNotificationService.createNotification(orderResponse.getCustomerPublicId(), NotificationType.ORDER_STATUS_UPDATE,"Order Update", msg);
            smsNotificationService.sendSms(orderResponse.getShippingPhone(), msg);

        } catch (Exception e) {
            log.warn("Failed to send status update notification for Order #{}: {}", orderResponse.getOrderPublicId(), e.getMessage());
        }
    }

    // 3️⃣ Send Refund Confirmation Email
    public void sendRefundEmail(OrderResponse orderResponse, String refundId) {
        try {
            String body = emailTemplateBuilder.buildRefundEmail(orderResponse, refundId);
            String subject = "💰 Refund Processed: Order #" + orderResponse.getOrderPublicId();
            emailService.sendMail(orderResponse.getEmail(), subject, body, "CognitoCart");

            // In-App & SMS
            String msg = "Refund of ₹" + orderResponse.getTotalAmount() + " initiated for Order #" + orderResponse.getOrderPublicId() + ". Ref ID: " + refundId;
            inAppNotificationService.createNotification(
                    orderResponse.getCustomerPublicId(),
                    NotificationType.REFUND_ISSUED,
                    "Refund Initiated",
                    msg);
            smsNotificationService.sendSms(orderResponse.getShippingPhone(), msg);

        } catch (Exception e) {
            log.warn("Failed to send refund notification for Order #{}: {}", orderResponse.getOrderPublicId(), e.getMessage());
        }
    }

    // 4️⃣ Send Invoice Email — HTML body + PDF attachment
    // InvoiceService generates the bytes → EmailService attaches it and sends
    @Async
    public void sendInvoiceEmail(OrderResponse order, byte[] invoicePdf){
        try{
            // Reuse the existing order-confirmation Thymeleaf template as the email body
            String body     = emailTemplateBuilder.buildOrderConfirmation(order);
            String subject  = "🧾 Your Invoice — Order #" + order.getOrderPublicId() + " | CognitoCart";
            String filename = "CognitoCart-Invoice-" + order.getOrderPublicId() + ".pdf";

            // Uses the new sendMailWithAttachment method we just added to EmailService
            emailService.sendMailWithAttachment(
                    order.getEmail(), subject, body, "CognitoCart Billing", invoicePdf, filename);

            log.info("Invoice email dispatched for Order #{}", order.getOrderPublicId());

            // In-App (No SMS for invoices)
            inAppNotificationService.createNotification(
                    order.getCustomerPublicId(),
                    NotificationType.SYSTEM_ALERT,
                    "Invoice Generated", "Your invoice for Order #" + order.getOrderPublicId() + " is ready.");
        } catch (Exception e) {
            log.warn("Failed to send invoice email for Order #{}: {}", order.getOrderPublicId(), e.getMessage());
        }
    }

    /**
     * // 5️⃣ Sends "Your order has arrived!" email when carrier marks package as DELIVERED.

     * CONCEPT — Why separate from sendStatusUpdateEmail()?
     * Delivery is the most important moment in the customer journey.
     * We use a dedicated template builder call that adds showReviewCta=true,
     * which renders the "⭐ Rate Your Purchase" button in the email.
     * This drives review submissions — critical for seller credibility.

     * @Async — runs on background thread, does not delay the carrier webhook response.
     */
    public void sendDeliveryConfirmationEmail(OrderResponse orderResponse) {
        try {
            String body = emailTemplateBuilder.buildDeliveryConfirmationEmail(orderResponse);
            String subject = "🎉 Delivered! Order #" + orderResponse.getOrderPublicId()
                    + " has arrived — how was it?";
            emailService.sendMail(orderResponse.getEmail(), subject, body, "CognitoCart");

            // In-App & SMS
            String msg = "Order #" + orderResponse.getOrderPublicId() + " has been delivered! Please rate your purchase.";
            inAppNotificationService.createNotification(orderResponse.getCustomerPublicId(), NotificationType.ORDER_STATUS_UPDATE, "Order Delivered", msg);
            smsNotificationService.sendSms(orderResponse.getShippingPhone(), msg);

            log.info("Delivery confirmation email sent for Order #{}", orderResponse.getOrderPublicId());

        } catch (Exception e) {
            log.warn("Failed to send delivery confirmation email for Order #{}: {}", orderResponse.getOrderPublicId(), e.getMessage());
        }
    }

    // 6️⃣ Send Return Rejection Email
    public void sendReturnRejectedEmail(OrderResponse orderResponse, String adminComment){
        try{
            String body = emailTemplateBuilder.buildReturnRejectedEmail(orderResponse, adminComment);
            String subject = "❌ Return Request Declined: Order #" + orderResponse.getOrderPublicId();
            emailService.sendMail(orderResponse.getEmail(), subject, body, "CognitoCart");

            // In-App & SMS
            String msg = "Your return request for Order #" + orderResponse.getOrderPublicId() + " was declined. Reason: " + adminComment;
            inAppNotificationService.createNotification(orderResponse.getCustomerPublicId(), NotificationType.RETURN_REJECTED, "Return Declined", msg);
            smsNotificationService.sendSms(orderResponse.getShippingPhone(), msg);

            log.info("Return rejection email sent to customer for Order #{}", orderResponse.getOrderPublicId());
        } catch (Exception e) {
            log.warn("Failed to send return rejection email for Order #{}: {}", orderResponse.getOrderPublicId(), e.getMessage());
        }
    }
}
