package com.abchr.platform.shared.mail;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailQueueConfig {

    public static final String QUEUE = "abchr.email";

    @Bean
    public Queue emailJobsQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding emailJobsBinding(Queue emailJobsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(emailJobsQueue).to(eventsExchange).with(EmailQueue.ROUTING_PREFIX + "#");
    }
}