package org.service.b.crypto.repository;

import org.service.b.crypto.model.CryptoPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CryptoPriceHistoryRepo extends JpaRepository<CryptoPriceHistory, Long> {

    List<CryptoPriceHistory> findByCoinIdAndRecordedAtAfterOrderByRecordedAtAsc(
        String coinId, LocalDateTime after);

    @Query("SELECT h FROM CryptoPriceHistory h WHERE h.coinId = :coinId ORDER BY h.recordedAt DESC")
    List<CryptoPriceHistory> findLatestByCoinId(String coinId,
        org.springframework.data.domain.Pageable pageable);

    void deleteByCoinIdAndRecordedAtBefore(String coinId, LocalDateTime before);
}
