package com.manish.smartcart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;


@SpringBootApplication
public class SmartcartApplication {

	public static void main(String[] args) {
        SpringApplication.run(SmartcartApplication.class, args);
	}
}
