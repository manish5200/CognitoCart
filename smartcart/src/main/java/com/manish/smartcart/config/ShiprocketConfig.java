package com.manish.smartcart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ShiprocketConfig {

    // RestClient = Spring 6's modern, fluent HTTP client (replaces RestTemplate)
    // We create a dedicated instance for Shiprocket with their base URL baked in.
    // Same concept as RazorpayConfig creates a dedicated RazorpayClient bean.
    @Bean(name = "shiprocketRestClient")
    public RestClient shiprocketRestClient(){
        return RestClient.builder()
                .baseUrl("https://apiv2.shiprocket.in/v1/external")
                .build();
    }
}
