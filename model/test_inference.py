#!/usr/bin/env python3
import json
from inference_server import predict_momentum, predict_execution_cost

def test_model_1():
    features = {
        "symbol": "RELIANCE",
        "return_1d": 0.012,
        "return_5d": 0.035,
        "return_20d": 0.065,
        "volatility_20d": 0.16,
        "rsi_14": 58.5,
        "volume_ratio_5d": 1.25
    }
    pred = predict_momentum(features)
    print("Model 1 Output:", json.dumps(pred, indent=2))
    assert pred["modelId"] == "MODEL-01-MOMENTUM"
    assert "predictedReturnPercent" in pred
    assert pred["predictedReturnPercent"] > 0
    print("Model 1 Test Passed!")

def test_model_2():
    features = {
        "symbol": "RELIANCE",
        "category": "Equity",
        "proposed_quantity": 5000,
        "current_ltp": 2840.50,
        "bid_ask_spread_bps": 3.5,
        "top5_depth_quantity": 25000,
        "order_to_adv_ratio": 0.04,
        "market_regime": "Normal"
    }
    pred = predict_execution_cost(features)
    print("Model 2 Output:", json.dumps(pred, indent=2))
    assert pred["modelId"] == "MODEL-02-EXEC-COST"
    assert pred["expectedSlippageBps"] > 0
    assert pred["estimatedExecutionCostINR"] > 0
    assert pred["netAchievableProceedsINR"] > 0
    print("Model 2 Test Passed!")

if __name__ == "__main__":
    test_model_1()
    test_model_2()
    print("ALL ML INFERENCE TESTS COMPLETED SUCCESSFULLY!")
