package com.abchr.platform.shared.logging;

import java.util.List;
import java.util.regex.Pattern;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SqlLogListener implements QueryExecutionListener {

    private static final Logger SQL = LogManager.getLogger("sql");
    private static final Pattern SKIP = Pattern.compile("(?i)flyway_schema_history");
    private static final Pattern POLL = Pattern.compile("(?i)from outbox_event where status = 'PENDING'.*for update skip locked");
    private static final Pattern SENSITIVE =
            Pattern.compile("(?i)\\b(password\\w*|\\w*token\\w*|secret\\w*|\\w*hash|bank_\\w+|pan|uan|\\w*_enc)\\b");
    private static final int MAX_ROWS = 3;

    private final boolean logParams;
    private final long slowMs;
    private final int maxValueLength;

    public SqlLogListener(boolean logParams, long slowMs, int maxValueLength) {
        this.logParams = logParams;
        this.slowMs = slowMs;
        this.maxValueLength = maxValueLength;
    }

    @Override
    public void beforeQuery(ExecutionInfo info, List<QueryInfo> queries) {}

    @Override
    public void afterQuery(ExecutionInfo info, List<QueryInfo> queries) {
        long millis = info.getElapsedTime();
        for (QueryInfo query : queries) {
            String text = query.getQuery() == null ? "" : query.getQuery().replaceAll("\\s+", " ").trim();
            if (SKIP.matcher(text).find()) {
                continue;
            }
            if (info.isSuccess() && millis < slowMs && POLL.matcher(text).find()) {
                continue;
            }
            SqlStats.record(millis);
            String batch = info.isBatch() ? " batch=" + info.getBatchSize() : "";
            String params = logParams ? " | params=" + params(text, query) : "";
            if (!info.isSuccess()) {
                Throwable failure = info.getThrowable();
                SQL.error("SQL FAILED {} ms{} {}{} error={}", millis, batch, text, params,
                        failure == null ? "unknown" : failure.toString());
            } else if (millis >= slowMs) {
                SQL.warn("SLOW SQL {} ms{} {}{}", millis, batch, text, params);
            } else {
                SQL.info("SQL {} ms{} {}{}", millis, batch, text, params);
            }
        }
    }

    private String params(String text, QueryInfo query) {
        if (SENSITIVE.matcher(text).find()) {
            return "[hidden]";
        }
        List<List<ParameterSetOperation>> rows = query.getParametersList();
        if (rows == null || rows.isEmpty()) {
            return "[]";
        }
        StringBuilder out = new StringBuilder();
        int shown = Math.min(rows.size(), MAX_ROWS);
        for (int row = 0; row < shown; row++) {
            if (row > 0) {
                out.append(", ");
            }
            out.append('[');
            List<ParameterSetOperation> values = rows.get(row);
            for (int index = 0; index < values.size(); index++) {
                if (index > 0) {
                    out.append(", ");
                }
                ParameterSetOperation operation = values.get(index);
                Object[] args = operation.getArgs();
                boolean absent = ParameterSetOperation.isSetNullParameterOperation(operation) || args == null || args.length < 2;
                out.append(render(absent ? null : args[1]));
            }
            out.append(']');
        }
        if (rows.size() > shown) {
            out.append(" +").append(rows.size() - shown).append(" more");
        }
        return out.toString();
    }

    private String render(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof byte[] bytes) {
            return "<" + bytes.length + " bytes>";
        }
        String text = String.valueOf(value);
        if (text.length() > maxValueLength) {
            text = text.substring(0, maxValueLength) + "...";
        }
        return value instanceof CharSequence ? "'" + text + "'" : text;
    }
}
