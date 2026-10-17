package com.abchr.platform.candidate;

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
public class ResumeStorage {

    private static final Logger LOG = LogManager.getLogger(ResumeStorage.class);

    public record Stored(String path, String fileName, String contentType, long size) {
    }

    private final Path root;

    public ResumeStorage(AppProperties properties) {
        this.root = Path.of(properties.storage().basePath()).toAbsolutePath().normalize().resolve("resumes");
    }

    public Stored store(UUID candidateId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            LOG.warn("Resume rejected for candidate {}: no file attached", candidateId);
            throw ApiException.rule(ErrorCodes.RESUME_INVALID, "Please attach a resume");
        }
        String original = sanitize(file.getOriginalFilename());
        String extension = extension(original);
        String contentType = switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> {
                LOG.warn("Resume rejected for candidate {}: unsupported extension \"{}\" size={}", candidateId,
                        extension, file.getSize());
                throw ApiException.rule(ErrorCodes.RESUME_INVALID, "Resume must be a PDF, DOC or DOCX file");
            }
        };
        byte[] head = readHead(file);
        if (!matchesSignature(extension, head)) {
            LOG.warn("Resume rejected for candidate {}: content does not match extension \"{}\" size={}", candidateId,
                    extension, file.getSize());
            throw ApiException.rule(ErrorCodes.RESUME_INVALID, "The file content does not match its type");
        }
        Path directory = root.resolve(candidateId.toString());
        String relative = candidateId + "/" + UUID.randomUUID() + "." + extension;
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw ApiException.rule(ErrorCodes.RESUME_INVALID, "Invalid file name");
        }
        try {
            Files.createDirectories(directory);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            LOG.error("Could not store resume for candidate {}", candidateId, ex);
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCodes.INTERNAL_ERROR, "Could not store the resume");
        }
        LOG.info("Resume stored for candidate {}: type={} size={} bytes path={}", candidateId, extension,
                file.getSize(), relative);
        return new Stored(relative, original, contentType, file.getSize());
    }

    public Path resolve(String relative) {
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw ApiException.notFound(ErrorCodes.RESUME_NOT_FOUND, "Resume file not found");
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
            LOG.warn("Could not delete resume file {}", relative, ex);
        }
    }

    private static byte[] readHead(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(8);
        } catch (IOException ex) {
            throw ApiException.rule(ErrorCodes.RESUME_INVALID, "The file could not be read");
        }
    }

    private static boolean matchesSignature(String extension, byte[] head) {
        if (head.length < 4) {
            return false;
        }
        return switch (extension) {
            case "pdf" -> head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F';
            case "docx" -> head[0] == 'P' && head[1] == 'K' && head[2] == 3 && head[3] == 4;
            case "doc" -> (head[0] & 0xFF) == 0xD0 && (head[1] & 0xFF) == 0xCF
                    && (head[2] & 0xFF) == 0x11 && (head[3] & 0xFF) == 0xE0;
            default -> false;
        };
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String sanitize(String name) {
        String base = name == null ? "resume" : name;
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (base.isEmpty()) {
            base = "resume";
        }
        return base.length() > 150 ? base.substring(base.length() - 150) : base;
    }
}
