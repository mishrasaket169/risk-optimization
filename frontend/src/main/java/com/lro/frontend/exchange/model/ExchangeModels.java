package com.lro.frontend.exchange.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class ExchangeModels {

    public record MarketOverview(
            String marketStatus, // "OPEN (Simulated)", "PRE-OPEN", "CLOSED"
            Instant simulatedClock,
            String activeRegime, // "Normal", "Bull", "Bear", "Crash"
            BigDecimal totalTurnoverINR,
            long totalVolume,
            int advancesCount,
            int declinesCount,
            int unchangedCount,
            List<MarketIndex> indices,
            List<String> recentMarketEvents
    ) {}

    public record MarketIndex(
            String symbol,
            String indexName,
            BigDecimal currentLevel,
            BigDecimal changePercent,
            BigDecimal highLevel,
            BigDecimal lowLevel
    ) {}

    public record InstrumentRecord(
            String isinOrId,
            String symbol,
            String name,
            String category, // "Equity", "Index", "ETF", "G-Sec", "T-Bill", "Corporate Bond", "Futures", "Options", "FX", "Commodity"
            String sector,
            BigDecimal tickSize,
            int lotSize,
            BigDecimal lastTradedPrice,
            BigDecimal changePercent,
            BigDecimal bidPrice,
            BigDecimal askPrice,
            long dayVolume,
            BigDecimal dayTurnoverINR,
            Instant lastQuoteTimestamp,
            // category-specific fields
            BigDecimal couponOrYieldPercent,
            LocalDate maturityOrExpiryDate,
            BigDecimal strikePrice,
            String optionType // "CE", "PE", or null
    ) {}

    public record OrderBookLevel(
            int levelNumber,
            int bidOrdersCount,
            long bidQuantity,
            BigDecimal bidPrice,
            BigDecimal askPrice,
            long askQuantity,
            int askOrdersCount
    ) {}

    public record OrderRecord(
            String orderId,
            String clientRefId,
            Instant placedAt,
            String symbol,
            String side, // "BUY", "SELL"
            String orderType, // "LIMIT", "MARKET"
            long totalQuantity,
            long filledQuantity,
            BigDecimal limitPrice,
            BigDecimal averageFillPrice,
            String status, // "Accepted", "Partially Filled", "Filled", "Cancelled", "Rejected"
            String timeInForce, // "DAY", "IOC"
            BigDecimal estimatedNotionalINR
    ) {}

    public record TradeRecord(
            String tradeId,
            String orderId,
            Instant executedAt,
            String symbol,
            String side,
            long quantity,
            BigDecimal price,
            BigDecimal grossValueINR
    ) {}
}
