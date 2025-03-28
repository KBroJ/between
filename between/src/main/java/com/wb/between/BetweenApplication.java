package com.wb.between;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class BetweenApplication {

	public static void main(String[] args) {
		SpringApplication.run(BetweenApplication.class, args);
	}

}
