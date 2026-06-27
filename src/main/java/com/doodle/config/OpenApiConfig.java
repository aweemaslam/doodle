package com.doodle.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Contact apiContact = new Contact()
                .name("Engineering Architecture Team")
                .email("architecture@doodle.com");

        Info apiInfo = new Info()
                .title("Doodle High-Performance Scheduling API")
                .version("1.0.0")
                .description("Distributed event-driven scheduling platform built for extreme concurrency. " +
                        "Utilizes an ultra-low latency Redis operational state engine backed by an asynchronous " +
                        "Kafka Transactional Outbox pattern to guarantee chronological consistency across global time zones.")
                .contact(apiContact);

        return new OpenAPI().info(apiInfo);
    }
}