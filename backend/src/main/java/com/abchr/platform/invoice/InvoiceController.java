package com.abchr.platform.invoice;

import com.abchr.platform.invoice.InvoiceDtos.CancelRequest;
import com.abchr.platform.invoice.InvoiceDtos.CreateInvoiceRequest;
import com.abchr.platform.invoice.InvoiceDtos.InvoiceDetail;
import com.abchr.platform.invoice.InvoiceDtos.InvoiceSummary;
import com.abchr.platform.invoice.InvoiceDtos.PaymentRequest;
import com.abchr.platform.invoice.InvoiceDtos.SendRequest;
import com.abchr.platform.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class InvoiceController {

    private static final String VIEW = "hasAnyAuthority('INVOICES_VIEW','INVOICES_MANAGE')";
    private static final String MANAGE = "hasAuthority('INVOICES_MANAGE')";

    private final InvoiceService invoiceService;
    private final InvoiceMailService mailService;

    public InvoiceController(InvoiceService invoiceService, InvoiceMailService mailService) {
        this.invoiceService = invoiceService;
        this.mailService = mailService;
    }

    @GetMapping("/invoices")
    @PreAuthorize(VIEW)
    public PageResponse<InvoiceSummary> search(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(defaultValue = "false") boolean overdue,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return invoiceService.search(status, clientId, overdue, q, page, size);
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize(VIEW)
    public InvoiceDetail get(@PathVariable UUID id) {
        return invoiceService.get(id);
    }

    @GetMapping("/placements/{placementId}/invoice")
    @PreAuthorize(VIEW)
    public ResponseEntity<InvoiceDetail> forPlacement(@PathVariable UUID placementId) {
        InvoiceDetail detail = invoiceService.forPlacement(placementId);
        return detail == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(detail);
    }

    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public InvoiceDetail create(@Valid @RequestBody CreateInvoiceRequest request) {
        return invoiceService.create(request.placementId());
    }

    @PostMapping("/invoices/{id}/send")
    @PreAuthorize(MANAGE)
    public InvoiceDetail send(@PathVariable UUID id, @Valid @RequestBody SendRequest request) {
        return mailService.send(id, request.to());
    }

    @PostMapping("/invoices/{id}/cancel")
    @PreAuthorize(MANAGE)
    public InvoiceDetail cancel(@PathVariable UUID id, @Valid @RequestBody CancelRequest request) {
        return invoiceService.cancel(id, request);
    }

    @PostMapping("/invoices/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public InvoiceDetail addPayment(@PathVariable UUID id, @Valid @RequestBody PaymentRequest request) {
        return invoiceService.addPayment(id, request);
    }

    @DeleteMapping("/invoices/{id}/payments/{paymentId}")
    @PreAuthorize(MANAGE)
    public InvoiceDetail removePayment(@PathVariable UUID id, @PathVariable UUID paymentId) {
        return invoiceService.removePayment(id, paymentId);
    }

    @GetMapping("/invoices/{id}/pdf")
    @PreAuthorize(VIEW)
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        byte[] bytes = mailService.pdf(id);
        InvoiceDetail detail = invoiceService.get(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("Invoice-" + detail.number().replace('/', '-') + ".pdf", StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }
}
