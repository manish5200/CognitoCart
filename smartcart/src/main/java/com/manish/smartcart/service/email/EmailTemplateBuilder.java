package com.manish.smartcart.service.email;

import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.model.user.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Builds HTML email content by processing Thymeleaf templates.
 */
@Component
@RequiredArgsConstructor
public class EmailTemplateBuilder {

    private final TemplateEngine templateEngine;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // Used in security notification emails — more human-readable
    private static final DateTimeFormatter SECURITY_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a 'IST'");

    // ─────────────────────────────────────────────────────────────────────────

    /** Builds the Welcome Email HTML for new user registrations. */
    public String buildWelcomeEmail(Users user) {
        Context context = new Context();
        context.setVariable("name", user.getFullName());
        context.setVariable("email", user.getEmail());
        return templateEngine.process("emails/welcome-email", context);
    }

    /** Builds the Order Confirmation HTML after successful checkout. */
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

    /** Builds the Order Status Update HTML when admin changes order state. */
    public String buildOrderStatusUpdate(OrderResponse order) {
        Context context = new Context();
        context.setVariable("customerName", order.getCustomerName());
        context.setVariable("orderId", order.getOrderId());
        context.setVariable("status", order.getStatus().name());

        String statusMessage = switch (order.getStatus()) {
            case CONFIRMED         -> "Great news! Your order has been confirmed and is being prepared.";
            case PACKED            -> "Your order has been packed and is ready to be picked up by the courier.";
            case SHIPPED           -> "Great news! Your order has been shipped and is on its way to you.";
            case OUT_FOR_DELIVERY  -> "Your order is out for delivery today. Keep an eye out!";
            case DELIVERED         -> "Your order has been delivered. We hope you love your purchase! 🎉";
            case CANCELLED         -> "Your order has been cancelled. If you have questions, please contact support.";
            case RETURN_REQUESTED  -> "We have received your return request and are processing it.";
            case RETURNED          -> "Your returned item has been received at our warehouse.";
            case REFUNDED          -> "Your refund has been issued successfully. It will reflect in 5-7 business days.";
            default                -> "The status of your order has been updated. Visit the app for details.";
        };
        context.setVariable("statusMessage", statusMessage);
        return templateEngine.process("emails/order-status", context);
    }

    /** Builds the Seller KYC Status HTML (approved / rejected). */
    public String buildSellerKycStatus(String sellerName, boolean isApproved, String comments) {
        Context context = new Context();
        context.setVariable("sellerName", sellerName);
        context.setVariable("isApproved", isApproved);
        context.setVariable("statusWord", isApproved ? "Approved" : "Rejected");
        context.setVariable("comments", comments != null ? comments : "No additional comments provided.");
        return templateEngine.process("emails/seller-kyc", context);
    }

    /**
     * Builds the password reset REQUEST email — the link the user clicks to choose a new password.
     * Token is embedded in the reset URL and stored in Redis with a 15-min TTL.
     */
    public String buildPasswordResetEmail(Users user, String token) {
        Context context = new Context();
        context.setVariable("name", user.getFullName());
        context.setVariable("email", user.getEmail()); // shown at bottom: "you're receiving this because..."
        // Full reset link — update base URL when deploying to production
        context.setVariable("resetLink",
                "https://cognitocart.com/reset-password?token=" + token);
        return templateEngine.process("emails/password-reset", context);
    }

    /**
     * Builds the password CHANGED security notification email.
     * Sent immediately after a successful reset — if it wasn't them, they can react immediately.
     * Real-world pattern: Google, GitHub, Amazon all send this.
     */
    public String buildPasswordChangedEmail(Users user) {
        Context context = new Context();
        context.setVariable("name", user.getFullName());
        context.setVariable("email", user.getEmail());
        // Human-readable timestamp: "March 13, 2026, at 10:30 AM IST"
        context.setVariable("changedAt", LocalDateTime.now().format(SECURITY_FORMATTER));
        return templateEngine.process("emails/password-changed", context);
    }

    /** Builds the email verification OTP email. */
    public String buildEmailVerificationEmail(Users user, String otp) {
        Context context = new Context();
        context.setVariable("name", user.getFullName());
        context.setVariable("otp", otp);  // The 6-digit code displayed in the email
        return templateEngine.process("emails/email-verification", context);
    }

    /** Builds the Refund Notification email for canceled orders. */
    public String buildRefundEmail(OrderResponse order, String refundId) {
        Context context = new Context();
        context.setVariable("customerName", order.getCustomerName());
        context.setVariable("orderId", order.getOrderId());
        context.setVariable("refundAmount", order.getTotalAmount()); // Full refund amount
        context.setVariable("refundId", refundId); // Inject the refund transaction ID
        return templateEngine.process("emails/refund-processed", context);
    }

    /** Builds the Cart Abandonment Recovery HTML */
    public String buildCartAbandonmentEmail(String customerName, int itemCount, String checkoutUrl){
        Context context = new Context();
        context.setVariable("name", customerName);
        context.setVariable("itemCount", itemCount);
        context.setVariable("checkoutUrl", checkoutUrl);
        return templateEngine.process("emails/cart-abandonment", context);
    }

    /** Builds the Wishlist FOMO Price Drop HTML */
    public String buildWishlistSaleEmail(String customerName, java.util.List<com.manish.smartcart.model.product.Product> products, String frontendUrl){
        Context context = new Context();
        context.setVariable("name", customerName);
        context.setVariable("products", products);
        context.setVariable("frontendUrl", frontendUrl);
        return templateEngine.process("emails/wishlist-sale", context);
    }

    /**
     * Builds the delivery confirmation email — reuses the existing order-status template
     * and passes showReviewCta=true to render the "Rate your purchase" button.

     * CONCEPT — Template reuse:
     * No need for a brand-new HTML file. The existing emails/order-status.html
     * already renders a beautiful green DELIVERED banner.
     * We just pass extra context variables to conditionally show the review CTA.
     */
    public String buildDeliveryConfirmationEmail(OrderResponse order) {
        Context context = new Context();
        context.setVariable("customerName", order.getCustomerName());
        context.setVariable("orderId", order.getOrderId());
        context.setVariable("status", order.getStatus().name()); // "DELIVERED" → green banner ✅
        context.setVariable("statusMessage",
                "Your order has been delivered! We hope you love your purchase. 🎉 " +
                        "Leave a review and help other shoppers.");
        // Tells order-status.html to render the green "Rate your purchase" button
        context.setVariable("showReviewCta", true);
        return templateEngine.process("emails/order-status", context);
    }

}

