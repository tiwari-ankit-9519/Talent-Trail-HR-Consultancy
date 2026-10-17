package com.abchr.platform.commission;

import com.abchr.platform.commission.CommissionDtos.AmountRequest;
import com.abchr.platform.commission.CommissionDtos.CommissionView;
import com.abchr.platform.commission.CommissionDtos.PayRequest;
import com.abchr.platform.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CommissionController {

    private static final String VIEW = "hasAnyAuthority('COMMISSIONS_VIEW','COMMISSIONS_VIEW_OWN','COMMISSIONS_MANAGE')";
    private static final String MANAGE = "hasAuthority('COMMISSIONS_MANAGE')";

    private final CommissionService service;

    public CommissionController(CommissionService service) {
        this.service = service;
    }

    @GetMapping("/commissions")
    @PreAuthorize(VIEW)
    public PageResponse<CommissionView> search(
            @RequestParam(required = false) CommissionStatus status,
            @RequestParam(required = false) UUID recruiterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(status, recruiterId, page, size);
    }

    @GetMapping("/placements/{placementId}/commission")
    @PreAuthorize(VIEW)
    public ResponseEntity<CommissionView> forPlacement(@PathVariable UUID placementId) {
        CommissionView view = service.forPlacement(placementId);
        return view == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(view);
    }

    @PutMapping("/commissions/{id}/amount")
    @PreAuthorize(MANAGE)
    public CommissionView setAmount(@PathVariable UUID id, @Valid @RequestBody AmountRequest request) {
        return service.setAmount(id, request);
    }

    @PostMapping("/commissions/{id}/pay")
    @PreAuthorize(MANAGE)
    public CommissionView pay(@PathVariable UUID id, @Valid @RequestBody PayRequest request) {
        return service.pay(id, request);
    }
}
