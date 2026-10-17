package com.abchr.platform.invoice;

import com.abchr.platform.invoice.InvoiceDtos.InvoiceDetail;
import com.abchr.platform.settings.CompanyService;
import com.abchr.platform.shared.mail.EmailHandler;
import com.abchr.platform.shared.mail.EmailStatus;
import com.abchr.platform.shared.mail.MailService;
import com.abchr.platform.shared.security.Actor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class InvoiceMailService implements EmailHandler {

    private static final Logger LOG = LogManager.getLogger(InvoiceMailService.class);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final InvoiceService invoiceService;
    private final InvoicePdfService pdfService;
    private final MailService mailService;
    private final CompanyService companyService;

    public InvoiceMailService(
            InvoiceService invoiceService,
            InvoicePdfService pdfService,
            MailService mailService,
            CompanyService companyService) {
        this.invoiceService = invoiceService;
        this.pdfService = pdfService;
        this.mailService = mailService;
        this.companyService = companyService;
    }

    public InvoiceDetail send(UUID id, List<String> to) {
        invoiceService.queueSend(id, to, Actor.current().email());
        return invoiceService.detail(id);
    }

    @Override
    public String kind() {
        return "INVOICE";
    }

    @Override
    public void handle(UUID id, List<String> args) {
        deliver(id, args);
    }

    @Override
    public void fail(UUID id, List<String> args, String reason) {
        invoiceService.recordSend(id, EmailStatus.FAILED, reason.length() > 480 ? reason.substring(0, 480) : reason,
                args);
    }

    private void deliver(UUID id, List<String> to) {
        Invoice invoice = invoiceService.findForSending(id);
        String company = invoice.sellerName;
        NumberFormat money = NumberFormat.getNumberInstance(Locale.of("en", "IN"));
        money.setMinimumFractionDigits(2);
        String subject = "Invoice " + invoice.number + " from " + company;
        String body = "Dear Sir/Madam,\n\n"
                + "Please find attached invoice " + invoice.number + " for Rs. " + money.format(invoice.totalAmount)
                + " towards the placement of " + invoice.candidateName + " as " + invoice.jobTitle + " ("
                + invoice.jobCode + "), who joined on " + DAY.format(invoice.joiningDate) + ".\n\n"
                + "Payment is due by " + DAY.format(invoice.dueDate) + ".\n\n"
                + "Regards,\n" + company;
        Path file = null;
        try {
            file = Files.createTempFile("invoice-", ".pdf");
            Files.write(file, pdfService.render(invoice));
            String fileName = "Invoice-" + invoice.number.replace('/', '-') + ".pdf";
            String replyTo = companyService.load().getEmail();
            MailService.Result result = mailService.send(to, subject, body,
                    new MailService.Attachment(file, fileName), replyTo == null ? Actor.current().email() : replyTo);
            invoiceService.recordSend(id, result.status(), result.error(), to);
        } catch (IOException ex) {
            LOG.error("Could not prepare invoice {} for email", invoice.number, ex);
            invoiceService.recordSend(id, EmailStatus.FAILED, "Could not prepare the PDF", to);
        } finally {
            if (file != null) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException ex) {
                    LOG.warn("Could not remove temporary file {}", file);
                }
            }
        }
    }

    public byte[] pdf(UUID id) {
        return pdfService.render(invoiceService.findForSending(id));
    }
}
