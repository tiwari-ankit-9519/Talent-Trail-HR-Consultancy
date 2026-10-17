package com.abchr.platform.client;

import com.abchr.platform.audit.AuditActions;
import com.abchr.platform.audit.AuditService;
import com.abchr.platform.audit.Changes;
import com.abchr.platform.client.ClientDtos.ClientDetail;
import com.abchr.platform.client.ClientDtos.ClientSummary;
import com.abchr.platform.client.ClientDtos.ContactRequest;
import com.abchr.platform.client.ClientDtos.ContactResponse;
import com.abchr.platform.client.ClientDtos.ProfileRequest;
import com.abchr.platform.client.ClientDtos.StatusRequest;
import com.abchr.platform.client.ClientDtos.TermsRequest;
import com.abchr.platform.client.ClientDtos.TermsResponse;
import com.abchr.platform.shared.persistence.VersionCheck;
import com.abchr.platform.shared.security.Permissions;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import com.abchr.platform.shared.web.PageResponse;
import com.abchr.platform.shared.web.Paging;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal MAX_MONTHS = new BigDecimal("12");

    private final ClientRepository clientRepository;
    private final ClientContactRepository contactRepository;
    private final AuditService auditService;

    public ClientService(
            ClientRepository clientRepository,
            ClientContactRepository contactRepository,
            AuditService auditService) {
        this.clientRepository = clientRepository;
        this.contactRepository = contactRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientSummary> search(String query, ClientStatus status, int page, int size) {
        Specification<Client> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("industry")), like),
                        cb.like(cb.lower(root.get("city")), like)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Client> result = clientRepository.findAll(spec, Paging.of(page, size, Sort.by("name").ascending()));
        return PageResponse.of(result, c -> new ClientSummary(
                c.getId(), c.getName(), c.getIndustry(), c.getCity(), c.getState(), c.getStatus(), c.getUpdatedAt()));
    }

    @Transactional(readOnly = true)
    public ClientDetail get(UUID id) {
        return toDetail(find(id));
    }

    @Transactional
    public ClientDetail create(ProfileRequest request) {
        String name = request.name().trim();
        if (clientRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict(ErrorCodes.CLIENT_NAME_EXISTS, "A client with this name already exists");
        }
        Client client = new Client(name);
        applyProfile(client, request);
        clientRepository.save(client);
        auditService.record(AuditActions.CLIENT_CREATED, AuditActions.ENTITY_CLIENT, client.getId().toString(),
                "Created client " + name, null);
        return toDetail(client);
    }

    @Transactional
    public ClientDetail update(UUID id, ProfileRequest request) {
        Client client = find(id);
        VersionCheck.verify(request.version(), client.getVersion(), Client.class, id);
        String name = request.name().trim();
        if (clientRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw ApiException.conflict(ErrorCodes.CLIENT_NAME_EXISTS, "A client with this name already exists");
        }
        Changes changes = Changes.create()
                .field("name", client.getName(), name)
                .field("legalName", client.getLegalName(), blankToNull(request.legalName()))
                .field("industry", client.getIndustry(), blankToNull(request.industry()))
                .field("website", client.getWebsite(), blankToNull(request.website()))
                .field("gstin", client.getGstin(), upperOrNull(request.gstin()))
                .field("pan", client.getPan(), upperOrNull(request.pan()))
                .field("addressLine1", client.getAddressLine1(), blankToNull(request.addressLine1()))
                .field("addressLine2", client.getAddressLine2(), blankToNull(request.addressLine2()))
                .field("city", client.getCity(), blankToNull(request.city()))
                .field("state", client.getState(), blankToNull(request.state()))
                .field("postalCode", client.getPostalCode(), blankToNull(request.postalCode()))
                .field("country", client.getCountry(), countryOrDefault(request.country()))
                .field("notes", client.getNotes(), blankToNull(request.notes()));
        applyProfile(client, request);
        client.setName(name);
        clientRepository.saveAndFlush(client);
        if (!changes.isEmpty()) {
            auditService.record(AuditActions.CLIENT_UPDATED, AuditActions.ENTITY_CLIENT, id.toString(),
                    changes.summary("Updated client " + name), changes.toJson());
        }
        return toDetail(client);
    }

    @Transactional
    public ClientDetail changeStatus(UUID id, StatusRequest request) {
        Client client = find(id);
        ClientStatus before = client.getStatus();
        if (before != request.status()) {
            client.setStatus(request.status());
            clientRepository.saveAndFlush(client);
            Changes changes = Changes.create().field("status", before, request.status());
            auditService.record(AuditActions.CLIENT_STATUS_CHANGED, AuditActions.ENTITY_CLIENT, id.toString(),
                    "Client " + client.getName() + " set to " + request.status(), changes.toJson());
        }
        return toDetail(client);
    }

    @Transactional
    public ClientDetail updateTerms(UUID id, TermsRequest request) {
        Client client = find(id);
        VersionCheck.verify(request.version(), client.getVersion(), Client.class, id);
        validateTerms(request);
        BigDecimal gstRate = request.gstApplicable() ? request.gstRate() : BigDecimal.ZERO;
        BigDecimal tdsRate = request.tdsApplicable() ? request.tdsRate() : BigDecimal.ZERO;
        Changes changes = Changes.create()
                .field("feeType", client.getFeeType(), request.feeType())
                .field("feeValue", client.getFeeValue(), request.feeValue())
                .field("gstApplicable", client.isGstApplicable(), request.gstApplicable())
                .field("gstRate", client.getGstRate(), gstRate)
                .field("tdsApplicable", client.isTdsApplicable(), request.tdsApplicable())
                .field("tdsRate", client.getTdsRate(), tdsRate)
                .field("paymentTermsDays", client.getPaymentTermsDays(), request.paymentTermsDays())
                .field("replacementPeriodDays", client.getReplacementPeriodDays(), request.replacementPeriodDays());
        client.setTerms(request.feeType(), request.feeValue(), request.gstApplicable(), gstRate,
                request.tdsApplicable(), tdsRate, request.paymentTermsDays(), request.replacementPeriodDays());
        clientRepository.saveAndFlush(client);
        if (!changes.isEmpty()) {
            auditService.record(AuditActions.CLIENT_TERMS_UPDATED, AuditActions.ENTITY_CLIENT, id.toString(),
                    changes.summary("Updated commercial terms of " + client.getName()), changes.toJson());
        }
        return toDetail(client);
    }

    @Transactional
    public ContactResponse addContact(UUID clientId, ContactRequest request) {
        Client client = find(clientId);
        ClientContact contact = new ClientContact(clientId, request.name().trim(), request.email().trim().toLowerCase());
        applyContact(contact, request);
        boolean first = contactRepository.findByClientIdOrderByPrimaryContactDescNameAsc(clientId).isEmpty();
        if (request.primaryContact() || first) {
            clearPrimary(clientId);
            contact.setPrimaryContact(true);
        }
        contactRepository.save(contact);
        auditService.record(AuditActions.CONTACT_ADDED, AuditActions.ENTITY_CONTACT, contact.getId().toString(),
                "Added contact " + contact.getName() + " to " + client.getName(), null);
        return toContact(contact);
    }

    @Transactional
    public ContactResponse updateContact(UUID clientId, UUID contactId, ContactRequest request) {
        Client client = find(clientId);
        ClientContact contact = findContact(clientId, contactId);
        String email = request.email().trim().toLowerCase();
        Changes changes = Changes.create()
                .field("name", contact.getName(), request.name().trim())
                .field("email", contact.getEmail(), email)
                .field("phone", contact.getPhone(), blankToNull(request.phone()))
                .field("designation", contact.getDesignation(), blankToNull(request.designation()))
                .field("primaryContact", contact.isPrimaryContact(), request.primaryContact() || contact.isPrimaryContact());
        applyContact(contact, request);
        contact.setEmail(email);
        if (request.primaryContact() && !contact.isPrimaryContact()) {
            clearPrimary(clientId);
            contact.setPrimaryContact(true);
        }
        contactRepository.saveAndFlush(contact);
        if (!changes.isEmpty()) {
            auditService.record(AuditActions.CONTACT_UPDATED, AuditActions.ENTITY_CONTACT, contactId.toString(),
                    changes.summary("Updated contact of " + client.getName()), changes.toJson());
        }
        return toContact(contact);
    }

    @Transactional
    public void removeContact(UUID clientId, UUID contactId) {
        Client client = find(clientId);
        ClientContact contact = findContact(clientId, contactId);
        boolean wasPrimary = contact.isPrimaryContact();
        contactRepository.delete(contact);
        contactRepository.flush();
        if (wasPrimary) {
            contactRepository.findByClientIdOrderByPrimaryContactDescNameAsc(clientId).stream().findFirst()
                    .ifPresent(next -> next.setPrimaryContact(true));
        }
        auditService.record(AuditActions.CONTACT_REMOVED, AuditActions.ENTITY_CONTACT, contactId.toString(),
                "Removed contact " + contact.getName() + " from " + client.getName(), null);
    }

    private void validateTerms(TermsRequest r) {
        if (r.feeValue().signum() <= 0) {
            throw invalid("Fee value must be greater than zero");
        }
        if (r.feeType() == FeeType.MONTHS_OF_SALARY && r.feeValue().compareTo(MAX_MONTHS) > 0) {
            throw invalid("Fee in months of salary cannot exceed 12");
        }
        if (r.feeType() == FeeType.CTC_PERCENT && r.feeValue().compareTo(HUNDRED) > 0) {
            throw invalid("Fee percentage cannot exceed 100");
        }
        checkRate(r.gstRate(), "GST rate");
        checkRate(r.tdsRate(), "TDS rate");
        if (r.paymentTermsDays() < 0 || r.paymentTermsDays() > 365) {
            throw invalid("Payment terms must be between 0 and 365 days");
        }
        if (r.replacementPeriodDays() < 0 || r.replacementPeriodDays() > 730) {
            throw invalid("Replacement period must be between 0 and 730 days");
        }
    }

    private void checkRate(BigDecimal rate, String label) {
        if (rate.signum() < 0 || rate.compareTo(HUNDRED) > 0) {
            throw invalid(label + " must be between 0 and 100");
        }
    }

    private ApiException invalid(String message) {
        return ApiException.rule(ErrorCodes.CLIENT_TERMS_INVALID, message);
    }

    private void applyProfile(Client client, ProfileRequest r) {
        client.setLegalName(blankToNull(r.legalName()));
        client.setIndustry(blankToNull(r.industry()));
        client.setWebsite(blankToNull(r.website()));
        client.setGstin(upperOrNull(r.gstin()));
        client.setPan(upperOrNull(r.pan()));
        client.setAddressLine1(blankToNull(r.addressLine1()));
        client.setAddressLine2(blankToNull(r.addressLine2()));
        client.setCity(blankToNull(r.city()));
        client.setState(blankToNull(r.state()));
        client.setPostalCode(blankToNull(r.postalCode()));
        client.setCountry(countryOrDefault(r.country()));
        client.setNotes(blankToNull(r.notes()));
    }

    private void applyContact(ClientContact contact, ContactRequest r) {
        contact.setName(r.name().trim());
        contact.setPhone(blankToNull(r.phone()));
        contact.setDesignation(blankToNull(r.designation()));
    }

    private void clearPrimary(UUID clientId) {
        for (ClientContact existing : contactRepository.findByClientIdOrderByPrimaryContactDescNameAsc(clientId)) {
            if (existing.isPrimaryContact()) {
                existing.setPrimaryContact(false);
            }
        }
        contactRepository.flush();
    }

    private Client find(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.CLIENT_NOT_FOUND, "Client not found"));
    }

    private ClientContact findContact(UUID clientId, UUID contactId) {
        return contactRepository.findByIdAndClientId(contactId, clientId)
                .orElseThrow(() -> ApiException.notFound(ErrorCodes.CONTACT_NOT_FOUND, "Contact not found"));
    }

    private ClientDetail toDetail(Client c) {
        TermsResponse terms = Permissions.hasAny("CLIENTS_TERMS_VIEW", "CLIENTS_TERMS_MANAGE")
                ? new TermsResponse(c.getFeeType(), c.getFeeValue(), c.isGstApplicable(), c.getGstRate(),
                        c.isTdsApplicable(), c.getTdsRate(), c.getPaymentTermsDays(), c.getReplacementPeriodDays())
                : null;
        List<ContactResponse> contacts = contactRepository
                .findByClientIdOrderByPrimaryContactDescNameAsc(c.getId()).stream().map(this::toContact).toList();
        return new ClientDetail(c.getId(), c.getName(), c.getLegalName(), c.getIndustry(), c.getWebsite(),
                c.getGstin(), c.getPan(), c.getAddressLine1(), c.getAddressLine2(), c.getCity(), c.getState(),
                c.getPostalCode(), c.getCountry(), c.getStatus(), c.getNotes(), terms, contacts, c.getVersion(),
                c.getCreatedAt(), c.getUpdatedAt());
    }

    private ContactResponse toContact(ClientContact c) {
        return new ContactResponse(c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getDesignation(),
                c.isPrimaryContact());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String upperOrNull(String value) {
        String v = blankToNull(value);
        return v == null ? null : v.toUpperCase();
    }

    private static String countryOrDefault(String value) {
        String v = blankToNull(value);
        return v == null ? "India" : v;
    }
}
