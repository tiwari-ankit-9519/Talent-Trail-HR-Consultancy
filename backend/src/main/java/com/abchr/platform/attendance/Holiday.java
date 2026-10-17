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
@Table(name = "holiday")
public class Holiday extends AuditableEntity {

    @Id
    UUID id = UUID.randomUUID();

    @Column(name = "holiday_date", nullable = false)
    LocalDate holidayDate;

    @Column(name = "name", nullable = false, length = 150)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 10)
    HolidayKind kind = HolidayKind.NATIONAL;

    protected Holiday() {
    }

    public static Holiday create(LocalDate date, String name, HolidayKind kind) {
        Holiday holiday = new Holiday();
        holiday.holidayDate = date;
        holiday.name = name;
        holiday.kind = kind;
        return holiday;
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getHolidayDate() {
        return holidayDate;
    }

    public HolidayKind getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }
}
