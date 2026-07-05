package org.service.b.crypto.controller;

import jakarta.validation.Valid;
import org.service.b.auth.serviceimpl.UserPrinciple;
import org.service.b.crypto.dto.CryptoTradeDto;
import org.service.b.crypto.dto.IndicatorDto;
import org.service.b.crypto.dto.LivePriceDto;
import org.service.b.crypto.dto.PriceAlertDto;
import org.service.b.crypto.dto.PriceHistoryDto;
import org.service.b.crypto.dto.PortfolioSummaryDto;
import org.service.b.crypto.dto.SupportResistanceDto;
import org.service.b.crypto.dto.BitpandaTestResultDto;
import org.service.b.crypto.dto.BitpandaCredentialStatusDto;
import org.service.b.crypto.dto.BitpandaSyncResultDto;
import org.service.b.crypto.form.AlertForm;
import org.service.b.crypto.form.TradeForm;
import org.service.b.crypto.form.BitpandaKeyForm;
import org.service.b.crypto.service.CryptoService;
import org.service.b.crypto.service.BitpandaService;
import org.service.b.crypto.service.BitpandaException;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/service/crypto")
public class CryptoRestApi {

    private final CryptoService cryptoService;
    private final BitpandaService bitpandaService;

    public CryptoRestApi(CryptoService cryptoService, BitpandaService bitpandaService) {
        this.cryptoService = cryptoService;
        this.bitpandaService = bitpandaService;
    }

    @GetMapping("/prices")
    public ResponseEntity<List<LivePriceDto>> getPrices() {
        return ResponseEntity.ok(cryptoService.getLivePrices());
    }

    @PostMapping("/prices/refresh")
    public ResponseEntity<Void> refreshPrices() {
        cryptoService.refreshPrices();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history/{coinId}")
    public ResponseEntity<List<PriceHistoryDto>> getHistory(@PathVariable String coinId,
                                                             @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(cryptoService.getPriceHistory(coinId, hours));
    }

    @GetMapping("/indicators/{coinId}")
    public ResponseEntity<IndicatorDto> getIndicators(@PathVariable String coinId) {
        return ResponseEntity.ok(cryptoService.getIndicators(coinId));
    }

    @GetMapping("/sr/{coinId}")
    public ResponseEntity<SupportResistanceDto> getSupportResistance(@PathVariable String coinId) {
        return ResponseEntity.ok(cryptoService.getSupportResistance(coinId));
    }

    @GetMapping("/portfolio")
    public ResponseEntity<PortfolioSummaryDto> getPortfolio(@AuthenticationPrincipal UserPrinciple user) {
        return ResponseEntity.ok(cryptoService.getPortfolio(user.getId()));
    }

    // Ad-hoc test: verify a Bitpanda API key and preview recent trades without storing it.
    @PostMapping("/bitpanda/test")
    public ResponseEntity<BitpandaTestResultDto> testBitpanda(@AuthenticationPrincipal UserPrinciple user,
                                                              @Valid @RequestBody BitpandaKeyForm form) {
        return ResponseEntity.ok(bitpandaService.testConnection(form.getApiKey()));
    }

    // Whether this user has a stored Bitpanda key, and when they last synced.
    @GetMapping("/bitpanda/credential")
    public ResponseEntity<BitpandaCredentialStatusDto> bitpandaStatus(@AuthenticationPrincipal UserPrinciple user) {
        return ResponseEntity.ok(bitpandaService.getStatus(user.getId()));
    }

    // Validate and store (encrypted) the user's Bitpanda API key.
    @PostMapping("/bitpanda/credential")
    public ResponseEntity<?> saveBitpandaCredential(@AuthenticationPrincipal UserPrinciple user,
                                                    @Valid @RequestBody BitpandaKeyForm form) {
        try {
            return ResponseEntity.ok(bitpandaService.saveCredential(user.getId(), form.getApiKey()));
        } catch (BitpandaException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/bitpanda/credential")
    public ResponseEntity<Void> deleteBitpandaCredential(@AuthenticationPrincipal UserPrinciple user) {
        bitpandaService.deleteCredential(user.getId());
        return ResponseEntity.noContent().build();
    }

    // Pull trades from Bitpanda and import new ones into the journal (idempotent).
    @PostMapping("/bitpanda/sync")
    public ResponseEntity<?> syncBitpanda(@AuthenticationPrincipal UserPrinciple user) {
        try {
            return ResponseEntity.ok(bitpandaService.sync(user.getId()));
        } catch (BitpandaException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/trades")
    public ResponseEntity<List<CryptoTradeDto>> getTrades(@AuthenticationPrincipal UserPrinciple user) {
        return ResponseEntity.ok(cryptoService.getTradesByUser(user.getId()));
    }

    @PostMapping("/trades")
    public ResponseEntity<CryptoTradeDto> createTrade(@AuthenticationPrincipal UserPrinciple user,
                                                       @Valid @RequestBody TradeForm form) {
        return new ResponseEntity<>(cryptoService.createTrade(user.getId(), form), HttpStatus.CREATED);
    }

    @DeleteMapping("/trades/{id}")
    public ResponseEntity<Void> deleteTrade(@AuthenticationPrincipal UserPrinciple user,
                                             @PathVariable Long id) {
        cryptoService.deleteTrade(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<PriceAlertDto>> getAlerts(@AuthenticationPrincipal UserPrinciple user) {
        return ResponseEntity.ok(cryptoService.getAlertsByUser(user.getId()));
    }

    @PostMapping("/alerts")
    public ResponseEntity<PriceAlertDto> createAlert(@AuthenticationPrincipal UserPrinciple user,
                                                      @Valid @RequestBody AlertForm form) {
        return new ResponseEntity<>(cryptoService.createAlert(user.getId(), form), HttpStatus.CREATED);
    }

    @PutMapping("/alerts/{id}/toggle")
    public ResponseEntity<PriceAlertDto> toggleAlert(@AuthenticationPrincipal UserPrinciple user,
                                                      @PathVariable Long id) {
        return ResponseEntity.ok(cryptoService.toggleAlert(id, user.getId()));
    }

    @DeleteMapping("/alerts/{id}")
    public ResponseEntity<Void> deleteAlert(@AuthenticationPrincipal UserPrinciple user,
                                             @PathVariable Long id) {
        cryptoService.deleteAlert(id, user.getId());
        return ResponseEntity.noContent().build();
    }

}
