package com.lro.exchange.service;

import com.lro.common.model.Money;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Continuous Double Auction Matching Engine implementing Prompt 17.
 * Enforces Price-Time priority matching, Level-2 5-depth order books,
 * and trade execution generation.
 */
public class ContinuousMatchingEngine {

    public enum Side { BUY, SELL }
    public enum OrderType { LIMIT, MARKET }
    public enum OrderStatus { ACCEPTED, PARTIALLY_FILLED, FILLED, CANCELLED, REJECTED }

    public static class Order {
        private final String orderId;
        private final String symbol;
        private final Side side;
        private final OrderType orderType;
        private final long initialQuantity;
        private long remainingQuantity;
        private final Money limitPrice;
        private final Instant timestamp;
        private OrderStatus status;

        public Order(String orderId, String symbol, Side side, OrderType orderType, long quantity, Money limitPrice) {
            this.orderId = orderId;
            this.symbol = symbol;
            this.side = side;
            this.orderType = orderType;
            this.initialQuantity = quantity;
            this.remainingQuantity = quantity;
            this.limitPrice = limitPrice;
            this.timestamp = Instant.now();
            this.status = OrderStatus.ACCEPTED;
        }

        public String getOrderId() { return orderId; }
        public String getSymbol() { return symbol; }
        public Side getSide() { return side; }
        public OrderType getOrderType() { return orderType; }
        public long getInitialQuantity() { return initialQuantity; }
        public long getRemainingQuantity() { return remainingQuantity; }
        public long quantity() { return remainingQuantity; }
        public Money getLimitPrice() { return limitPrice; }
        public Instant getTimestamp() { return timestamp; }
        public OrderStatus getStatus() { return status; }

        public void reduceQuantity(long fillQty) {
            this.remainingQuantity -= fillQty;
            this.status = this.remainingQuantity == 0 ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
        }

        public void cancel() {
            this.status = OrderStatus.CANCELLED;
        }
    }

    public record Trade(String symbol, long quantity, Money executedPrice) {}

    public record TradeExecution(
            String tradeId,
            String symbol,
            String buyOrderId,
            String sellOrderId,
            long quantity,
            Money executionPrice,
            Instant executedAt
    ) {}

    public record DepthLevel(
            int level,
            long bidQty,
            Money bidPrice,
            Money askPrice,
            long askQty
    ) {}

    private static class OrderBook {
        // Bids: highest price first, then earliest timestamp
        final PriorityQueue<Order> bids = new PriorityQueue<>(
                Comparator.comparing(Order::getLimitPrice, Comparator.reverseOrder())
                        .thenComparing(Order::getTimestamp)
        );

        // Asks: lowest price first, then earliest timestamp
        final PriorityQueue<Order> asks = new PriorityQueue<>(
                Comparator.comparing(Order::getLimitPrice)
                        .thenComparing(Order::getTimestamp)
        );
    }

    private final Map<String, OrderBook> books = new ConcurrentHashMap<>();
    private final List<TradeExecution> tradeHistory = new CopyOnWriteArrayList<>();
    private final Set<String> orderIds = ConcurrentHashMap.newKeySet();

    public record OrderBookSnapshot(List<Order> bids, List<Order> asks) {}

    public synchronized OrderBookSnapshot getOrderBook(String symbol) {
        OrderBook book = books.get(symbol.toUpperCase());
        if (book == null) return new OrderBookSnapshot(List.of(), List.of());
        return new OrderBookSnapshot(new ArrayList<>(book.bids), new ArrayList<>(book.asks));
    }

    public synchronized List<TradeExecution> submitOrder(Order order) {
        if (!orderIds.add(order.getOrderId())) {
            throw new IllegalArgumentException("Duplicate orderId: " + order.getOrderId());
        }

        OrderBook book = books.computeIfAbsent(order.getSymbol().toUpperCase(), k -> new OrderBook());
        List<TradeExecution> fills = new ArrayList<>();

        if (order.getSide() == Side.BUY) {
            // Match against Asks
            while (order.getRemainingQuantity() > 0 && !book.asks.isEmpty()) {
                Order bestAsk = book.asks.peek();
                if (order.getOrderType() == OrderType.LIMIT && order.getLimitPrice().compareTo(bestAsk.getLimitPrice()) < 0) {
                    break; // Price limit not met
                }

                long fillQty = Math.min(order.getRemainingQuantity(), bestAsk.getRemainingQuantity());
                Money execPrice = bestAsk.getLimitPrice(); // Passive limit order price priority

                order.reduceQuantity(fillQty);
                bestAsk.reduceQuantity(fillQty);

                if (bestAsk.getRemainingQuantity() == 0) {
                    book.asks.poll();
                }

                TradeExecution trade = new TradeExecution(
                        UUID.randomUUID().toString(),
                        order.getSymbol(),
                        order.getOrderId(),
                        bestAsk.getOrderId(),
                        fillQty,
                        execPrice,
                        Instant.now()
                );
                fills.add(trade);
                tradeHistory.add(trade);
            }

            if (order.getRemainingQuantity() > 0 && order.getOrderType() == OrderType.LIMIT) {
                book.bids.add(order);
            }
        } else {
            // Match against Bids
            while (order.getRemainingQuantity() > 0 && !book.bids.isEmpty()) {
                Order bestBid = book.bids.peek();
                if (order.getOrderType() == OrderType.LIMIT && order.getLimitPrice().compareTo(bestBid.getLimitPrice()) > 0) {
                    break;
                }

                long fillQty = Math.min(order.getRemainingQuantity(), bestBid.getRemainingQuantity());
                Money execPrice = bestBid.getLimitPrice();

                order.reduceQuantity(fillQty);
                bestBid.reduceQuantity(fillQty);

                if (bestBid.getRemainingQuantity() == 0) {
                    book.bids.poll();
                }

                TradeExecution trade = new TradeExecution(
                        UUID.randomUUID().toString(),
                        order.getSymbol(),
                        bestBid.getOrderId(),
                        order.getOrderId(),
                        fillQty,
                        execPrice,
                        Instant.now()
                );
                fills.add(trade);
                tradeHistory.add(trade);
            }

            if (order.getRemainingQuantity() > 0 && order.getOrderType() == OrderType.LIMIT) {
                book.asks.add(order);
            }
        }

        return fills;
    }

    public synchronized List<DepthLevel> getLevel2Depth(String symbol) {
        OrderBook book = books.get(symbol.toUpperCase());
        if (book == null) return List.of();

        List<Order> sortedBids = new ArrayList<>(book.bids);
        sortedBids.sort(Comparator.comparing(Order::getLimitPrice, Comparator.reverseOrder()));

        List<Order> sortedAsks = new ArrayList<>(book.asks);
        sortedAsks.sort(Comparator.comparing(Order::getLimitPrice));

        List<DepthLevel> depth = new ArrayList<>();
        int levels = Math.min(5, Math.max(sortedBids.size(), sortedAsks.size()));

        for (int i = 0; i < levels; i++) {
            Money bPrice = i < sortedBids.size() ? sortedBids.get(i).getLimitPrice() : Money.ZERO;
            long bQty = i < sortedBids.size() ? sortedBids.get(i).getRemainingQuantity() : 0L;

            Money aPrice = i < sortedAsks.size() ? sortedAsks.get(i).getLimitPrice() : Money.ZERO;
            long aQty = i < sortedAsks.size() ? sortedAsks.get(i).getRemainingQuantity() : 0L;

            depth.add(new DepthLevel(i + 1, bQty, bPrice, aPrice, aQty));
        }

        return depth;
    }

    public List<TradeExecution> getTradeHistory() {
        return Collections.unmodifiableList(tradeHistory);
    }
}
