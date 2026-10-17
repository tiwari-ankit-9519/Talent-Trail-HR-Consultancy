package com.abchr.platform.shared.mail;

import java.util.List;
import java.util.UUID;

public interface EmailHandler {

    String kind();

    void handle(UUID id, List<String> args);

    void fail(UUID id, List<String> args, String reason);
}
