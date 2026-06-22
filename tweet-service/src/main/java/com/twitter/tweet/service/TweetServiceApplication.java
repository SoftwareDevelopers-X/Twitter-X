package com.twitter.tweet.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.core.env.Environment;
import org.springframework.boot.CommandLineRunner;
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableKafka
public class TweetServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TweetServiceApplication.class, args);
	}

	@Bean
	CommandLineRunner test(Environment env) {
		return args -> {
			System.out.println("URL      = " + env.getProperty("spring.datasource.url"));
			System.out.println("USERNAME = " + env.getProperty("spring.datasource.username"));
			System.out.println("PASSWORD = " + env.getProperty("spring.datasource.password"));
		};
	}

}
