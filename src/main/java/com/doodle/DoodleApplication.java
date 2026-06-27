package com.doodle;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT2M")
public class DoodleApplication {

	static void main(String[] args) {
		SpringApplication.run(DoodleApplication.class, args);
	}
}
