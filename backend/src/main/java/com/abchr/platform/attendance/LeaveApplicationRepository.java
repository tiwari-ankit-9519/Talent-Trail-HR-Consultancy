package com.abchr.platform.attendance;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, UUID> {

    List<LeaveApplication> findByUserIdAndStatusInAndFromDateLessThanEqualAndToDateGreaterThanEqual(
            UUID userId, Collection<LeaveStatus> statuses, LocalDate to, LocalDate from);

    List<LeaveApplication> findByStatusInAndFromDateLessThanEqualAndToDateGreaterThanEqual(
            Collection<LeaveStatus> statuses, LocalDate to, LocalDate from);

    List<LeaveApplication> findByUserIdAndLeaveTypeIdAndStatusInAndFromDateBetween(
            UUID userId, UUID leaveTypeId, Collection<LeaveStatus> statuses, LocalDate from, LocalDate to);

    List<LeaveApplication> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<LeaveApplication> findByStatusOrderByCreatedAtDesc(LeaveStatus status, Pageable pageable);

    List<LeaveApplication> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByLeaveTypeId(UUID leaveTypeId);
}
