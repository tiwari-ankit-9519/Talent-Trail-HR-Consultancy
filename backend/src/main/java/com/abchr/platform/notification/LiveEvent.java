package com.abchr.platform.notification;

import java.util.List;
import java.util.UUID;

public record LiveEvent(List<UUID> userIds, LivePayload payload) {
}
