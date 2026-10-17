package com.abchr.platform.imports;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public final class ImportDtos {

    public static final int MAX_ROWS = 1000;

    private ImportDtos() {
    }

    public record ImportRequest(@NotNull @Size(min = 1, max = MAX_ROWS) List<Map<String, String>> rows) {
    }

    public record RowResult(int row, String label, String status, List<String> messages) {
    }

    public record Credential(String email, String fullName, String password) {
    }

    public record ImportReport(
            String type,
            boolean committed,
            int total,
            int valid,
            int imported,
            int duplicates,
            int invalid,
            List<RowResult> rows,
            List<Credential> credentials) {
    }
}
