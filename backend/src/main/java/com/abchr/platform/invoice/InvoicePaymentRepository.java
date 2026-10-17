package com.abchr.platform.invoice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, UUID> {

    List<InvoicePayment> findByInvoiceIdOrderByPaidOnAscCreatedAtAsc(UUID invoiceId);

    Optional<InvoicePayment> findByIdAndInvoiceId(UUID id, UUID invoiceId);
}
