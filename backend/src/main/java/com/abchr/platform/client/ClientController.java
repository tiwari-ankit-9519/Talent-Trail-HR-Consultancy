package com.abchr.platform.client;

import com.abchr.platform.client.ClientDtos.ClientDetail;
import com.abchr.platform.client.ClientDtos.ClientSummary;
import com.abchr.platform.client.ClientDtos.ContactRequest;
import com.abchr.platform.client.ClientDtos.ContactResponse;
import com.abchr.platform.client.ClientDtos.ProfileRequest;
import com.abchr.platform.client.ClientDtos.StatusRequest;
import com.abchr.platform.client.ClientDtos.TermsRequest;
import com.abchr.platform.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private static final String VIEW = "hasAnyAuthority('CLIENTS_VIEW','CLIENTS_MANAGE')";
    private static final String MANAGE = "hasAuthority('CLIENTS_MANAGE')";
    private static final String TERMS = "hasAuthority('CLIENTS_TERMS_MANAGE')";

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    @PreAuthorize(VIEW)
    public PageResponse<ClientSummary> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ClientStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return clientService.search(q, status, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW)
    public ClientDetail get(@PathVariable UUID id) {
        return clientService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public ClientDetail create(@Valid @RequestBody ProfileRequest request) {
        return clientService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(MANAGE)
    public ClientDetail update(@PathVariable UUID id, @Valid @RequestBody ProfileRequest request) {
        return clientService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(MANAGE)
    public ClientDetail changeStatus(@PathVariable UUID id, @Valid @RequestBody StatusRequest request) {
        return clientService.changeStatus(id, request);
    }

    @PutMapping("/{id}/terms")
    @PreAuthorize(TERMS)
    public ClientDetail updateTerms(@PathVariable UUID id, @Valid @RequestBody TermsRequest request) {
        return clientService.updateTerms(id, request);
    }

    @PostMapping("/{id}/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(MANAGE)
    public ContactResponse addContact(@PathVariable UUID id, @Valid @RequestBody ContactRequest request) {
        return clientService.addContact(id, request);
    }

    @PutMapping("/{id}/contacts/{contactId}")
    @PreAuthorize(MANAGE)
    public ContactResponse updateContact(
            @PathVariable UUID id, @PathVariable UUID contactId, @Valid @RequestBody ContactRequest request) {
        return clientService.updateContact(id, contactId, request);
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(MANAGE)
    public void removeContact(@PathVariable UUID id, @PathVariable UUID contactId) {
        clientService.removeContact(id, contactId);
    }
}
