package com.manish.smartcart.service.notifications;

import com.manish.smartcart.dto.order.OrderResponse;
import com.manish.smartcart.model.order.Address;
import com.manish.smartcart.service.EmailService;
import com.manish.smartcart.util.AppConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class OrderNotificationService {

    private final EmailService emailService;
    private final String cachedTemplate; // Cache the HTML in memory
    public OrderNotificationService(EmailService emailService) {
        this.emailService = emailService;
        this.cachedTemplate = loadTemplate(); // Load once at startup
    }

    // 1️⃣ Send email
    public void sendEmailNotification(OrderResponse orderResponse){
        try{
            // Use the cachedTemplate instead of loading from disk
            String body = replacePlaceholders(this.cachedTemplate, orderResponse);
            String subject = "✅ Order Confirmed! Order #" + orderResponse.getOrderId();
            emailService.sendMail(orderResponse.getEmail(), subject, body,"CognitoCart");
        } catch (Exception e) {
            log.warn("Error in sending message {}",e.getMessage());
        }

    }

    // 2️⃣Load HTML file
    private String loadTemplate(){
        try{
            ClassPathResource resource = new ClassPathResource("templates/email/order-confirmation.html");
            return StreamUtils.copyToString(
                    resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("CRITICAL: Failed to load email template", e);
            return "";// Fallback
        }
    }

    // 3️⃣YOU manually set placeholders here
    private String replacePlaceholders(String template, OrderResponse orderResponse){
        return template
                .replace("{{CUSTOMER_NAME}}",orderResponse.getCustomerName())
                .replace("{{STORE_NAME}}", AppConstants.STORE_NAME)
                .replace("{{ORDER_ID}}",orderResponse.getOrderId().toString())
                .replace("{{ORDER_DATE}}",orderResponse.getOrderDate().toString())
                .replace("{{PAYMENT_METHOD}}","CASH BY DEFAULT - Will implement later")
                .replace("{{TOTAL_AMOUNT}}",orderResponse.getTotalAmount().toString())
                .replace("{{SHIPPING_ADDRESS}}",formatAddress(orderResponse.getShippingAddress()))
                .replace("{{SUPPORT_EMAIL}}","manishneelambar@gmail.com");
    }


    //Address Helper  -> took help of AI
    private String formatAddress(Address address) {
        return """
        <p>
            <b>%s</b><br>
            📞 %s<br>
            %s<br>
            %s, %s<br>
            %s - %s
        </p>
        """.formatted(
                address.getRecipientName(),
                address.getPhone(),
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getCountry(),
                address.getPostalCode()
        );
    }

}
