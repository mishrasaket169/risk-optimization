package com.lro.frontend;

import com.lro.frontend.bank.service.BankService;
import com.lro.frontend.bank.service.MockBankService;
import com.lro.frontend.bank.service.PostgresBankService;
import com.lro.frontend.common.UIComponents;
import com.lro.frontend.exchange.service.MockExchangeService;
import com.lro.frontend.lro.service.MockLroService;
import com.lro.frontend.views.BankViews;
import com.lro.frontend.views.ExchangeViews;
import com.lro.frontend.views.LroViews;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RenderSnapshotsTest {

    private static final String ARTIFACTS_DIR = "C:/Users/Saket/.gemini/antigravity-ide/brain/6d741ffa-dbac-4ad8-85cd-fd1426c21b21/";

    @Test
    void renderAllThreeViews() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        try {
            Platform.startup(() -> {
                try {
                    renderFullAppScreenshots();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(() -> {
                try {
                    renderFullAppScreenshots();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean done = latch.await(25, TimeUnit.SECONDS);
        System.out.println("Snapshots rendered successfully: " + done);
    }

    private void renderFullAppScreenshots() throws Exception {
        BankService bankService;
        PostgresBankService pg = null;
        boolean isPg = false;
        try {
            pg = new PostgresBankService();
            if (pg.isConnected()) {
                bankService = pg;
                isPg = true;
            } else {
                bankService = new MockBankService();
            }
        } catch (Exception ex) {
            bankService = new MockBankService();
        }

        MockExchangeService exchangeService = new MockExchangeService();
        MockLroService lroService = new MockLroService();

        BankViews bankViews = new BankViews(bankService, () -> {});
        ExchangeViews exchangeViews = new ExchangeViews(exchangeService, () -> {});
        LroViews lroViews = new LroViews(lroService, bankService, () -> {});

        String cssPath = getClass().getResource("/com/lro/frontend/css/style.css").toExternalForm();

        // 1. Bank Simulator Shell - Dashboard
        BorderPane bankShell = buildFullShell("BANK", "Institution Dashboard", bankViews.buildDashboard("BANK-01-LRG", id -> {}), isPg);
        saveSceneSnapshot(bankShell, cssPath, ARTIFACTS_DIR + "lro_bank_full_app.png");

        // 1b. Bank Simulator - Customer Directory (500 items per page, full height)
        BorderPane bankCustShell = buildFullShell("BANK", "Customer Directory", bankViews.buildCustomersView("BANK-02-MED"), isPg);
        saveSceneSnapshot(bankCustShell, cssPath, ARTIFACTS_DIR + "lro_bank_customers.png");

        // 1c. Bank Simulator - Accounts & CASA (500 items per page, full height)
        BorderPane bankAccShell = buildFullShell("BANK", "Accounts & CASA", bankViews.buildAccountsView("BANK-02-MED"), isPg);
        saveSceneSnapshot(bankAccShell, cssPath, ARTIFACTS_DIR + "lro_bank_accounts.png");

        // 1d. Bank Simulator - Loans & NPA Management (500 items per page, full height)
        BorderPane bankLoansShell = buildFullShell("BANK", "Loans & NPA Management", bankViews.buildLoansView("BANK-02-MED"), isPg);
        saveSceneSnapshot(bankLoansShell, cssPath, ARTIFACTS_DIR + "lro_bank_loans.png");

        // 1e. Bank Simulator - Treasury & G-Sec Holdings (500 items per page, full height)
        BorderPane bankTreasuryShell = buildFullShell("BANK", "Treasury & G-Sec Holdings", bankViews.buildTreasuryView("BANK-01-LRG"), isPg);
        saveSceneSnapshot(bankTreasuryShell, cssPath, ARTIFACTS_DIR + "lro_bank_treasury.png");

        // 1f. Bank Simulator - Authoritative Double-Entry Ledger (500 items per page, full height)
        BorderPane bankLedgerShell = buildFullShell("BANK", "General Ledger", bankViews.buildLedgerView("BANK-01-LRG"), isPg);
        saveSceneSnapshot(bankLedgerShell, cssPath, ARTIFACTS_DIR + "lro_bank_ledger.png");

        // 1g. Bank Simulator - Institutional Balance Sheet (Executive KPI, Classified Statement & 2-Year Ledger)
        BorderPane bankBalanceSheetShell = buildFullShell("BANK", "Balance Sheet", bankViews.buildBalanceSheetView("BANK-01-LRG"), isPg);
        saveSceneSnapshot(bankBalanceSheetShell, cssPath, ARTIFACTS_DIR + "lro_bank_balance_sheet.png");

        // 2. Exchange Simulator Shell - Market Overview
        BorderPane exchShell = buildFullShell("EXCHANGE", "Market Overview", exchangeViews.buildOverview(), isPg);
        saveSceneSnapshot(exchShell, cssPath, ARTIFACTS_DIR + "lro_exchange_full_app.png");

        // 2b. Exchange Simulator Shell - Stock Directory
        BorderPane exchStockShell = buildFullShell("EXCHANGE", "Stock", exchangeViews.buildStocksView(sym -> {}), isPg);
        saveSceneSnapshot(exchStockShell, cssPath, ARTIFACTS_DIR + "lro_exchange_stocks.png");

        // 2c. Exchange Simulator Shell - Bond Directory
        BorderPane exchBondShell = buildFullShell("EXCHANGE", "Bond", exchangeViews.buildBondsView(sym -> {}), isPg);
        saveSceneSnapshot(exchBondShell, cssPath, ARTIFACTS_DIR + "lro_exchange_bonds.png");

        // 2d. Exchange Simulator Shell - Securities Directory
        BorderPane exchSecShell = buildFullShell("EXCHANGE", "Securities", exchangeViews.buildSecuritiesView(sym -> {}), isPg);
        saveSceneSnapshot(exchSecShell, cssPath, ARTIFACTS_DIR + "lro_exchange_securities.png");

        // 2e. Exchange Simulator - Focused Market Movement Card
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        double[] movementPoints = {24150.0, 24380.0, 24520.0, 24850.5, 24690.0, 24960.0, 25120.0};
        VBox lineChartNode = UIComponents.createLineMovementChart(
                days,
                movementPoints,
                3,
                "Today :",
                "24,850.50 (₹4,505 Cr)",
                "#4F46E5"
        );
        VBox movementCard = UIComponents.createChartCard(
                "Market Movement",
                "₹ 685.00 Cr",
                "+2.32%",
                "Total traded value & index movement in week",
                "Week",
                lineChartNode
        );
        movementCard.setPrefWidth(850);
        movementCard.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 20px;");
        saveSceneSnapshot(movementCard, cssPath, ARTIFACTS_DIR + "lro_exchange_market_movement.png");

        // 3. Liquidity Optimizer Shell
        BorderPane lroShell = buildFullShell("LRO", "Liquidity Dashboard", lroViews.buildDashboard("BANK-01-LRG", id -> {}), isPg);
        saveSceneSnapshot(lroShell, cssPath, ARTIFACTS_DIR + "lro_optimizer_full_app.png");

        if (pg != null) {
            pg.close();
        }
    }

    private BorderPane buildFullShell(String mode, String activeNavTitle, Pane viewContent, boolean isPg) {
        BorderPane mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("main-canvas");

        // Top Section: Header Bar with Logo and increased height (+10%)
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

        // Module Switcher Tabs with authentic colors
        HBox switcher = new HBox(8);
        switcher.setAlignment(Pos.CENTER_LEFT);

        Button btnBank = new Button("🏦 Bank Simulator");
        btnBank.getStyleClass().add("BANK".equals(mode) ? "tab-btn-bank-active" : "tab-btn-inactive");

        Button btnExch = new Button("📈 Exchange Simulator");
        btnExch.getStyleClass().add("EXCHANGE".equals(mode) ? "tab-btn-exchange-active" : "tab-btn-inactive");

        Button btnLro = new Button("⚡ Liquidity Optimizer");
        btnLro.getStyleClass().add("LRO".equals(mode) ? "tab-btn-lro-active" : "tab-btn-inactive");

        switcher.getChildren().addAll(btnBank, btnExch, btnLro);

        Region spacerTop = new Region();
        HBox.setHgrow(spacerTop, Priority.ALWAYS);

        Button btnToggleSim = new Button("⏸ Pause");
        btnToggleSim.getStyleClass().add("btn-secondary");
        btnToggleSim.setStyle("-fx-font-size: 12px; -fx-padding: 6 14 6 14; -fx-background-radius: 6; -fx-font-weight: 600; -fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-border-color: #CBD5E1; -fx-border-radius: 6;");

        header.getChildren().addAll(brandBox, switcher, spacerTop, btnToggleSim);
        mainLayout.setTop(header);

        // Left Navigation Rail
        VBox nav = new VBox();
        nav.getStyleClass().add("nav-rail");
        nav.setPrefWidth(260);
        nav.setMinWidth(260);

        Label navHeader = new Label();
        navHeader.getStyleClass().add("nav-section-header");

        String selectedTitle = activeNavTitle != null ? activeNavTitle : 
                ("BANK".equals(mode) ? "Institution Dashboard" : "EXCHANGE".equals(mode) ? "Market Overview" : "Liquidity Dashboard");

        if ("BANK".equals(mode)) {
            navHeader.setText("BANK SIMULATOR NAVIGATION");
            nav.getChildren().addAll(
                    navHeader,
                    createNavRow("🏛", "Institution Dashboard", "Institution Dashboard".equals(selectedTitle)),
                    createNavRow("👥", "Customer Directory", "Customer Directory".equals(selectedTitle)),
                    createNavRow("💳", "Accounts & CASA", "Accounts & CASA".equals(selectedTitle)),
                    createNavRow("📊", "Loans & NPA Management", "Loans & NPA Management".equals(selectedTitle)),
                    createNavRow("🏛", "Treasury & G-Sec Holdings", "Treasury & G-Sec Holdings".equals(selectedTitle)),
                    createNavRow("📖", "General Ledger", "General Ledger".equals(selectedTitle)),
                    createNavRow("⚖", "Balance Sheet", "Balance Sheet".equals(selectedTitle)),
                    createNavRow("⌛", "Cash-Flow Maturity Ladder", "Cash-Flow Maturity Ladder".equals(selectedTitle))
            );
        } else if ("EXCHANGE".equals(mode)) {
            navHeader.setText("EXCHANGE SIMULATOR NAVIGATION");
            nav.getChildren().addAll(
                    navHeader,
                    createNavRow("🌐", "Market Overview", "Market Overview".equals(selectedTitle)),
                    createNavRow("📈", "Stock", "Stock".equals(selectedTitle)),
                    createNavRow("🏛", "Bond", "Bond".equals(selectedTitle)),
                    createNavRow("🛡", "Securities", "Securities".equals(selectedTitle)),
                    createNavRow("🎟", "Trading Order Ticket", "Trading Order Ticket".equals(selectedTitle)),
                    createNavRow("📊", "Market Depth Order Book", "Market Depth Order Book".equals(selectedTitle)),
                    createNavRow("⚙", "Market Regime Controls", "Market Regime Controls".equals(selectedTitle))
            );
        } else {
            navHeader.setText("LIQUIDITY OPTIMIZER (LRO)");
            nav.getChildren().addAll(
                    navHeader,
                    createNavRow("🎯", "Liquidity Dashboard", "Liquidity Dashboard".equals(selectedTitle)),
                    createNavRow("💼", "Portfolio & Haircuts", "Portfolio & Haircuts".equals(selectedTitle)),
                    createNavRow("⏳", "Maturity Mismatch Ladder", "Maturity Mismatch Ladder".equals(selectedTitle)),
                    createNavRow("🤖", "ML Model Transparency", "ML Model Transparency".equals(selectedTitle)),
                    createNavRow("📝", "Approval Review Queue", "Approval Review Queue".equals(selectedTitle)),
                    createNavRow("⚡", "Stress Testing Workspace", "Stress Testing Workspace".equals(selectedTitle)),
                    createNavRow("🔔", "Surveillance Alerts", "Surveillance Alerts".equals(selectedTitle))
            );
        }

        mainLayout.setLeft(nav);

        // Center View wrapped in clean scroll pane
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: #F8FAFC; -fx-background-color: transparent;");
        scrollPane.setContent(viewContent);
        mainLayout.setCenter(scrollPane);

        return mainLayout;
    }

    private Button createNavRow(String iconText, String title, boolean isActive) {
        Button btn = new Button();
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
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
        return btn;
    }

    private void saveSceneSnapshot(Pane content, String cssPath, String outputPath) throws Exception {
        Scene scene = new Scene(content, 1420, 920);
        if (cssPath != null) {
            scene.getStylesheets().add(cssPath);
        }

        content.applyCss();
        content.layout();

        WritableImage image = scene.snapshot(null);
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader pr = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bi.setRGB(x, y, pr.getArgb(x, y));
            }
        }

        File outFile = new File(outputPath);
        if (outFile.getParentFile() != null) {
            outFile.getParentFile().mkdirs();
        }
        ImageIO.write(bi, "png", outFile);
        System.out.println("Snapshot successfully written to: " + outFile.getAbsolutePath());
    }
}
