package com.abchr.platform.notification;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiveConfig {

    public static final String EXCHANGE = "abchr.live";

    @Bean
    public FanoutExchange liveExchange() {
        return new FanoutExchange(EXCHANGE, false, false);
    }

    @Bean
    public Queue liveQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Binding liveBinding(Queue liveQueue, FanoutExchange liveExchange) {
        return BindingBuilder.bind(liveQueue).to(liveExchange);
    }
}
