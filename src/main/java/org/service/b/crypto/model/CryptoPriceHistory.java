package org.service.b.crypto.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "crypto_price_history")
@Data
public class CryptoPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coin_id", nullable = false)
    private String coinId;

    @Column(name = "price_usd", nullable = false)
    private BigDecimal priceUsd;

    @Column(name = "price_eur")
    private BigDecimal priceEur;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

}
