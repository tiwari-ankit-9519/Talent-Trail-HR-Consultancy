package com.abchr.platform.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    Optional<Attendance> findByUserIdAndWorkDate(UUID userId, LocalDate workDate);

    List<Attendance> findByUserIdAndWorkDateBetween(UUID userId, LocalDate from, LocalDate to);

    List<Attendance> findByWorkDate(LocalDate workDate);

    List<Attendance> findByWorkDateBetween(LocalDate from, LocalDate to);
}
