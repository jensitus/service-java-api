package org.service.b.crypto.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BitpandaCredentialStatusDto {
    private boolean hasKey;
    private LocalDateTime createdAt;
    private LocalDateTime lastSyncedAt;
}
