package com.abchr.platform.shared.mail;

import com.abchr.platform.shared.config.AppProperties;
import jakarta.mail.internet.MimeMessage;
import java.nio.file.Path;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger LOG = LogManager.getLogger(MailService.class);

    public record Attachment(Path path, String fileName) {
    }

    public record Result(EmailStatus status, String error) {
    }

    private final ObjectProvider<JavaMailSender> senderProvider;
    private final AppProperties properties;
    private final String host;

    public MailService(
            ObjectProvider<JavaMailSender> senderProvider,
            AppProperties properties,
            @Value("${spring.mail.host:}") String host) {
        this.senderProvider = senderProvider;
        this.properties = properties;
        this.host = host;
    }

    @jakarta.annotation.PostConstruct
    void logStatus() {
        LOG.info("Mail settings: hostSet={} host={} mailSenderBean={}", host != null && !host.isBlank(),
                host == null || host.isBlank() ? "-" : host, senderProvider.getIfAvailable() != null);
    }

    public boolean isConfigured() {
        return host != null && !host.isBlank() && senderProvider.getIfAvailable() != null;
    }

    public Result send(List<String> to, String subject, String body, Attachment attachment, String replyTo) {
        if (!isConfigured()) {
            LOG.warn("Email not sent to {}: SMTP not ready hostSet={} mailSenderBean={} (set MAIL_HOST in .env and restart)",
                    to, host != null && !host.isBlank(), senderProvider.getIfAvailable() != null);
            return new Result(EmailStatus.NOT_CONFIGURED, "Email is not configured on the server");
        }
        if (to == null || to.isEmpty()) {
            return new Result(EmailStatus.FAILED, "No recipient");
        }
        try {
            JavaMailSender sender = senderProvider.getObject();
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, attachment != null, "UTF-8");
            helper.setFrom(properties.mail().from());
            helper.setTo(to.toArray(String[]::new));
            if (replyTo != null && !replyTo.isBlank() && !"system".equals(replyTo) && !"unknown".equals(replyTo)) {
                helper.setReplyTo(replyTo);
            }
            helper.setSubject(subject);
            helper.setText(body, false);
            if (attachment != null) {
                helper.addAttachment(attachment.fileName(), new FileSystemResource(attachment.path()));
            }
            sender.send(message);
            LOG.info("Email sent to {} subject=\"{}\" attachment={}", to, subject,
                    attachment == null ? "none" : attachment.fileName());
            return new Result(EmailStatus.SENT, null);
        } catch (Exception ex) {
            LOG.error("Email to {} failed subject=\"{}\"", to, subject, ex);
            String reason = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            return new Result(EmailStatus.FAILED, reason.length() > 480 ? reason.substring(0, 480) : reason);
        }
    }
}
