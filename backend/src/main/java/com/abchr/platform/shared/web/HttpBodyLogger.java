package com.abchr.platform.shared.web;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
public class HttpBodyLogger {

    public static final int MAX_BYTES = 8192;

    private static final Logger LOG = LogManager.getLogger("access.body");
    private static final String REDACTED = "[REDACTED]";
    private static final String[] SENSITIVE = {
        "password", "passwd", "token", "authorization", "secret", "apikey", "gstin", "aadhaar", "aadhar",
        "pan", "email", "phone", "mobile", "dateofbirth", "dob", "salary", "ctc", "gross", "basic", "hra", "tds", "allowance",
        "bank", "ifsc", "uan", "account"
    };

    private final ObjectMapper objectMapper;

    public HttpBodyLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void logRequest(ContentCachingRequestWrapper request) {
        byte[] body = request.getContentAsByteArray();
        if (body.length == 0) {
            LOG.debug("HTTP request body is empty or was not cached");
            return;
        }
        if (!isJson(request.getContentType())) {
            LOG.info("HTTP request body omitted because content is not JSON contentType={}", request.getContentType());
            return;
        }
        LOG.info("HTTP request body={}", format(body, request.getCharacterEncoding()));
    }

    public void logResponse(ContentCachingResponseWrapper response) {
        byte[] body = response.getContentAsByteArray();
        if (body.length == 0) {
            LOG.debug("HTTP response body is empty");
            return;
        }
        if (!isJson(response.getContentType())) {
            LOG.info("HTTP response body omitted because content is not JSON contentType={}", response.getContentType());
            return;
        }
        LOG.info("HTTP response body={}", format(body, StandardCharsets.UTF_8.name()));
    }

    private String format(byte[] body, String encoding) {
        Charset charset = Charset.forName(StringUtils.hasText(encoding) ? encoding : StandardCharsets.UTF_8.name());
        int length = Math.min(body.length, MAX_BYTES);
        String raw = new String(body, 0, length, charset);
        try {
            JsonNode root = objectMapper.readTree(raw);
            if (root == null) {
                return "[empty JSON]";
            }
            redact(root);
            String result = objectMapper.writeValueAsString(root);
            return body.length > MAX_BYTES ? result + " [TRUNCATED]" : result;
        } catch (Exception ex) {
            return body.length > MAX_BYTES
                    ? "[JSON body omitted: larger than log limit]"
                    : "[JSON body omitted: could not be safely redacted]";
        }
    }

    private void redact(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            for (Map.Entry<String, JsonNode> field : object.properties()) {
                if (isSensitive(field.getKey())) {
                    object.put(field.getKey(), REDACTED);
                } else {
                    redact(field.getValue());
                }
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                redact(element);
            }
        }
    }

    private boolean isSensitive(String name) {
        String normalized = name.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        for (String key : SENSITIVE) {
            if (key.length() <= 3 ? normalized.equals(key) : normalized.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean isJson(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        try {
            MediaType type = MediaType.parseMediaType(contentType);
            return MediaType.APPLICATION_JSON.isCompatibleWith(type) || type.getSubtype().endsWith("+json");
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
