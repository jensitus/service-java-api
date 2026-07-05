package org.service.b.crypto.repository;

import org.service.b.crypto.model.BitpandaCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BitpandaCredentialRepo extends JpaRepository<BitpandaCredential, Long> {
    Optional<BitpandaCredential> findByUserId(Long userId);
}
