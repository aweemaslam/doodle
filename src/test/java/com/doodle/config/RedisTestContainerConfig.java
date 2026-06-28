package com.doodle.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

public class RedisTestContainerConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    static {
        redis.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {

        MapPropertySource redisProps = new MapPropertySource(
                "redisTestProps",
                Map.of(
                        "spring.data.redis.host", redis.getHost(),
                        "spring.data.redis.port", redis.getMappedPort(6379)
                )
        );

        context.getEnvironment().getPropertySources().addFirst(redisProps);
    }
}