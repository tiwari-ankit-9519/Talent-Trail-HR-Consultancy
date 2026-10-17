package com.abchr.platform.audit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Changes {

    private final Map<String, Object[]> entries = new LinkedHashMap<>();

    private Changes() {
    }

    public static Changes create() {
        return new Changes();
    }

    public Changes field(String name, Object before, Object after) {
        if (!same(before, after)) {
            entries.put(name, new Object[] {before, after});
        }
        return this;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public String summary(String prefix) {
        if (entries.isEmpty()) {
            return prefix;
        }
        return prefix + ": " + String.join(", ", entries.keySet());
    }

    public String toJson() {
        Map<String, Object> out = new LinkedHashMap<>();
        entries.forEach((name, pair) -> {
            Map<String, Object> change = new LinkedHashMap<>();
            change.put("old", pair[0]);
            change.put("new", pair[1]);
            out.put(name, change);
        });
        return JsonWriter.write(out);
    }

    static boolean same(Object a, Object b) {
        if (a instanceof BigDecimal x && b instanceof BigDecimal y) {
            return x.compareTo(y) == 0;
        }
        if (a instanceof Collection<?> x && b instanceof Collection<?> y) {
            return new HashSet<>(x).equals(new HashSet<>(y));
        }
        return Objects.equals(a, b);
    }

    public static List<String> sortedNames(Collection<?> values) {
        List<String> names = new ArrayList<>();
        for (Object value : values) {
            names.add(value instanceof Enum<?> e ? e.name() : String.valueOf(value));
        }
        names.sort(String::compareTo);
        return names;
    }
}
