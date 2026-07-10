package com.integrador.Turismo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TurismoApplication {

	public static void main(String[] args) {
		SpringApplication.run(TurismoApplication.class, args);
		System.out.println("HOLA MUNDO BACKEND");
	}

}
