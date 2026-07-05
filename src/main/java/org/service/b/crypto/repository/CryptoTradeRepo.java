package org.service.b.crypto.repository;

import org.service.b.crypto.model.CryptoTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CryptoTradeRepo extends JpaRepository<CryptoTrade, Long> {
    List<CryptoTrade> findByUserIdOrderByTradedAtDesc(Long userId);

    @Query("select t.externalId from CryptoTrade t where t.userId = ?1 and t.externalId is not null")
    List<String> findExternalIdsByUserId(Long userId);
}
