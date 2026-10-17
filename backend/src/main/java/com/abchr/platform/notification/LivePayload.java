package com.abchr.platform.notification;

import java.time.Instant;
import java.util.UUID;

public record LivePayload(String topic, UUID id, String title, String body, String link, Instant createdAt) {
}
