package com.lro.frontend.views;

import com.lro.frontend.bank.model.BankModels.BankProfile;
import com.lro.frontend.bank.service.BankService;
import com.lro.frontend.common.Formatters;
import com.lro.frontend.common.UIComponents;
import com.lro.frontend.lro.model.LroModels.*;
import com.lro.frontend.lro.service.LroService;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Liquidity Risk Optimizer (LRO) View Builders implementing Prompts 07 and 08:
 * Decision-support dashboard, Educational LCR/NSFR estimates, MTM vs Realizable proceeds,
 * Model 1 & 2 Transparency Insights, Recommendation Ranking & Human Approval queue,
 * Stress Testing workspace, Risk Alerts, and Financial Glossary.
 */
public class LroViews {

    private final LroService lroService;
    private final BankService bankService;
    private final Runnable onDataChanged;

    public LroViews(LroService lroService, BankService bankService, Runnable onDataChanged) {
        this.lroService = lroService;
        this.bankService = bankService;
        this.onDataChanged = onDataChanged;
    }

    /**
     * PROMPT 07: LRO Executive Decision-Support Dashboard.
     */
    public VBox buildDashboard(String selectedBankId, java.util.function.Consumer<String> onSelectBank) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        LiquiditySnapshot snap = lroService.getSnapshot(selectedBankId);
        BankProfile bank = bankService.getBankProfile(selectedBankId).orElseThrow();

        // 1. Bank Selector Bar
        HBox selectorBar = new HBox(12);
        selectorBar.setAlignment(Pos.CENTER_LEFT);
        selectorBar.setPadding(new Insets(10, 16, 10, 16));
        selectorBar.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 8, 0, 0, 2);");

        Label selLabel = new Label("Target Portfolio / Institution:");
        selLabel.setStyle("-fx-text-fill: #0F172A; -fx-font-weight: bold;");

        ComboBox<String> bankCombo = new ComboBox<>();
        for (BankProfile p : bankService.getAllBankProfiles()) {
            bankCombo.getItems().add(p.bankId() + " — " + p.bankName());
        }
        bankCombo.setValue(bank.bankId() + " — " + bank.bankName());
        bankCombo.setOnAction(e -> {
            String val = bankCombo.getValue();
            if (val != null) {
                onSelectBank.accept(val.split(" — ")[0].trim());
            }
        });

        Label freshnessLabel = new Label("Stream: " + snap.marketDataFreshnessStatus());
        freshnessLabel.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label modelLabel = new Label("Inference: " + snap.modelServingStatus());
        modelLabel.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 11px; -fx-font-weight: bold;");

        selectorBar.getChildren().addAll(selLabel, bankCombo, freshnessLabel, modelLabel);

        // 2. Core Liquidity KPIs
        HBox kpiRow1 = new HBox(12);
        kpiRow1.getChildren().addAll(
                UIComponents.createKpiCard("Available Cash Buffer", Formatters.formatINRCrores(snap.cashAvailableINR()), "Immediate Settlement Reserve", "Unrestricted cash held in bank vaults and current account with Reserve Bank of India.", "#0284C7"),
                UIComponents.createKpiCard("Liquid Assets (Est. HQLA)", Formatters.formatINRCrores(snap.liquidAssetsHQLA_INR()), "Unencumbered Sovereign Securities", "High Quality Liquid Assets convertible into cash immediately under market haircuts.", "#059669"),
                UIComponents.createKpiCard("Projected 1D Net Gap", Formatters.formatINRCrores(snap.projectedFundingGap1D_INR()), snap.projectedFundingGap1D_INR().compareTo(BigDecimal.ZERO) < 0 ? "Funding Deficit" : "Funding Surplus", "Contractual cash inflows minus outflows due within next 24 hours.", snap.projectedFundingGap1D_INR().compareTo(BigDecimal.ZERO) < 0 ? "#DC2626" : "#059669"),
                UIComponents.createKpiCard("Projected 30D Funding Gap", Formatters.formatINRCrores(snap.projectedFundingGap30D_INR()), "Cumulative 30-Day Liquidity", "Net cumulative funding gap over standard 30-day Basel stress horizon.", snap.projectedFundingGap30D_INR().compareTo(BigDecimal.ZERO) < 0 ? "#DC2626" : "#0284C7")
        );

        // 3. Educational Regulatory Indicators
        HBox kpiRow2 = new HBox(12);
        String lcrColor = snap.educationalLcrPercent().doubleValue() < 100.0 ? "#DC2626" : "#059669";
        String nsfrColor = snap.educationalNsfrPercent().doubleValue() < 100.0 ? "#DC2626" : "#059669";

        kpiRow2.getChildren().addAll(
                UIComponents.createKpiCard("Educational LCR Estimate", Formatters.formatPercent(snap.educationalLcrPercent()), "Baseline Target: ≥ 100.0%", "Illustrative Liquidity Coverage Ratio = HQLA / Net 30D Outflows. Educational estimate only; not official regulatory report.", lcrColor),
                UIComponents.createKpiCard("Educational NSFR Estimate", Formatters.formatPercent(snap.educationalNsfrPercent()), "Baseline Target: ≥ 100.0%", "Illustrative Net Stable Funding Ratio = Available Stable Funding / Required Stable Funding. Educational estimate only.", nsfrColor),
                UIComponents.createKpiCard("24h Portfolio MTM Movement", (snap.marketValueMovement24hPercent().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + Formatters.formatPercent(snap.marketValueMovement24hPercent()), "Mark-to-Market Variance", "Aggregate percentage shift in liquid securities value over past 24 hours.", "#4F46E5"),
                UIComponents.createKpiCard("Active Risk Alerts", String.valueOf(lroService.getRiskAlerts(selectedBankId).size()), "Automated Surveillance Flags", "Monitored threshold breaches in liquidity, capital, or market depth.", "#D97706")
        );

        // 4. Regulatory Caveat & Decision Support Summary
        VBox caveatBox = new VBox(6);
        caveatBox.setPadding(new Insets(12, 16, 12, 16));
        caveatBox.setStyle("-fx-background-color: #EFF6FF; -fx-background-radius: 8px; -fx-border-color: #BFDBFE; -fx-border-radius: 8px;");

        Label caveatTitle = new Label("REGULATORY COMPLIANCE NOTICE & GOVERNANCE POLICY");
        caveatTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1D4ED8; -fx-font-size: 11px;");

        Label caveatText = new Label("All Liquidity Coverage Ratio (LCR) and Net Stable Funding Ratio (NSFR) metrics displayed are heuristic decision-support approximations. Official RBI prudential regulatory filings require full statutory run-off factors, credit-line commitments, and certified risk officer approval. Autonomous automated trading is strictly prohibited; all proposed liquidations require human committee review.");
        caveatText.setWrapText(true);
        caveatText.setStyle("-fx-text-fill: #1E40AF; -fx-font-size: 11px;");

        caveatBox.getChildren().addAll(caveatTitle, caveatText);

        root.getChildren().addAll(selectorBar, kpiRow1, kpiRow2, caveatBox);
        return root;
    }

    /**
     * PROMPT 07 & 08: Portfolio Holdings Drill-Down (MTM vs Realizable proceeds, Model 2 slippage).
     */
    public VBox buildPortfolioView(String bankId) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        TableView<AssetLiquidationMetric> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<AssetLiquidationMetric, String> colInst = new TableColumn<>("Asset / Instrument");
        colInst.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().instrumentName()));

        TableColumn<AssetLiquidationMetric, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().category()));

        TableColumn<AssetLiquidationMetric, String> colMtm = new TableColumn<>("Market Value (MTM)");
        colMtm.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().markToMarketValueINR())));

        TableColumn<AssetLiquidationMetric, String> colHaircut = new TableColumn<>("Haircut %");
        colHaircut.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatPercent(d.getValue().haircutPercent())));

        TableColumn<AssetLiquidationMetric, String> colProceeds = new TableColumn<>("Est. Realizable Proceeds");
        colProceeds.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().estimatedLiquidationProceedsINR())));

        TableColumn<AssetLiquidationMetric, String> colSlippage = new TableColumn<>("Model 2 Slippage");
        colSlippage.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatBps(d.getValue().estimatedSlippageBps())));

        TableColumn<AssetLiquidationMetric, String> colCost = new TableColumn<>("Est. Execution Cost");
        colCost.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().estimatedExecutionCostINR())));

        TableColumn<AssetLiquidationMetric, String> colMethod = new TableColumn<>("Valuation Engine");
        colMethod.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().valuationMethod()));

        TableColumn<AssetLiquidationMetric, String> colConf = new TableColumn<>("Model Confidence");
        colConf.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().modelConfidence()));

        table.getColumns().addAll(colInst, colCat, colMtm, colHaircut, colProceeds, colSlippage, colCost, colMethod, colConf);
        table.getItems().setAll(lroService.getAssetLiquidationMetrics(bankId));

        Label explain = new Label("Notice: Realizable proceeds account for regulatory haircuts, depth impact, and Model 2 algorithmic slippage.");
        explain.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        Label sectionHeader = new Label("Liquid Assets Portfolio: Mark-to-Market & Liquidation Cost Analytics");
        sectionHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        root.getChildren().addAll(sectionHeader, table, explain);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    /**
     * PROMPT 08: Model Insights & Transparency Screen (Model 1 & Model 2).
     */
    public VBox buildModelInsightsView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        Label title = new Label("Machine Learning Governance & Model Transparency Registry");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        root.getChildren().add(title);

        for (ModelInsight mi : lroService.getModelInsights()) {
            VBox card = new VBox(10);
            card.setPadding(new Insets(16));
            card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 8, 0, 0, 2);");

            HBox cardHeader = new HBox(12);
            cardHeader.setAlignment(Pos.CENTER_LEFT);

            Label name = new Label(mi.modelName());
            name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2563EB;");

            Label ver = new Label("[" + mi.version() + "]");
            ver.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold;");

            Label drift = UIComponents.createStatusChip(mi.driftStatus(), "healthy");

            cardHeader.getChildren().addAll(name, ver, drift);

            GridPane grid = new GridPane();
            grid.setHgap(20);
            grid.setVgap(6);

            grid.add(new Label("Target Definition:"), 0, 0);
            grid.add(new Label(mi.targetDefinition()), 1, 0);

            grid.add(new Label("Forecast Horizon:"), 0, 1);
            grid.add(new Label(mi.forecastHorizon()), 1, 1);

            grid.add(new Label("Training Provenance:"), 0, 2);
            grid.add(new Label(mi.trainingDataSource()), 1, 2);

            grid.add(new Label("Validation Benchmark:"), 0, 3);
            grid.add(new Label(mi.primaryMetric()), 1, 3);

            grid.add(new Label("Last Retraining:"), 0, 4);
            grid.add(new Label(Formatters.formatDateTime(mi.lastTrainedAt())), 1, 4);

            for (javafx.scene.Node n : grid.getChildren()) {
                if (GridPane.getColumnIndex(n) == 0) n.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold;");
                else n.setStyle("-fx-text-fill: #0F172A;");
            }

            Label lim = new Label("Limitations & Constraints: " + mi.limitationsNotice());
            lim.setWrapText(true);
            lim.setStyle("-fx-text-fill: #92400E; -fx-font-size: 11px; -fx-padding: 8 12 8 12; -fx-background-color: #FEF3C7; -fx-background-radius: 6px; -fx-border-color: #FDE68A; -fx-border-radius: 6px;");

            card.getChildren().addAll(cardHeader, grid, lim);
            root.getChildren().add(card);
        }

        return root;
    }

    /**
     * PROMPT 08: Decision-Support Recommendation Review & Human Approval Queue.
     */
    public VBox buildRecommendationsView(String bankId) {
        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        Label title = new Label("Treasury Liquidity Decision-Support Recommendations (Human Approval Queue)");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label sub = new Label("All actions generated by the optimization engine require explicit two-officer human authorization. No autonomous trading.");
        sub.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        TableView<RecommendationItem> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<RecommendationItem, String> colId = new TableColumn<>("Rec ID");
        colId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().recommendationId()));

        TableColumn<RecommendationItem, String> colAsset = new TableColumn<>("Proposed Asset");
        colAsset.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().assetName()));

        TableColumn<RecommendationItem, String> colAction = new TableColumn<>("Action");
        colAction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().proposedAction()));

        TableColumn<RecommendationItem, String> colProceeds = new TableColumn<>("Net Proceeds");
        colProceeds.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().netProceedsINR())));

        TableColumn<RecommendationItem, String> colCost = new TableColumn<>("Estimated Cost");
        colCost.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().estimatedCostINR())));

        TableColumn<RecommendationItem, String> colBenefit = new TableColumn<>("LCR Benefit");
        colBenefit.setCellValueFactory(d -> new SimpleStringProperty("+" + Formatters.formatPercent(d.getValue().liquidityRatioBenefitPercent())));

        TableColumn<RecommendationItem, String> colConf = new TableColumn<>("Confidence");
        colConf.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatPercent(d.getValue().modelConfidencePercent())));

        TableColumn<RecommendationItem, String> colStatus = new TableColumn<>("Review Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status()));

        table.getColumns().addAll(colId, colAsset, colAction, colProceeds, colCost, colBenefit, colConf, colStatus);
        table.getItems().setAll(lroService.getRecommendations(bankId));

        HBox btnBar = new HBox(12);
        btnBar.setAlignment(Pos.CENTER_LEFT);

        Button btnApprove = new Button("✓ Authorize Recommendation");
        btnApprove.getStyleClass().add("btn-success");

        Button btnReject = new Button("✗ Reject Recommendation");
        btnReject.getStyleClass().add("btn-danger");

        Button btnExplain = new Button("ⓘ View Decision Rationale");
        btnExplain.getStyleClass().add("btn-secondary");

        btnBar.getChildren().addAll(btnApprove, btnReject, btnExplain);

        btnApprove.setOnAction(e -> {
            RecommendationItem sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && sel.status().equals("Pending Review")) {
                lroService.approveRecommendation(sel.recommendationId(), "Validated against treasury cash-ladder requirement");
                table.getItems().setAll(lroService.getRecommendations(bankId));
                onDataChanged.run();
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Recommendation authorized by treasury officer.");
                a.showAndWait();
            }
        });

        btnReject.setOnAction(e -> {
            RecommendationItem sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && sel.status().equals("Pending Review")) {
                lroService.rejectRecommendation(sel.recommendationId(), "Excess spread widening risk");
                table.getItems().setAll(lroService.getRecommendations(bankId));
                onDataChanged.run();
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Recommendation rejected.");
                a.showAndWait();
            }
        });

        btnExplain.setOnAction(e -> {
            RecommendationItem sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("Optimization Rationale & Risk Factors");
                a.setHeaderText(sel.assetName() + " — " + sel.proposedAction());
                a.setContentText("Decision Rationale:\n" + sel.rationaleText() + "\n\nRisk Factors & Limitations:\n" + sel.riskFactors());
                a.showAndWait();
            }
        });

        root.getChildren().addAll(title, sub, table, btnBar);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    /**
     * PROMPT 08: Stress Testing Scenario Workspace.
     */
    public VBox buildStressScenariosView(String bankId) {
        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        Label title = new Label("Reproducible Macro Stress Testing Workspace");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        TableView<StressTestResult> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<StressTestResult, String> colScen = new TableColumn<>("Macro Stress Scenario");
        colScen.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().scenarioName()));

        TableColumn<StressTestResult, String> colBaseCash = new TableColumn<>("Baseline Cash (₹)");
        colBaseCash.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINRCrores(d.getValue().baselineCashINR())));

        TableColumn<StressTestResult, String> colStressedCash = new TableColumn<>("Stressed Cash (₹)");
        colStressedCash.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINRCrores(d.getValue().stressedCashINR())));

        TableColumn<StressTestResult, String> colBaseGap = new TableColumn<>("Baseline 30D Gap");
        colBaseGap.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINRCrores(d.getValue().baselineGapINR())));

        TableColumn<StressTestResult, String> colStressedGap = new TableColumn<>("Stressed 30D Gap");
        colStressedGap.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINRCrores(d.getValue().stressedFundingGapINR())));

        TableColumn<StressTestResult, String> colBaseLcr = new TableColumn<>("Baseline LCR");
        colBaseLcr.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatPercent(d.getValue().baselineLcrPercent())));

        TableColumn<StressTestResult, String> colStressedLcr = new TableColumn<>("Stressed LCR");
        colStressedLcr.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatPercent(d.getValue().stressedLcrPercent())));

        TableColumn<StressTestResult, String> colSev = new TableColumn<>("Impact Severity");
        colSev.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().severityLevel()));

        table.getColumns().addAll(colScen, colBaseCash, colStressedCash, colBaseGap, colStressedGap, colBaseLcr, colStressedLcr, colSev);
        table.getItems().setAll(lroService.runStressScenarios(bankId));

        Button btnRun = new Button("↻ Re-Execute Deterministic Stress Scenarios");
        btnRun.getStyleClass().add("btn-primary");
        btnRun.setOnAction(e -> {
            table.getItems().setAll(lroService.runStressScenarios(bankId));
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Macro scenarios computed deterministically across portfolio lots.");
            a.showAndWait();
        });

        root.getChildren().addAll(title, table, btnRun);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    /**
     * PROMPT 08: Risk Alerts Queue & Educational Glossary.
     */
    public VBox buildAlertsReportsView(String bankId) {
        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        Label title = new Label("Liquidity Surveillance Alerts & Reporting Suite");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        TableView<RiskAlert> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<RiskAlert, String> colSev = new TableColumn<>("Severity");
        colSev.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().severity()));

        TableColumn<RiskAlert, String> colTitle = new TableColumn<>("Alert Title");
        colTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().title()));

        TableColumn<RiskAlert, String> colDesc = new TableColumn<>("Condition Description");
        colDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().description()));

        TableColumn<RiskAlert, String> colTime = new TableColumn<>("Triggered Time");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatDateTime(d.getValue().triggeredAt())));

        TableColumn<RiskAlert, String> colAck = new TableColumn<>("Acknowledged");
        colAck.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().acknowledged() ? "Yes" : "Pending"));

        table.getColumns().addAll(colSev, colTitle, colDesc, colTime, colAck);
        table.getItems().setAll(lroService.getRiskAlerts(bankId));

        HBox actions = new HBox(12);
        Button btnAck = new Button("Acknowledge Selected Alert");
        btnAck.getStyleClass().add("btn-secondary");
        btnAck.setOnAction(e -> {
            RiskAlert sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                lroService.acknowledgeAlert(sel.alertId());
                table.getItems().setAll(lroService.getRiskAlerts(bankId));
                onDataChanged.run();
            }
        });

        Button btnExport = new Button("Export Liquidity Audit Summary (CSV)");
        btnExport.getStyleClass().add("btn-primary");
        btnExport.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Synthetic compliance report exported with deterministic timestamp.");
            a.showAndWait();
        });

        Button btnGlossary = new Button("📖 Financial Terminology Glossary");
        btnGlossary.getStyleClass().add("btn-secondary");
        btnGlossary.setOnAction(e -> {
            UIComponents.showGlossaryDialog(
                    "Liquidity Coverage Ratio (LCR)",
                    "LCR measures the proportion of highly liquid assets held by financial institutions, to ensure their ongoing ability to meet short-term obligations in a 30-day stressed scenario.",
                    "LCR = (High Quality Liquid Assets) / (Total Net Cash Outflows over 30 Days) × 100%",
                    "Mandated by Basel III and Reserve Bank of India with minimum threshold of 100%."
            );
        });

        actions.getChildren().addAll(btnAck, btnExport, btnGlossary);

        root.getChildren().addAll(title, table, actions);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }
}
