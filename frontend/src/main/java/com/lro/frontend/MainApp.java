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
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.Instant;

/**
 * Main JavaFX Desktop Application Shell implementing the shared multi-app ecosystem:
 * (A) Bank Simulator
 * (B) Exchange Simulator
 * (C) Liquidity Risk Optimizer (LRO)
 */
public class MainApp extends Application {

    public enum AppMode {
        BANK_SIMULATOR,
        EXCHANGE_SIMULATOR,
        LIQUIDITY_OPTIMIZER
    }

    private AppMode currentApp = AppMode.LIQUIDITY_OPTIMIZER;
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
        mainLayout.setStyle("-fx-background-color: #F8FAFC;");

        // 1. Top Section (Notice Banner + Brand/Header Bar)
        VBox topContainer = new VBox();
        topContainer.getChildren().addAll(UIComponents.createSimulatorNoticeBanner(), createHeaderBar());
        mainLayout.setTop(topContainer);

        // 2. Left Navigation Rail
        navRail = new VBox(8);
        navRail.getStyleClass().add("nav-rail");
        navRail.setPrefWidth(240);
        mainLayout.setLeft(navRail);

        // 3. Center Content Area
        contentScrollPane = new ScrollPane();
        contentScrollPane.setFitToWidth(true);
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

        Scene scene = new Scene(mainLayout, 1400, 880);
        try {
            scene.getStylesheets().add(getClass().getResource("/com/lro/frontend/css/style.css").toExternalForm());
        } catch (Exception ex) {
            System.err.println("Warning: style.css not found on classpath, using inline styles: " + ex.getMessage());
        }

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
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
        header.setPadding(new Insets(10, 20, 10, 20));

        // Brand Title
        Label brand = new Label("LRO ECOSYSTEM");
        brand.getStyleClass().add("brand-title");

        Label badge = new Label("RESEARCH SIMULATOR");
        badge.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4; -fx-border-color: #E2E8F0; -fx-border-radius: 4;");

        // App Switcher Tabs
        HBox switcher = new HBox(8);
        switcher.setAlignment(Pos.CENTER_LEFT);

        Button btnBank = createTabButton("🏦 Bank Simulator", AppMode.BANK_SIMULATOR, "#059669");
        Button btnExch = createTabButton("📈 Exchange Simulator", AppMode.EXCHANGE_SIMULATOR, "#4F46E5");
        Button btnLro = createTabButton("⚡ Liquidity Optimizer", AppMode.LIQUIDITY_OPTIMIZER, "#D97706");

        switcher.getChildren().addAll(btnBank, btnExch, btnLro);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status & Clock
        clockLabel = new Label("Sim Clock: " + Formatters.formatTime(Instant.now()) + " IST");
        clockLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px; -fx-font-family: monospace; -fx-font-weight: 600;");

        Button btnHelp = new Button("📖 Glossary");
        btnHelp.getStyleClass().add("btn-secondary");
        btnHelp.setOnAction(e -> UIComponents.showGlossaryDialog(
                "Liquidity Risk Optimizer Overview",
                "Educational and research simulation platform modelling bank balance sheets, continuous double-auction market matching, and liquidity risk optimization.",
                "Three integrated modules: Bank Simulator, Exchange Simulator, and LRO Decision Support Engine.",
                "Simulated environment complying with RBI/Basel III framework principles."
        ));

        header.getChildren().addAll(brand, badge, switcher, spacer, clockLabel, btnHelp);
        return header;
    }

    private Button createTabButton(String label, AppMode mode, String color) {
        Button btn = new Button(label);
        btn.setStyle(currentApp == mode
                ? "-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand;"
                : "-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-border-color: #E2E8F0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 14; -fx-cursor: hand;");
        btn.setOnAction(e -> {
            if (this.currentApp != mode) {
                this.currentApp = mode;
                this.currentNavTag = "DASHBOARD";
                // Update header switcher styles
                VBox top = (VBox) mainLayout.getTop();
                top.getChildren().set(1, createHeaderBar());
                refreshNavigationRail();
                refreshCurrentView();
            }
        });
        return btn;
    }

    private void refreshNavigationRail() {
        navRail.getChildren().clear();

        Label header = new Label();
        header.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B; -fx-padding: 0 10 6 10;");

        if (currentApp == AppMode.BANK_SIMULATOR) {
            header.setText("BANK SIMULATOR NAVIGATION");
            navRail.getChildren().addAll(
                    header,
                    createNavButton("📊 Institution Dashboard", "DASHBOARD"),
                    createNavButton("👥 Customer Directory", "CUSTOMERS"),
                    createNavButton("💳 Accounts & CASA", "ACCOUNTS"),
                    createNavButton("💰 Fixed Deposits", "DEPOSITS"),
                    createNavButton("📑 Loans & NPA Management", "LOANS"),
                    createNavButton("🏛️ Treasury & G-Sec Holdings", "TREASURY"),
                    createNavButton("📒 General Ledger", "LEDGER"),
                    createNavButton("⏳ Cash-Flow Maturity Ladder", "LADDER")
            );
        } else if (currentApp == AppMode.EXCHANGE_SIMULATOR) {
            header.setText("EXCHANGE SIMULATOR NAVIGATION");
            navRail.getChildren().addAll(
                    header,
                    createNavButton("🌐 Market Overview", "DASHBOARD"),
                    createNavButton("📋 Instrument Catalogue", "CATALOGUE"),
                    createNavButton("🎟️ Trading Order Ticket", "TICKET"),
                    createNavButton("📊 Market Depth Order Book", "DEPTH"),
                    createNavButton("📜 Order & Trade Blotters", "BLOTTER"),
                    createNavButton("⚙️ Market Regime Controls", "REGIMES")
            );
        } else {
            header.setText("LIQUIDITY OPTIMIZER (LRO)");
            navRail.getChildren().addAll(
                    header,
                    createNavButton("🎯 Liquidity Dashboard", "DASHBOARD"),
                    createNavButton("💼 Portfolio & Haircuts", "PORTFOLIO"),
                    createNavButton("⏳ Maturity Mismatch Ladder", "LADDER"),
                    createNavButton("🧠 ML Model Transparency", "INSIGHTS"),
                    createNavButton("📝 Approval Review Queue", "RECOMMENDATIONS"),
                    createNavButton("🌪️ Stress Testing Workspace", "STRESS"),
                    createNavButton("🔔 Surveillance Alerts", "ALERTS")
            );
        }
    }

    private Button createNavButton(String title, String navTag) {
        Button btn = new Button(title);
        btn.setMaxWidth(Double.MAX_VALUE);
        boolean isActive = currentNavTag.equalsIgnoreCase(navTag);
        btn.setStyle(isActive
                ? "-fx-background-color: #EFF6FF; -fx-text-fill: #2563EB; -fx-font-weight: bold; -fx-alignment: BASELINE_LEFT; -fx-padding: 8 12; -fx-background-radius: 6; -fx-border-color: #BFDBFE; -fx-border-radius: 6;"
                : "-fx-background-color: transparent; -fx-text-fill: #475569; -fx-alignment: BASELINE_LEFT; -fx-padding: 8 12; -fx-background-radius: 6;");

        btn.setOnAction(e -> {
            this.currentNavTag = navTag;
            refreshNavigationRail();
            refreshCurrentView();
        });
        return btn;
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
