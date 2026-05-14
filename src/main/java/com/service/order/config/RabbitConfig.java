package com.service.order.config;

import org.springframework.amqp.core.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String QUEUE = "payment.result.queue";
    public static final String EXCHANGE = "payment.exchange";
    public static final String ROUTING_KEY = "payment.result";

    @Bean
    public Queue paymentResultQueue() {
        return new Queue(QUEUE, true); // durable
    }

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Binding paymentBinding(Queue paymentResultQueue, DirectExchange paymentExchange) {
        return BindingBuilder
                .bind(paymentResultQueue)
                .to(paymentExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public CommandLineRunner test() {
        return args -> {
            System.out.println("🔥 RabbitConfig SI se está ejecutando");
        };
    }
}