package com.lro.ml.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lro.common.model.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Model 2 Adapter implementing Prompt 22:
 * Connects to XGBoost Execution Cost & Slippage microservice with deterministic order-book fallback.
 */
public class Model2ExecutionCostAdapter {

    private static final Logger log = LoggerFactory.getLogger(Model2ExecutionCostAdapter.class);

    public record ExecutionEstimate(
            String modelId,
            String version,
            String symbol,
            String category,
            long proposedQuantity,
            BigDecimal expectedSlippageBps,
            Money estimatedExecutionCostINR,
            Money netAchievableProceedsINR,
            BigDecimal completionProbability,
            String valuationEngine, // "Python XGBoost Microservice" or "Deterministic Order-Book Depth Fallback"
            Instant timestamp,
            String limitationsNotice
    ) {
        public double slippageBps() {
            return expectedSlippageBps.doubleValue();
        }

        public String provenance() {
            return valuationEngine;
        }
    }

    private final String inferenceUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public Model2ExecutionCostAdapter() {
        this(System.getenv().getOrDefault("LRO_ML_INFERENCE_URL", "http://127.0.0.1:8082"));
    }

    public Model2ExecutionCostAdapter(String baseUrl) {
        this.inferenceUrl = baseUrl + "/predict/execution-cost";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(500))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public ExecutionEstimate estimateCost(String symbol, long quantity, Money ltp) {
        return estimate(symbol, "Equity", quantity, ltp, new BigDecimal("4.00"), 20000L, new BigDecimal("0.02"), "Normal");
    }

    public ExecutionEstimate estimate(String symbol, String category, long quantity, Money ltp,
                                      BigDecimal spreadBps, long depthLevel5Qty, BigDecimal advRatio, String regime) {
        Map<String, Object> req = new HashMap<>();
        req.put("symbol", symbol);
        req.put("category", category);
        req.put("proposed_quantity", quantity);
        req.put("current_ltp", ltp.getAmount().doubleValue());
        req.put("bid_ask_spread_bps", spreadBps.doubleValue());
        req.put("top5_depth_quantity", depthLevel5Qty);
        req.put("order_to_adv_ratio", advRatio.doubleValue());
        req.put("market_regime", regime);

        try {
            String jsonPayload = objectMapper.writeValueAsString(req);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(inferenceUrl))
                    .timeout(Duration.ofMillis(1200))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return new ExecutionEstimate(
                        root.path("modelId").asText("MODEL-02-EXEC-COST"),
                        root.path("modelVersion").asText("1.4.2-prod"),
                        symbol,
                        category,
                        quantity,
                        BigDecimal.valueOf(root.path("expectedSlippageBps").asDouble()).setScale(2, RoundingMode.HALF_EVEN),
                        Money.of(BigDecimal.valueOf(root.path("estimatedExecutionCostINR").asDouble())),
                        Money.of(BigDecimal.valueOf(root.path("netAchievableProceedsINR").asDouble())),
                        BigDecimal.valueOf(root.path("completionProbability").asDouble()).setScale(2, RoundingMode.HALF_EVEN),
                        "Python XGBoost Microservice",
                        Instant.now(),
                        root.path("limitations").asText()
                );
            }
        } catch (Exception ex) {
            log.debug("Model 2 HTTP service call failed ({}), activating deterministic fallback", ex.getMessage());
        }

        // Deterministic Fallback: Square-root law + depth impact
        double grossProceeds = ltp.getAmount().doubleValue() * quantity;
        double halfSpreadBps = spreadBps.doubleValue() * 0.5;
        double depthImpactBps = 12.0 * Math.sqrt(Math.max(0.001, advRatio.doubleValue()));
        double totalSlippageBps = halfSpreadBps + depthImpactBps;

        double cost = grossProceeds * (totalSlippageBps / 10000.0);
        double net = grossProceeds - cost;

        return new ExecutionEstimate(
                "MODEL-02-EXEC-COST",
                "1.4.2-fallback",
                symbol,
                category,
                quantity,
                BigDecimal.valueOf(totalSlippageBps).setScale(2, RoundingMode.HALF_EVEN),
                Money.of(BigDecimal.valueOf(cost)),
                Money.of(BigDecimal.valueOf(net)),
                new BigDecimal("0.90"),
                "Deterministic Order-Book Depth Fallback",
                Instant.now(),
                "Deterministic fallback estimate based on spread and order depth. Not guaranteed broker execution."
        );
    }
}
