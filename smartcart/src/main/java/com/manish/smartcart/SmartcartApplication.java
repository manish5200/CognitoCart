package com.manish.smartcart;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

@Slf4j
@SpringBootApplication
@EnableRetry // <--- Enable the retry engine
@EnableScheduling
@EnableJpaAuditing
@EnableAsync // This allows @Async methods to run in a separate thread
public class SmartcartApplication {

	public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SmartcartApplication.class, args);
        ConfigurableEnvironment appEnv = context.getEnvironment();
        log.info("Current Spring Boot Environment : {}", appEnv.getActiveProfiles()[0]);
    }

    @Bean
    public CommandLineRunner flywayChecker(Flyway flyway) {
        return args -> {
            System.out.println("🚀 FLYWAY DEBUGGER STARTING...");
            System.out.println("📍 Locations searched: " + Arrays.toString(flyway.getConfiguration().getLocations()));
            flyway.migrate();
            System.out.println("✅ Flyway migration finished successfully!");
        };
    }
}
