package org.service.b.crypto.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "crypto_trade")
@Data
public class CryptoTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coin_id", nullable = false)
    private String coinId;

    @Column(name = "side", nullable = false)
    private String side; // BUY or SELL

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "price_per_unit", nullable = false)
    private BigDecimal pricePerUnit;

    @Column(name = "fee")
    private BigDecimal fee;

    @Column(name = "traded_at", nullable = false)
    private LocalDateTime tradedAt;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "currency", length = 3)
    private String currency = "EUR";

    @Column(name = "source", length = 20)
    private String source = "MANUAL"; // MANUAL or BITPANDA

    @Column(name = "external_id", length = 100)
    private String externalId; // Bitpanda trade id, for dedup

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
