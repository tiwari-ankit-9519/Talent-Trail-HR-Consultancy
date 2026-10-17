package com.abchr.platform.shared.logging;

public final class SqlStats {

    public record Snapshot(long count, long millis) {}

    private static final ThreadLocal<long[]> CURRENT = new ThreadLocal<>();

    private SqlStats() {}

    public static void begin() {
        CURRENT.set(new long[2]);
    }

    public static void record(long millis) {
        long[] values = CURRENT.get();
        if (values != null) {
            values[0]++;
            values[1] += millis;
        }
    }

    public static Snapshot snapshot() {
        long[] values = CURRENT.get();
        return values == null ? new Snapshot(0, 0) : new Snapshot(values[0], values[1]);
    }

    public static void end() {
        CURRENT.remove();
    }
}
