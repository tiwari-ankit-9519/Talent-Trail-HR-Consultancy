package com.abchr.platform.invoice;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    @Query(value = "insert into invoice_counter (fy, last_number) values (:fy, 1) "
            + "on conflict (fy) do update set last_number = invoice_counter.last_number + 1 "
            + "returning last_number", nativeQuery = true)
    Integer nextNumber(@Param("fy") String fy);

    Optional<Invoice> findByPlacementIdAndStatusNot(UUID placementId, InvoiceStatus status);
}
