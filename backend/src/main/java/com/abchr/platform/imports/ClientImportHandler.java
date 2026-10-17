package com.abchr.platform.imports;

import static com.abchr.platform.imports.ImportSupport.text;

import com.abchr.platform.client.ClientDtos.ClientDetail;
import com.abchr.platform.client.ClientDtos.ContactRequest;
import com.abchr.platform.client.ClientDtos.ProfileRequest;
import com.abchr.platform.client.ClientRepository;
import com.abchr.platform.client.ClientService;
import com.abchr.platform.imports.ImportDtos.ImportReport;
import com.abchr.platform.imports.ImportDtos.RowResult;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class ClientImportHandler implements ImportHandler {

    private static final Map<String, String> LABELS = Map.of("gstin", "GSTIN", "pan", "PAN", "name", "Client name",
            "email", "Contact email");

    private final ClientService clientService;
    private final ClientRepository clientRepository;
    private final Validator validator;
    private final TransactionTemplate tx;

    ClientImportHandler(
            ClientService clientService,
            ClientRepository clientRepository,
            Validator validator,
            PlatformTransactionManager transactionManager) {
        this.clientService = clientService;
        this.clientRepository = clientRepository;
        this.validator = validator;
        this.tx = new TransactionTemplate(transactionManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public String type() {
        return "clients";
    }

    @Override
    public String[] requiredPermissions() {
        return new String[] {"CLIENTS_MANAGE"};
    }

    @Override
    public ImportReport process(List<Map<String, String>> rows, boolean commit) {
        Map<String, List<Integer>> groups = new LinkedHashMap<>();
        RowResult[] results = new RowResult[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            String name = text(rows.get(i), "name");
            if (name == null) {
                results[i] = ImportSupport.result(i, "", ImportSupport.INVALID, List.of("Client name is required"));
                continue;
            }
            groups.computeIfAbsent(name.toLowerCase(), k -> new ArrayList<>()).add(i);
        }
        for (List<Integer> indexes : groups.values()) {
            processGroup(rows, indexes, results, commit);
        }
        return ImportSupport.report(type(), commit, List.of(results), List.of());
    }

    private void processGroup(List<Map<String, String>> rows, List<Integer> indexes, RowResult[] results,
            boolean commit) {
        Map<String, String> first = rows.get(indexes.get(0));
        String name = text(first, "name");
        if (clientRepository.existsByNameIgnoreCase(name)) {
            for (int index : indexes) {
                results[index] = ImportSupport.result(index, name, ImportSupport.DUPLICATE,
                        List.of("A client with this name already exists"));
            }
            return;
        }
        List<String> groupErrors = ImportSupport.list();
        ProfileRequest profile = new ProfileRequest(name, text(first, "legalName"), text(first, "industry"),
                text(first, "website"), text(first, "gstin"), text(first, "pan"), text(first, "addressLine1"),
                text(first, "addressLine2"), text(first, "city"), text(first, "state"), text(first, "postalCode"),
                text(first, "country"), null, null);
        ImportSupport.violations(validator, profile, LABELS, groupErrors);
        List<ContactRequest> contacts = new ArrayList<>();
        Map<Integer, List<String>> rowErrors = new LinkedHashMap<>();
        boolean primarySet = false;
        for (int index : indexes) {
            Map<String, String> row = rows.get(index);
            List<String> errors = ImportSupport.list();
            String contactName = text(row, "contactName");
            String contactEmail = text(row, "contactEmail");
            if (contactName != null || contactEmail != null || text(row, "contactPhone") != null) {
                boolean primary = ImportSupport.bool(row, "contactPrimary") && !primarySet;
                ContactRequest contact = new ContactRequest(contactName, contactEmail, text(row, "contactPhone"),
                        text(row, "contactDesignation"), primary);
                ImportSupport.violations(validator, contact, Map.of("name", "Contact name", "email", "Contact email"),
                        errors);
                if (errors.isEmpty()) {
                    contacts.add(contact);
                    primarySet = primarySet || primary;
                }
            }
            rowErrors.put(index, errors);
        }
        boolean anyError = !groupErrors.isEmpty() || rowErrors.values().stream().anyMatch(list -> !list.isEmpty());
        if (anyError) {
            for (int index : indexes) {
                List<String> messages = new ArrayList<>(groupErrors);
                messages.addAll(rowErrors.get(index));
                if (messages.isEmpty()) {
                    messages.add("Another row for this client has errors");
                }
                results[index] = ImportSupport.result(index, name, ImportSupport.INVALID, messages);
            }
            return;
        }
        if (!commit) {
            for (int index : indexes) {
                results[index] = ImportSupport.result(index, name, ImportSupport.VALID, List.of());
            }
            return;
        }
        try {
            tx.executeWithoutResult(status -> {
                ClientDetail client = clientService.create(profile);
                for (ContactRequest contact : contacts) {
                    clientService.addContact(client.id(), contact);
                }
            });
            for (int index : indexes) {
                results[index] = ImportSupport.result(index, name, ImportSupport.IMPORTED, List.of());
            }
        } catch (RuntimeException ex) {
            String message = ex.getMessage() == null ? "Could not import this client" : ex.getMessage();
            for (int index : indexes) {
                results[index] = ImportSupport.result(index, name, ImportSupport.INVALID, List.of(message));
            }
        }
    }
}
