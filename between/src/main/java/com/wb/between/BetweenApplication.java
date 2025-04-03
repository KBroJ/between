package com.wb.between;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
@ComponentScan(basePackages = {"com.wb.between", "com.another.package"}) // Controller가 있는 패키지 포함
public class BetweenApplication {

	public static void main(String[] args) {
		SpringApplication.run(BetweenApplication.class, args);
	}

}
