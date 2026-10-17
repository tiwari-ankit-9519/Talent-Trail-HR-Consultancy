package com.abchr.platform.profile;

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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class EmployeeDocumentStorage {

    private static final Logger LOG = LogManager.getLogger(EmployeeDocumentStorage.class);

    public record Stored(String path, String fileName, String contentType, long size) {
    }

    private final Path root;

    public EmployeeDocumentStorage(AppProperties properties) {
        this.root = Path.of(properties.storage().basePath()).toAbsolutePath().normalize().resolve("employee-documents");
    }

    public Stored store(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.rule(ErrorCodes.PROFILE_DOCUMENT_INVALID, "Please attach a file");
        }
        String original = sanitize(file.getOriginalFilename());
        String extension = extension(original);
        byte[] head = readHead(file);
        String contentType = switch (extension) {
            case "pdf" -> check(head, new int[] {'%', 'P', 'D', 'F'}, "application/pdf");
            case "png" -> check(head, new int[] {0x89, 'P', 'N', 'G'}, "image/png");
            case "jpg", "jpeg" -> check(head, new int[] {0xFF, 0xD8, 0xFF}, "image/jpeg");
            case "docx" -> check(head, new int[] {'P', 'K', 3, 4},
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            default -> null;
        };
        if (contentType == null) {
            LOG.warn("Employee document rejected for user {}: extension=\"{}\" size={}", userId, extension,
                    file.getSize());
            throw ApiException.rule(ErrorCodes.PROFILE_DOCUMENT_INVALID,
                    "The file must be a PDF, PNG, JPG or DOCX and match its type");
        }
        String relative = userId + "/" + UUID.randomUUID() + "." + extension;
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw ApiException.rule(ErrorCodes.PROFILE_DOCUMENT_INVALID, "Invalid file name");
        }
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            LOG.error("Could not store employee document for user {}", userId, ex);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR,
                    "Could not store the file");
        }
        LOG.info("Employee document stored for user {}: type={} size={} bytes", userId, extension, file.getSize());
        return new Stored(relative, original, contentType, file.getSize());
    }

    public Path resolve(String relative) {
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw ApiException.notFound(ErrorCodes.PROFILE_DOCUMENT_NOT_FOUND, "Document file not found");
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
            LOG.warn("Could not delete employee document file {}", relative, ex);
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

    private static byte[] readHead(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(8);
        } catch (IOException ex) {
            throw ApiException.rule(ErrorCodes.PROFILE_DOCUMENT_INVALID, "The file could not be read");
        }
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String sanitize(String name) {
        String base = name == null ? "document" : name;
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (base.isEmpty()) {
            base = "document";
        }
        return base.length() > 150 ? base.substring(base.length() - 150) : base;
    }
}
