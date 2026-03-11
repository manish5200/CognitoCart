package com.manish.smartcart.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        // Will throw RazorpayException if keys are invalid format, but with
        // placeholders
        // it shouldn't crash until an actual API call is made.
        return new RazorpayClient(keyId, keySecret);
    }
}
