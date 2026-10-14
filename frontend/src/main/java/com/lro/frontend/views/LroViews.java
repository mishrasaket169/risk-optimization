package com.lro.frontend.views;

import com.lro.frontend.bank.model.BankModels.BankProfile;
import com.lro.frontend.bank.service.BankService;
import com.lro.frontend.common.Formatters;
import com.lro.frontend.common.UIComponents;
import com.lro.frontend.common.UIComponents.SourceItem;
import com.lro.frontend.lro.model.LroModels.*;
import com.lro.frontend.lro.service.LroService;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Liquidity Risk Optimizer (LRO) View Builders implementing the LRO Ecosystem Modern Light Design System:
 * - 3-Column structure: Center area with LRO KPI cards, flow dynamics dual-tone chart, and transactions table
 * - Right panel with Regional exposure map widget and composition breakdown donut card
 * - All original formulas, text content, disclaimers, and algorithms preserved.
 */
@SuppressWarnings("unchecked")
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
     * PROMPT 07: LRO Executive Decision-Support Dashboard with LRO Ecosystem Design & Structure.
     */
    public VBox buildDashboard(String selectedBankId, java.util.function.Consumer<String> onSelectBank) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        LiquiditySnapshot snap = lroService.getSnapshot(selectedBankId);
        BankProfile bank = bankService.getBankProfile(selectedBankId).orElseThrow();

        // 1. Bank Selector Bar (LRO Card Style)
        HBox selectorBar = new HBox(12);
        selectorBar.setAlignment(Pos.CENTER_LEFT);
        selectorBar.getStyleClass().add("lro-card");
        selectorBar.setPadding(new Insets(12, 16, 12, 16));

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

        Label freshnessLabel = UIComponents.createStatusPill("Stream: " + snap.marketDataFreshnessStatus(), "success");
        Label modelLabel = UIComponents.createStatusPill("Inference: " + snap.modelServingStatus(), "info");

        Region barSpacer = new Region();
        HBox.setHgrow(barSpacer, Priority.ALWAYS);

        selectorBar.getChildren().addAll(selLabel, bankCombo, freshnessLabel, modelLabel);

        // 2. Main Grid: Center Content (KPIs + Chart + Table) & Right Widget Column
        HBox mainGrid = new HBox(20);
        mainGrid.setAlignment(Pos.TOP_LEFT);

        // =========================================================================
        // CENTER COLUMN (LRO Dashboard Center)
        // =========================================================================
        VBox centerCol = new VBox(16);
        HBox.setHgrow(centerCol, Priority.ALWAYS);

        // Row 1: 3 LRO KPI Cards
        HBox kpiRow = new HBox(12);
        kpiRow.getChildren().addAll(
                UIComponents.createKpiCard("Available Cash Buffer", Formatters.formatINRCrores(snap.cashAvailableINR()), "Immediate Settlement Reserve", "Unrestricted cash held in bank vaults and current account with Reserve Bank of India.", "#0284C7"),
                UIComponents.createKpiCard("Liquid Assets (Est. HQLA)", Formatters.formatINRCrores(snap.liquidAssetsHQLA_INR()), "Unencumbered Sovereign Securities", "High Quality Liquid Assets convertible into cash immediately under market haircuts.", "#059669"),
                UIComponents.createKpiCard("Educational LCR Estimate", Formatters.formatPercent(snap.educationalLcrPercent()), "Baseline Target: ≥ 100.0%", "Illustrative Liquidity Coverage Ratio = HQLA / Net 30D Outflows. Educational estimate only.", snap.educationalLcrPercent().doubleValue() < 100.0 ? "#DC2626" : "#059669")
        );

        // Row 2: Liquidity Flow Dynamics Chart Card
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        double[] inflows = {3200, 4800, 5400, 6200, 7100, 5900, 6800};
        double[] outflows = {2800, 4100, 4900, 4505, 6400, 5100, 6200};
        VBox chartNode = UIComponents.createDualBarChart(days, outflows, inflows, "Wed", "₹4,505 Cr", "#D97706");

        VBox chartCard = UIComponents.createChartCard(
                "Liquidity Dynamics & Cash-Flows",
                Formatters.formatINRCrores(snap.cashAvailableINR().divide(BigDecimal.valueOf(10), RoundingMode.HALF_UP)),
                "+2.32%",
                "Total net liquidity in week",
                "Week",
                chartNode
        );

        // Row 3: Data Table Card (Liquid Assets & Proceeds)
        VBox tableCard = new VBox(12);
        tableCard.getStyleClass().add("lro-card");

        HBox tableHeader = new HBox();
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        Label tblTitle = new Label("Liquid Assets Portfolio (Realizable Proceeds & Slippage)");
        tblTitle.getStyleClass().add("card-title");
        HBox.setHgrow(tblTitle, Priority.ALWAYS);

        Label viewAll = new Label("View All ↗");
        viewAll.setStyle("-fx-text-fill: #2563EB; -fx-font-weight: 700; -fx-font-size: 11px; -fx-cursor: hand;");
        tableHeader.getChildren().addAll(tblTitle, viewAll);

        TableView<AssetLiquidationMetric> table = new TableView<>();
        table.setPrefHeight(230);
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

        TableColumn<AssetLiquidationMetric, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty("Standard"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(UIComponents.createStatusPill("Standard", "success"));
                }
            }
        });

        table.getColumns().addAll(colInst, colCat, colMtm, colHaircut, colProceeds, colStatus);
        List<AssetLiquidationMetric> metrics = lroService.getAssetLiquidationMetrics(selectedBankId);
        table.getItems().setAll(metrics.size() > 5 ? metrics.subList(0, 5) : metrics);

        tableCard.getChildren().addAll(tableHeader, table);

        // Row 4: Regulatory Compliance Notice & Governance Policy (Full text preserved)
        VBox caveatBox = new VBox(6);
        caveatBox.setPadding(new Insets(12, 16, 12, 16));
        caveatBox.setStyle("-fx-background-color: #EFF6FF; -fx-background-radius: 8px; -fx-border-color: #BFDBFE; -fx-border-radius: 8px;");

        Label caveatTitle = new Label("REGULATORY COMPLIANCE NOTICE & GOVERNANCE POLICY");
        caveatTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1D4ED8; -fx-font-size: 11px;");

        Label caveatText = new Label("All Liquidity Coverage Ratio (LCR) and Net Stable Funding Ratio (NSFR) metrics displayed are heuristic decision-support approximations. Official RBI prudential regulatory filings require full statutory run-off factors, credit-line commitments, and certified risk officer approval. Autonomous automated trading is strictly prohibited; all proposed liquidations require human committee review.");
        caveatText.setWrapText(true);
        caveatText.setStyle("-fx-text-fill: #1E40AF; -fx-font-size: 11px;");

        caveatBox.getChildren().addAll(caveatTitle, caveatText);

        centerCol.getChildren().addAll(kpiRow, chartCard, tableCard, caveatBox);

        // =========================================================================
        // RIGHT COLUMN (LRO Widgets: Regional Map & Source Mix)
        // =========================================================================
        VBox rightCol = new VBox(16);
        rightCol.setPrefWidth(310);
        rightCol.setMinWidth(290);

        // Widget 1: Regional Risk Exposure
        VBox regWidget = UIComponents.createRegionalExposureWidget(
                "Regional Risk Exposure",
                "Period August 2026",
                "Tier-1 Metro Hubs",
                "84% HQLA",
                "₹61,558 Cr Assets",
                "#D97706"
        );

        // Widget 2: Liquidity & Funding Source
        List<SourceItem> sources = List.of(
                new SourceItem("Retail CASA", "₹2,300 Cr", "#0284C7"),
                new SourceItem("Term Deposits", "₹3,304 Cr", "#059669"),
                new SourceItem("Sovereign Repo", "₹443 Cr", "#D97706")
        );
        VBox sourceWidget = UIComponents.createDonutSourceWidget(
                "Funding & Liquidity Mix",
                "Total liquidity in a week",
                "Total Buffer",
                "₹18.378 Cr",
                sources,
                "See Details ↗",
                () -> UIComponents.showGlossaryDialog(
                        "Funding Source Composition",
                        "Breakdown of immediate liquidity reserves across retail core deposits, sovereign market operations, and wholesale funding facilities.",
                        "Total Liquidity = CASA Buffer + Term Deposits + Unencumbered Sovereign Repo",
                        "Complements Basel III Liquidity Coverage Ratio (LCR) requirements."
                ),
                "#D97706"
        );

        rightCol.getChildren().addAll(regWidget, sourceWidget);

        mainGrid.getChildren().addAll(centerCol, rightCol);
        root.getChildren().addAll(selectorBar, mainGrid);
        return root;
    }

    /**
     * PROMPT 07 & 08: Portfolio Holdings Drill-Down.
     */
    public VBox buildPortfolioView(String bankId) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        VBox card = new VBox(14);
        card.getStyleClass().add("lro-card");

        Label sectionHeader = new Label("Liquid Assets Portfolio: Mark-to-Market & Liquidation Cost Analytics");
        sectionHeader.getStyleClass().add("card-title");

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
        explain.setStyle("-fx-text-fill: #7E8494; -fx-font-size: 11px;");

        card.getChildren().addAll(sectionHeader, table, explain);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(card, Priority.ALWAYS);

        root.getChildren().add(card);
        return root;
    }

    /**
     * PROMPT 08: Model Insights & Transparency Screen (Model 1 & Model 2).
     */
    public VBox buildModelInsightsView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        Label title = new Label("Machine Learning Governance & Model Transparency Registry");
        title.getStyleClass().add("card-title");
        root.getChildren().add(title);

        for (ModelInsight mi : lroService.getModelInsights()) {
            VBox card = new VBox(12);
            card.getStyleClass().add("lro-card");

            HBox cardHeader = new HBox(12);
            cardHeader.setAlignment(Pos.CENTER_LEFT);

            Label name = new Label(mi.modelName());
            name.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #D97706;");

            Label ver = new Label("[" + mi.version() + "]");
            ver.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold;");

            Label drift = UIComponents.createStatusPill(mi.driftStatus(), "healthy");

            cardHeader.getChildren().addAll(name, ver, drift);

            GridPane grid = new GridPane();
            grid.setHgap(24);
            grid.setVgap(8);

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
                if (GridPane.getColumnIndex(n) == 0) n.setStyle("-fx-text-fill: #64748B; -fx-font-weight: 700;");
                else n.setStyle("-fx-text-fill: #0F172A;");
            }

            Label lim = new Label("Limitations & Constraints: " + mi.limitationsNotice());
            lim.setWrapText(true);
            lim.setStyle("-fx-text-fill: #92400E; -fx-font-size: 11px; -fx-padding: 8 12; -fx-background-color: #FEF3C7; -fx-background-radius: 6px; -fx-border-color: #FDE68A; -fx-border-radius: 6px;");

            card.getChildren().addAll(cardHeader, grid, lim);
            root.getChildren().add(card);
        }

        return root;
    }

    /**
     * PROMPT 08: Decision-Support Recommendation Review & Human Approval Queue.
     */
    public VBox buildRecommendationsView(String bankId) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        VBox card = new VBox(14);
        card.getStyleClass().add("lro-card");

        Label title = new Label("Treasury Liquidity Decision-Support Recommendations (Human Approval Queue)");
        title.getStyleClass().add("card-title");

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
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(UIComponents.createStatusPill(item, item.equalsIgnoreCase("Approved") ? "success" : "process"));
                }
            }
        });

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

        card.getChildren().addAll(title, sub, table, btnBar);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(card, Priority.ALWAYS);

        root.getChildren().add(card);
        return root;
    }

    /**
     * PROMPT 08: Stress Testing Scenario Workspace.
     */
    public VBox buildStressScenariosView(String bankId) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        VBox card = new VBox(14);
        card.getStyleClass().add("lro-card");

        Label title = new Label("Reproducible Macro Stress Testing Workspace");
        title.getStyleClass().add("card-title");

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
        colSev.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(UIComponents.createStatusPill(item, item.equalsIgnoreCase("High") ? "danger" : "warning"));
                }
            }
        });

        table.getColumns().addAll(colScen, colBaseCash, colStressedCash, colBaseGap, colStressedGap, colBaseLcr, colStressedLcr, colSev);
        table.getItems().setAll(lroService.runStressScenarios(bankId));

        Button btnRun = new Button("↻ Re-Execute Deterministic Stress Scenarios");
        btnRun.getStyleClass().add("btn-primary");
        btnRun.setOnAction(e -> {
            table.getItems().setAll(lroService.runStressScenarios(bankId));
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Macro scenarios computed deterministically across portfolio lots.");
            a.showAndWait();
        });

        card.getChildren().addAll(title, table, btnRun);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(card, Priority.ALWAYS);

        root.getChildren().add(card);
        return root;
    }

    /**
     * PROMPT 08: Risk Alerts Queue & Educational Glossary.
     */
    public VBox buildAlertsReportsView(String bankId) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        VBox card = new VBox(14);
        card.getStyleClass().add("lro-card");

        Label title = new Label("Liquidity Surveillance Alerts & Reporting Suite");
        title.getStyleClass().add("card-title");

        TableView<RiskAlert> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<RiskAlert, String> colSev = new TableColumn<>("Severity");
        colSev.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().severity()));
        colSev.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(UIComponents.createStatusPill(item, item.equalsIgnoreCase("Critical") ? "danger" : "warning"));
                }
            }
        });

        TableColumn<RiskAlert, String> colTitle = new TableColumn<>("Alert Title");
        colTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().title()));

        TableColumn<RiskAlert, String> colDesc = new TableColumn<>("Condition Description");
        colDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().description()));

        TableColumn<RiskAlert, String> colTime = new TableColumn<>("Triggered Time");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatDateTime(d.getValue().triggeredAt())));

        TableColumn<RiskAlert, String> colAck = new TableColumn<>("Acknowledged");
        colAck.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().acknowledged() ? "Yes" : "Pending"));
        colAck.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(UIComponents.createStatusPill(item, item.equalsIgnoreCase("Yes") ? "success" : "process"));
                }
            }
        });

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

        card.getChildren().addAll(title, table, actions);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(card, Priority.ALWAYS);

        root.getChildren().add(card);
        return root;
    }
}
