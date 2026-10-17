package com.abchr.platform.placement;

import com.abchr.platform.shared.config.AppProperties;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class JoiningEmailStorage {

    private static final Logger LOG = LogManager.getLogger(JoiningEmailStorage.class);

    public record Stored(String path, String fileName, String contentType, long size) {
    }

    private final Path root;

    public JoiningEmailStorage(AppProperties properties) {
        this.root = Path.of(properties.storage().basePath()).toAbsolutePath().normalize().resolve("joining-emails");
    }

    public Stored store(UUID placementId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            LOG.warn("Joining email rejected for placement {}: no file attached", placementId);
            throw ApiException.rule(ErrorCodes.JOINING_EMAIL_INVALID, "Please attach the joining email");
        }
        String original = sanitize(file.getOriginalFilename());
        String extension = extension(original);
        byte[] head = readHead(file);
        String contentType = switch (extension) {
            case "pdf" -> check(head, new int[] {'%', 'P', 'D', 'F'}, "application/pdf");
            case "png" -> check(head, new int[] {0x89, 'P', 'N', 'G'}, "image/png");
            case "jpg", "jpeg" -> check(head, new int[] {0xFF, 0xD8, 0xFF}, "image/jpeg");
            case "msg" -> check(head, new int[] {0xD0, 0xCF, 0x11, 0xE0}, "application/vnd.ms-outlook");
            case "eml" -> isPlainText(head) ? "message/rfc822" : null;
            default -> null;
        };
        if (contentType == null) {
            LOG.warn("Joining email rejected for placement {}: extension=\"{}\" size={} does not match its content",
                    placementId, extension, file.getSize());
            throw ApiException.rule(ErrorCodes.JOINING_EMAIL_INVALID,
                    "The file must be a PDF, EML, MSG, PNG or JPG and match its type");
        }
        String relative = placementId + "/" + UUID.randomUUID() + "." + extension;
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw ApiException.rule(ErrorCodes.JOINING_EMAIL_INVALID, "Invalid file name");
        }
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            LOG.error("Could not store joining email for placement {}", placementId, ex);
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCodes.INTERNAL_ERROR, "Could not store the file");
        }
        LOG.info("Joining email stored for placement {}: type={} size={} bytes path={}", placementId, extension,
                file.getSize(), relative);
        return new Stored(relative, original, contentType, file.getSize());
    }

    public Path resolve(String relative) {
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw ApiException.notFound(ErrorCodes.JOINING_EMAIL_NOT_FOUND, "Joining email file not found");
        }
        return path;
    }

    public void delete(String relative) {
        if (relative == null) {
            return;
        }
        try {
            Path path = root.resolve(relative).normalize();
            if (path.startsWith(root)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ex) {
            LOG.warn("Could not delete joining email file {}", relative, ex);
        }
    }

    private static String check(byte[] head, int[] signature, String contentType) {
        if (head.length < signature.length) {
            return null;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((head[i] & 0xFF) != signature[i]) {
                return null;
            }
        }
        return contentType;
    }

    private static boolean isPlainText(byte[] head) {
        if (head.length == 0) {
            return false;
        }
        for (byte b : head) {
            if (b == 0) {
                return false;
            }
        }
        return true;
    }

    private static byte[] readHead(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(512);
        } catch (IOException ex) {
            throw ApiException.rule(ErrorCodes.JOINING_EMAIL_INVALID, "The file could not be read");
        }
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String sanitize(String name) {
        String base = name == null ? "joining-email" : name;
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (base.isEmpty()) {
            base = "joining-email";
        }
        return base.length() > 150 ? base.substring(base.length() - 150) : base;
    }
}
