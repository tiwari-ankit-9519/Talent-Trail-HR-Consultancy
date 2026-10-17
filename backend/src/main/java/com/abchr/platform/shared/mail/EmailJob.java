package com.abchr.platform.shared.mail;

import java.util.List;

public record EmailJob(String kind, String id, String actor, List<String> args, String requestId) {
}
