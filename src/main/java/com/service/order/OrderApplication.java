package com.service.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;	
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.service.order")
@EnableScheduling
@EnableRabbit
public class OrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderApplication.class, args);
	}

	@Bean
	CommandLineRunner forceQueue(RabbitTemplate rabbitTemplate) {
		return args -> {
			System.out.println("🚀 FORZANDO CREACIÓN DE COLA");

			rabbitTemplate.execute(channel -> {
				channel.queueDeclare("payment.result.queue", true, false, false, null);
				return null;
			});
		};
	}

}
