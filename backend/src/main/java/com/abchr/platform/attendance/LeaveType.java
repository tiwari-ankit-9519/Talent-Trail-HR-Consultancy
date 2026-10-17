package com.abchr.platform.attendance;

import com.abchr.platform.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "leave_type")
public class LeaveType extends AuditableEntity {

    @Id
    UUID id = UUID.randomUUID();

    @Column(name = "name", nullable = false, length = 60)
    String name;

    @Column(name = "yearly_days", nullable = false)
    BigDecimal yearlyDays = BigDecimal.ZERO;

    @Column(name = "unpaid", nullable = false)
    boolean unpaid;

    @Column(name = "active", nullable = false)
    boolean active = true;

    @Column(name = "sort_order", nullable = false)
    int sortOrder;

    protected LeaveType() {
    }

    public static LeaveType create() {
        return new LeaveType();
    }

    public UUID getId() {
        return id;
    }
}
