package com.lro.frontend;

import com.lro.frontend.bank.service.MockBankService;
import com.lro.frontend.common.Formatters;
import com.lro.frontend.common.UIComponents;
import com.lro.frontend.exchange.service.MockExchangeService;
import com.lro.frontend.lro.service.MockLroService;
import com.lro.frontend.views.BankViews;
import com.lro.frontend.views.ExchangeViews;
import com.lro.frontend.views.LroViews;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.Instant;

/**
 * Main JavaFX Desktop Application Shell implementing the LRO Financial
 * Ecosystem:
 * (A) Bank Simulator (Green Theme)
 * (B) Exchange Simulator (Royal Blue Theme)
 * (C) Liquidity Risk Optimizer (Amber Theme)
 * Fully aligned with official LRO branding, selection states, and color scheme.
 */
public class MainApp extends Application {

    public enum AppMode {
        BANK_SIMULATOR,
        EXCHANGE_SIMULATOR,
        LIQUIDITY_OPTIMIZER
    }

    private AppMode currentApp = AppMode.BANK_SIMULATOR;
    private String selectedBankId = "BANK-01-LRG";
    private String selectedInstrument = "RELIANCE";
    private String currentNavTag = "DASHBOARD";

    // Services
    private MockBankService bankService;
    private MockExchangeService exchangeService;
    private MockLroService lroService;

    // View Factories
    private BankViews bankViews;
    private ExchangeViews exchangeViews;
    private LroViews lroViews;

    // UI Structure
    private BorderPane mainLayout;
    private VBox navRail;
    private ScrollPane contentScrollPane;
    private Label clockLabel;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Liquidity Risk Optimizer (LRO) — Financial Simulator Ecosystem");

        initServices();

        mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("main-canvas");

        // 1. Top Section (Brand/Header Bar)
        mainLayout.setTop(createHeaderBar());

        // 2. Left Navigation Rail
        navRail = new VBox(6);
        navRail.getStyleClass().add("nav-rail");
        navRail.setPrefWidth(260);
        navRail.setMinWidth(260);
        mainLayout.setLeft(navRail);

        // 3. Center Content Area
        contentScrollPane = new ScrollPane();
        contentScrollPane.setFitToWidth(true);
        contentScrollPane.setFitToHeight(true);
        contentScrollPane.setStyle("-fx-background: #F8FAFC; -fx-background-color: transparent;");
        mainLayout.setCenter(contentScrollPane);

        // Start clock timer
        Timeline clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (clockLabel != null) {
                clockLabel.setText("Sim Clock: " + Formatters.formatTime(Instant.now()) + " IST");
            }
        }));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();

        // Initial View Render
        refreshNavigationRail();
        refreshCurrentView();

        Scene scene = new Scene(mainLayout, 1420, 900);
        try {
            scene.getStylesheets().add(getClass().getResource("/com/lro/frontend/css/style.css").toExternalForm());
        } catch (Exception ex) {
            System.err.println("Warning: style.css not found on classpath: " + ex.getMessage());
        }

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1150);
        primaryStage.setMinHeight(720);
        primaryStage.show();
    }

    private void initServices() {
        this.bankService = new MockBankService();
        this.exchangeService = new MockExchangeService();
        this.lroService = new MockLroService();

        Runnable onDataChanged = this::refreshCurrentView;
        this.bankViews = new BankViews(bankService, onDataChanged);
        this.exchangeViews = new ExchangeViews(exchangeService, onDataChanged);
        this.lroViews = new LroViews(lroService, bankService, onDataChanged);
    }

    private HBox createHeaderBar() {
        HBox header = new HBox(16);
        header.getStyleClass().add("app-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(13, 22, 13, 22));
        header.setPrefHeight(58);
        header.setMinHeight(56);

        // Brand Logo + Title
        HBox brandBox = new HBox(10);
        brandBox.setAlignment(Pos.CENTER_LEFT);

        try {
            var is = getClass().getResourceAsStream("/com/lro/frontend/images/lro_logo.png");
            if (is != null) {
                ImageView logoView = new ImageView(new Image(is));
                logoView.setFitHeight(26);
                logoView.setFitWidth(26);
                logoView.setPreserveRatio(true);
                logoView.setSmooth(true);

                StackPane logoBox = new StackPane(logoView);
                logoBox.setMinWidth(26);
                logoBox.setPrefWidth(26);
                logoBox.setMaxWidth(26);
                logoBox.setAlignment(Pos.CENTER);
                brandBox.getChildren().add(logoBox);
            }
        } catch (Exception ex) {
            System.err.println("Warning: Could not load logo: " + ex.getMessage());
        }

        Label brand = new Label("LRO ECOSYSTEM");
        brand.getStyleClass().add("brand-title");
        brandBox.getChildren().add(brand);

        // App Switcher Module Tabs
        HBox switcher = new HBox(8);
        switcher.setAlignment(Pos.CENTER_LEFT);

        Button btnBank = createTabButton("🏦 Bank Simulator", AppMode.BANK_SIMULATOR, "tab-btn-bank-active");
        Button btnExch = createTabButton("📈 Exchange Simulator", AppMode.EXCHANGE_SIMULATOR,
                "tab-btn-exchange-active");
        Button btnLro = createTabButton("⚡ Liquidity Optimizer", AppMode.LIQUIDITY_OPTIMIZER, "tab-btn-lro-active");

        switcher.getChildren().addAll(btnBank, btnExch, btnLro);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status & Clock
        clockLabel = new Label("Sim Clock: " + Formatters.formatTime(Instant.now()) + " IST");
        clockLabel.setStyle(
                "-fx-text-fill: #475569; -fx-font-size: 12px; -fx-font-family: monospace; -fx-font-weight: 600;");

        Button btnHelp = new Button("📖 Glossary");
        btnHelp.getStyleClass().add("btn-secondary");
        btnHelp.setOnAction(e -> UIComponents.showGlossaryDialog(
                "Liquidity Risk Optimizer Overview",
                "Educational and research simulation platform modelling bank balance sheets, continuous double-auction market matching, and liquidity risk optimization.",
                "Three integrated modules: Bank Simulator, Exchange Simulator, and LRO Decision Support Engine.",
                "Simulated environment complying with RBI/Basel III framework principles."));

        header.getChildren().addAll(brandBox, switcher, spacer, clockLabel, btnHelp);
        return header;
    }

    private Button createTabButton(String label, AppMode mode, String activeClass) {
        Button btn = new Button(label);
        boolean isActive = currentApp == mode;
        btn.getStyleClass().add(isActive ? activeClass : "tab-btn-inactive");
        btn.setOnAction(e -> {
            if (this.currentApp != mode) {
                this.currentApp = mode;
                this.currentNavTag = "DASHBOARD";
                updateHeaderBar();
                refreshNavigationRail();
                refreshCurrentView();
            }
        });
        return btn;
    }

    private void updateHeaderBar() {
        mainLayout.setTop(createHeaderBar());
    }

    private void refreshNavigationRail() {
        navRail.getChildren().clear();

        Label header = new Label();
        header.getStyleClass().add("nav-section-header");

        if (currentApp == AppMode.BANK_SIMULATOR) {
            header.setText("BANK SIMULATOR NAVIGATION");
            navRail.getChildren().addAll(
                    header,
                    createNavButton("🏛", "Institution Dashboard", "DASHBOARD"),
                    createNavButton("👥", "Customer Directory", "CUSTOMERS"),
                    createNavButton("💳", "Accounts", "ACCOUNTS"),
                    createNavButton("💰", "Fixed Deposits", "DEPOSITS"),
                    createNavButton("📊", "Loans", "LOANS"),
                    createNavButton("🏛", "Bonds & Equity", "TREASURY"),
                    createNavButton("📖", "General Ledger", "LEDGER"),
                    createNavButton("⌛", "Cash-Flow Maturity Ladder", "LADDER"));
        } else if (currentApp == AppMode.EXCHANGE_SIMULATOR) {
            header.setText("EXCHANGE SIMULATOR NAVIGATION");
            navRail.getChildren().addAll(
                    header,
                    createNavButton("🌐", "Market Overview", "DASHBOARD"),
                    createNavButton("📋", "Instrument Catalogue", "CATALOGUE"),
                    createNavButton("🎟", "Trading Order Ticket", "TICKET"),
                    createNavButton("📊", "Market Depth Order Book", "DEPTH"),
                    createNavButton("📜", "Order & Trade Blotters", "BLOTTER"),
                    createNavButton("⚙", "Market Regime Controls", "REGIMES"));
        } else {
            header.setText("LIQUIDITY OPTIMIZER (LRO)");
            navRail.getChildren().addAll(
                    header,
                    createNavButton("🎯", "Liquidity Dashboard", "DASHBOARD"),
                    createNavButton("💼", "Portfolio & Haircuts", "PORTFOLIO"),
                    createNavButton("⏳", "Maturity Mismatch Ladder", "LADDER"),
                    createNavButton("🤖", "ML Model Transparency", "INSIGHTS"),
                    createNavButton("📝", "Approval Review Queue", "RECOMMENDATIONS"),
                    createNavButton("⚡", "Stress Testing Workspace", "STRESS"),
                    createNavButton("🔔", "Surveillance Alerts", "ALERTS"));
        }
    }

    private Button createNavButton(String iconText, String title, String navTag) {
        Button btn = new Button();
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        boolean isActive = currentNavTag.equalsIgnoreCase(navTag);
        btn.getStyleClass().add(isActive ? "nav-item-active" : "nav-item");

        String cleanIcon = iconText.replaceAll("[\\p{M}\\uFE00-\\uFE0F]", "").trim();
        Label iconLbl = new Label(cleanIcon);
        iconLbl.setMinWidth(26);
        iconLbl.setPrefWidth(26);
        iconLbl.setMaxWidth(26);
        iconLbl.setAlignment(Pos.CENTER);
        iconLbl.setStyle(isActive
                ? "-fx-text-fill: #2563EB; -fx-font-size: 14px; -fx-font-family: 'Segoe UI Emoji', 'Segoe UI Symbol', sans-serif;"
                : "-fx-text-fill: #475569; -fx-font-size: 14px; -fx-font-family: 'Segoe UI Emoji', 'Segoe UI Symbol', sans-serif;");

        String cleanTitle = title.replaceAll("[\\uFE00-\\uFE0F]", "").trim();
        Label textLbl = new Label(cleanTitle);
        textLbl.setStyle(isActive
                ? "-fx-text-fill: #2563EB; -fx-font-weight: bold; -fx-font-size: 13px;"
                : "-fx-text-fill: #475569; -fx-font-weight: 600; -fx-font-size: 13px;");
        HBox.setHgrow(textLbl, Priority.ALWAYS);

        HBox row = new HBox(10, iconLbl, textLbl);
        row.setAlignment(Pos.CENTER_LEFT);
        btn.setGraphic(row);
        btn.setText(null);

        btn.setOnAction(e -> {
            this.currentNavTag = navTag;
            refreshNavigationRail();
            refreshCurrentView();
        });
        return btn;
    }

    private Button createNavButton(String fullTitle, String navTag) {
        String icon = "";
        String label = fullTitle;
        int spaceIdx = fullTitle.indexOf(' ');
        if (spaceIdx > 0) {
            icon = fullTitle.substring(0, spaceIdx);
            label = fullTitle.substring(spaceIdx + 1);
        }
        return createNavButton(icon, label, navTag);
    }

    private void refreshCurrentView() {
        VBox view;
        if (currentApp == AppMode.BANK_SIMULATOR) {
            view = switch (currentNavTag.toUpperCase()) {
                case "CUSTOMERS" -> bankViews.buildCustomersView(selectedBankId);
                case "ACCOUNTS" -> bankViews.buildAccountsView(selectedBankId);
                case "DEPOSITS" -> bankViews.buildDepositsView(selectedBankId);
                case "LOANS" -> bankViews.buildLoansView(selectedBankId);
                case "TREASURY" -> bankViews.buildTreasuryView(selectedBankId);
                case "LEDGER" -> bankViews.buildLedgerView(selectedBankId);
                case "LADDER" -> bankViews.buildLadderView(selectedBankId);
                default -> bankViews.buildDashboard(selectedBankId, bankId -> {
                    this.selectedBankId = bankId;
                    refreshCurrentView();
                });
            };
        } else if (currentApp == AppMode.EXCHANGE_SIMULATOR) {
            view = switch (currentNavTag.toUpperCase()) {
                case "CATALOGUE" -> exchangeViews.buildInstrumentCatalogue(sym -> {
                    this.selectedInstrument = sym;
                    this.currentNavTag = "DEPTH";
                    refreshNavigationRail();
                    refreshCurrentView();
                });
                case "TICKET" -> exchangeViews.buildOrderTicket(selectedInstrument);
                case "DEPTH" -> exchangeViews.buildOrderBookDepth(selectedInstrument);
                case "BLOTTER" -> exchangeViews.buildBlotter();
                case "REGIMES" -> exchangeViews.buildRegimesControl();
                default -> exchangeViews.buildOverview();
            };
        } else {
            view = switch (currentNavTag.toUpperCase()) {
                case "PORTFOLIO" -> lroViews.buildPortfolioView(selectedBankId);
                case "LADDER" -> bankViews.buildLadderView(selectedBankId);
                case "INSIGHTS" -> lroViews.buildModelInsightsView();
                case "RECOMMENDATIONS" -> lroViews.buildRecommendationsView(selectedBankId);
                case "STRESS" -> lroViews.buildStressScenariosView(selectedBankId);
                case "ALERTS" -> lroViews.buildAlertsReportsView(selectedBankId);
                default -> lroViews.buildDashboard(selectedBankId, bankId -> {
                    this.selectedBankId = bankId;
                    refreshCurrentView();
                });
            };
        }

        contentScrollPane.setContent(view);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
