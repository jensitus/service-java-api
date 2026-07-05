package org.service.b.crypto.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BitpandaSyncResultDto {
    private boolean success;
    private String message;
    private int totalFetched;
    private int imported;
    private int skippedDuplicates;
    private int skippedUntracked;
    private int skippedUnfinished;
    private LocalDateTime lastSyncedAt;
}
