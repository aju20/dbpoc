package com.safire;

import com.safire.services.PerformanceEvaluator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class SafireApplication {
    public static void main (String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SafireApplication.class, args);
        context.getBean(PerformanceEvaluator.class).evaluatePerformanceWithExistingData();
    }
}
