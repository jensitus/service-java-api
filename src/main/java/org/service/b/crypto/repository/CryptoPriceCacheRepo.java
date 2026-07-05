package org.service.b.crypto.repository;

import org.service.b.crypto.model.CryptoPriceCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CryptoPriceCacheRepo extends JpaRepository<CryptoPriceCache, Long> {
    Optional<CryptoPriceCache> findByCoinId(String coinId);
    List<CryptoPriceCache> findAllByOrderByCoinIdAsc();
}
