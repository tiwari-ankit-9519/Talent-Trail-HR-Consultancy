package com.abchr.platform.notification;

import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class LiveListener {

    private static final Logger LOG = LogManager.getLogger(LiveListener.class);

    private final LiveHub hub;
    private final ObjectMapper objectMapper;

    public LiveListener(LiveHub hub, ObjectMapper objectMapper) {
        this.hub = hub;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "#{liveQueue.name}")
    public void onMessage(Message message) {
        try {
            LiveEvent event = objectMapper.readValue(new String(message.getBody(), StandardCharsets.UTF_8),
                    LiveEvent.class);
            hub.dispatch(event);
        } catch (RuntimeException ex) {
            LOG.warn("Dropping unreadable live event: {}", ex.toString());
        }
    }
}
