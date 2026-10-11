package com.lro.frontend.exchange.service;

import com.lro.frontend.exchange.model.ExchangeModels.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ExchangeService {
    MarketOverview getMarketOverview();
    List<InstrumentRecord> getInstruments(String categoryFilter, String query);
    Optional<InstrumentRecord> getInstrument(String symbol);
    List<OrderBookLevel> getOrderBookDepth(String symbol);
    List<OrderRecord> getOrderBlotter(String statusFilter);
    List<TradeRecord> getTradeBlotter();

    // Order placement & simulation actions
    OrderRecord submitSimulatedOrder(String symbol, String side, String orderType, long quantity, BigDecimal price, String tif);
    boolean cancelSimulatedOrder(String orderId);
    void updateMarketRegime(String newRegime, int speedFactor, long seed);
}
