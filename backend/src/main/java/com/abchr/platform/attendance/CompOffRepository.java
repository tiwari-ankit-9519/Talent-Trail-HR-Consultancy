package com.abchr.platform.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompOffRepository extends JpaRepository<CompOff, UUID> {

    Optional<CompOff> findByUserIdAndEarnedDate(UUID userId, LocalDate earnedDate);

    List<CompOff> findByUserIdOrderByEarnedDateDesc(UUID userId);

    List<CompOff> findByUserIdAndStatusAndExpiresOnGreaterThanEqualOrderByEarnedDate(
            UUID userId, CompOffStatus status, LocalDate expiresOn);
}
