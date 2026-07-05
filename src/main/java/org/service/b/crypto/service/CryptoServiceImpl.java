package org.service.b.crypto.service;

import jakarta.annotation.PostConstruct;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.service.b.crypto.dto.*;
import org.service.b.crypto.form.AlertForm;
import org.service.b.crypto.form.TradeForm;
import org.service.b.crypto.model.*;
import org.service.b.crypto.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CryptoServiceImpl implements CryptoService {

    private static final Logger logger = LoggerFactory.getLogger(CryptoServiceImpl.class);

    private static final List<String> TRACKED_COINS = List.of("bitcoin", "litecoin", "ethereum", "ripple");
    private static final Map<String, String> COIN_NAMES = Map.of(
        "bitcoin", "Bitcoin",
        "litecoin", "Litecoin",
        "ethereum", "Ethereum",
        "ripple", "XRP"
    );
    private static final String COINGECKO_URL =
        "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,litecoin,ethereum,ripple" +
        "&vs_currencies=usd,eur&include_24hr_change=true&include_market_cap=true";

    private final CryptoPriceCacheRepo priceRepo;
    private final CryptoPriceHistoryRepo historyRepo;
    private final CryptoTradeRepo tradeRepo;
    private final PriceAlertRepo alertRepo;
    private final RestClient restClient;

    public CryptoServiceImpl(CryptoPriceCacheRepo priceRepo, CryptoPriceHistoryRepo historyRepo,
                              CryptoTradeRepo tradeRepo, PriceAlertRepo alertRepo,
                              RestClient restClient) {
        this.priceRepo = priceRepo;
        this.historyRepo = historyRepo;
        this.tradeRepo = tradeRepo;
        this.alertRepo = alertRepo;
        this.restClient = restClient;
    }

    @PostConstruct
    public void initPrices() {
        refreshPrices();
    }

    @Override
    public List<LivePriceDto> getLivePrices() {
        return priceRepo.findAllByOrderByCoinIdAsc().stream()
                        .map(this::toDto)
                        .toList();
    }

    @Override
    @Scheduled(fixedDelay = 120_000)
    @SchedulerLock(name = "refreshCryptoPrices", lockAtMostFor = "PT1M50S", lockAtLeastFor = "PT10S")
    public void refreshPrices() {
        try {
            Map<String, Map<String, Double>> response = restClient.get()
                .uri(COINGECKO_URL)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

            if (response == null) return;

            LocalDateTime now = LocalDateTime.now();

            for (String coinId : TRACKED_COINS) {
                Map<String, Double> data = response.get(coinId);
                if (data == null) continue;

                BigDecimal price = BigDecimal.valueOf(data.getOrDefault("usd", 0.0));

                // Update live cache
                CryptoPriceCache cache = priceRepo.findByCoinId(coinId)
                    .orElseGet(() -> {
                        CryptoPriceCache c = new CryptoPriceCache();
                        c.setCoinId(coinId);
                        c.setCoinName(COIN_NAMES.getOrDefault(coinId, coinId));
                        return c;
                    });
                BigDecimal priceEur = BigDecimal.valueOf(data.getOrDefault("eur", 0.0));
                cache.setPriceUsd(price);
                cache.setPriceEur(priceEur);
                cache.setPriceChange24h(BigDecimal.valueOf(data.getOrDefault("usd_24h_change", 0.0)));
                cache.setPriceChange24hEur(BigDecimal.valueOf(data.getOrDefault("eur_24h_change", 0.0)));
                Double mcap = data.get("usd_market_cap");
                if (mcap != null) cache.setMarketCapUsd(BigDecimal.valueOf(mcap));
                Double mcapEur = data.get("eur_market_cap");
                if (mcapEur != null) cache.setMarketCapEur(BigDecimal.valueOf(mcapEur));
                cache.setFetchedAt(now);
                priceRepo.save(cache);

                // Save to history
                CryptoPriceHistory history = new CryptoPriceHistory();
                history.setCoinId(coinId);
                history.setPriceUsd(price);
                history.setPriceEur(priceEur);
                history.setRecordedAt(now);
                historyRepo.save(history);
            }

            // Keep only last 30 days of history
            LocalDateTime cutoff = now.minusDays(30);
            for (String coinId : TRACKED_COINS) {
                historyRepo.deleteByCoinIdAndRecordedAtBefore(coinId, cutoff);
            }

            checkAlerts();
            logger.info("Crypto prices refreshed at {}", now);
        } catch (Exception e) {
            logger.error("Failed to refresh crypto prices: {}", e.getMessage());
        }
    }

    @Override
    public List<PriceHistoryDto> getPriceHistory(String coinId, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return historyRepo.findByCoinIdAndRecordedAtAfterOrderByRecordedAtAsc(coinId, since)
                          .stream()
                          .map(h -> {
                              PriceHistoryDto dto = new PriceHistoryDto();
                              dto.setRecordedAt(h.getRecordedAt());
                              dto.setPriceUsd(h.getPriceUsd());
                              dto.setPriceEur(h.getPriceEur());
                              return dto;
                          })
                          .toList();
    }

    @Override
    public IndicatorDto getIndicators(String coinId) {
        // Use last 50 data points (enough for RSI-14 + MA-30)
        List<CryptoPriceHistory> raw = historyRepo.findLatestByCoinId(coinId, PageRequest.of(0, 50));

        IndicatorDto dto = new IndicatorDto();
        dto.setCoinId(coinId);
        dto.setCoinName(COIN_NAMES.getOrDefault(coinId, coinId));
        dto.setDataPoints(raw.size());

        if (raw.isEmpty()) return dto;

        // Reverse to chronological order (oldest → newest)
        List<BigDecimal> prices = new ArrayList<>();
        List<BigDecimal> pricesEur = new ArrayList<>();
        for (int i = raw.size() - 1; i >= 0; i--) {
            prices.add(raw.get(i).getPriceUsd());
            if (raw.get(i).getPriceEur() != null) pricesEur.add(raw.get(i).getPriceEur());
        }

        dto.setCurrentPrice(prices.get(prices.size() - 1));
        dto.setMa7(calculateMA(prices, 7));
        dto.setMa14(calculateMA(prices, 14));
        dto.setMa30(calculateMA(prices, 30));
        dto.setRsi14(calculateRSI(prices, 14));

        if (!pricesEur.isEmpty()) {
            dto.setCurrentPriceEur(pricesEur.get(pricesEur.size() - 1));
            dto.setMa7Eur(calculateMA(pricesEur, 7));
            dto.setMa14Eur(calculateMA(pricesEur, 14));
            dto.setMa30Eur(calculateMA(pricesEur, 30));
        }

        // Interpretation signals
        if (dto.getRsi14() != null) {
            double rsi = dto.getRsi14().doubleValue();
            if (rsi < 30) dto.setRsiSignal("OVERSOLD");
            else if (rsi > 70) dto.setRsiSignal("OVERBOUGHT");
            else dto.setRsiSignal("NEUTRAL");
        }

        if (dto.getMa7() != null && dto.getMa14() != null) {
            dto.setMaSignal(dto.getMa7().compareTo(dto.getMa14()) > 0 ? "BULLISH" : "BEARISH");
        }

        return dto;
    }

    @Override
    public SupportResistanceDto getSupportResistance(String coinId) {
        // Use up to 200 points (~6-7 hours of 2-min data) for S/R detection
        List<CryptoPriceHistory> raw = historyRepo.findLatestByCoinId(coinId, PageRequest.of(0, 200));

        SupportResistanceDto dto = new SupportResistanceDto();
        dto.setCoinId(coinId);
        dto.setCoinName(COIN_NAMES.getOrDefault(coinId, coinId));
        dto.setDataPoints(raw.size());
        dto.setResistanceLevels(List.of());
        dto.setSupportLevels(List.of());

        if (raw.size() < 15) return dto;

        // Reverse to chronological order (oldest → newest)
        List<BigDecimal> prices = new ArrayList<>();
        for (int i = raw.size() - 1; i >= 0; i--) prices.add(raw.get(i).getPriceUsd());

        BigDecimal currentPrice = prices.get(prices.size() - 1);
        dto.setCurrentPrice(currentPrice);

        int window = 5; // require 5 points on each side to qualify as a swing
        List<BigDecimal> swingHighs = findSwings(prices, window, true);
        List<BigDecimal> swingLows  = findSwings(prices, window, false);

        // Cluster tolerance: 1% of current price
        double tolerancePct = 0.01;
        List<BigDecimal> resistance = clusterLevels(swingHighs, tolerancePct);
        List<BigDecimal> support    = clusterLevels(swingLows,  tolerancePct);

        // Only keep levels above current price for resistance, below for support
        resistance = resistance.stream()
            .filter(l -> l.compareTo(currentPrice) > 0)
            .sorted()
            .limit(3)
            .toList();
        support = support.stream()
            .filter(l -> l.compareTo(currentPrice) < 0)
            .sorted(java.util.Comparator.reverseOrder())
            .limit(3)
            .toList();

        dto.setResistanceLevels(resistance);
        dto.setSupportLevels(support);
        if (!resistance.isEmpty()) dto.setNearestResistance(resistance.get(0));
        if (!support.isEmpty())    dto.setNearestSupport(support.get(0));
        return dto;
    }

    private List<BigDecimal> findSwings(List<BigDecimal> prices, int window, boolean highs) {
        List<BigDecimal> result = new ArrayList<>();
        for (int i = window; i < prices.size() - window; i++) {
            BigDecimal p = prices.get(i);
            boolean isSwing = true;
            for (int j = i - window; j <= i + window; j++) {
                if (j == i) continue;
                int cmp = prices.get(j).compareTo(p);
                if (highs ? cmp >= 0 : cmp <= 0) { isSwing = false; break; }
            }
            if (isSwing) result.add(p);
        }
        return result;
    }

    private List<BigDecimal> clusterLevels(List<BigDecimal> levels, double tolerancePct) {
        if (levels.isEmpty()) return List.of();
        List<BigDecimal> sorted = new ArrayList<>(levels);
        sorted.sort(java.util.Comparator.naturalOrder());

        List<List<BigDecimal>> clusters = new ArrayList<>();
        List<BigDecimal> current = new ArrayList<>();
        current.add(sorted.get(0));

        for (int i = 1; i < sorted.size(); i++) {
            BigDecimal prev = current.get(current.size() - 1);
            BigDecimal diff = sorted.get(i).subtract(prev).abs();
            BigDecimal threshold = prev.multiply(BigDecimal.valueOf(tolerancePct));
            if (diff.compareTo(threshold) <= 0) {
                current.add(sorted.get(i));
            } else {
                clusters.add(current);
                current = new ArrayList<>();
                current.add(sorted.get(i));
            }
        }
        clusters.add(current);

        // Average each cluster; prefer clusters with more touches (more significant)
        return clusters.stream()
            .sorted((a, b) -> b.size() - a.size())
            .map(cluster -> cluster.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(cluster.size()), 2, RoundingMode.HALF_UP))
            .toList();
    }

    // RSI using Wilder's smoothing method
    private BigDecimal calculateRSI(List<BigDecimal> prices, int period) {
        if (prices.size() < period + 1) return null;

        List<BigDecimal> gains = new ArrayList<>();
        List<BigDecimal> losses = new ArrayList<>();
        for (int i = 1; i < prices.size(); i++) {
            BigDecimal change = prices.get(i).subtract(prices.get(i - 1));
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                gains.add(change);
                losses.add(BigDecimal.ZERO);
            } else {
                gains.add(BigDecimal.ZERO);
                losses.add(change.abs());
            }
        }

        BigDecimal avgGain = gains.subList(0, period).stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        BigDecimal avgLoss = losses.subList(0, period).stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);

        for (int i = period; i < gains.size(); i++) {
            avgGain = avgGain.multiply(BigDecimal.valueOf(period - 1))
                .add(gains.get(i))
                .divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
            avgLoss = avgLoss.multiply(BigDecimal.valueOf(period - 1))
                .add(losses.get(i))
                .divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        }

        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.valueOf(100);
        BigDecimal rs = avgGain.divide(avgLoss, 8, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(100)
            .subtract(BigDecimal.valueOf(100)
                .divide(BigDecimal.ONE.add(rs), 2, RoundingMode.HALF_UP));
    }

    private BigDecimal calculateMA(List<BigDecimal> prices, int period) {
        if (prices.size() < period) return null;
        List<BigDecimal> window = prices.subList(prices.size() - period, prices.size());
        return window.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(period), 2, RoundingMode.HALF_UP);
    }

    private void checkAlerts() {
        List<PriceAlert> activeAlerts = alertRepo.findByActiveAndTriggeredFalse(true);
        for (PriceAlert alert : activeAlerts) {
            Optional<CryptoPriceCache> price = priceRepo.findByCoinId(alert.getCoinId());
            if (price.isEmpty()) continue;
            BigDecimal current = price.get().getPriceUsd();
            boolean triggered = "ABOVE".equals(alert.getCondition())
                ? current.compareTo(alert.getTargetPrice()) >= 0
                : current.compareTo(alert.getTargetPrice()) <= 0;
            if (triggered) {
                alert.setTriggered(true);
                alert.setActive(false);
                alertRepo.save(alert);
                logger.info("Alert triggered: {} {} {} (current: {})",
                    alert.getCoinId(), alert.getCondition(), alert.getTargetPrice(), current);
            }
        }
    }

    @Override
    public List<CryptoTradeDto> getTradesByUser(Long userId) {
        return tradeRepo.findByUserIdOrderByTradedAtDesc(userId).stream()
                        .map(this::toTradeDto).toList();
    }

    @Override
    public CryptoTradeDto createTrade(Long userId, TradeForm form) {
        CryptoTrade trade = new CryptoTrade();
        trade.setUserId(userId);
        trade.setCoinId(form.getCoinId());
        trade.setSide(form.getSide().toUpperCase());
        trade.setQuantity(form.getQuantity());
        trade.setPricePerUnit(form.getPricePerUnit());
        trade.setFee(form.getFee() != null ? form.getFee() : BigDecimal.ZERO);
        trade.setCurrency(form.getCurrency() != null ? form.getCurrency().toUpperCase() : "EUR");
        trade.setSource("MANUAL");
        trade.setTradedAt(form.getTradedAt());
        trade.setNote(form.getNote());
        trade.setCreatedAt(LocalDateTime.now());
        return toTradeDto(tradeRepo.save(trade));
    }

    @Override
    public void deleteTrade(Long tradeId, Long userId) {
        tradeRepo.findById(tradeId).ifPresent(t -> {
            if (t.getUserId().equals(userId)) tradeRepo.delete(t);
        });
    }

    @Override
    public List<PriceAlertDto> getAlertsByUser(Long userId) {
        return alertRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                        .map(this::toAlertDto).toList();
    }

    @Override
    public PriceAlertDto createAlert(Long userId, AlertForm form) {
        PriceAlert alert = new PriceAlert();
        alert.setUserId(userId);
        alert.setCoinId(form.getCoinId());
        alert.setTargetPrice(form.getTargetPrice());
        alert.setCondition(form.getCondition().toUpperCase());
        alert.setActive(true);
        alert.setTriggered(false);
        alert.setCreatedAt(LocalDateTime.now());
        return toAlertDto(alertRepo.save(alert));
    }

    @Override
    public PriceAlertDto toggleAlert(Long alertId, Long userId) {
        PriceAlert alert = alertRepo.findById(alertId)
            .filter(a -> a.getUserId().equals(userId))
            .orElseThrow(() -> new RuntimeException("Alert not found"));
        alert.setActive(!alert.getActive());
        if (alert.getActive()) alert.setTriggered(false);
        return toAlertDto(alertRepo.save(alert));
    }

    @Override
    public void deleteAlert(Long alertId, Long userId) {
        alertRepo.findById(alertId).ifPresent(a -> {
            if (a.getUserId().equals(userId)) alertRepo.delete(a);
        });
    }

    private LivePriceDto toDto(CryptoPriceCache c) {
        LivePriceDto dto = new LivePriceDto();
        dto.setCoinId(c.getCoinId());
        dto.setCoinName(c.getCoinName());
        dto.setPriceUsd(c.getPriceUsd());
        dto.setPriceChange24h(c.getPriceChange24h());
        dto.setMarketCapUsd(c.getMarketCapUsd());
        dto.setPriceEur(c.getPriceEur());
        dto.setPriceChange24hEur(c.getPriceChange24hEur());
        dto.setMarketCapEur(c.getMarketCapEur());
        dto.setFetchedAt(c.getFetchedAt());
        return dto;
    }

    @Override
    public PortfolioSummaryDto getPortfolio(Long userId) {
        List<CryptoTrade> trades = tradeRepo.findByUserIdOrderByTradedAtDesc(userId);
        Map<String, List<CryptoTrade>> byCoin = trades.stream()
            .collect(Collectors.groupingBy(CryptoTrade::getCoinId));

        List<PortfolioPositionDto> positions = new ArrayList<>();
        for (Map.Entry<String, List<CryptoTrade>> entry : byCoin.entrySet()) {
            String coinId = entry.getKey();
            List<CryptoTrade> coinTrades = entry.getValue();

            BigDecimal totalBuyQty = BigDecimal.ZERO;
            BigDecimal totalBuyValue = BigDecimal.ZERO;
            BigDecimal totalSellQty = BigDecimal.ZERO;
            BigDecimal totalFees = BigDecimal.ZERO;

            for (CryptoTrade t : coinTrades) {
                if ("BUY".equals(t.getSide())) {
                    totalBuyQty = totalBuyQty.add(t.getQuantity());
                    totalBuyValue = totalBuyValue.add(t.getQuantity().multiply(t.getPricePerUnit()));
                } else {
                    totalSellQty = totalSellQty.add(t.getQuantity());
                }
                if (t.getFee() != null) totalFees = totalFees.add(t.getFee());
            }

            BigDecimal netQty = totalBuyQty.subtract(totalSellQty);
            if (netQty.compareTo(BigDecimal.ZERO) <= 0 || totalBuyQty.compareTo(BigDecimal.ZERO) == 0) continue;

            BigDecimal avgBuyPrice = totalBuyValue.divide(totalBuyQty, 8, RoundingMode.HALF_UP);
            CryptoPriceCache cache = priceRepo.findByCoinId(coinId).orElse(null);
            BigDecimal currentPrice = cache != null ? cache.getPriceUsd() : BigDecimal.ZERO;
            BigDecimal currentPriceEur = cache != null && cache.getPriceEur() != null ? cache.getPriceEur() : BigDecimal.ZERO;
            BigDecimal totalInvested = netQty.multiply(avgBuyPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal currentValue = netQty.multiply(currentPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal currentValueEur = netQty.multiply(currentPriceEur).setScale(2, RoundingMode.HALF_UP);
            BigDecimal unrealizedPnl = currentValue.subtract(totalInvested);
            BigDecimal unrealizedPnlPct = totalInvested.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : unrealizedPnl.multiply(BigDecimal.valueOf(100)).divide(totalInvested, 2, RoundingMode.HALF_UP);
            BigDecimal unrealizedPnlEur = currentValueEur.subtract(totalInvested);
            BigDecimal unrealizedPnlPctEur = totalInvested.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : unrealizedPnlEur.multiply(BigDecimal.valueOf(100)).divide(totalInvested, 2, RoundingMode.HALF_UP);

            PortfolioPositionDto pos = new PortfolioPositionDto();
            pos.setCoinId(coinId);
            pos.setCoinName(COIN_NAMES.getOrDefault(coinId, coinId));
            pos.setNetQuantity(netQty.stripTrailingZeros());
            pos.setAvgBuyPrice(avgBuyPrice.setScale(2, RoundingMode.HALF_UP));
            pos.setCurrentPrice(currentPrice);
            pos.setCurrentPriceEur(currentPriceEur);
            pos.setTotalInvested(totalInvested);
            pos.setCurrentValue(currentValue);
            pos.setCurrentValueEur(currentValueEur);
            pos.setUnrealizedPnl(unrealizedPnl);
            pos.setUnrealizedPnlPct(unrealizedPnlPct);
            pos.setUnrealizedPnlEur(unrealizedPnlEur);
            pos.setUnrealizedPnlPctEur(unrealizedPnlPctEur);
            pos.setTotalFees(totalFees);
            positions.add(pos);
        }

        BigDecimal totalInvested = positions.stream().map(PortfolioPositionDto::getTotalInvested)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCurrentValue = positions.stream().map(PortfolioPositionDto::getCurrentValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCurrentValueEur = positions.stream().map(PortfolioPositionDto::getCurrentValueEur)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPnl = totalCurrentValue.subtract(totalInvested);
        BigDecimal totalPnlPct = totalInvested.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
            : totalPnl.multiply(BigDecimal.valueOf(100)).divide(totalInvested, 2, RoundingMode.HALF_UP);
        BigDecimal totalPnlEur = totalCurrentValueEur.subtract(totalInvested);
        BigDecimal totalPnlPctEur = totalInvested.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
            : totalPnlEur.multiply(BigDecimal.valueOf(100)).divide(totalInvested, 2, RoundingMode.HALF_UP);

        PortfolioSummaryDto summary = new PortfolioSummaryDto();
        summary.setPositions(positions);
        summary.setTotalInvested(totalInvested.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalCurrentValue(totalCurrentValue.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalCurrentValueEur(totalCurrentValueEur.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalUnrealizedPnl(totalPnl.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalUnrealizedPnlPct(totalPnlPct);
        summary.setTotalUnrealizedPnlEur(totalPnlEur.setScale(2, RoundingMode.HALF_UP));
        summary.setTotalUnrealizedPnlPctEur(totalPnlPctEur);
        return summary;
    }

    private CryptoTradeDto toTradeDto(CryptoTrade t) {
        CryptoTradeDto dto = new CryptoTradeDto();
        dto.setId(t.getId());
        dto.setCoinId(t.getCoinId());
        dto.setSide(t.getSide());
        dto.setQuantity(t.getQuantity());
        dto.setPricePerUnit(t.getPricePerUnit());
        dto.setFee(t.getFee());
        dto.setTotalValue(t.getQuantity().multiply(t.getPricePerUnit()));
        dto.setTradedAt(t.getTradedAt());
        dto.setNote(t.getNote());
        dto.setCurrency(t.getCurrency() != null ? t.getCurrency() : "EUR");
        dto.setSource(t.getSource() != null ? t.getSource() : "MANUAL");
        priceRepo.findByCoinId(t.getCoinId()).ifPresent(cache -> {
            dto.setCurrentPriceUsd(cache.getPriceUsd());
            dto.setCurrentPriceEur(cache.getPriceEur());
            if ("BUY".equals(t.getSide())) {
                BigDecimal fee = t.getFee() != null ? t.getFee() : BigDecimal.ZERO;
                // P&L uses the current price in the same currency as the trade entry
                BigDecimal currentInTradeCurrency = "EUR".equals(dto.getCurrency())
                    && cache.getPriceEur() != null ? cache.getPriceEur() : cache.getPriceUsd();
                dto.setUnrealizedPnl(currentInTradeCurrency.subtract(t.getPricePerUnit())
                    .multiply(t.getQuantity()).subtract(fee).setScale(2, RoundingMode.HALF_UP));
            }
        });
        return dto;
    }

    private PriceAlertDto toAlertDto(PriceAlert a) {
        PriceAlertDto dto = new PriceAlertDto();
        dto.setId(a.getId());
        dto.setCoinId(a.getCoinId());
        dto.setTargetPrice(a.getTargetPrice());
        dto.setCondition(a.getCondition());
        dto.setActive(a.getActive());
        dto.setTriggered(a.getTriggered());
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }

}
