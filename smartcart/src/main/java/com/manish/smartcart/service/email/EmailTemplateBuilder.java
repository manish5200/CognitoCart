package com.manish.smartcart.service.email;

import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.model.user.Users;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;

/**
 * Builds HTML email content by processing Thymeleaf templates.
 */
@Component
@RequiredArgsConstructor
public class EmailTemplateBuilder {

    private final TemplateEngine templateEngine;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    /**
     * Builds the Welcome Email HTML for new user registrations.
     */
    public String buildWelcomeEmail(Users user) {
        Context context = new Context();
        context.setVariable("name", user.getFullName());
        context.setVariable("email", user.getEmail());

        // We look for a template named "welcome-email.html" in
        // src/main/resources/templates/emails/
        return templateEngine.process("emails/welcome-email", context);
    }

    /**
     * Builds the Order Confirmation HTML after successful checkout.
     */
    public String buildOrderConfirmation(OrderResponse order) {
        Context context = new Context();
        context.setVariable("customerName", order.getCustomerName());
        context.setVariable("orderId", order.getOrderId());
        context.setVariable("orderDate", order.getOrderDate().format(DATE_FORMATTER));
        context.setVariable("status", order.getStatus().name());
        context.setVariable("shippingAddress", order.getShippingAddress());
        context.setVariable("items", order.getItems());
        context.setVariable("totalAmount", order.getTotalAmount());
        context.setVariable("couponCode", order.getCouponCode());
        context.setVariable("discountAmount", order.getDiscountAmount());

        return templateEngine.process("emails/order-confirmation", context);
    }

    /**
     * Builds the Order Status Update HTML for admins updating orders.
     */
    public String buildOrderStatusUpdate(OrderResponse order) {
        Context context = new Context();
        context.setVariable("customerName", order.getCustomerName());
        context.setVariable("orderId", order.getOrderId());
        context.setVariable("status", order.getStatus().name());

        // Add dynamic messaging based on status
        String statusMessage = switch (order.getStatus()) {
            case SHIPPED -> "Great news! Your order has been shipped and is on its way to you.";
            case OUT_FOR_DELIVERY -> "Your order is out for delivery today. Keep an eye out!";
            case DELIVERED -> "Your order has been delivered. Enjoy your purchase!";
            case CANCELLED -> "Your order has been cancelled. If you have questions, please contact support.";
            case RETURN_REQUESTED -> "We have received your return request and are processing it.";
            case REFUNDED -> "Your refund has been issued successfully.";
            default -> "The status of your order has been updated.";
        };
        context.setVariable("statusMessage", statusMessage);

        return templateEngine.process("emails/order-status", context);
    }

    /**
     * Builds the Seller KYC Status HTML.
     */
    public String buildSellerKycStatus(String sellerName, boolean isApproved, String comments) {
        Context context = new Context();
        context.setVariable("sellerName", sellerName);
        context.setVariable("isApproved", isApproved);
        context.setVariable("statusWord", isApproved ? "Approved" : "Rejected");
        context.setVariable("comments", comments != null ? comments : "No additional comments provided.");

        return templateEngine.process("emails/seller-kyc", context);
    }
}
