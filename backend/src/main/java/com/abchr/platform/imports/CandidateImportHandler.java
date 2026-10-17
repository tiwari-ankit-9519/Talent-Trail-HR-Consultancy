package com.abchr.platform.imports;

import static com.abchr.platform.imports.ImportSupport.decimal;
import static com.abchr.platform.imports.ImportSupport.integer;
import static com.abchr.platform.imports.ImportSupport.text;

import com.abchr.platform.candidate.CandidateDtos.SaveCandidateRequest;
import com.abchr.platform.candidate.CandidateRepository;
import com.abchr.platform.candidate.CandidateService;
import com.abchr.platform.candidate.CandidateSource;
import com.abchr.platform.imports.ImportDtos.ImportReport;
import com.abchr.platform.imports.ImportDtos.RowResult;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class CandidateImportHandler implements ImportHandler {

    private static final Map<String, String> LABELS = Map.of("fullName", "Full name", "email", "Email", "phone",
            "Phone", "noticePeriodDays", "Notice period", "skills", "Skills", "notes", "Notes");

    private final CandidateService candidateService;
    private final CandidateRepository candidateRepository;
    private final Validator validator;
    private final TransactionTemplate tx;

    CandidateImportHandler(
            CandidateService candidateService,
            CandidateRepository candidateRepository,
            Validator validator,
            PlatformTransactionManager transactionManager) {
        this.candidateService = candidateService;
        this.candidateRepository = candidateRepository;
        this.validator = validator;
        this.tx = new TransactionTemplate(transactionManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public String type() {
        return "candidates";
    }

    @Override
    public String[] requiredPermissions() {
        return new String[] {"CANDIDATES_MANAGE"};
    }

    @Override
    public ImportReport process(List<Map<String, String>> rows, boolean commit) {
        List<RowResult> results = new ArrayList<>();
        Map<String, Integer> seen = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            List<String> errors = ImportSupport.list();
            String name = text(row, "fullName");
            String email = text(row, "email");
            String label = name == null ? email : name;
            SaveCandidateRequest request = build(row, errors);
            if (email != null) {
                String key = email.toLowerCase();
                Integer first = seen.putIfAbsent(key, i + 2);
                if (first != null) {
                    results.add(ImportSupport.result(i, label, ImportSupport.DUPLICATE,
                            List.of("Same email as row " + first + " in this file")));
                    continue;
                }
                if (errors.isEmpty() && candidateRepository.findByEmailIgnoreCase(key).isPresent()) {
                    results.add(ImportSupport.result(i, label, ImportSupport.DUPLICATE,
                            List.of("A candidate with this email already exists")));
                    continue;
                }
            }
            if (!errors.isEmpty()) {
                results.add(ImportSupport.result(i, label, ImportSupport.INVALID, errors));
                continue;
            }
            if (!commit) {
                results.add(ImportSupport.result(i, label, ImportSupport.VALID, errors));
                continue;
            }
            try {
                tx.executeWithoutResult(status -> candidateService.create(request));
                results.add(ImportSupport.result(i, label, ImportSupport.IMPORTED, errors));
            } catch (RuntimeException ex) {
                results.add(ImportSupport.result(i, label, ImportSupport.INVALID,
                        List.of(ex.getMessage() == null ? "Could not import this row" : ex.getMessage())));
            }
        }
        return ImportSupport.report(type(), commit, results, List.of());
    }

    private SaveCandidateRequest build(Map<String, String> row, List<String> errors) {
        CandidateSource source = ImportSupport.enumValue(row, "source", "Source", CandidateSource.class,
                CandidateSource.OTHER, errors);
        SaveCandidateRequest request = new SaveCandidateRequest(text(row, "fullName"), text(row, "email"),
                text(row, "phone"), text(row, "currentCompany"), text(row, "currentTitle"),
                decimal(row, "totalExperienceYears", "Experience", errors),
                decimal(row, "currentCtc", "Current CTC", errors),
                decimal(row, "expectedCtc", "Expected CTC", errors),
                integer(row, "noticePeriodDays", "Notice period", errors), text(row, "currentLocation"),
                text(row, "preferredLocation"), text(row, "skills"), source, text(row, "notes"), null);
        ImportSupport.violations(validator, request, LABELS, errors);
        return request;
    }
}
