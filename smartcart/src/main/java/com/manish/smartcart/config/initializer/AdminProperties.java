package com.manish.smartcart.config.initializer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "admin")
public class AdminProperties {
    private String email;
    private String password;
    private String name;
    private String phone;
}
