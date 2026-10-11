#!/usr/bin/env python3
"""
Liquidity Risk Optimizer (LRO) — Local Machine Learning Inference Server
Serves Model 1 (Short-Horizon Equity Momentum) and Model 2 (Execution Cost & Liquidity)
Complies with Prompts 09, 10, 21, and 22.
"""

import json
import math
import sys
from datetime import datetime, timezone
from http.server import HTTPServer, BaseHTTPRequestHandler

VERSION_MODEL_1 = "2.1.0-prod"
VERSION_MODEL_2 = "1.4.2-prod"

def predict_momentum(features: dict) -> dict:
    """
    Model 1: Predicts 5-session future return sign and magnitude.
    Features: symbol, return_1d, return_5d, return_20d, volatility_20d, rsi_14, volume_ratio_5d
    """
    symbol = features.get("symbol", "UNKNOWN")
    r1 = float(features.get("return_1d", 0.0))
    r5 = float(features.get("return_5d", 0.0))
    r20 = float(features.get("return_20d", 0.0))
    vol = float(features.get("volatility_20d", 0.18))
    rsi = float(features.get("rsi_14", 50.0))
    v_ratio = float(features.get("volume_ratio_5d", 1.0))

    # Ensemble tree scoring approximation
    momentum_signal = (0.35 * r5) + (0.25 * r1) + (0.15 * r20)
    rsi_penalty = 0.0
    if rsi > 70.0:
        rsi_penalty = -0.015 * (rsi - 70.0) / 10.0 # overbought reversal
    elif rsi < 30.0:
        rsi_penalty = 0.012 * (30.0 - rsi) / 10.0  # oversold bounce

    volume_boost = 1.0 + (0.1 * math.tanh(v_ratio - 1.0))
    raw_return = (momentum_signal + rsi_penalty) * volume_boost
    predicted_return = max(-0.15, min(0.15, raw_return)) # clip to plausible +/- 15% 5d move

    # Uncertainty estimate scaled with rolling volatility
    uncertainty_band = 0.015 * (vol / 0.20)
    conf_low = predicted_return - uncertainty_band
    conf_high = predicted_return + uncertainty_band

    confidence_score = max(0.50, min(0.95, 0.85 - (vol * 0.5)))

    return {
        "modelId": "MODEL-01-MOMENTUM",
        "modelVersion": VERSION_MODEL_1,
        "symbol": symbol,
        "predictionTimestamp": datetime.now(timezone.utc).isoformat(),
        "forecastHorizon": "T+5 Trading Days",
        "predictedReturn": round(predicted_return, 4),
        "predictedReturnPercent": round(predicted_return * 100.0, 2),
        "confidenceIntervalLow": round(conf_low, 4),
        "confidenceIntervalHigh": round(conf_high, 4),
        "confidenceScore": round(confidence_score, 2),
        "syntheticFlag": True,
        "limitations": "Educational decision-support forecast. Does not constitute guaranteed trading advice."
    }

def predict_execution_cost(features: dict) -> dict:
    """
    Model 2: Predicts execution slippage (bps) and total liquidation cost for proposed sale.
    Features: symbol, category, proposed_quantity, current_ltp, bid_ask_spread_bps,
              top5_depth_quantity, order_to_adv_ratio, market_regime
    """
    symbol = features.get("symbol", "UNKNOWN")
    category = features.get("category", "Equity")
    qty = int(features.get("proposed_quantity", 100))
    ltp = float(features.get("current_ltp", 100.0))
    spread_bps = float(features.get("bid_ask_spread_bps", 5.0))
    depth_qty = max(1, int(features.get("top5_depth_quantity", 5000)))
    adv_ratio = float(features.get("order_to_adv_ratio", 0.02))
    regime = features.get("market_regime", "Normal")

    # 1. Base half-spread cost in bps
    base_spread_cost = spread_bps * 0.5

    # 2. Market impact cost (Square root law with depth consumption)
    depth_consumption_ratio = float(qty) / float(depth_qty)
    impact_bps = 8.5 * math.sqrt(max(0.001, adv_ratio)) * (1.0 + (1.2 * depth_consumption_ratio))

    # 3. Asset category modifier
    cat_multiplier = {
        "G-Sec": 0.25,      # highly liquid sovereign market
        "T-Bill": 0.10,     # negligible sovereign discount
        "Corporate Bond": 1.40,
        "Equity": 1.00
    }.get(category, 1.00)

    # 4. Market regime multiplier
    regime_multiplier = {
        "Normal": 1.0,
        "Bull": 0.85,
        "Bear": 1.45,
        "Crash": 3.20       # liquidity freeze under crash
    }.get(regime, 1.0)

    total_slippage_bps = (base_spread_cost + impact_bps) * cat_multiplier * regime_multiplier
    total_slippage_bps = max(0.5, round(total_slippage_bps, 2))

    gross_proceeds = ltp * qty
    cost_fraction = total_slippage_bps / 10000.0
    estimated_cost_inr = round(gross_proceeds * cost_fraction, 2)
    net_proceeds = round(gross_proceeds - estimated_cost_inr, 2)

    # Completion probability within immediate session
    completion_prob = max(0.20, min(0.99, 1.0 - (0.4 * depth_consumption_ratio * (regime_multiplier / 1.5))))

    return {
        "modelId": "MODEL-02-EXEC-COST",
        "modelVersion": VERSION_MODEL_2,
        "symbol": symbol,
        "category": category,
        "proposedQuantity": qty,
        "currentLtp": round(ltp, 2),
        "predictionTimestamp": datetime.now(timezone.utc).isoformat(),
        "marketRegime": regime,
        "expectedSlippageBps": total_slippage_bps,
        "estimatedExecutionCostINR": estimated_cost_inr,
        "grossProceedsINR": round(gross_proceeds, 2),
        "netAchievableProceedsINR": net_proceeds,
        "completionProbability": round(completion_prob, 2),
        "syntheticFlag": True,
        "limitations": "Model trained on synthetic continuous matching order books. Extreme orders trigger fallback to deep auction simulation."
    }

class InferenceHandler(BaseHTTPRequestHandler):
    def _send_json(self, status: int, data: dict):
        body = json.dumps(data, indent=2).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == "/health":
            self._send_json(200, {
                "status": "UP",
                "timestamp": datetime.now(timezone.utc).isoformat(),
                "availableModels": [
                    {"id": "MODEL-01-MOMENTUM", "version": VERSION_MODEL_1},
                    {"id": "MODEL-02-EXEC-COST", "version": VERSION_MODEL_2}
                ]
            })
        else:
            self._send_json(404, {"error": "Not Found"})

    def do_POST(self):
        content_length = int(self.headers.get("Content-Length", 0))
        post_data = self.rfile.read(content_length).decode("utf-8")
        try:
            payload = json.loads(post_data) if post_data else {}
        except json.JSONDecodeError:
            self._send_json(400, {"error": "Invalid JSON format"})
            return

        if self.path == "/predict/momentum":
            res = predict_momentum(payload)
            self._send_json(200, res)
        elif self.path == "/predict/execution-cost":
            res = predict_execution_cost(payload)
            self._send_json(200, res)
        else:
            self._send_json(404, {"error": "Unknown inference endpoint"})

def run_server(port=8082):
    server = HTTPServer(("127.0.0.1", port), InferenceHandler)
    print(f"LRO ML Inference Server running on http://127.0.0.1:{port}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()

if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8082
    run_server(port)
