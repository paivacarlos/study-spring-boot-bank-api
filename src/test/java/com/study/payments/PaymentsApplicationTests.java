package com.study.payments;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("Smoke Test: Spring Boot Application Context")
class PaymentsApplicationTests {

	@Test
	@DisplayName("Should successfully load the Spring Boot application context and all managed beans")
	void contextLoads() {
	}

}
