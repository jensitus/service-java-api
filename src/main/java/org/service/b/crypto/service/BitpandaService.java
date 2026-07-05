package org.service.b.crypto.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.service.b.crypto.dto.BitpandaCredentialStatusDto;
import org.service.b.crypto.dto.BitpandaSyncResultDto;
import org.service.b.crypto.dto.BitpandaTestResultDto;
import org.service.b.crypto.dto.BitpandaTradeDto;
import org.service.b.crypto.model.BitpandaCredential;
import org.service.b.crypto.model.CryptoTrade;
import org.service.b.crypto.repository.BitpandaCredentialRepo;
import org.service.b.crypto.repository.CryptoTradeRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bitpanda retail API (read-only) integration.
 *
 * Stores an encrypted per-user API key and imports trades on demand ("sync on
 * button click"). Imports are idempotent: each Bitpanda trade id is recorded as
 * external_id and re-syncing skips trades already imported.
 */
@Service
public class BitpandaService {

    private static final Logger logger = LoggerFactory.getLogger(BitpandaService.class);

    private static final String TRADES_URL = "https://api.bitpanda.com/v1/trades?page=%d&page_size=%d";
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 20; // safety cap: up to 2000 trades per sync
    private static final long SYNC_COOLDOWN_SECONDS = 60; // per-user throttle between syncs

    // Bitpanda ticker symbols -> our internal coin ids
    private static final Map<String, String> SYMBOL_TO_COIN = Map.of(
        "BTC", "bitcoin",
        "LTC", "litecoin",
        "ETH", "ethereum",
        "XRP", "ripple"
    );

    private final RestClient restClient;
    private final CryptoTradeRepo tradeRepo;
    private final BitpandaCredentialRepo credentialRepo;
    private final EncryptionService encryptionService;

    public BitpandaService(RestClient restClient, CryptoTradeRepo tradeRepo,
                           BitpandaCredentialRepo credentialRepo, EncryptionService encryptionService) {
        this.restClient = restClient;
        this.tradeRepo = tradeRepo;
        this.credentialRepo = credentialRepo;
        this.encryptionService = encryptionService;
    }

    // --- Proof-of-concept / ad-hoc test (key not stored) ---

    public BitpandaTestResultDto testConnection(String apiKey) {
        BitpandaTestResultDto result = new BitpandaTestResultDto();
        try {
            BitpandaTradesResponse response = fetchPage(apiKey, 1, 25);
            List<BitpandaTradeDto> trades = new ArrayList<>();
            if (response.data() != null) {
                for (BitpandaTradesResponse.Data d : response.data()) {
                    if (d.attributes() != null) trades.add(toPreviewDto(d));
                }
            }
            int total = response.meta() != null && response.meta().totalCount() != null
                ? response.meta().totalCount() : trades.size();
            result.setSuccess(true);
            result.setTotalCount(total);
            result.setTrades(trades);
            result.setMessage("Connected. Bitpanda reports " + total
                + " trade(s); showing the " + trades.size() + " most recent.");
        } catch (BitpandaException e) {
            result.setSuccess(false);
            result.setMessage(e.getMessage());
        }
        return result;
    }

    // --- Stored credential management ---

    public BitpandaCredentialStatusDto getStatus(Long userId) {
        return credentialRepo.findByUserId(userId)
            .map(this::toStatus)
            .orElseGet(BitpandaCredentialStatusDto::new); // hasKey = false
    }

    @Transactional
    public BitpandaCredentialStatusDto saveCredential(Long userId, String apiKey) {
        // Validate the key before storing it — a single cheap call.
        fetchPage(apiKey, 1, 1);

        BitpandaCredential cred = credentialRepo.findByUserId(userId)
            .orElseGet(BitpandaCredential::new);
        cred.setUserId(userId);
        cred.setApiKeyEncrypted(encryptionService.encrypt(apiKey));
        if (cred.getCreatedAt() == null) cred.setCreatedAt(LocalDateTime.now());
        credentialRepo.save(cred);
        return toStatus(cred);
    }

    @Transactional
    public void deleteCredential(Long userId) {
        credentialRepo.findByUserId(userId).ifPresent(credentialRepo::delete);
    }

    // --- Sync ---

    @Transactional
    public BitpandaSyncResultDto sync(Long userId) {
        BitpandaCredential cred = credentialRepo.findByUserId(userId)
            .orElseThrow(() -> new BitpandaException("No Bitpanda API key saved. Connect your account first."));

        // Per-user cooldown: protect Bitpanda's rate limit (and our server's IP).
        if (cred.getLastSyncedAt() != null) {
            long sinceLast = Duration.between(cred.getLastSyncedAt(), LocalDateTime.now()).getSeconds();
            if (sinceLast < SYNC_COOLDOWN_SECONDS) {
                throw new BitpandaException("You synced very recently. Please wait "
                    + (SYNC_COOLDOWN_SECONDS - sinceLast) + " seconds before syncing again.");
            }
        }

        String apiKey = encryptionService.decrypt(cred.getApiKeyEncrypted());

        Set<String> existing = new HashSet<>(tradeRepo.findExternalIdsByUserId(userId));

        BitpandaSyncResultDto result = new BitpandaSyncResultDto();
        int page = 1;
        while (page <= MAX_PAGES) {
            BitpandaTradesResponse response = fetchPage(apiKey, page, PAGE_SIZE);
            if (response.data() == null || response.data().isEmpty()) break;

            for (BitpandaTradesResponse.Data d : response.data()) {
                result.setTotalFetched(result.getTotalFetched() + 1);
                BitpandaTradesResponse.Attributes a = d.attributes();
                if (a == null) continue;

                if (!"finished".equalsIgnoreCase(a.status())) {
                    result.setSkippedUnfinished(result.getSkippedUnfinished() + 1);
                    continue;
                }
                String coinId = a.cryptocoinSymbol() != null
                    ? SYMBOL_TO_COIN.get(a.cryptocoinSymbol().toUpperCase()) : null;
                if (coinId == null) {
                    result.setSkippedUntracked(result.getSkippedUntracked() + 1);
                    continue;
                }
                if (existing.contains(d.id())) {
                    result.setSkippedDuplicates(result.getSkippedDuplicates() + 1);
                    continue;
                }

                tradeRepo.save(toTrade(userId, d.id(), coinId, a));
                existing.add(d.id());
                result.setImported(result.getImported() + 1);
            }

            if (response.data().size() < PAGE_SIZE) break; // last page
            page++;
        }

        cred.setLastSyncedAt(LocalDateTime.now());
        credentialRepo.save(cred);

        result.setSuccess(true);
        result.setLastSyncedAt(cred.getLastSyncedAt());
        result.setMessage(String.format(
            "Imported %d new trade(s). Skipped %d already-imported, %d untracked coin(s), %d unfinished.",
            result.getImported(), result.getSkippedDuplicates(),
            result.getSkippedUntracked(), result.getSkippedUnfinished()));
        return result;
    }

    // --- Helpers ---

    private CryptoTrade toTrade(Long userId, String externalId, String coinId,
                               BitpandaTradesResponse.Attributes a) {
        CryptoTrade t = new CryptoTrade();
        t.setUserId(userId);
        t.setCoinId(coinId);
        t.setSide(a.type() != null ? a.type().toUpperCase() : "BUY");
        t.setQuantity(orZero(parse(a.amountCryptocoin())));
        t.setPricePerUnit(orZero(parse(a.price())));
        t.setFee(BigDecimal.ZERO); // Bitpanda retail bakes fees into the spread
        t.setCurrency("EUR");      // retail accounts settle in the account fiat (EUR for AT users)
        t.setSource("BITPANDA");
        t.setExternalId(externalId);
        t.setTradedAt(a.time() != null ? parseDate(a.time().dateIso8601()) : LocalDateTime.now());
        t.setNote("Imported from Bitpanda");
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }

    private BitpandaTradeDto toPreviewDto(BitpandaTradesResponse.Data d) {
        BitpandaTradesResponse.Attributes a = d.attributes();
        BitpandaTradeDto dto = new BitpandaTradeDto();
        dto.setBitpandaId(d.id());
        dto.setType(a.type());
        dto.setStatus(a.status());
        dto.setCoinSymbol(a.cryptocoinSymbol());
        dto.setCryptoAmount(parse(a.amountCryptocoin()));
        dto.setFiatAmount(parse(a.amountFiat()));
        dto.setPrice(parse(a.price()));
        dto.setTradedAt(a.time() != null ? parseDate(a.time().dateIso8601()) : null);
        String coinId = a.cryptocoinSymbol() != null
            ? SYMBOL_TO_COIN.get(a.cryptocoinSymbol().toUpperCase()) : null;
        dto.setMappedCoinId(coinId);
        dto.setTracked(coinId != null);
        return dto;
    }

    private BitpandaTradesResponse fetchPage(String apiKey, int page, int pageSize) {
        try {
            BitpandaTradesResponse response = restClient.get()
                .uri(String.format(TRADES_URL, page, pageSize))
                .header("X-API-KEY", apiKey)
                .retrieve()
                .body(BitpandaTradesResponse.class);
            if (response == null) {
                throw new BitpandaException("Bitpanda returned an empty response.");
            }
            return response;
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw new BitpandaException("Bitpanda rejected the API key (" + e.getStatusCode()
                + "). Check that it is valid and has the Trading scope.");
        } catch (BitpandaException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Bitpanda request failed: {}", e.getMessage());
            throw new BitpandaException("Could not reach Bitpanda: " + e.getMessage());
        }
    }

    private BitpandaCredentialStatusDto toStatus(BitpandaCredential cred) {
        BitpandaCredentialStatusDto dto = new BitpandaCredentialStatusDto();
        dto.setHasKey(true);
        dto.setCreatedAt(cred.getCreatedAt());
        dto.setLastSyncedAt(cred.getLastSyncedAt());
        return dto;
    }

    private BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private BigDecimal parse(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseDate(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return OffsetDateTime.parse(iso).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    // --- Bitpanda response mapping (only the fields we need) ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BitpandaTradesResponse(List<Data> data, Meta meta) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Data(String id, Attributes attributes) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Attributes(
            String type,
            String status,
            String price,
            @JsonProperty("amount_fiat") String amountFiat,
            @JsonProperty("amount_cryptocoin") String amountCryptocoin,
            @JsonProperty("cryptocoin_symbol") String cryptocoinSymbol,
            Time time
        ) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Time(@JsonProperty("date_iso8601") String dateIso8601, String unix) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Meta(@JsonProperty("total_count") Integer totalCount) {}
    }
}
