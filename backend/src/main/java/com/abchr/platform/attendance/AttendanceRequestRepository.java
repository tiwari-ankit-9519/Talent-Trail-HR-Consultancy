package com.abchr.platform.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRequestRepository extends JpaRepository<AttendanceRequest, UUID> {

    boolean existsByUserIdAndWorkDateAndStatus(UUID userId, LocalDate workDate, RequestStatus status);

    List<AttendanceRequest> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<AttendanceRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status, Pageable pageable);

    List<AttendanceRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AttendanceRequest> findByUserIdAndWorkDateBetweenAndStatus(
            UUID userId, LocalDate from, LocalDate to, RequestStatus status);
}
