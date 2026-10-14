package com.lro.frontend.views;

import com.lro.frontend.common.Formatters;
import com.lro.frontend.common.UIComponents;
import com.lro.frontend.exchange.model.ExchangeModels.*;
import com.lro.frontend.exchange.service.ExchangeService;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Exchange Simulator View Builders implementing Prompts 05 and 06:
 * Market Overview, Instrument Catalogue, Trading Ticket, Market Depth Order Book,
 * Order/Trade Blotters, and Market Regime controls.
 */
@SuppressWarnings("unchecked")
public class ExchangeViews {

    private final ExchangeService exchangeService;
    private final Runnable onDataChanged;
    private Runnable activeViewRefresher;

    public ExchangeViews(ExchangeService exchangeService, Runnable onDataChanged) {
        this.exchangeService = exchangeService;
        this.onDataChanged = onDataChanged;
    }

    public void refreshActiveView() {
        if (activeViewRefresher != null) {
            activeViewRefresher.run();
        }
    }

    /**
     * PROMPT 05: Market Overview Screen with simulated trading clock, broad index cards,
     * turnover, market breadth, and real-time simulated events.
     */
    public VBox buildOverview() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        MarketOverview ov = exchangeService.getMarketOverview();

        // 1. Status Bar (LRO Card Style)
        HBox statusBar = new HBox(12);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.getStyleClass().add("lro-card");
        statusBar.setPadding(new Insets(12, 20, 12, 20));

        Label statusLabel = new Label("Exchange Status: " + ov.marketStatus());
        statusLabel.setStyle("-fx-text-fill: #0F172A; -fx-font-weight: 700;");

        Label clockLabel = new Label("Simulated Clock: " + Formatters.formatDateTime(ov.simulatedClock()));
        clockLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");

        Label regimeChip = UIComponents.createStatusPill("Regime: " + ov.activeRegime(), ov.activeRegime().equalsIgnoreCase("Crash") ? "danger" : "normal");

        statusBar.getChildren().addAll(statusLabel, clockLabel, regimeChip);

        // 2. Main Grid: Center Content (KPIs + Chart + Table) & Right Widget Column
        HBox mainGrid = new HBox(20);
        mainGrid.setAlignment(Pos.TOP_LEFT);

        // =========================================================================
        // CENTER COLUMN (LRO Dashboard Center)
        // =========================================================================
        VBox centerCol = new VBox(16);
        HBox.setHgrow(centerCol, Priority.ALWAYS);

        // Row 1: 3 LRO KPI Cards matching the mockup top row
        HBox kpiRow = new HBox(14);
        MarketIndex idx1 = ov.indices().size() > 0 ? ov.indices().get(0) : null;
        MarketIndex idx2 = ov.indices().size() > 1 ? ov.indices().get(1) : null;

        String idx1Name = idx1 != null ? idx1.indexName() : "NIFTY 50";
        String idx1Val = idx1 != null ? Formatters.formatQuantity(idx1.currentLevel().longValue()) : "24,120";
        String idx1Chg = idx1 != null ? ((idx1.changePercent().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + Formatters.formatPercent(idx1.changePercent())) : "+6.32%";
        boolean idx1Pos = idx1 == null || idx1.changePercent().compareTo(BigDecimal.ZERO) >= 0;

        String idx2Name = idx2 != null ? idx2.indexName() : "SENSEX";
        String idx2Val = idx2 != null ? Formatters.formatQuantity(idx2.currentLevel().longValue()) : "79,450";
        String idx2Chg = idx2 != null ? ((idx2.changePercent().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + Formatters.formatPercent(idx2.changePercent())) : "+3.54%";
        boolean idx2Pos = idx2 == null || idx2.changePercent().compareTo(BigDecimal.ZERO) >= 0;

        kpiRow.getChildren().addAll(
                UIComponents.createLroKpiCard(idx1Name, idx1Val, idx1Chg, idx1Pos, "Simulated broad market benchmark index", "Primary equity benchmark index."),
                UIComponents.createLroKpiCard(idx2Name, idx2Val, idx2Chg, idx2Pos, "Simulated 30-stock benchmark index", "Large-cap benchmark index."),
                UIComponents.createLroKpiCard("Total Market Turnover", Formatters.formatINRCrores(ov.totalTurnoverINR()), "+8.12%", true, "Daily Simulated Gross Value", "Aggregated rupee traded volume across continuous auction books.")
        );

        // Row 2: "Market Movement" Line Graph Card
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        double[] movementPoints = {24150.0, 24380.0, 24520.0, 24850.5, 24690.0, 24960.0, 25120.0};
        VBox chartNode = UIComponents.createLineMovementChart(
                days,
                movementPoints,
                3,
                "Today :",
                "24,850.50 (₹4,505 Cr)",
                "#4F46E5"
        );

        VBox chartCard = UIComponents.createChartCard(
                "Market Movement",
                Formatters.formatINRCrores(ov.totalTurnoverINR().divide(BigDecimal.valueOf(10), RoundingMode.HALF_UP)),
                "+2.32%",
                "Total traded value & index movement in week",
                "Week",
                chartNode
        );

        // Row 3: Data Table Card matching "Costumer Transaction"
        VBox tableCard = new VBox(12);
        tableCard.getStyleClass().add("lro-card");

        HBox tableHeader = new HBox();
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        Label tblTitle = new Label("Costumer Transaction (Recent Executions & Trades)");
        tblTitle.getStyleClass().add("card-title");
        HBox.setHgrow(tblTitle, Priority.ALWAYS);

        Label viewAll = new Label("View All ↗");
        viewAll.setStyle("-fx-text-fill: #4F46E5; -fx-font-weight: 700; -fx-font-size: 11px; -fx-cursor: hand;");
        tableHeader.getChildren().addAll(tblTitle, viewAll);

        TableView<TradeRecord> table = new TableView<>();
        table.setPrefHeight(230);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<TradeRecord, String> colTradeId = new TableColumn<>("Trade ID");
        colTradeId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().tradeId()));

        TableColumn<TradeRecord, String> colSymbol = new TableColumn<>("Symbol");
        colSymbol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().symbol()));

        TableColumn<TradeRecord, String> colQty = new TableColumn<>("Quantity");
        colQty.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatQuantity(d.getValue().quantity())));

        TableColumn<TradeRecord, String> colPrice = new TableColumn<>("Execution Price");
        colPrice.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().price())));

        TableColumn<TradeRecord, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty("Success"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(UIComponents.createStatusPill("Success", "success"));
                }
            }
        });

        table.getColumns().addAll(colTradeId, colSymbol, colQty, colPrice, colStatus);
        List<TradeRecord> trades = exchangeService.getTradeBlotter();
        table.getItems().setAll(trades.size() > 5 ? trades.subList(0, 5) : trades);

        tableCard.getChildren().addAll(tableHeader, table);

        centerCol.getChildren().addAll(kpiRow, chartCard, tableCard);

        // =========================================================================
        // RIGHT COLUMN (LRO Widgets: Regional Map & Social Source)
        // =========================================================================
        VBox rightCol = new VBox(16);
        rightCol.setPrefWidth(320);
        rightCol.setMinWidth(300);

        // Widget 1: 10-Yr Benchmark Bond Yield KPI Tile (Matches LRO KPI card styling, replacing Costumer by Country)
        VBox bondYieldTile = UIComponents.createLroKpiCard(
                "10-Yr Benchmark Bond Yield",
                "7.18%",
                "-0.04%",
                false,
                "Sovereign 10-Yr G-Sec Benchmark",
                "Aggregated benchmark sovereign yield curve anchor for Indian debt markets."
        );

        // Widget 2: Volume Source Distribution (Matches "Social Source" card)
        List<UIComponents.SourceItem> sources = List.of(
                new UIComponents.SourceItem("Equities", "450,000", "#0F172A"),
                new UIComponents.SourceItem("G-Sec Sovereign", "520,000", "#CBD5E1"),
                new UIComponents.SourceItem("Derivatives", "230,000", "#4F46E5")
        );
        VBox sourceWidget = UIComponents.createDonutSourceWidget(
                "Social Source (Turnover)",
                "Turnover distribution by asset class",
                "Total Volume",
                "1.2M Lots",
                sources,
                "See Details ↗",
                () -> UIComponents.showGlossaryDialog(
                        "Exchange Volume & Turnover Distribution",
                        "Continuous multi-lateral double auction order matching data across capital market and debt segments.",
                        "Total Volume = Equities + Sovereign Debt + Derivatives Lots",
                        "Ensures resilient continuous double-auction execution."
                ),
                "#4F46E5"
        );

        rightCol.getChildren().addAll(bondYieldTile, sourceWidget);

        mainGrid.getChildren().addAll(centerCol, rightCol);
        root.getChildren().addAll(statusBar, mainGrid);
        return root;
    }

    /**
     * Stock Directory View: Equities & listed company shares.
     */
    public VBox buildStocksView(java.util.function.Consumer<String> onSelectInstrument) {
        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        // Header Title Banner
        HBox titleBox = new HBox(12);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Stock Directory");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        Label subtitle = new Label("Cash Equity Shares & Listed Blue-Chip Stocks");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B; -fx-font-weight: 500;");
        Label chip = UIComponents.createStatusChip("Equities / Listed Shares", "info");
        titleBox.getChildren().addAll(title, subtitle, chip);

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> sectorCombo = new ComboBox<>();
        sectorCombo.getItems().addAll("All", "Energy / Conglomerate", "Banking & Financials", "Information Technology",
                "Public Sector Banking", "Telecommunications", "Infrastructure & Capital Goods", "Automotive", "FMCG / Diversified");
        sectorCombo.setValue("All");

        TextField searchField = new TextField();
        searchField.setPromptText("Search symbol or company name...");
        searchField.setPrefWidth(260);

        Label countLabel = new Label();
        countLabel.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold; -fx-font-size: 12px;");

        filterBar.getChildren().addAll(new Label("Sector:"), sectorCombo, searchField, countLabel);

        TableView<StockRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<StockRecord, String> colSym = new TableColumn<>("Symbol");
        colSym.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().symbol()));

        TableColumn<StockRecord, String> colName = new TableColumn<>("Company Name");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().companyName()));

        TableColumn<StockRecord, String> colSector = new TableColumn<>("Sector / Industry");
        colSector.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().sector()));

        TableColumn<StockRecord, String> colLtp = new TableColumn<>("LTP (₹)");
        colLtp.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().lastTradedPrice())));

        TableColumn<StockRecord, String> colChg = new TableColumn<>("Change %");
        colChg.setCellValueFactory(d -> new SimpleStringProperty((d.getValue().changePercent().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + Formatters.formatPercent(d.getValue().changePercent())));

        TableColumn<StockRecord, String> colBid = new TableColumn<>("Bid Price (₹)");
        colBid.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().bidPrice())));

        TableColumn<StockRecord, String> colAsk = new TableColumn<>("Ask Price (₹)");
        colAsk.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().askPrice())));

        TableColumn<StockRecord, String> colVol = new TableColumn<>("Volume");
        colVol.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatQuantity(d.getValue().dayVolume())));

        TableColumn<StockRecord, String> colTurnover = new TableColumn<>("Turnover (₹)");
        colTurnover.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINRCrores(d.getValue().dayTurnoverINR())));

        table.getColumns().addAll(colSym, colName, colSector, colLtp, colChg, colBid, colAsk, colVol, colTurnover);

        table.setOnMouseClicked(e -> {
            StockRecord sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && onSelectInstrument != null) {
                onSelectInstrument.accept(sel.symbol());
            }
        });

        UIComponents.PaginationBar<StockRecord> paginationBar = new UIComponents.PaginationBar<>(table, 500);

        Runnable refreshTable = () -> {
            String selectedSec = sectorCombo.getValue();
            String q = searchField.getText();
            List<StockRecord> list = exchangeService.getStocks(selectedSec, q);
            paginationBar.setData(list);
            countLabel.setText("Total: " + String.format("%,d", list.size()) + " stocks");
        };
        sectorCombo.setOnAction(e -> refreshTable.run());
        searchField.textProperty().addListener((obs, o, n) -> refreshTable.run());
        refreshTable.run();

        this.activeViewRefresher = refreshTable;

        Label tip = new Label("Tip: Click any stock to view its order book depth or launch an order ticket.");
        tip.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        root.getChildren().addAll(titleBox, filterBar, table, paginationBar, tip);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    /**
     * Bond Directory View: Corporate debentures & institutional fixed income.
     */
    public VBox buildBondsView(java.util.function.Consumer<String> onSelectInstrument) {
        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        // Header Title Banner
        HBox titleBox = new HBox(12);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Bond Directory");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        Label subtitle = new Label("Corporate Debentures & Institutional Fixed-Income Bonds");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B; -fx-font-weight: 500;");
        Label chip = UIComponents.createStatusChip("Rated Fixed Income", "info");
        titleBox.getChildren().addAll(title, subtitle, chip);

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> sectorCombo = new ComboBox<>();
        sectorCombo.getItems().addAll("All", "Financial Services", "PSU / Infrastructure", "Power & Utilities",
                "Energy & Petrochem", "Metals & Mining", "National Highways", "Automotive & Transport", "Telecommunications");
        sectorCombo.setValue("All");

        TextField searchField = new TextField();
        searchField.setPromptText("Search bond symbol, issuer...");
        searchField.setPrefWidth(260);

        Label countLabel = new Label();
        countLabel.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold; -fx-font-size: 12px;");

        filterBar.getChildren().addAll(new Label("Issuer Sector:"), sectorCombo, searchField, countLabel);

        TableView<BondRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<BondRecord, String> colSym = new TableColumn<>("Bond Symbol");
        colSym.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().symbol()));

        TableColumn<BondRecord, String> colName = new TableColumn<>("Issuer / Bond Series");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().issuerSeries()));

        TableColumn<BondRecord, String> colSector = new TableColumn<>("Sector / Type");
        colSector.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().sectorType()));

        TableColumn<BondRecord, String> colCoupon = new TableColumn<>("Coupon Rate %");
        colCoupon.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().couponRatePercent() != null ? Formatters.formatPercent(d.getValue().couponRatePercent()) : "N/A"));

        TableColumn<BondRecord, String> colLtp = new TableColumn<>("Clean Price (₹)");
        colLtp.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().cleanPrice())));

        TableColumn<BondRecord, String> colChg = new TableColumn<>("Change %");
        colChg.setCellValueFactory(d -> new SimpleStringProperty((d.getValue().changePercent().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + Formatters.formatPercent(d.getValue().changePercent())));

        TableColumn<BondRecord, String> colBid = new TableColumn<>("Bid (₹)");
        colBid.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().bidPrice())));

        TableColumn<BondRecord, String> colAsk = new TableColumn<>("Ask (₹)");
        colAsk.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().askPrice())));

        TableColumn<BondRecord, String> colVol = new TableColumn<>("Volume");
        colVol.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatQuantity(d.getValue().dayVolume())));

        TableColumn<BondRecord, String> colMaturity = new TableColumn<>("Maturity Date");
        colMaturity.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().maturityDate() != null ? d.getValue().maturityDate().toString() : "Perpetual"));

        table.getColumns().addAll(colSym, colName, colSector, colCoupon, colLtp, colChg, colBid, colAsk, colVol, colMaturity);

        table.setOnMouseClicked(e -> {
            BondRecord sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && onSelectInstrument != null) {
                onSelectInstrument.accept(sel.symbol());
            }
        });

        UIComponents.PaginationBar<BondRecord> paginationBar = new UIComponents.PaginationBar<>(table, 500);

        Runnable refreshTable = () -> {
            String selectedSec = sectorCombo.getValue();
            String q = searchField.getText();
            List<BondRecord> list = exchangeService.getBonds(selectedSec, q);
            paginationBar.setData(list);
            countLabel.setText("Total: " + String.format("%,d", list.size()) + " bonds");
        };
        sectorCombo.setOnAction(e -> refreshTable.run());
        searchField.textProperty().addListener((obs, o, n) -> refreshTable.run());
        refreshTable.run();

        this.activeViewRefresher = refreshTable;

        Label tip = new Label("Tip: Click any bond to view its order book depth or launch an order ticket.");
        tip.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        root.getChildren().addAll(titleBox, filterBar, table, paginationBar, tip);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    /**
     * Securities Directory View: Government Securities (G-Secs) & Sovereign Treasury Bills (T-Bills).
     */
    public VBox buildSecuritiesView(java.util.function.Consumer<String> onSelectInstrument) {
        VBox root = new VBox(14);
        root.setPadding(new Insets(20));

        // Header Title Banner
        HBox titleBox = new HBox(12);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Securities Directory");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        Label subtitle = new Label("Government Securities (G-Secs) & Sovereign Treasury Bills");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B; -fx-font-weight: 500;");
        Label chip = UIComponents.createStatusChip("Sovereign G-Secs", "normal");
        titleBox.getChildren().addAll(title, subtitle, chip);

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("All", "G-Sec", "T-Bill");
        typeCombo.setValue("All");

        TextField searchField = new TextField();
        searchField.setPromptText("Search security symbol, tenor...");
        searchField.setPrefWidth(260);

        Label countLabel = new Label();
        countLabel.setStyle("-fx-text-fill: #64748B; -fx-font-weight: bold; -fx-font-size: 12px;");

        filterBar.getChildren().addAll(new Label("Security Type:"), typeCombo, searchField, countLabel);

        TableView<SecurityRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<SecurityRecord, String> colSym = new TableColumn<>("Security Symbol");
        colSym.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().symbol()));

        TableColumn<SecurityRecord, String> colName = new TableColumn<>("Security Description");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().securityDescription()));

        TableColumn<SecurityRecord, String> colType = new TableColumn<>("Category");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().category()));

        TableColumn<SecurityRecord, String> colYield = new TableColumn<>("Benchmark Yield %");
        colYield.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().benchmarkYieldPercent() != null ? Formatters.formatPercent(d.getValue().benchmarkYieldPercent()) : "N/A"));

        TableColumn<SecurityRecord, String> colLtp = new TableColumn<>("Clean Price (₹)");
        colLtp.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().cleanPrice())));

        TableColumn<SecurityRecord, String> colChg = new TableColumn<>("Change %");
        colChg.setCellValueFactory(d -> new SimpleStringProperty((d.getValue().changePercent().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + Formatters.formatPercent(d.getValue().changePercent())));

        TableColumn<SecurityRecord, String> colBid = new TableColumn<>("Bid (₹)");
        colBid.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().bidPrice())));

        TableColumn<SecurityRecord, String> colAsk = new TableColumn<>("Ask (₹)");
        colAsk.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().askPrice())));

        TableColumn<SecurityRecord, String> colVol = new TableColumn<>("Volume");
        colVol.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatQuantity(d.getValue().dayVolume())));

        TableColumn<SecurityRecord, String> colMaturity = new TableColumn<>("Maturity Date");
        colMaturity.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().maturityDate() != null ? d.getValue().maturityDate().toString() : "N/A"));

        table.getColumns().addAll(colSym, colName, colType, colYield, colLtp, colChg, colBid, colAsk, colVol, colMaturity);

        table.setOnMouseClicked(e -> {
            SecurityRecord sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && onSelectInstrument != null) {
                onSelectInstrument.accept(sel.symbol());
            }
        });

        UIComponents.PaginationBar<SecurityRecord> paginationBar = new UIComponents.PaginationBar<>(table, 500);

        Runnable refreshTable = () -> {
            String selectedType = typeCombo.getValue();
            String q = searchField.getText();
            List<SecurityRecord> list = exchangeService.getSecurities(selectedType, q);
            paginationBar.setData(list);
            countLabel.setText("Total: " + String.format("%,d", list.size()) + " securities");
        };
        typeCombo.setOnAction(e -> refreshTable.run());
        searchField.textProperty().addListener((obs, o, n) -> refreshTable.run());
        refreshTable.run();

        this.activeViewRefresher = refreshTable;

        Label tip = new Label("Tip: Click any security to view its order book depth or launch an order ticket.");
        tip.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        root.getChildren().addAll(titleBox, filterBar, table, paginationBar, tip);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    /**
     * Backward-compatible instrument catalogue forwarding to Stocks view.
     */
    public VBox buildInstrumentCatalogue(java.util.function.Consumer<String> onSelectInstrument) {
        return buildStocksView(onSelectInstrument);
    }

    /**
     * PROMPT 06: Order Ticket Panel for simulated order entry and validation.
     */
    public VBox buildOrderTicket(String defaultSymbol) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setMaxWidth(600);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 8, 0, 0, 2);");

        Label title = new Label("Simulated Trading Order Ticket");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        InstrumentRecord current = exchangeService.getInstrument(defaultSymbol != null ? defaultSymbol : "RELIANCE").orElseThrow();

        ComboBox<String> instCombo = new ComboBox<>();
        for (InstrumentRecord ir : exchangeService.getInstruments("All", "")) {
            instCombo.getItems().add(ir.symbol());
        }
        instCombo.setValue(current.symbol());

        HBox sideBox = new HBox(12);
        ToggleGroup sideGroup = new ToggleGroup();
        RadioButton rbBuy = new RadioButton("BUY");
        rbBuy.setToggleGroup(sideGroup);
        rbBuy.setSelected(true);
        rbBuy.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");

        RadioButton rbSell = new RadioButton("SELL");
        rbSell.setToggleGroup(sideGroup);
        rbSell.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
        sideBox.getChildren().addAll(new Label("Side:"), rbBuy, rbSell);

        HBox typeBox = new HBox(12);
        ToggleGroup typeGroup = new ToggleGroup();
        RadioButton rbLimit = new RadioButton("LIMIT");
        rbLimit.setToggleGroup(typeGroup);
        rbLimit.setSelected(true);

        RadioButton rbMarket = new RadioButton("MARKET");
        rbMarket.setToggleGroup(typeGroup);
        typeBox.getChildren().addAll(new Label("Order Type:"), rbLimit, rbMarket);

        TextField qtyField = new TextField("100");
        TextField priceField = new TextField(current.lastTradedPrice().toPlainString());

        ComboBox<String> tifCombo = new ComboBox<>();
        tifCombo.getItems().addAll("DAY", "IOC");
        tifCombo.setValue("DAY");

        Label notionalLabel = new Label("Estimated Notional: " + Formatters.formatINR(current.lastTradedPrice().multiply(new BigDecimal("100"))));
        notionalLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2563EB; -fx-font-size: 13px;");

        Runnable updateNotional = () -> {
            try {
                long qty = Long.parseLong(qtyField.getText());
                BigDecimal pr = rbMarket.isSelected() ? current.lastTradedPrice() : new BigDecimal(priceField.getText());
                notionalLabel.setText("Estimated Notional: " + Formatters.formatINR(pr.multiply(BigDecimal.valueOf(qty))));
            } catch (Exception ignored) {}
        };
        qtyField.textProperty().addListener((obs, o, n) -> updateNotional.run());
        priceField.textProperty().addListener((obs, o, n) -> updateNotional.run());
        typeGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            priceField.setDisable(rbMarket.isSelected());
            updateNotional.run();
        });

        Button btnSubmit = new Button("Submit Simulated Order");
        btnSubmit.getStyleClass().add("btn-primary");
        btnSubmit.setPrefWidth(220);

        btnSubmit.setOnAction(e -> {
            try {
                long qty = Long.parseLong(qtyField.getText());
                BigDecimal pr = rbMarket.isSelected() ? current.lastTradedPrice() : new BigDecimal(priceField.getText());
                String side = rbBuy.isSelected() ? "BUY" : "SELL";
                String oType = rbMarket.isSelected() ? "MARKET" : "LIMIT";

                OrderRecord order = exchangeService.submitSimulatedOrder(instCombo.getValue(), side, oType, qty, pr, tifCombo.getValue());
                onDataChanged.run();

                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("Simulated Order Confirmation");
                a.setHeaderText("Order Executed on Simulated Engine");
                a.setContentText("Order ID: " + order.orderId() + "\nStatus: " + order.status() + "\nExecuted Notional: " + Formatters.formatINR(order.estimatedNotionalINR()));
                a.showAndWait();
            } catch (Exception ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Invalid order input: " + ex.getMessage());
                a.showAndWait();
            }
        });

        root.getChildren().addAll(
                title,
                new HBox(12, new Label("Instrument:"), instCombo),
                sideBox,
                typeBox,
                new HBox(12, new Label("Quantity:"), qtyField),
                new HBox(12, new Label("Limit Price (₹):"), priceField),
                new HBox(12, new Label("Time in Force:"), tifCombo),
                notionalLabel,
                btnSubmit
        );
        return root;
    }

    /**
     * PROMPT 06: Market Depth (Level 2 5-depth order book ladder).
     */
    public VBox buildOrderBookDepth(String symbol) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        String sym = symbol != null ? symbol : "RELIANCE";
        InstrumentRecord inst = exchangeService.getInstrument(sym).orElseThrow();
        List<OrderBookLevel> levels = exchangeService.getOrderBookDepth(sym);

        Label header = new Label("Level-2 Market Depth Order Book: " + inst.name() + " (" + sym + ")");
        header.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        TableView<OrderBookLevel> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<OrderBookLevel, String> colBidOrders = new TableColumn<>("Bid Orders");
        colBidOrders.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().bidOrdersCount())));

        TableColumn<OrderBookLevel, String> colBidQty = new TableColumn<>("Bid Qty");
        colBidQty.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatQuantity(d.getValue().bidQuantity())));

        TableColumn<OrderBookLevel, String> colBidPrice = new TableColumn<>("Bid Price (₹)");
        colBidPrice.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().bidPrice())));

        TableColumn<OrderBookLevel, String> colAskPrice = new TableColumn<>("Ask Price (₹)");
        colAskPrice.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().askPrice())));

        TableColumn<OrderBookLevel, String> colAskQty = new TableColumn<>("Ask Qty");
        colAskQty.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatQuantity(d.getValue().askQuantity())));

        TableColumn<OrderBookLevel, String> colAskOrders = new TableColumn<>("Ask Orders");
        colAskOrders.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().askOrdersCount())));

        table.getColumns().addAll(colBidOrders, colBidQty, colBidPrice, colAskPrice, colAskQty, colAskOrders);
        table.getItems().setAll(levels);

        BigDecimal spread = levels.get(0).askPrice().subtract(levels.get(0).bidPrice()).setScale(2, RoundingMode.HALF_UP);
        Label spreadLabel = new Label("Top of Book Spread: ₹ " + spread + " | Last Trade: " + Formatters.formatINR(inst.lastTradedPrice()));
        spreadLabel.setStyle("-fx-text-fill: #2563EB; -fx-font-weight: bold;");

        root.getChildren().addAll(header, table, spreadLabel);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    /**
     * PROMPT 06: Order Blotter & Trade Blotter.
     */
    public VBox buildBlotter() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        // Orders Table
        Label lblOrders = new Label("Order Blotter (Simulated Placed & Working Orders)");
        lblOrders.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0F172A;");

        TableView<OrderRecord> ordTable = new TableView<>();
        ordTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        ordTable.setPrefHeight(240);

        TableColumn<OrderRecord, String> cId = new TableColumn<>("Order ID");
        cId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().orderId()));

        TableColumn<OrderRecord, String> cTime = new TableColumn<>("Placed Time");
        cTime.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatTime(d.getValue().placedAt())));

        TableColumn<OrderRecord, String> cSym = new TableColumn<>("Symbol");
        cSym.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().symbol()));

        TableColumn<OrderRecord, String> cSide = new TableColumn<>("Side");
        cSide.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().side()));

        TableColumn<OrderRecord, String> cQty = new TableColumn<>("Total / Filled");
        cQty.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().totalQuantity() + " / " + d.getValue().filledQuantity()));

        TableColumn<OrderRecord, String> cPrice = new TableColumn<>("Price (₹)");
        cPrice.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().limitPrice())));

        TableColumn<OrderRecord, String> cStatus = new TableColumn<>("Status");
        cStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status()));

        ordTable.getColumns().addAll(cId, cTime, cSym, cSide, cQty, cPrice, cStatus);
        ordTable.getItems().setAll(exchangeService.getOrderBlotter("All"));

        // Trades Table
        Label lblTrades = new Label("Trade Blotter (Simulated Executed Fills)");
        lblTrades.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0F172A;");

        TableView<TradeRecord> trdTable = new TableView<>();
        trdTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        trdTable.setPrefHeight(220);

        TableColumn<TradeRecord, String> tId = new TableColumn<>("Trade ID");
        tId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().tradeId()));

        TableColumn<TradeRecord, String> tTime = new TableColumn<>("Executed At");
        tTime.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatTime(d.getValue().executedAt())));

        TableColumn<TradeRecord, String> tSym = new TableColumn<>("Symbol");
        tSym.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().symbol()));

        TableColumn<TradeRecord, String> tSide = new TableColumn<>("Side");
        tSide.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().side()));

        TableColumn<TradeRecord, String> tQty = new TableColumn<>("Quantity");
        tQty.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatQuantity(d.getValue().quantity())));

        TableColumn<TradeRecord, String> tPrice = new TableColumn<>("Fill Price (₹)");
        tPrice.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().price())));

        TableColumn<TradeRecord, String> tVal = new TableColumn<>("Gross Value (₹)");
        tVal.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().grossValueINR())));

        trdTable.getColumns().addAll(tId, tTime, tSym, tSide, tQty, tPrice, tVal);
        trdTable.getItems().setAll(exchangeService.getTradeBlotter());

        root.getChildren().addAll(lblOrders, ordTable, lblTrades, trdTable);
        return root;
    }

    /**
     * PROMPT 06: Market Regime Controls (Normal, Bull, Bear, Crash).
     */
    public VBox buildRegimesControl() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setMaxWidth(640);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 8, 0, 0, 2);");

        Label title = new Label("Market Regime & Simulation Parameters");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label warn = new Label("Notice: Changing market regimes dynamically modifies volatility, spread widening, and liquidation slippage in the simulated matching engine.");
        warn.setWrapText(true);
        warn.setStyle("-fx-text-fill: #B45309; -fx-font-size: 12px; -fx-background-color: #FEF3C7; -fx-padding: 8px 12px; -fx-background-radius: 6px; -fx-border-color: #FDE68A; -fx-border-radius: 6px;");

        ComboBox<String> regimeCombo = new ComboBox<>();
        regimeCombo.getItems().addAll("Normal", "Bull", "Bear", "Crash");
        regimeCombo.setValue("Normal");

        Slider speedSlider = new Slider(1, 10, 1);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(2);

        TextField seedField = new TextField("42");
        seedField.setPrefWidth(120);

        Button btnApply = new Button("Apply Regime Settings");
        btnApply.getStyleClass().add("btn-primary");
        btnApply.setOnAction(e -> {
            try {
                long seed = Long.parseLong(seedField.getText());
                int speed = (int) speedSlider.getValue();
                exchangeService.updateMarketRegime(regimeCombo.getValue(), speed, seed);
                onDataChanged.run();

                Alert a = new Alert(Alert.AlertType.INFORMATION, "Market regime updated to " + regimeCombo.getValue() + ".");
                a.showAndWait();
            } catch (Exception ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Invalid parameters: " + ex.getMessage());
                a.showAndWait();
            }
        });

        root.getChildren().addAll(
                title, warn,
                new HBox(12, new Label("Simulation Regime:"), regimeCombo),
                new VBox(6, new Label("Simulation Speed Factor (1x to 10x):"), speedSlider),
                new HBox(12, new Label("Deterministic Random Seed:"), seedField),
                btnApply
        );
        return root;
    }
}
