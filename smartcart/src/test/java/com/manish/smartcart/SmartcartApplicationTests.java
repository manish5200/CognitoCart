package com.manish.smartcart;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.junit.jupiter.api.Disabled;

@SpringBootTest
@Disabled("Fails in CI environments without a database")
class SmartcartApplicationTests {

	@Test
	void contextLoads() {
	}

}
