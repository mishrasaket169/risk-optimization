package com.lro.frontend;

import com.lro.frontend.bank.service.BankService;
import com.lro.frontend.bank.service.BankTransactionSimulator;
import com.lro.frontend.bank.service.MockBankService;
import com.lro.frontend.bank.service.PostgresBankService;
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
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main JavaFX Desktop Application Shell implementing the LRO Financial
 * Ecosystem:
 * (A) Bank Simulator (Green Theme)
 * (B) Exchange Simulator (Royal Blue Theme)
 * (C) Liquidity Risk Optimizer (Amber Theme)
 * Fully connected to authoritative PostgreSQL database with live CDC LISTEN/NOTIFY
 * and real-time high-throughput transaction simulator.
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
    private BankService bankService;
    private PostgresBankService pgBankService;
    private MockExchangeService exchangeService;
    private MockLroService lroService;
    private BankTransactionSimulator simulator;

    private boolean isPostgresConnected = false;
    private final AtomicBoolean needsLiveRefresh = new AtomicBoolean(false);

    // View Factories
    private BankViews bankViews;
    private ExchangeViews exchangeViews;
    private LroViews lroViews;

    // UI Structure
    private BorderPane mainLayout;
    private VBox navRail;
    private ScrollPane contentScrollPane;
    private Button btnToggleSim;

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

        // Live data refresh timeline (debounced to 250ms for rapid live ledger updates)
        Timeline liveRefreshTimeline = new Timeline(new KeyFrame(Duration.millis(250), e -> {
            if (needsLiveRefresh.compareAndSet(true, false)) {
                if (currentApp == AppMode.BANK_SIMULATOR) {
                    bankViews.refreshActiveView();
                }
            }
        }));
        liveRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        liveRefreshTimeline.play();

        // Initial View Render
        refreshNavigationRail();
        refreshCurrentView();

        Scene scene = new Scene(mainLayout, 1420, 900);
        try {
            scene.getStylesheets().add(getClass().getResource("/com/lro/frontend/css/style.css").toExternalForm());
        } catch (Exception ex) {
            System.err.println("Warning: style.css not found on classpath: " + ex.getMessage());
        }

        primaryStage.setOnCloseRequest(e -> {
            if (simulator != null) {
                simulator.stop();
            }
            if (pgBankService != null) {
                pgBankService.close();
            }
        });

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1150);
        primaryStage.setMinHeight(720);
        primaryStage.show();
    }

    private void initServices() {
        try {
            System.out.println("Connecting to authoritative PostgreSQL database (lro_db)...");
            PostgresBankService pg = new PostgresBankService();
            if (pg.isConnected()) {
                this.pgBankService = pg;
                this.bankService = pg;
                this.isPostgresConnected = true;
                System.out.println("Successfully connected to live PostgreSQL database (lro_db)!");
            } else {
                System.out.println("PostgreSQL connection check failed, falling back to MockBankService.");
                this.bankService = new MockBankService();
                this.isPostgresConnected = false;
            }
        } catch (Exception ex) {
            System.err.println("Error connecting to PostgreSQL: " + ex.getMessage() + ", falling back to MockBankService.");
            this.bankService = new MockBankService();
            this.isPostgresConnected = false;
        }

        if (isPostgresConnected && pgBankService != null) {
            this.simulator = new BankTransactionSimulator(pgBankService.getDataSource());
            this.simulator.start();
            pgBankService.addDataChangeListener(() -> needsLiveRefresh.set(true));
        }

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

        // Pause / Resume Button placed where Glossary was (far right)
        if (simulator != null) {
            btnToggleSim = new Button(simulator.isRunning() ? "⏸ Pause" : "▶ Resume");
            btnToggleSim.getStyleClass().add("btn-secondary");
            btnToggleSim.setStyle(simulator.isRunning()
                    ? "-fx-font-size: 12px; -fx-padding: 6 14 6 14; -fx-background-radius: 6; -fx-font-weight: 600; -fx-cursor: hand; -fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-border-color: #CBD5E1; -fx-border-radius: 6;"
                    : "-fx-font-size: 12px; -fx-padding: 6 14 6 14; -fx-background-radius: 6; -fx-font-weight: 600; -fx-cursor: hand; -fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-border-color: #FCD34D; -fx-border-radius: 6;");

            btnToggleSim.setOnAction(e -> {
                if (simulator.isRunning()) {
                    simulator.pause();
                    btnToggleSim.setText("▶ Resume");
                    btnToggleSim.setStyle("-fx-font-size: 12px; -fx-padding: 6 14 6 14; -fx-background-radius: 6; -fx-font-weight: 600; -fx-cursor: hand; -fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-border-color: #FCD34D; -fx-border-radius: 6;");
                } else {
                    simulator.resume();
                    btnToggleSim.setText("⏸ Pause");
                    btnToggleSim.setStyle("-fx-font-size: 12px; -fx-padding: 6 14 6 14; -fx-background-radius: 6; -fx-font-weight: 600; -fx-cursor: hand; -fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");
                }
            });

            header.getChildren().addAll(brandBox, switcher, spacer, btnToggleSim);
        } else {
            header.getChildren().addAll(brandBox, switcher);
        }
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
                    createNavButton("📊", "Loans", "LOANS"),
                    createNavButton("🏛", "Bonds & Equity", "TREASURY"),
                    createNavButton("📖", "General Ledger", "LEDGER"),
                    createNavButton("⌛", "Cash-Flow Maturity Ladder", "LADDER"));
        } else if (currentApp == AppMode.EXCHANGE_SIMULATOR) {
            header.setText("EXCHANGE SIMULATOR NAVIGATION");
            navRail.getChildren().addAll(
                    header,
                    createNavButton("🌐", "Market Overview", "DASHBOARD"),
                    createNavButton("📈", "Stock", "STOCKS"),
                    createNavButton("🏛", "Bond", "BONDS"),
                    createNavButton("🛡", "Securities", "SECURITIES"),
                    createNavButton("🎟", "Trading Order Ticket", "TICKET"),
                    createNavButton("📊", "Market Depth Order Book", "DEPTH"),
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

    private void refreshCurrentView() {
        VBox view;
        if (currentApp == AppMode.BANK_SIMULATOR) {
            view = switch (currentNavTag.toUpperCase()) {
                case "CUSTOMERS" -> bankViews.buildCustomersView(selectedBankId);
                case "ACCOUNTS" -> bankViews.buildAccountsView(selectedBankId);
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
                case "STOCKS" -> exchangeViews.buildStocksView(sym -> {
                    this.selectedInstrument = sym;
                    this.currentNavTag = "DEPTH";
                    refreshNavigationRail();
                    refreshCurrentView();
                });
                case "BONDS" -> exchangeViews.buildBondsView(sym -> {
                    this.selectedInstrument = sym;
                    this.currentNavTag = "DEPTH";
                    refreshNavigationRail();
                    refreshCurrentView();
                });
                case "SECURITIES" -> exchangeViews.buildSecuritiesView(sym -> {
                    this.selectedInstrument = sym;
                    this.currentNavTag = "DEPTH";
                    refreshNavigationRail();
                    refreshCurrentView();
                });
                case "CATALOGUE" -> exchangeViews.buildStocksView(sym -> {
                    this.selectedInstrument = sym;
                    this.currentNavTag = "DEPTH";
                    refreshNavigationRail();
                    refreshCurrentView();
                });
                case "TICKET" -> exchangeViews.buildOrderTicket(selectedInstrument);
                case "DEPTH" -> exchangeViews.buildOrderBookDepth(selectedInstrument);
                case "BLOTTER" -> exchangeViews.buildStocksView(sym -> {
                    this.selectedInstrument = sym;
                    this.currentNavTag = "DEPTH";
                    refreshNavigationRail();
                    refreshCurrentView();
                });
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
