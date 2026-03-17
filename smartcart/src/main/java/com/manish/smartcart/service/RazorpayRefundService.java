package com.manish.smartcart.service;

import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class RazorpayRefundService {

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    /**
     * Issues a full refund for a captured Razorpay payment.
     *
     * SDK 1.4.x requires a JSONObject param even for full refunds — can't pass zero args.
     * We pass "speed: normal" (RBI-compliant standard timeline, 5-7 business days).
     * We do NOT pass "amount" — Razorpay automatically refunds the full captured amount.
     * This avoids any paise rounding or mismatch bugs entirely.
     *
     * @param paymentId Razorpay payment ID from the Order (e.g., pay_XXXXX)
     * @param amount    Kept for logging only — NOT sent to Razorpay
     * @return Razorpay Refund ID (e.g., rfnd_XXXXX) — used in the email & for audit
     */
    public String initiateFullRefund(String paymentId, BigDecimal amount) {
        log.info("Initiating full refund for Payment ID: {} | Amount: Rs {}", paymentId, amount);

        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            // Must pass a JSONObject to satisfy SDK 1.4.x method signature.
            // We pass an EMPTY object — no "speed" (UPI doesn't support it),
            // no "amount" (Razorpay automatically refunds the full captured amount).
            JSONObject refundRequest = new JSONObject();

            Refund refund = razorpayClient.payments.refund(paymentId, refundRequest);
            String refundId = refund.get("id");

            log.info("Refund successful! Razorpay Refund ID: {} for Payment ID: {}", refundId, paymentId);
            return refundId;

        } catch (Exception e) {
            log.error("Refund failed for Payment ID {}: {}", paymentId, e.getMessage());
            throw new RuntimeException("Refund processing failed. Please contact support.");
        }
    }
}
