package com.lro.frontend;

import com.lro.frontend.bank.service.MockBankService;
import com.lro.frontend.common.Formatters;
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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
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
        MockBankService bankService = new MockBankService();
        MockExchangeService exchangeService = new MockExchangeService();
        MockLroService lroService = new MockLroService();

        BankViews bankViews = new BankViews(bankService, () -> {});
        ExchangeViews exchangeViews = new ExchangeViews(exchangeService, () -> {});
        LroViews lroViews = new LroViews(lroService, bankService, () -> {});

        String cssPath = getClass().getResource("/com/lro/frontend/css/style.css").toExternalForm();

        // 1. Bank Simulator Shell
        BorderPane bankShell = buildFullShell("BANK", bankViews.buildDashboard("BANK-01-LRG", id -> {}));
        saveSceneSnapshot(bankShell, cssPath, ARTIFACTS_DIR + "lro_bank_full_app.png");

        // 2. Exchange Simulator Shell
        BorderPane exchShell = buildFullShell("EXCHANGE", exchangeViews.buildOverview());
        saveSceneSnapshot(exchShell, cssPath, ARTIFACTS_DIR + "lro_exchange_full_app.png");

        // 3. Liquidity Optimizer Shell
        BorderPane lroShell = buildFullShell("LRO", lroViews.buildDashboard("BANK-01-LRG", id -> {}));
        saveSceneSnapshot(lroShell, cssPath, ARTIFACTS_DIR + "lro_optimizer_full_app.png");
    }

    private BorderPane buildFullShell(String mode, Pane viewContent) {
        BorderPane mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("main-canvas");

        // Top Section: Header Bar with Logo and increased height (+10%)
        HBox header = new HBox(16);
        header.getStyleClass().add("app-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(13, 20, 13, 20));
        header.setPrefHeight(58);
        header.setMinHeight(56);

        // Brand Logo + Title
        HBox brandBox = new HBox(10);
        brandBox.setAlignment(Pos.CENTER_LEFT);

        try {
            var is = getClass().getResourceAsStream("/com/lro/frontend/images/lro_logo.png");
            if (is != null) {
                ImageView logoView = new ImageView(new Image(is));
                logoView.setFitHeight(34);
                logoView.setFitWidth(34);
                logoView.setPreserveRatio(true);
                logoView.setSmooth(true);
                brandBox.getChildren().add(logoView);
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

        Label clock = new Label("Sim Clock: " + Formatters.formatTime(Instant.now()) + " IST");
        clock.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px; -fx-font-family: monospace; -fx-font-weight: 600;");

        Button btnHelp = new Button("📖 Glossary");
        btnHelp.getStyleClass().add("btn-secondary");

        header.getChildren().addAll(brandBox, switcher, spacerTop, clock, btnHelp);
        mainLayout.setTop(header);

        // Left Navigation Rail
        VBox nav = new VBox();
        nav.getStyleClass().add("nav-rail");
        nav.setPrefWidth(240);
        nav.setMinWidth(240);

        Label navHeader = new Label();
        navHeader.getStyleClass().add("nav-section-header");

        if ("BANK".equals(mode)) {
            navHeader.setText("BANK SIMULATOR NAVIGATION");
            nav.getChildren().addAll(
                    navHeader,
                    createNavRow("🏛 Institution Dashboard", true),
                    createNavRow("👥 Customer Directory", false),
                    createNavRow("💳 Accounts & CASA", false),
                    createNavRow("💰 Fixed Deposits", false),
                    createNavRow("📊 Loans & NPA Management", false),
                    createNavRow("🏛 Treasury & G-Sec Holdings", false),
                    createNavRow("📖 General Ledger", false),
                    createNavRow("⌛ Cash-Flow Maturity Ladder", false)
            );
        } else if ("EXCHANGE".equals(mode)) {
            navHeader.setText("EXCHANGE SIMULATOR NAVIGATION");
            nav.getChildren().addAll(
                    navHeader,
                    createNavRow("📈 Market Overview", true),
                    createNavRow("📖 Instrument Catalogue", false),
                    createNavRow("⚡ Trading Order Ticket", false),
                    createNavRow("📊 Market Depth Order Book", false),
                    createNavRow("📋 Order & Trade Blotters", false),
                    createNavRow("⚖ Market Regime Controls", false)
            );
        } else {
            navHeader.setText("LIQUIDITY OPTIMIZER (LRO)");
            nav.getChildren().addAll(
                    navHeader,
                    createNavRow("📈 Liquidity Dashboard", true),
                    createNavRow("💳 Portfolio & Haircuts", false),
                    createNavRow("⌛ Maturity Mismatch Ladder", false),
                    createNavRow("🤖 ML Model Transparency", false),
                    createNavRow("📋 Approval Review Queue", false),
                    createNavRow("⚡ Stress Testing Workspace", false),
                    createNavRow("⚠️ Surveillance Alerts", false)
            );
        }

        mainLayout.setLeft(nav);

        // Center View wrapped in clean scroll pane
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #F8FAFC; -fx-background-color: transparent;");
        scrollPane.setContent(viewContent);
        mainLayout.setCenter(scrollPane);

        return mainLayout;
    }

    private Button createNavRow(String title, boolean isActive) {
        Button btn = new Button(title);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add(isActive ? "nav-item-active" : "nav-item");
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
