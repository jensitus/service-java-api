package org.service.b.crypto.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_alert")
@Data
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "coin_id", nullable = false)
    private String coinId;

    @Column(name = "target_price", nullable = false)
    private BigDecimal targetPrice;

    @Column(name = "condition_", nullable = false)
    private String condition; // ABOVE or BELOW

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "triggered")
    private Boolean triggered = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
