package com.hari.currencyconverter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@PropertySource("file:/var/personal_projects/currency_converter/application.properties")
public class CurrencyConverterApplication {

	public static void main(String[] args) {

		SpringApplication.run(CurrencyConverterApplication.class, args);
	}

}
