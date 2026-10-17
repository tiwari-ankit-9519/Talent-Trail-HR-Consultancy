package com.abchr.platform.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false, updatable = false)
    UUID userId;

    @Column(name = "topic", nullable = false, updatable = false, length = 30)
    String topic;

    @Column(name = "title", nullable = false, updatable = false, length = 200)
    String title;

    @Column(name = "body", updatable = false, length = 500)
    String body;

    @Column(name = "link", updatable = false, length = 200)
    String link;

    @Column(name = "read_at")
    Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt = Instant.now();

    protected Notification() {
    }

    static Notification create(UUID userId, String topic, String title, String body, String link) {
        Notification notification = new Notification();
        notification.userId = userId;
        notification.topic = topic;
        notification.title = title;
        notification.body = body;
        notification.link = link;
        return notification;
    }
}
