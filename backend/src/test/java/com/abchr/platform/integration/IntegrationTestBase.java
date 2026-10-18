package com.abchr.platform.integration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "app.auth.rate-limit-attempts=1000")
abstract class IntegrationTestBase {

    static final String ADMIN_EMAIL = "admin@talenttrail.test";
    static final String ADMIN_PASSWORD = "Adm1n-Passw0rd!";
    static final String ADMIN_NEW_PASSWORD = "Adm1n-N3w-Passw0rd!";
    private static boolean adminPasswordChanged;

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");
    private static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4-management-alpine");
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    private static final Path STORAGE = createStorage();

    static {
        POSTGRES.start();
        RABBIT.start();
        REDIS.start();
    }

    private static Path createStorage() {
        try {
            return Files.createTempDirectory("abchr-it-files");
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.docker.compose.enabled", () -> "false");
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registry.add("app.security.jwt-secret", () -> "integration-test-secret-with-more-than-32-bytes");
        registry.add("app.security.field-key", () -> "integration-test-field-key-with-32-plus-chars");
        registry.add("app.cors.allowed-origins", () -> "http://localhost:3000");
        registry.add("app.storage.base-path", STORAGE::toString);
        registry.add("app.mail.from", () -> "noreply@talenttrail.test");
        registry.add("app.auth.cookie-secure", () -> "false");
        registry.add("app.auth.bootstrap-admin-email", () -> ADMIN_EMAIL);
        registry.add("app.auth.bootstrap-admin-password", () -> ADMIN_PASSWORD);
        registry.add("app.monitoring.interval-ms", () -> "3600000");
        registry.add("logging.file.name", () -> STORAGE.resolve("platform.log").toString());
    }

    @Autowired
    Environment environment;

    final JsonMapper json = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newHttpClient();

    record Reply(int status, String body) {

        JsonNode tree() {
            return new JsonMapper().readTree(body);
        }
    }

    Reply call(String method, String path, String token, String body) {
        return call(method, path, token, body, java.util.Map.of());
    }

    Reply call(String method, String path, String token, String body, java.util.Map<String, String> headers) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + environment.getProperty("local.server.port") + path));
            if (body == null) {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                builder.header("Content-Type", "application/json");
                builder.method(method, HttpRequest.BodyPublishers.ofString(body));
            }
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
            headers.forEach(builder::header);
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Reply(response.statusCode(), response.body());
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    Reply get(String path, String token) {
        return call("GET", path, token, null);
    }

    Reply post(String path, String token, String body) {
        return call("POST", path, token, body);
    }

    String login(String email, String password) {
        Reply reply = post("/api/v1/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
        if (reply.status() != 200) {
            throw new IllegalStateException("Login failed with " + reply.status() + ": " + reply.body());
        }
        return reply.tree().get("accessToken").asString();
    }

    String loginReady(String email, String password, String newPassword) {
        Reply reply = post("/api/v1/auth/login", null,
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
        if (reply.status() != 200) {
            throw new IllegalStateException("Login failed with " + reply.status() + ": " + reply.body());
        }
        String token = reply.tree().get("accessToken").asString();
        if (!reply.tree().get("user").get("mustChangePassword").asBoolean()) {
            return token;
        }
        Reply changed = post("/api/v1/auth/change-password", token,
                "{\"currentPassword\":\"" + password + "\",\"newPassword\":\"" + newPassword + "\"}");
        if (changed.status() != 200) {
            throw new IllegalStateException("Password change failed with " + changed.status() + ": " + changed.body());
        }
        return changed.tree().get("accessToken").asString();
    }

    String adminToken() {
        synchronized (IntegrationTestBase.class) {
            if (adminPasswordChanged) {
                return login(ADMIN_EMAIL, ADMIN_NEW_PASSWORD);
            }
            String token = loginReady(ADMIN_EMAIL, ADMIN_PASSWORD, ADMIN_NEW_PASSWORD);
            adminPasswordChanged = true;
            return token;
        }
    }
}
