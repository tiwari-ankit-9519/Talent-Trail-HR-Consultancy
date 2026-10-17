package com.abchr.platform.shared.outbox;

import com.abchr.platform.shared.config.AppProperties;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange eventsExchange(AppProperties properties) {
        return ExchangeBuilder.topicExchange(properties.outbox().exchange()).durable(true).build();
    }
}
