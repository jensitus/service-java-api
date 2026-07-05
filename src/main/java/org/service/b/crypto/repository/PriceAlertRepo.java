package org.service.b.crypto.repository;

import org.service.b.crypto.model.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceAlertRepo extends JpaRepository<PriceAlert, Long> {
    List<PriceAlert> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<PriceAlert> findByActiveAndTriggeredFalse(Boolean active);
}
