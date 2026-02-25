package com.manish.smartcart;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableRetry
@EnableScheduling
@EnableJpaAuditing
@EnableAsync
public class SmartcartApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SmartcartApplication.class, args);
        ConfigurableEnvironment appEnv = context.getEnvironment();

        // Bug fix: getActiveProfiles() can return empty array if no profile is set
        String activeProfiles = appEnv.getActiveProfiles().length > 0
                ? String.join(", ", appEnv.getActiveProfiles())
                : "default";

        log.info("Active Spring Profile(s): {}", activeProfiles);
        log.info("🚀 CognitoCart application started successfully on port {}",
                appEnv.getProperty("server.port", "8080"));
    }
}
