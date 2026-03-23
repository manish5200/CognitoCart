package com.manish.smartcart.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {
    // This file acts as the primary toggle switch for the entire Application's background Batch threads
}
