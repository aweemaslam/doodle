package com.doodle.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Doodle High-Performance Scheduling API")
                        .version("1.0.0")
                        .description("Distributed event-driven scheduling platform capable of handling extreme concurrency. " +
                                     "Booking operations utilize an ultra-low latency Redis Lua engine backed by an asynchronous Kafka outbox relay to guarantee consistency across global IANA timezones.")
                        .contact(new Contact()
                                .name("Engineering Architecture Team")
                                .email("architecture@doodle.com")));
    }
}