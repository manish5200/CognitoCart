package com.manish.smartcart.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Registers a dedicated RestClient bean for all Delhivery API calls.
 * <p>
 * WHY separate bean: Delhivery uses different base URLs for staging vs prod.
 * Injecting this bean prevents hardcoding URLs across service classes.
 * Pattern mirrors RazorpayConfig — one bean, one responsibility.
 */
@Configuration
public class DelhiveryConfig {

    // Staging: https://staging-express.delhivery.com
    // Production: https://track.delhivery.com
    // Controlled via application-dev.yml — no code change needed on deploy.
    @Value("${delhivery.base-url}")
    private String baseUrl;

    @Bean(name = "delhiveryRestClient")
    public RestClient delhiveryRestClient(){
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
