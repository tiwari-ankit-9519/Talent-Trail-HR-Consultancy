package com.abchr.platform.reminder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TemplateCatalog {

    public record Variable(String name, String description, String sample) {
    }

    public record Definition(
            String code,
            String name,
            String description,
            String audience,
            List<Variable> variables,
            String subject,
            String body) {

        public Map<String, String> samples() {
            Map<String, String> out = new LinkedHashMap<>();
            variables.forEach(v -> out.put(v.name(), v.sample()));
            return out;
        }

        public boolean allows(String variable) {
            return variables.stream().anyMatch(v -> v.name().equals(variable));
        }
    }

    public static final String INVOICE_SEND = "INVOICE_SEND";
    public static final String OFFER_EMAIL = "OFFER_EMAIL";
    public static final String INVOICE_DUE_SOON = "INVOICE_DUE_SOON";
    public static final String INVOICE_OVERDUE = "INVOICE_OVERDUE";
    public static final String REPLACEMENT_ENDING = "REPLACEMENT_ENDING";
    public static final String CONSENT_PENDING = "CONSENT_PENDING";
    public static final String DOCUMENT_EXPIRY = "DOCUMENT_EXPIRY";

    private static final Map<String, Definition> ALL = new LinkedHashMap<>();

    static {
        add(new Definition(INVOICE_SEND, "Invoice email", "Sent to the client with the invoice PDF attached.",
                "CLIENT",
                List.of(var("company", "Your company name", "Talent Trail HR Solutions"),
                        var("invoiceNumber", "Invoice number", "TT/2026-27/0012"),
                        var("amount", "Invoice total in rupees", "Rs. 59,000.00"),
                        var("candidate", "Candidate who joined", "Asha Verma"),
                        var("jobTitle", "Job title", "Senior Accountant"),
                        var("jobCode", "Job code", "JOB-0042"),
                        var("joiningDate", "Joining date", "01 Oct 2026"),
                        var("dueDate", "Payment due date", "31 Oct 2026")),
                "Invoice {{invoiceNumber}} from {{company}}",
                "Dear Sir/Madam,\n\nPlease find attached invoice {{invoiceNumber}} for {{amount}} towards the "
                        + "placement of {{candidate}} as {{jobTitle}} ({{jobCode}}), who joined on {{joiningDate}}.\n\n"
                        + "Payment is due by {{dueDate}}.\n\nRegards,\n{{company}}"));
        add(new Definition(OFFER_EMAIL, "Candidate offer email", "Sent to the candidate when an offer is emailed.",
                "CANDIDATE",
                List.of(var("company", "Your company name", "Talent Trail HR Solutions"),
                        var("candidate", "Candidate name", "Asha Verma"),
                        var("client", "Client company", "Acme Industries"),
                        var("jobTitle", "Job title", "Senior Accountant"),
                        var("ctc", "Offered CTC per year", "INR 8,40,000"),
                        var("joiningDate", "Expected joining date", "01 Oct 2026")),
                "Congratulations! You have been selected for {{jobTitle}} at {{client}}",
                "Dear {{candidate}},\n\nCongratulations! {{client}} has selected you for the position of "
                        + "{{jobTitle}}.\n\nOffered CTC: {{ctc}} per year\nExpected joining date: {{joiningDate}}\n\n"
                        + "Please reply to this email to confirm that you accept the offer. If you have any "
                        + "questions, or if you are unable to accept, please let us know in your reply.\n\n"
                        + "Once you confirm, we will coordinate the joining formalities with {{client}}.\n\n"
                        + "Regards,\n{{company}}"));
        add(new Definition(INVOICE_DUE_SOON, "Invoice payment reminder",
                "Reminder to the client on or before the due date.", "CLIENT",
                List.of(var("company", "Your company name", "Talent Trail HR Solutions"),
                        var("client", "Client company", "Acme Industries"),
                        var("invoiceNumber", "Invoice number", "TT/2026-27/0012"),
                        var("candidate", "Candidate who joined", "Asha Verma"),
                        var("outstanding", "Amount still to be paid", "Rs. 59,000.00"),
                        var("dueDate", "Payment due date", "31 Oct 2026"),
                        var("daysLeft", "Days remaining, 0 if due today", "3")),
                "Payment reminder: invoice {{invoiceNumber}} is due on {{dueDate}}",
                "Dear Sir/Madam,\n\nThis is a friendly reminder that invoice {{invoiceNumber}} for the placement of "
                        + "{{candidate}} has {{outstanding}} outstanding and is due on {{dueDate}} "
                        + "({{daysLeft}} days from today).\n\nIf you have already made the payment, please ignore "
                        + "this message and share the payment details so we can update our records.\n\n"
                        + "Regards,\n{{company}}"));
        add(new Definition(INVOICE_OVERDUE, "Invoice overdue notice", "Reminder to the client after the due date.",
                "CLIENT",
                List.of(var("company", "Your company name", "Talent Trail HR Solutions"),
                        var("client", "Client company", "Acme Industries"),
                        var("invoiceNumber", "Invoice number", "TT/2026-27/0012"),
                        var("candidate", "Candidate who joined", "Asha Verma"),
                        var("outstanding", "Amount still to be paid", "Rs. 59,000.00"),
                        var("dueDate", "Payment due date", "31 Oct 2026"),
                        var("daysOverdue", "Days past the due date", "7")),
                "Overdue: invoice {{invoiceNumber}} was due on {{dueDate}}",
                "Dear Sir/Madam,\n\nInvoice {{invoiceNumber}} for the placement of {{candidate}} was due on "
                        + "{{dueDate}} and is now {{daysOverdue}} days overdue, with {{outstanding}} outstanding."
                        + "\n\nKindly arrange the payment at the earliest. If it has already been made, please share "
                        + "the payment details so we can update our records.\n\nRegards,\n{{company}}"));
        add(new Definition(REPLACEMENT_ENDING, "Replacement period ending",
                "Internal alert to the recruiter before a free replacement period ends.", "STAFF",
                List.of(var("recruiter", "Recruiter name", "Ravi Kumar"),
                        var("candidate", "Placed candidate", "Asha Verma"),
                        var("client", "Client company", "Acme Industries"),
                        var("jobTitle", "Job title", "Senior Accountant"),
                        var("endDate", "Last day of the replacement period", "30 Dec 2026"),
                        var("daysLeft", "Days remaining", "7")),
                "Replacement period ending: {{candidate}} at {{client}}",
                "Hi {{recruiter}},\n\nThe free replacement period for {{candidate}} ({{jobTitle}} at {{client}}) "
                        + "ends on {{endDate}}, in {{daysLeft}} days.\n\nPlease check in with the client and the "
                        + "candidate. Your commission becomes payable after the period ends and the invoice is "
                        + "paid."));
        add(new Definition(CONSENT_PENDING, "Candidate consent pending",
                "Internal alert to the recruiter when consent has not been recorded.", "STAFF",
                List.of(var("recruiter", "Recruiter email", "ravi@talenttrail.in"),
                        var("candidate", "Candidate name", "Asha Verma"),
                        var("candidateEmail", "Candidate email", "asha@example.com"),
                        var("daysWaiting", "Days since the candidate was added", "5")),
                "Consent pending: {{candidate}}",
                "Hi,\n\n{{candidate}} ({{candidateEmail}}) was added {{daysWaiting}} days ago and we have not "
                        + "recorded consent yet. Details cannot be shared with a client until consent is on "
                        + "record.\n\nPlease contact the candidate and record their consent."));
        add(new Definition(DOCUMENT_EXPIRY, "Document expiry", "Sent to the employee when a document is expiring.",
                "STAFF",
                List.of(var("employee", "Employee name", "Meera Singh"),
                        var("document", "Document title", "Driving licence"),
                        var("expiryDate", "Expiry date", "15 Nov 2026"),
                        var("daysLeft", "Days remaining, 0 if it expires today", "30")),
                "Document expiring: {{document}}",
                "Hi {{employee}},\n\nYour document \"{{document}}\" expires on {{expiryDate}} ({{daysLeft}} days "
                        + "from today).\n\nPlease share the renewed copy with HR."));
    }

    private TemplateCatalog() {
    }

    private static Variable var(String name, String description, String sample) {
        return new Variable(name, description, sample);
    }

    private static void add(Definition definition) {
        ALL.put(definition.code(), definition);
    }

    public static List<Definition> all() {
        return List.copyOf(ALL.values());
    }

    public static Definition find(String code) {
        return ALL.get(code);
    }
}
