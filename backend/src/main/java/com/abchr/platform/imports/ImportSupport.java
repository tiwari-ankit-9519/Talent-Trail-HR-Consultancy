package com.abchr.platform.imports;

import com.abchr.platform.imports.ImportDtos.ImportReport;
import com.abchr.platform.imports.ImportDtos.RowResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

final class ImportSupport {

    static final String VALID = "VALID";
    static final String IMPORTED = "IMPORTED";
    static final String DUPLICATE = "DUPLICATE";
    static final String INVALID = "INVALID";

    private ImportSupport() {
    }

    static String text(Map<String, String> row, String key) {
        String value = row.get(key);
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static BigDecimal decimal(Map<String, String> row, String key, String label, List<String> errors) {
        String value = text(row, key);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", ""));
        } catch (NumberFormatException ex) {
            errors.add(label + " must be a number");
            return null;
        }
    }

    static Integer integer(Map<String, String> row, String key, String label, List<String> errors) {
        String value = text(row, key);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", "")).intValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
            errors.add(label + " must be a whole number");
            return null;
        }
    }

    static LocalDate date(Map<String, String> row, String key, String label, List<String> errors) {
        String value = text(row, key);
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value);
        } catch (DateTimeParseException ex) {
            errors.add(label + " must be a date like 2026-04-01");
            return null;
        }
    }

    static boolean bool(Map<String, String> row, String key) {
        String value = text(row, key);
        return value != null && List.of("yes", "y", "true", "1").contains(value.toLowerCase());
    }

    static <T extends Enum<T>> T enumValue(
            Map<String, String> row, String key, String label, Class<T> type, T fallback, List<String> errors) {
        String value = text(row, key);
        if (value == null) {
            return fallback;
        }
        String normal = value.toUpperCase().replace(' ', '_').replace('-', '_');
        for (T constant : type.getEnumConstants()) {
            if (constant.name().equals(normal)) {
                return constant;
            }
        }
        errors.add(label + " must be one of " + String.join(", ",
                java.util.Arrays.stream(type.getEnumConstants()).map(Enum::name).toList()));
        return fallback;
    }

    static void violations(Validator validator, Object target, Map<String, String> labels, List<String> errors) {
        Set<ConstraintViolation<Object>> found = validator.validate(target);
        TreeSet<String> sorted = new TreeSet<>();
        for (ConstraintViolation<Object> violation : found) {
            String field = violation.getPropertyPath().toString();
            sorted.add(labels.getOrDefault(field, field) + " " + violation.getMessage());
        }
        errors.addAll(sorted);
    }

    static ImportReport report(
            String type, boolean committed, List<RowResult> rows, List<ImportDtos.Credential> credentials) {
        int valid = 0;
        int imported = 0;
        int duplicates = 0;
        int invalid = 0;
        for (RowResult row : rows) {
            switch (row.status()) {
                case VALID -> valid++;
                case IMPORTED -> imported++;
                case DUPLICATE -> duplicates++;
                default -> invalid++;
            }
        }
        return new ImportReport(type, committed, rows.size(), valid, imported, duplicates, invalid, rows,
                credentials);
    }

    static RowResult result(int index, String label, String status, List<String> messages) {
        return new RowResult(index + 2, label == null ? "" : label, status, List.copyOf(messages));
    }

    static List<String> list() {
        return new ArrayList<>();
    }
}
