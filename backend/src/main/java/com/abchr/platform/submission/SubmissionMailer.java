package com.abchr.platform.submission;

import com.abchr.platform.candidate.Candidate;
import com.abchr.platform.client.Client;
import com.abchr.platform.job.Job;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SubmissionMailer {

    private final ZoneId zone;
    private final DateTimeFormatter dateTime;

    public SubmissionMailer(@Value("${app.timezone:Asia/Kolkata}") String zoneId) {
        this.zone = ZoneId.of(zoneId);
        this.dateTime = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy 'at' hh:mm a", Locale.ENGLISH).withZone(zone);
    }

    public String submissionSubject(Candidate candidate, Job job) {
        return "Candidate profile: " + candidate.getFullName() + " for " + job.getTitle() + " (" + job.getCode() + ")";
    }

    public String submissionBody(Candidate candidate, Job job, Client client, String greetingNames, String note) {
        StringBuilder text = new StringBuilder();
        text.append("Dear ").append(greetingNames).append(",\n\n");
        text.append("Please find below the profile of a candidate for the position of ")
                .append(job.getTitle()).append(" (").append(job.getCode()).append(").\n\n");
        line(text, "Candidate", candidate.getFullName());
        line(text, "Current role", role(candidate));
        line(text, "Total experience", candidate.getTotalExperienceYears() == null
                ? null : candidate.getTotalExperienceYears().stripTrailingZeros().toPlainString() + " years");
        line(text, "Current CTC", money(candidate.getCurrentCtc()));
        line(text, "Expected CTC", money(candidate.getExpectedCtc()));
        line(text, "Notice period", candidate.getNoticePeriodDays() == null
                ? null : candidate.getNoticePeriodDays() + " days");
        line(text, "Current location", candidate.getCurrentLocation());
        line(text, "Preferred location", candidate.getPreferredLocation());
        line(text, "Key skills", candidate.getSkills());
        line(text, "Email", candidate.getEmail());
        line(text, "Phone", candidate.getPhone());
        if (note != null && !note.isBlank()) {
            text.append("\nNote from Talent Trail HR Solutions:\n").append(note.trim()).append("\n");
        }
        text.append("\nThe candidate has agreed to the sharing of this profile with ").append(client.getName())
                .append(". ").append(candidate.hasResume() ? "The resume is attached.\n" : "\n");
        text.append("\nPlease reply to this email with your feedback or to arrange an interview.\n\n");
        text.append("Regards,\nTalent Trail HR Solutions\n");
        return text.toString();
    }

    public String interviewSubject(String kind, Candidate candidate, Job job, int round) {
        return kind + ": Interview round " + round + " for " + candidate.getFullName() + " - " + job.getTitle()
                + " (" + job.getCode() + ")";
    }

    public String interviewBody(String kind, Interview interview, Candidate candidate, Job job, Client client) {
        StringBuilder text = new StringBuilder();
        text.append(switch (kind) {
            case "Cancelled" -> "The interview below has been cancelled.\n\n";
            case "Rescheduled" -> "The interview below has been rescheduled. Please note the new details.\n\n";
            default -> "An interview has been scheduled. Details are below.\n\n";
        });
        line(text, "Candidate", candidate.getFullName());
        line(text, "Position", job.getTitle() + " (" + job.getCode() + ")");
        line(text, "Company", client.getName());
        line(text, "Round", String.valueOf(interview.getRoundNumber()));
        line(text, "Date and time", dateTime.format(interview.getScheduledAt()) + " (" + zone.getId() + ")");
        line(text, "Duration", interview.getDurationMinutes() + " minutes");
        line(text, "Mode", switch (interview.getMode()) {
            case PHONE -> "Phone";
            case VIDEO -> "Video call";
            case IN_PERSON -> "In person";
        });
        line(text, switch (interview.getMode()) {
            case PHONE -> "Phone number";
            case VIDEO -> "Meeting link";
            case IN_PERSON -> "Venue";
        }, interview.getLocation());
        line(text, "Interviewer", interview.getInterviewerName());
        if (interview.getNotes() != null && !interview.getNotes().isBlank()) {
            text.append("\nNotes:\n").append(interview.getNotes().trim()).append("\n");
        }
        text.append("\nRegards,\nTalent Trail HR Solutions\n");
        return text.toString();
    }

    public String format(Instant instant) {
        return dateTime.format(instant);
    }

    private static String role(Candidate candidate) {
        String title = candidate.getCurrentTitle();
        String company = candidate.getCurrentCompany();
        if (title != null && company != null) {
            return title + " at " + company;
        }
        return title != null ? title : company;
    }

    private static String money(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return "INR " + NumberFormat.getNumberInstance(Locale.of("en", "IN")).format(value);
    }

    private static void line(StringBuilder text, String label, String value) {
        if (value != null && !value.isBlank()) {
            text.append(label).append(": ").append(value.trim()).append("\n");
        }
    }
}
