package com.abchr.platform.attendance;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveEntryRepository extends JpaRepository<LeaveEntry, UUID> {

    List<LeaveEntry> findByUserIdAndLeaveDateBetweenAndStatusInOrderByLeaveDate(
            UUID userId, LocalDate from, LocalDate to, Collection<LeaveStatus> statuses);

    List<LeaveEntry> findByLeaveDateBetweenAndStatusOrderByLeaveDate(LocalDate from, LocalDate to, LeaveStatus status);

    boolean existsByHolidayIdAndStatusIn(UUID holidayId, Collection<LeaveStatus> statuses);

    Optional<LeaveEntry> findByUserIdAndLeaveDateAndStatusIn(UUID userId, LocalDate date, Collection<LeaveStatus> statuses);

    long countByUserIdAndKindAndStatusInAndLeaveDateBetween(
            UUID userId, LeaveKind kind, Collection<LeaveStatus> statuses, LocalDate from, LocalDate to);

    List<LeaveEntry> findByStatusOrderByCreatedAtDesc(LeaveStatus status, Pageable pageable);

    List<LeaveEntry> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
