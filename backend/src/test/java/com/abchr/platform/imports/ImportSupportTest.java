package com.abchr.platform.imports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import com.abchr.platform.auth.PasswordPolicy;
import com.abchr.platform.candidate.CandidateSource;
import com.abchr.platform.imports.EmployeeImportHandler;
import com.abchr.platform.imports.ImportSupport;

class ImportSupportTest {

    @Test
    void blankCellsBecomeNull() {
        assertNull(ImportSupport.text(Map.of("a", "   "), "a"));
        assertEquals("x", ImportSupport.text(Map.of("a", " x "), "a"));
    }

    @Test
    void numbersAllowThousandSeparatorsAndReportBadValues() {
        List<String> errors = new ArrayList<>();
        assertEquals(new BigDecimal("1200000"), ImportSupport.decimal(Map.of("c", "1,200,000"), "c", "CTC", errors));
        assertTrue(errors.isEmpty());
        assertNull(ImportSupport.integer(Map.of("n", "abc"), "n", "Notice", errors));
        assertEquals(List.of("Notice must be a whole number"), errors);
    }

    @Test
    void datesMustBeIso() {
        List<String> errors = new ArrayList<>();
        assertEquals("2026-04-01", String.valueOf(ImportSupport.date(Map.of("d", "2026-04-01"), "d", "Date", errors)));
        assertNull(ImportSupport.date(Map.of("d", "01/04/2026"), "d", "Date", errors));
        assertEquals(1, errors.size());
    }

    @Test
    void enumsAcceptFriendlyTextAndDefault() {
        List<String> errors = new ArrayList<>();
        assertEquals(CandidateSource.APPLY_LINK, ImportSupport.enumValue(Map.of("s", "apply link"), "s", "Source",
                CandidateSource.class, CandidateSource.OTHER, errors));
        assertEquals(CandidateSource.OTHER,
                ImportSupport.enumValue(Map.of(), "s", "Source", CandidateSource.class, CandidateSource.OTHER, errors));
        assertTrue(errors.isEmpty());
        ImportSupport.enumValue(Map.of("s", "twitter"), "s", "Source", CandidateSource.class, CandidateSource.OTHER,
                errors);
        assertEquals(1, errors.size());
    }

    @Test
    void generatedPasswordsSatisfyThePolicy() {
        EmployeeImportHandler handler = new EmployeeImportHandler(null, null, null, null, null, null);
        for (int i = 0; i < 200; i++) {
            PasswordPolicy.validate(handler.generatePassword(), "someone@example.com");
        }
    }
}
