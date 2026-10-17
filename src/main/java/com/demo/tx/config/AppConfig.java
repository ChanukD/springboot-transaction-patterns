package com.demo.tx.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Retryable (Spring Framework 7 built-in) and @Scheduled. */
@Configuration
@EnableResilientMethods
@EnableScheduling
public class AppConfig {
}
