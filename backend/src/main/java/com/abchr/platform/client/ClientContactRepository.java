package com.abchr.platform.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientContactRepository extends JpaRepository<ClientContact, UUID> {

    List<ClientContact> findByClientIdOrderByPrimaryContactDescNameAsc(UUID clientId);

    Optional<ClientContact> findByIdAndClientId(UUID id, UUID clientId);
}
