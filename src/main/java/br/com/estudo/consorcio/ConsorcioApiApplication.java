package br.com.estudo.consorcio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ConsorcioApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConsorcioApiApplication.class, args);
	}

}
