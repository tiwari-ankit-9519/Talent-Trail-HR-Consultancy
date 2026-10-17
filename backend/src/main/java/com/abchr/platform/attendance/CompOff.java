package com.abchr.platform.attendance;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "comp_off")
public class CompOff extends AuditableEntity {

    @Id
    UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false, updatable = false)
    UUID userId;

    @Column(name = "earned_date", nullable = false, updatable = false)
    LocalDate earnedDate;

    @Column(name = "expires_on", nullable = false, updatable = false)
    LocalDate expiresOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    CompOffStatus status = CompOffStatus.AVAILABLE;

    @Column(name = "used_on")
    LocalDate usedOn;

    @Column(name = "source", nullable = false, updatable = false, length = 10)
    String source;

    @Column(name = "note", length = 300)
    String note;

    protected CompOff() {
    }

    public static CompOff create(UUID userId, LocalDate earnedDate, LocalDate expiresOn, String source, String note) {
        CompOff credit = new CompOff();
        credit.userId = userId;
        credit.earnedDate = earnedDate;
        credit.expiresOn = expiresOn;
        credit.source = source;
        credit.note = note;
        return credit;
    }

    public UUID getId() {
        return id;
    }
}
