package com.doodle;

import com.doodle.config.RedisTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = RedisTestContainerConfig.class)

class DoodleApplicationTests {

	@Test
	void contextLoads() {
	}
}
