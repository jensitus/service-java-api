package org.service.b.crypto.service;

import org.service.b.crypto.dto.CryptoTradeDto;
import org.service.b.crypto.dto.IndicatorDto;
import org.service.b.crypto.dto.LivePriceDto;
import org.service.b.crypto.dto.PriceAlertDto;
import org.service.b.crypto.dto.PriceHistoryDto;
import org.service.b.crypto.dto.PortfolioSummaryDto;
import org.service.b.crypto.dto.SupportResistanceDto;
import org.service.b.crypto.form.AlertForm;
import org.service.b.crypto.form.TradeForm;

import java.util.List;

public interface CryptoService {

    List<LivePriceDto> getLivePrices();

    void refreshPrices();

    PortfolioSummaryDto getPortfolio(Long userId);

    List<CryptoTradeDto> getTradesByUser(Long userId);

    CryptoTradeDto createTrade(Long userId, TradeForm form);

    void deleteTrade(Long tradeId, Long userId);

    List<PriceHistoryDto> getPriceHistory(String coinId, int hours);

    IndicatorDto getIndicators(String coinId);

    SupportResistanceDto getSupportResistance(String coinId);

    List<PriceAlertDto> getAlertsByUser(Long userId);

    PriceAlertDto createAlert(Long userId, AlertForm form);

    PriceAlertDto toggleAlert(Long alertId, Long userId);

    void deleteAlert(Long alertId, Long userId);

}
