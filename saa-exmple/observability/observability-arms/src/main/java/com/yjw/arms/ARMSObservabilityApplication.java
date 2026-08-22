
package com.yjw.arms;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ARMSObservabilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(ARMSObservabilityApplication.class, args);
    }

    @Bean
    public OpenTelemetry openTelemetry() {
        // suppress the initialization of OpenTelemetry SDK in micrometer
        return GlobalOpenTelemetry.get();
    }

}

