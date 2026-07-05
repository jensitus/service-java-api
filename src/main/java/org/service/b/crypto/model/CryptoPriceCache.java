package org.service.b.crypto.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "crypto_price_cache")
@Data
public class CryptoPriceCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coin_id", unique = true, nullable = false)
    private String coinId;

    @Column(name = "coin_name", nullable = false)
    private String coinName;

    @Column(name = "price_usd", nullable = false)
    private BigDecimal priceUsd;

    @Column(name = "price_change_24h")
    private BigDecimal priceChange24h;

    @Column(name = "market_cap_usd")
    private BigDecimal marketCapUsd;

    @Column(name = "price_eur")
    private BigDecimal priceEur;

    @Column(name = "price_change_24h_eur")
    private BigDecimal priceChange24hEur;

    @Column(name = "market_cap_eur")
    private BigDecimal marketCapEur;

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt;

}
