package com.manish.smartcart.service;

import com.manish.smartcart.model.order.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    /**
     * Talks to Razorpay APIs to create a physical Order ID on their end.
     * 
     * @param order the local Order entity
     * @return The Razorpay generated Order ID (e.g. order_IluGWxBm9U8zJ8)
     */
    public String createRazorpayOrder(Order order) {
        try {
            JSONObject orderRequest = new JSONObject();
            // Razorpay expects amount in paisa (smallest currency unit), so multiply by 100
            // Note: order.getTotalAmount() is ALREADY the final discounted amount from the
            // Cart!
            BigDecimal amountInPaisa = order.getTotalAmount().multiply(new BigDecimal("100"));

            orderRequest.put("amount", amountInPaisa.intValue());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + order.getId());

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            return razorpayOrder.get("id");

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay Order for Local Order ID {}", order.getId(), e);
            throw new RuntimeException("Payment Gateway Error: Unable to initiate payment.");
        }
    }

    /**
     * Cryptographically verifies the payment signature returned by the frontend.
     * Never trust the frontend's claim of "Success" without this check.
     */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            // Razorpay's Utils class abstracts the HMAC SHA256 hashing format:
            // generated_signature = hmac_sha256(order_id + "|" + razorpay_payment_id,
            // secret)
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            return Utils.verifyPaymentSignature(options, keySecret);
        } catch (RazorpayException e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }
}
