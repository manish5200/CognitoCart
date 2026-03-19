package com.manish.smartcart.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    // CONCEPT: @Bean tells Spring: "run this method, take the returned object,
    // and register it in the ApplicationContext under type Cloudinary."
    // Any class annotated with @Service/@Component can now @Autowire it.
    @Bean
    public Cloudinary cloudinary() {
        // Cloudinary SDK accepts credentials via a Map
        Map<String, String> config = new HashMap<>();

        config.put("cloud_name", cloudName);  // Your Cloudinary account name

        config.put("api_key", apiKey);        // Public API key

        config.put("api_secret", apiSecret);  // Secret key (never expose to client!)

        config.put("secure", "true");         // Always return https:// URLs

        return new Cloudinary(config);
    }
}
