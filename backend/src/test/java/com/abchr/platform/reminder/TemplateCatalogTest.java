package com.abchr.platform.reminder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import com.abchr.platform.reminder.EmailTemplateService;
import com.abchr.platform.reminder.ReminderService;
import com.abchr.platform.reminder.TemplateCatalog;

class TemplateCatalogTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9]*)\\s*}}");

    @Test
    void everyDefaultTemplateOnlyUsesItsOwnVariables() {
        for (TemplateCatalog.Definition definition : TemplateCatalog.all()) {
            for (String text : List.of(definition.subject(), definition.body())) {
                Matcher matcher = PLACEHOLDER.matcher(text);
                while (matcher.find()) {
                    assertTrue(definition.allows(matcher.group(1)),
                            definition.code() + " uses unknown " + matcher.group(1));
                }
            }
        }
    }

    @Test
    void fillingSamplesLeavesNoPlaceholders() {
        for (TemplateCatalog.Definition definition : TemplateCatalog.all()) {
            String filled = EmailTemplateService.fill(definition.body(), definition.samples(), false);
            assertFalse(filled.contains("{{"), definition.code());
        }
    }

    @Test
    void fillReplacesValuesAndKeepsSubjectOnOneLine() {
        String out = EmailTemplateService.fill("Hi {{ name }}", Map.of("name", "Asha\r\nVerma"), true);
        assertEquals("Hi Asha Verma", out);
    }

    @Test
    void valuesWithDollarSignsAreInsertedLiterally() {
        assertEquals("Pay $1 now", EmailTemplateService.fill("Pay {{amount}} now", Map.of("amount", "$1"), false));
    }

    @Test
    void offsetsAreParsedFromStoredText() {
        assertEquals(List.of(-3, 0, 7), ReminderService.parseOffsets("-3, 0,7"));
        assertEquals(List.of(), ReminderService.parseOffsets(""));
    }
}
