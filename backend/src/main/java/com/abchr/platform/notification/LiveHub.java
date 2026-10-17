package com.abchr.platform.notification;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

@Component
public class LiveHub {

    private static final Logger LOG = LogManager.getLogger(LiveHub.class);
    private static final long TIMEOUT_MS = 30L * 60 * 1000;

    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public LiveHub(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter register(UUID userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable cleanup = () -> remove(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> {
            cleanup.run();
            emitter.complete();
        });
        emitter.onError(error -> cleanup.run());
        try {
            emitter.send(SseEmitter.event().name("ready").data("ok"));
        } catch (IOException | RuntimeException ex) {
            cleanup.run();
        }
        return emitter;
    }

    public void dispatch(LiveEvent event) {
        String json = objectMapper.writeValueAsString(event.payload());
        for (UUID userId : event.userIds()) {
            List<SseEmitter> list = emitters.get(userId);
            if (list == null) {
                continue;
            }
            for (SseEmitter emitter : list) {
                send(userId, emitter, SseEmitter.event().name("live").data(json));
            }
        }
    }

    @Scheduled(fixedDelay = 25000)
    public void heartbeat() {
        emitters.forEach((userId, list) -> {
            for (SseEmitter emitter : list) {
                send(userId, emitter, SseEmitter.event().comment("ping"));
            }
        });
    }

    public int connections() {
        return emitters.values().stream().mapToInt(List::size).sum();
    }

    private void send(UUID userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | RuntimeException ex) {
            remove(userId, emitter);
            try {
                emitter.complete();
            } catch (RuntimeException ignored) {
                LOG.debug("Emitter already closed");
            }
        }
    }

    private void remove(UUID userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null) {
            return;
        }
        list.remove(emitter);
        if (list.isEmpty()) {
            emitters.remove(userId, list);
        }
    }
}
