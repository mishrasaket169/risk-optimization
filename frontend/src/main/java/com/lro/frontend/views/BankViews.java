package com.lro.frontend.views;

import com.lro.frontend.bank.model.BankModels.*;
import com.lro.frontend.bank.service.BankService;
import com.lro.frontend.common.Formatters;
import com.lro.frontend.common.UIComponents;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Bank Simulator View Builders implementing Prompts 02, 03, and 04:
 * Dashboard, Bank Selector, Customer directory, Accounts, Deposits, Loans & NPAs,
 * Treasury holdings, Double-entry Ledger, and Cash-Flow maturity ladder.
 */
@SuppressWarnings("unchecked")
public class BankViews {

    private final BankService bankService;
    private final Runnable onDataChanged;
    private Runnable activeViewRefresher;

    public BankViews(BankService bankService, Runnable onDataChanged) {
        this.bankService = bankService;
        this.onDataChanged = onDataChanged;
    }

    public void refreshActiveView() {
        if (activeViewRefresher != null) {
            activeViewRefresher.run();
        } else if (onDataChanged != null) {
            onDataChanged.run();
        }
    }

    /**
     * PROMPT 02: Bank Overview Dashboard with 4-bank selector, asset/liability KPIs,
     * NPA ratios, maturity summaries, and simulation disclaimers.
     */
    public VBox buildDashboard(String selectedBankId, java.util.function.Consumer<String> onSelectBank) {
        this.activeViewRefresher = null;
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        BankProfile profile = bankService.getBankProfile(selectedBankId).orElseThrow();

        // 1. Bank Selector Bar (LRO Card Style)
        HBox selectorBar = new HBox(12);
        selectorBar.setAlignment(Pos.CENTER_LEFT);
        selectorBar.getStyleClass().add("lro-card");
        selectorBar.setPadding(new Insets(12, 16, 12, 16));

        Label selLabel = new Label("Selected Financial Institution:");
        selLabel.setStyle("-fx-text-fill: #0F172A; -fx-font-weight: bold;");

        ComboBox<String> bankCombo = new ComboBox<>();
        for (BankProfile p : bankService.getAllBankProfiles()) {
            bankCombo.getItems().add(p.bankId() + " — " + p.bankName());
        }
        bankCombo.setValue(profile.bankId() + " — " + profile.bankName());
        bankCombo.setOnAction(e -> {
            String val = bankCombo.getValue();
            if (val != null) {
                String id = val.split(" — ")[0].trim();
                onSelectBank.accept(id);
            }
        });

        Label statusChip = UIComponents.createStatusPill(profile.healthStatus(), profile.healthStatus());
        Label tierLabel = new Label("(" + profile.tierDescription() + ")");
        tierLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        selectorBar.getChildren().addAll(selLabel, bankCombo, statusChip, tierLabel);

        // 2. Main Grid: Center Content (KPIs + Chart + Table) & Right Widget Column
        HBox mainGrid = new HBox(20);
        mainGrid.setAlignment(Pos.TOP_LEFT);

        // =========================================================================
        // CENTER COLUMN (Bank Simulator Dashboard Center)
        // =========================================================================
        VBox centerCol = new VBox(16);
        HBox.setHgrow(centerCol, Priority.ALWAYS);

        // Row 1: 3 LRO KPI Cards matching Bank Simulator metrics
        HBox kpiRow = new HBox(12);
        VBox cardAssets = UIComponents.createKpiCard("Total Assets", Formatters.formatINRCrores(profile.totalAssets()), "Verified Book Size", "Total resources owned by the bank including loans and investments.", "#0284C7");
        VBox cardDeposits = UIComponents.createKpiCard("Total Deposits", Formatters.formatINRCrores(profile.totalDeposits()), "Customer CASA & FDs", "Core funding source from retail and corporate depositors.", "#059669");
        VBox cardLoans = UIComponents.createKpiCard("Gross Loan Portfolio", Formatters.formatINRCrores(profile.totalLoans()), "Sanctioned Credit Assets", "Total outstanding advances to retail, SME and corporate borrowers.", "#4F46E5");
        kpiRow.getChildren().addAll(cardAssets, cardDeposits, cardLoans);

        // Row 2: Asset & Liability Flow Dynamic Chart Card
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        double[] advances = {4200, 5800, 6400, 7500, 8100, 6900, 7800};
        double[] deposits = {3800, 5100, 5900, 4505, 7400, 6100, 7200};
        VBox chartNode = UIComponents.createDualBarChart(days, deposits, advances, "Wed", "₹4,505 Cr", "#059669");

        VBox chartCard = UIComponents.createChartCard(
                "Asset & Liability Flow Statistics",
                Formatters.formatINRCrores(profile.cashBalance().divide(BigDecimal.valueOf(10), RoundingMode.HALF_UP)),
                "+2.32%",
                "Total net flow in week",
                "Week",
                chartNode
        );

        // Row 3: Data Table Card (Accounts & Credit Lines)
        VBox tableCard = new VBox(12);
        tableCard.getStyleClass().add("lro-card");

        HBox tableHeader = new HBox();
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        Label tblTitle = new Label("Customer Accounts & Credit Lines");
        tblTitle.getStyleClass().add("card-title");
        HBox.setHgrow(tblTitle, Priority.ALWAYS);

        Label viewAll = new Label("View All ↗");
        viewAll.setStyle("-fx-text-fill: #2563EB; -fx-font-weight: 700; -fx-font-size: 11px; -fx-cursor: hand;");
        tableHeader.getChildren().addAll(tblTitle, viewAll);

        TableView<AccountRecord> table = new TableView<>();
        table.setPrefHeight(230);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<AccountRecord, String> colAccId = new TableColumn<>("Account ID");
        colAccId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().accountId()));

        TableColumn<AccountRecord, String> colCustId = new TableColumn<>("Customer ID");
        colCustId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().customerId()));

        TableColumn<AccountRecord, String> colType = new TableColumn<>("Account Type");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().accountType()));

        TableColumn<AccountRecord, String> colBal = new TableColumn<>("Available Balance");
        colBal.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().availableBalance())));

        TableColumn<AccountRecord, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(UIComponents.createStatusPill(item, item.equalsIgnoreCase("Active") ? "success" : "process"));
                }
            }
        });

        table.getColumns().addAll(colAccId, colCustId, colType, colBal, colStatus);
        List<AccountRecord> accs = bankService.getAccounts(selectedBankId, "");
        table.getItems().setAll(accs.size() > 5 ? accs.subList(0, 5) : accs);

        tableCard.getChildren().addAll(tableHeader, table);

        centerCol.getChildren().addAll(kpiRow, chartCard, tableCard);

        // =========================================================================
        // RIGHT COLUMN (Bank Simulator Widgets: Branch Reach & Deposit Mix)
        // =========================================================================
        VBox rightCol = new VBox(16);
        rightCol.setPrefWidth(310);
        rightCol.setMinWidth(290);

        // Widget 1: Regional Branch Network
        VBox regWidget = UIComponents.createRegionalExposureWidget(
                "Branch Network & Reach",
                "Period August 2026",
                "Western Zone Metro",
                "92% Quality",
                "12,000 Accounts",
                "#059669"
        );

        // Widget 2: Deposit Composition
        List<UIComponents.SourceItem> sources = List.of(
                new UIComponents.SourceItem("Savings CASA", "25,000", "#0284C7"),
                new UIComponents.SourceItem("Term Deposits", "12,500", "#059669"),
                new UIComponents.SourceItem("NRI & Demat", "4,500", "#4F46E5")
        );
        VBox sourceWidget = UIComponents.createDonutSourceWidget(
                "Deposit Composition",
                "Core retail vs wholesale mix",
                "Total Book",
                "₹72.5K Cr",
                sources,
                "See Details ↗",
                () -> UIComponents.showGlossaryDialog(
                        "Deposit Composition Analysis",
                        "Breakdown of retail and corporate customer deposit accounts determining funding stickiness and run-off vulnerability under liquidity stress.",
                        "Core Deposits = Savings Accounts + Retail Term Deposits",
                        "High CASA ratios reduce funding cost and bolster regulatory NSFR."
                ),
                "#059669"
        );

        // Widget 3 & 4: Gross NPA Ratio and Net Interest Margin (NIM)
        VBox npaCard = UIComponents.createKpiCard(
                "Gross NPA Ratio",
                Formatters.formatPercent(profile.grossNpaRatio()),
                profile.grossNpaRatio().compareTo(new BigDecimal("2.5")) <= 0 ? "Optimal Quality" : "Monitored Quality",
                "Ratio of non-performing assets to total gross advances indicating loan portfolio health.",
                "#059669"
        );
        npaCard.setPadding(new Insets(10, 14, 10, 14));

        VBox nimCard = UIComponents.createKpiCard(
                "Net Interest Margin (NIM)",
                Formatters.formatPercent(profile.netInterestMargin()),
                "Annualized Yield Spread",
                "Difference between interest earned and interest expended divided by interest-earning assets.",
                "#E11D48"
        );
        nimCard.setPadding(new Insets(10, 14, 10, 14));

        rightCol.getChildren().addAll(regWidget, sourceWidget, npaCard, nimCard);

        mainGrid.getChildren().addAll(centerCol, rightCol);
        root.getChildren().addAll(selectorBar, mainGrid);

        this.activeViewRefresher = () -> {
            BankProfile p = bankService.getBankProfile(selectedBankId).orElse(profile);
            UIComponents.updateKpiCardValue(cardAssets, Formatters.formatINRCrores(p.totalAssets()));
            UIComponents.updateKpiCardValue(cardDeposits, Formatters.formatINRCrores(p.totalDeposits()));
            UIComponents.updateKpiCardValue(cardLoans, Formatters.formatINRCrores(p.totalLoans()));
            UIComponents.updateKpiCardValue(npaCard, Formatters.formatPercent(p.grossNpaRatio()));
            UIComponents.updateKpiCardValue(nimCard, Formatters.formatPercent(p.netInterestMargin()));

            List<AccountRecord> currentAccs = bankService.getAccounts(selectedBankId, "");
            table.getItems().setAll(currentAccs.size() > 5 ? currentAccs.subList(0, 5) : currentAccs);
        };

        return root;
    }

    /**
     * PROMPT 03: Customers Directory Screen with search, segment filter, and KYC badges.
     */
    public VBox buildCustomersView(String bankId) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Search customer name or ID...");
        searchField.setPrefWidth(260);

        ComboBox<String> segmentCombo = new ComboBox<>();
        segmentCombo.getItems().addAll("All", "Retail", "SME", "Corporate");
        segmentCombo.setValue("All");

        Label countLabel = new Label();
        countLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        filterBar.getChildren().addAll(new Label("Filter Segment:"), segmentCombo, searchField, countLabel);

        TableView<CustomerRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<CustomerRecord, String> colId = new TableColumn<>("Customer ID");
        colId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().customerId()));

        TableColumn<CustomerRecord, String> colName = new TableColumn<>("Full Name");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().fullName()));

        TableColumn<CustomerRecord, String> colSegment = new TableColumn<>("Segment");
        colSegment.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().segment()));

        TableColumn<CustomerRecord, String> colKyc = new TableColumn<>("KYC Status");
        colKyc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().kycStatus()));
        colKyc.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label chip = UIComponents.createStatusChip(item, item.equalsIgnoreCase("Verified") ? "success" : "danger");
                    setGraphic(chip);
                    setText(null);
                }
            }
        });

        TableColumn<CustomerRecord, String> colLoc = new TableColumn<>("Location Category");
        colLoc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().locationCategory()));
        colLoc.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String type = switch (item) {
                        case "Tier 1" -> "info";
                        case "Tier 2" -> "default";
                        case "Tier 3" -> "warning";
                        default -> "normal";
                    };
                    Label chip = UIComponents.createStatusChip(item, type);
                    setGraphic(chip);
                    setText(null);
                }
            }
        });

        TableColumn<CustomerRecord, String> colRisk = new TableColumn<>("Risk Rating");
        colRisk.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().riskCategory()));

        TableColumn<CustomerRecord, String> colAccs = new TableColumn<>("Linked Accounts");
        colAccs.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().activeAccountsCount())));

        TableColumn<CustomerRecord, String> colRelVal = new TableColumn<>("Relationship Value");
        colRelVal.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().totalRelationshipValue())));

        TableColumn<CustomerRecord, String> colBranch = new TableColumn<>("Assigned Branch");
        colBranch.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().branchName()));

        table.getColumns().addAll(colId, colName, colSegment, colKyc, colLoc, colRisk, colAccs, colRelVal, colBranch);

        UIComponents.PaginationBar<CustomerRecord> paginationBar = new UIComponents.PaginationBar<>(table, 500);

        Runnable refreshTable = () -> {
            List<CustomerRecord> list = bankService.getCustomers(bankId, searchField.getText(), segmentCombo.getValue());
            paginationBar.setData(list);
            countLabel.setText("Total: " + String.format("%,d", list.size()) + " records");
        };

        searchField.textProperty().addListener((obs, o, n) -> refreshTable.run());
        segmentCombo.setOnAction(e -> refreshTable.run());
        refreshTable.run();

        this.activeViewRefresher = refreshTable;
        root.getChildren().addAll(filterBar, table, paginationBar);
        return root;
    }

    /**
     * PROMPT 03: Operational Accounts & CASA Balances Screen.
     */
    public VBox buildAccountsView(String bankId) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Search account ID or customer ID...");
        searchField.setPrefWidth(280);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("All", "Saving", "Salary", "Current", "Fixed Deposit", "Recurring Deposit", "NRI", "Demat");
        typeCombo.setValue("All");

        Label countLabel = new Label();
        countLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        filterBar.getChildren().addAll(new Label("Filter Account Type:"), typeCombo, searchField, countLabel);

        TableView<AccountRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<AccountRecord, String> colAccId = new TableColumn<>("Account ID");
        colAccId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().accountId()));

        TableColumn<AccountRecord, String> colCustId = new TableColumn<>("Customer ID");
        colCustId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().customerId()));

        TableColumn<AccountRecord, String> colType = new TableColumn<>("Account Type");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().accountType()));

        TableColumn<AccountRecord, String> colBal = new TableColumn<>("Available Balance");
        colBal.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().availableBalance())));

        TableColumn<AccountRecord, String> colHold = new TableColumn<>("Lien / Hold Amount");
        colHold.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().holdAmount())));

        TableColumn<AccountRecord, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status()));

        TableColumn<AccountRecord, String> colOpened = new TableColumn<>("Opened Date");
        colOpened.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatDate(d.getValue().openedDate())));

        table.getColumns().addAll(colAccId, colCustId, colType, colBal, colHold, colStatus, colOpened);

        UIComponents.PaginationBar<AccountRecord> paginationBar = new UIComponents.PaginationBar<>(table, 500);

        Runnable refreshTable = () -> {
            List<AccountRecord> list = bankService.getAccounts(bankId, searchField.getText(), typeCombo.getValue());
            paginationBar.setData(list);
            countLabel.setText("Total: " + String.format("%,d", list.size()) + " accounts");
        };

        searchField.textProperty().addListener((obs, o, n) -> refreshTable.run());
        typeCombo.setOnAction(e -> refreshTable.run());
        refreshTable.run();

        this.activeViewRefresher = refreshTable;
        root.getChildren().addAll(filterBar, table, paginationBar);
        return root;
    }

    /**
     * PROMPT 03: Fixed & Recurring Deposits Screen.
     */
    public VBox buildDepositsView(String bankId) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        Label sectionHeader = new Label("Active Fixed Term & Recurring Deposits Portfolio");
        sectionHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        TextField searchField = new TextField();
        searchField.setPromptText("Search deposit reference or customer ID...");
        searchField.setPrefWidth(280);

        Label countLabel = new Label();
        countLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        filterBar.getChildren().addAll(sectionHeader, searchField, countLabel);

        TableView<DepositRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<DepositRecord, String> colDepId = new TableColumn<>("Deposit Reference");
        colDepId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().depositId()));

        TableColumn<DepositRecord, String> colCustId = new TableColumn<>("Depositor ID");
        colCustId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().customerId()));

        TableColumn<DepositRecord, String> colType = new TableColumn<>("Product Type");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().productType()));

        TableColumn<DepositRecord, String> colPrincipal = new TableColumn<>("Principal Amount");
        colPrincipal.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().principalAmount())));

        TableColumn<DepositRecord, String> colRate = new TableColumn<>("Rate (p.a.)");
        colRate.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatPercent(d.getValue().interestRatePercent())));

        TableColumn<DepositRecord, String> colStart = new TableColumn<>("Booking Date");
        colStart.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().startDate().toString()));

        TableColumn<DepositRecord, String> colMaturity = new TableColumn<>("Maturity Date");
        colMaturity.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().maturityDate().toString()));

        TableColumn<DepositRecord, String> colMatAmt = new TableColumn<>("Maturity Value");
        colMatAmt.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().maturityAmount())));

        TableColumn<DepositRecord, String> colInstruct = new TableColumn<>("Maturity Mandate");
        colInstruct.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().renewalInstruction()));

        table.getColumns().addAll(colDepId, colCustId, colType, colPrincipal, colRate, colStart, colMaturity, colMatAmt, colInstruct);

        UIComponents.PaginationBar<DepositRecord> paginationBar = new UIComponents.PaginationBar<>(table, 500);

        Runnable refreshTable = () -> {
            List<DepositRecord> list = bankService.getDeposits(bankId, searchField.getText());
            paginationBar.setData(list);
            countLabel.setText("Total: " + String.format("%,d", list.size()) + " deposits");
        };

        searchField.textProperty().addListener((obs, o, n) -> refreshTable.run());
        refreshTable.run();

        this.activeViewRefresher = refreshTable;
        root.getChildren().addAll(filterBar, table, paginationBar);
        return root;
    }

    /**
     * PROMPT 03: Loans, Repayments, and IRAC NPA Classification Screen.
     */
    public VBox buildLoansView(String bankId) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> npaCombo = new ComboBox<>();
        npaCombo.getItems().addAll("All", "NPA Only", "Standard", "SMA-0", "SMA-1", "SMA-2", "Sub-Standard NPA");
        npaCombo.setValue("All");

        TextField searchField = new TextField();
        searchField.setPromptText("Search loan account or customer ID...");
        searchField.setPrefWidth(260);

        Button btnNewLoan = new Button("+ Originate Demo Loan");
        btnNewLoan.getStyleClass().add("btn-primary");
        btnNewLoan.setOnAction(e -> showOriginateLoanModal(bankId));

        Label countLabel = new Label();
        countLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        filterBar.getChildren().addAll(new Label("Filter Classification:"), npaCombo, searchField, btnNewLoan, countLabel);

        TableView<LoanRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<LoanRecord, String> colLnId = new TableColumn<>("Loan Account");
        colLnId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().loanId()));

        TableColumn<LoanRecord, String> colType = new TableColumn<>("Loan Category");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().loanType()));

        TableColumn<LoanRecord, String> colPrincipal = new TableColumn<>("Principal Outstanding");
        colPrincipal.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().principalOutstanding())));

        TableColumn<LoanRecord, String> colRate = new TableColumn<>("Interest Rate");
        colRate.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatPercent(d.getValue().interestRatePercent())));

        TableColumn<LoanRecord, String> colDpd = new TableColumn<>("DPD (Days Past Due)");
        colDpd.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().daysPastDue() + " days"));

        TableColumn<LoanRecord, String> colIrac = new TableColumn<>("IRAC Prudential Status");
        colIrac.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().iracClassification()));

        TableColumn<LoanRecord, String> colCollateral = new TableColumn<>("Collateral Value");
        colCollateral.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().collateralValue())));

        TableColumn<LoanRecord, String> colEmi = new TableColumn<>("Monthly EMI");
        colEmi.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().monthlyEmi())));

        table.getColumns().addAll(colLnId, colType, colPrincipal, colRate, colDpd, colIrac, colCollateral, colEmi);

        UIComponents.PaginationBar<LoanRecord> paginationBar = new UIComponents.PaginationBar<>(table, 500);

        Runnable refreshTable = () -> {
            List<LoanRecord> list = bankService.getLoans(bankId, searchField.getText(), npaCombo.getValue());
            paginationBar.setData(list);
            countLabel.setText("Total: " + String.format("%,d", list.size()) + " loans");
        };

        searchField.textProperty().addListener((obs, o, n) -> refreshTable.run());
        npaCombo.setOnAction(e -> refreshTable.run());
        refreshTable.run();

        this.activeViewRefresher = refreshTable;
        root.getChildren().addAll(filterBar, table, paginationBar);
        return root;
    }

    /**
     * PROMPT 04: Treasury Holdings (G-Secs, T-Bills, Corporate Bonds, Equities, Book vs Market Value).
     */
    public VBox buildTreasuryView(String bankId) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Search instrument name or holding ID...");
        searchField.setPrefWidth(280);

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll(
                "All",
                "Government Bond",
                "Treasury Bill",
                "Corporate Bond",
                "Commercial Paper",
                "Government Securities",
                "Municipal Bond",
                "Mortgage-backed Securities",
                "ETF",
                "Gold commodities"
        );
        categoryCombo.setValue("All");

        Label countLabel = new Label();
        countLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        filterBar.getChildren().addAll(new Label("Filter Category:"), categoryCombo, searchField, countLabel);

        TableView<TreasuryHolding> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<TreasuryHolding, String> colId = new TableColumn<>("Holding ID");
        colId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().holdingId()));

        TableColumn<TreasuryHolding, String> colInst = new TableColumn<>("Security / Instrument");
        colInst.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().instrumentName()));

        TableColumn<TreasuryHolding, String> colCat = new TableColumn<>("Asset Class");
        colCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().category()));

        TableColumn<TreasuryHolding, String> colQty = new TableColumn<>("Holding Quantity");
        colQty.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatQuantity(d.getValue().quantity())));

        TableColumn<TreasuryHolding, String> colBook = new TableColumn<>("Book Value (Amortized)");
        colBook.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().bookValue())));

        TableColumn<TreasuryHolding, String> colMtm = new TableColumn<>("Simulated Market Value");
        colMtm.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().simulatedMarketValue())));

        TableColumn<TreasuryHolding, String> colProceeds = new TableColumn<>("Est. Realizable Proceeds");
        colProceeds.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().estimatedLiquidationProceeds())));

        TableColumn<TreasuryHolding, String> colEncumbrance = new TableColumn<>("Encumbrance");
        colEncumbrance.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().encumbranceStatus()));
        colEncumbrance.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String chipType = item.toLowerCase().contains("unencumbered") ? "verified" : "tier2";
                    Label chip = UIComponents.createStatusChip(item.contains("(") ? item.substring(0, item.indexOf('(')).trim() : item, chipType);
                    setGraphic(chip);
                    setText(null);
                }
            }
        });

        TableColumn<TreasuryHolding, String> colYield = new TableColumn<>("Yield / Coupon");
        colYield.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatPercent(d.getValue().couponOrYieldPercent())));

        table.getColumns().addAll(colId, colInst, colCat, colQty, colBook, colMtm, colProceeds, colEncumbrance, colYield);

        UIComponents.PaginationBar<TreasuryHolding> paginationBar = new UIComponents.PaginationBar<>(table, 500);

        Runnable refreshTable = () -> {
            List<TreasuryHolding> list = bankService.getTreasuryHoldings(bankId, searchField.getText(), categoryCombo.getValue());
            paginationBar.setData(list);
            countLabel.setText("Total: " + String.format("%,d", list.size()) + " holdings");
        };

        searchField.textProperty().addListener((obs, o, n) -> refreshTable.run());
        categoryCombo.setOnAction(e -> refreshTable.run());
        refreshTable.run();

        this.activeViewRefresher = refreshTable;
        root.getChildren().addAll(filterBar, table, paginationBar);
        return root;
    }

    /**
     * PROMPT 04: Banking Transaction Ledger (Double-Entry Debit & Credit Entries).
     */
    public VBox buildLedgerView(String bankId) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        Label ledgerHeader = new Label("Double-Entry Authoritative Financial Ledger");
        ledgerHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        TextField searchField = new TextField();
        searchField.setPromptText("Search reference, account, type, or method...");
        searchField.setPrefWidth(280);

        Label countLabel = new Label();
        countLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        filterBar.getChildren().addAll(ledgerHeader, searchField, countLabel);

        TableView<LedgerEntry> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<LedgerEntry, String> colTxn = new TableColumn<>("Transaction Ref");
        colTxn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().transactionRef()));

        TableColumn<LedgerEntry, String> colTime = new TableColumn<>("Timestamp (IST)");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatDateTime(d.getValue().timestamp())));

        TableColumn<LedgerEntry, String> colAcc = new TableColumn<>("General Ledger Account");
        colAcc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().accountReference()));

        TableColumn<LedgerEntry, String> colDebit = new TableColumn<>("Debit (INR)");
        colDebit.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().debitINR().compareTo(BigDecimal.ZERO) > 0 ? Formatters.formatINR(d.getValue().debitINR()) : "—"));

        TableColumn<LedgerEntry, String> colCredit = new TableColumn<>("Credit (INR)");
        colCredit.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().creditINR().compareTo(BigDecimal.ZERO) > 0 ? Formatters.formatINR(d.getValue().creditINR()) : "—"));

        TableColumn<LedgerEntry, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().type()));

        TableColumn<LedgerEntry, String> colMethod = new TableColumn<>("Method");
        colMethod.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().method()));

        TableColumn<LedgerEntry, String> colStatus = new TableColumn<>("Posting Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status()));

        table.getColumns().addAll(colTxn, colTime, colAcc, colDebit, colCredit, colType, colMethod, colStatus);

        UIComponents.PaginationBar<LedgerEntry> paginationBar = new UIComponents.PaginationBar<>(table, 500);

        Runnable refreshTable = () -> {
            List<LedgerEntry> all = bankService.getLedgerEntries(bankId);
            String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            List<LedgerEntry> filtered = all.stream().filter(e -> {
                if (q.isEmpty()) return true;
                return (e.transactionRef() != null && e.transactionRef().toLowerCase().contains(q))
                        || (e.accountReference() != null && e.accountReference().toLowerCase().contains(q))
                        || (e.type() != null && e.type().toLowerCase().contains(q))
                        || (e.method() != null && e.method().toLowerCase().contains(q));
            }).toList();
            paginationBar.setData(filtered);
            countLabel.setText("Total: " + String.format("%,d", filtered.size()) + " entries");
        };

        searchField.textProperty().addListener((obs, o, n) -> refreshTable.run());
        refreshTable.run();

        this.activeViewRefresher = refreshTable;
        root.getChildren().addAll(filterBar, table, paginationBar);
        return root;
    }

    /**
     * Balance Sheet View: Executive Financial Position & 2-Year Daily Transaction Ledger.
     */
    public VBox buildBalanceSheetView(String bankId) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setFillWidth(true);

        BankProfile profile = bankService.getBankProfile(bankId).orElse(null);

        // 1. Header & Bank Status Bar
        HBox headerBar = new HBox(12);
        headerBar.setAlignment(Pos.CENTER_LEFT);

        Label pageTitle = new Label("Institutional Balance Sheet & 2-Year Historical Ledger");
        pageTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");

        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);

        Label bankBadge = new Label(profile != null ? (profile.bankName() + " (" + profile.tierDescription() + ")") : bankId);
        bankBadge.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 6px; -fx-font-size: 11px; -fx-border-color: #CBD5E1; -fx-border-radius: 6px;");

        Label livePill = new Label("● LIVE RUNNING LEDGER ATTACHED");
        livePill.setStyle("-fx-background-color: #ECFDF5; -fx-text-fill: #059669; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 6px; -fx-font-size: 11px;");

        headerBar.getChildren().addAll(pageTitle, spacerHeader, bankBadge, livePill);

        // 2. Top Executive KPI Cards
        BalanceSheetSummary summary = bankService.getBalanceSheetSummary(bankId);

        HBox kpiRow = new HBox(12);
        VBox cardAssets = UIComponents.createKpiCard("Total Assets", Formatters.formatINRCrores(summary.totalAssets()), "Resources Owned", "Sum of loan advances, treasury holdings, and cash/RBI reserves.", "#0284C7");
        VBox cardLiabilities = UIComponents.createKpiCard("Total Liabilities", Formatters.formatINRCrores(summary.totalLiabilities()), "Obligations & Deposits", "Core customer CASA, term deposits, and interbank borrowings.", "#D97706");
        VBox cardEquity = UIComponents.createKpiCard("Total Equity / Net Worth", Formatters.formatINRCrores(summary.totalEquity()), "Shareholder Funds", "Paid-up share capital and accumulated statutory surplus reserves.", "#059669");
        VBox cardInvariance = UIComponents.createKpiCard("Accounting Invariance", summary.isBalanced() ? "BALANCED ✓" : "IMBALANCE ⚠", "Assets = Liabilities + Equity", "Fundamental accounting identity: Total Assets must equal Liabilities plus Equity.", summary.isBalanced() ? "#10B981" : "#EF4444");

        kpiRow.getChildren().addAll(cardAssets, cardLiabilities, cardEquity, cardInvariance);

        // 3. Dual-Panel Classified Financial Statement Card
        VBox statementCard = new VBox(12);
        statementCard.getStyleClass().add("lro-card");
        statementCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-border-width: 1px; -fx-padding: 16px;");

        HBox stmtHeader = new HBox(10);
        stmtHeader.setAlignment(Pos.CENTER_LEFT);
        Label stmtTitle = new Label("Classified Financial Balance Sheet Position (RBI Form A Format)");
        stmtTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label stmtSubtitle = new Label("Authoritative Double-Entry Position");
        stmtSubtitle.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        stmtHeader.getChildren().addAll(stmtTitle, stmtSubtitle);

        HBox dualPanels = new HBox(16);
        dualPanels.setAlignment(Pos.TOP_LEFT);

        // Left Panel: ASSETS
        VBox assetsPanel = new VBox(8);
        HBox.setHgrow(assetsPanel, Priority.ALWAYS);
        assetsPanel.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 6px; -fx-border-color: #E2E8F0; -fx-border-radius: 6px; -fx-padding: 12px;");

        Label assetsTitle = new Label("ASSETS (Application of Funds)");
        assetsTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #0284C7;");

        VBox assetsItems = new VBox(6);
        assetsItems.getChildren().addAll(
                createBalanceSheetRow("Loans & Advances (Credit Portfolio)", Formatters.formatINRCrores(summary.loansAndAdvances()), calcShare(summary.loansAndAdvances(), summary.totalAssets())),
                createBalanceSheetRow("Treasury & G-Sec Investments", Formatters.formatINRCrores(summary.treasuryInvestments()), calcShare(summary.treasuryInvestments(), summary.totalAssets())),
                createBalanceSheetRow("Cash & Balances with RBI (CRR)", Formatters.formatINRCrores(summary.cashAndRbiReserves()), calcShare(summary.cashAndRbiReserves(), summary.totalAssets())),
                createBalanceSheetRow("Other Assets & Clearing Placements", Formatters.formatINRCrores(summary.otherAssets()), calcShare(summary.otherAssets(), summary.totalAssets()))
        );

        HBox assetsTotalRow = createTotalBalanceSheetRow("TOTAL ASSETS", Formatters.formatINRCrores(summary.totalAssets()), "#0284C7");
        assetsPanel.getChildren().addAll(assetsTitle, new Separator(), assetsItems, new Separator(), assetsTotalRow);

        // Right Panel: LIABILITIES & EQUITY
        VBox liabPanel = new VBox(8);
        HBox.setHgrow(liabPanel, Priority.ALWAYS);
        liabPanel.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 6px; -fx-border-color: #E2E8F0; -fx-border-radius: 6px; -fx-padding: 12px;");

        Label liabTitle = new Label("LIABILITIES & EQUITY (Sources of Funds)");
        liabTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #D97706;");

        VBox liabItems = new VBox(6);
        liabItems.getChildren().addAll(
                createBalanceSheetRow("CASA Deposits (Demand Liabilities)", Formatters.formatINRCrores(summary.casaDeposits()), calcShare(summary.casaDeposits(), summary.totalAssets())),
                createBalanceSheetRow("Term & Fixed Deposits (Time Liabilities)", Formatters.formatINRCrores(summary.termDeposits()), calcShare(summary.termDeposits(), summary.totalAssets())),
                createBalanceSheetRow("Interbank Borrowings & Repo", Formatters.formatINRCrores(summary.interbankBorrowings()), calcShare(summary.interbankBorrowings(), summary.totalAssets())),
                createBalanceSheetRow("Other Liabilities & Provisions", Formatters.formatINRCrores(summary.otherLiabilities()), calcShare(summary.otherLiabilities(), summary.totalAssets())),
                createBalanceSheetRow("Share Capital & Core Equity", Formatters.formatINRCrores(summary.shareCapital()), calcShare(summary.shareCapital(), summary.totalAssets())),
                createBalanceSheetRow("Statutory Reserves & Retained Surplus", Formatters.formatINRCrores(summary.reservesAndSurplus()), calcShare(summary.reservesAndSurplus(), summary.totalAssets()))
        );

        BigDecimal totalLiabAndEquity = summary.totalLiabilities().add(summary.totalEquity());
        HBox liabTotalRow = createTotalBalanceSheetRow("TOTAL LIABILITIES & EQUITY", Formatters.formatINRCrores(totalLiabAndEquity), "#059669");
        liabPanel.getChildren().addAll(liabTitle, new Separator(), liabItems, new Separator(), liabTotalRow);

        dualPanels.getChildren().addAll(assetsPanel, liabPanel);
        statementCard.getChildren().addAll(stmtHeader, dualPanels);

        // 4. Historical 2-Year Daily Transaction & Running Balance Ledger Table
        VBox tableContainer = new VBox(12);
        tableContainer.setFillWidth(true);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        Label tableHeader = new Label("2-Year Daily Transaction & Running Balance Ledger");
        tableHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        TextField searchField = new TextField();
        searchField.setPromptText("Search record ID, account, line item, type, or method...");
        searchField.setPrefWidth(300);

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("All Categories", "Asset", "Liability", "Equity");
        categoryCombo.setValue("All Categories");

        Label countLabel = new Label("Total: " + String.format("%,d", summary.totalHistoricalTransactions()) + " historical transactions");
        countLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px; -fx-font-weight: 600;");

        filterBar.getChildren().addAll(tableHeader, searchField, categoryCombo, countLabel);

        TableView<BalanceSheetRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setMaxHeight(Double.MAX_VALUE);
        table.setPrefHeight(450);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<BalanceSheetRecord, String> colRecId = new TableColumn<>("Record ID");
        colRecId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().recordId()));
        colRecId.setMinWidth(110);

        TableColumn<BalanceSheetRecord, String> colTime = new TableColumn<>("Timestamp (IST)");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatDateTime(d.getValue().timestamp())));
        colTime.setMinWidth(140);

        TableColumn<BalanceSheetRecord, String> colLineItem = new TableColumn<>("Line Item Head");
        colLineItem.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().lineItem()));
        colLineItem.setMinWidth(170);

        TableColumn<BalanceSheetRecord, String> colCategory = new TableColumn<>("Category");
        colCategory.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().category()));
        colCategory.setMinWidth(90);

        TableColumn<BalanceSheetRecord, String> colAccRef = new TableColumn<>("Account Ref");
        colAccRef.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().accountReference()));
        colAccRef.setMinWidth(120);

        TableColumn<BalanceSheetRecord, String> colDebit = new TableColumn<>("Debit (INR)");
        colDebit.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().debitINR().compareTo(BigDecimal.ZERO) > 0 ? Formatters.formatINR(d.getValue().debitINR()) : "—"));
        colDebit.setMinWidth(120);

        TableColumn<BalanceSheetRecord, String> colCredit = new TableColumn<>("Credit (INR)");
        colCredit.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().creditINR().compareTo(BigDecimal.ZERO) > 0 ? Formatters.formatINR(d.getValue().creditINR()) : "—"));
        colCredit.setMinWidth(120);

        TableColumn<BalanceSheetRecord, String> colRunningBal = new TableColumn<>("Running Balance (INR)");
        colRunningBal.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().runningBalanceINR())));
        colRunningBal.setMinWidth(150);

        TableColumn<BalanceSheetRecord, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().type()));
        colType.setMinWidth(120);

        TableColumn<BalanceSheetRecord, String> colMethod = new TableColumn<>("Method");
        colMethod.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().method()));
        colMethod.setMinWidth(90);

        TableColumn<BalanceSheetRecord, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status()));
        colStatus.setMinWidth(80);

        table.getColumns().addAll(colRecId, colTime, colLineItem, colCategory, colAccRef, colDebit, colCredit, colRunningBal, colType, colMethod, colStatus);

        UIComponents.PaginationBar<BalanceSheetRecord> paginationBar = new UIComponents.PaginationBar<>(table, 500);

        Runnable refreshTable = () -> {
            String q = searchField.getText() == null ? "" : searchField.getText().trim();
            String cat = categoryCombo.getValue();
            List<BalanceSheetRecord> records = bankService.getBalanceSheetRecords(bankId, q, cat, 2000);
            paginationBar.setData(records);
            countLabel.setText(String.format("Loaded %,d entries (of %,d total in 2-year history)", records.size(), summary.totalHistoricalTransactions()));
        };

        searchField.textProperty().addListener((obs, o, n) -> refreshTable.run());
        categoryCombo.setOnAction(e -> refreshTable.run());
        refreshTable.run();

        tableContainer.getChildren().addAll(filterBar, table, paginationBar);

        this.activeViewRefresher = () -> {
            BalanceSheetSummary latestSummary = bankService.getBalanceSheetSummary(bankId);
            UIComponents.updateKpiCardValue(cardAssets, Formatters.formatINRCrores(latestSummary.totalAssets()));
            UIComponents.updateKpiCardValue(cardLiabilities, Formatters.formatINRCrores(latestSummary.totalLiabilities()));
            UIComponents.updateKpiCardValue(cardEquity, Formatters.formatINRCrores(latestSummary.totalEquity()));
            UIComponents.updateKpiCardValue(cardInvariance, latestSummary.isBalanced() ? "BALANCED ✓" : "IMBALANCE ⚠");
            refreshTable.run();
        };

        root.getChildren().addAll(headerBar, kpiRow, statementCard, tableContainer);
        return root;
    }

    private HBox createBalanceSheetRow(String title, String amount, String sharePct) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(3, 4, 3, 4));

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;");
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        Label lblShare = new Label(sharePct);
        lblShare.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px; -fx-font-weight: 500;");
        lblShare.setMinWidth(48);
        lblShare.setAlignment(Pos.CENTER_RIGHT);

        Label lblAmt = new Label(amount);
        lblAmt.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 12px; -fx-font-weight: 700;");
        lblAmt.setMinWidth(110);
        lblAmt.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(lblTitle, lblShare, lblAmt);
        return row;
    }

    private HBox createTotalBalanceSheetRow(String title, String amount, String accentColor) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #0F172A; -fx-padding: 8 12; -fx-background-radius: 4px;");

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: 800;");
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        Label lblAmt = new Label(amount);
        lblAmt.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 13px; -fx-font-weight: 800;");

        row.getChildren().addAll(lblTitle, lblAmt);
        return row;
    }

    private String calcShare(BigDecimal part, BigDecimal whole) {
        if (part == null || whole == null || whole.compareTo(BigDecimal.ZERO) <= 0) return "0.0%";
        BigDecimal pct = part.multiply(new BigDecimal("100")).divide(whole, 1, RoundingMode.HALF_UP);
        return pct + "%";
    }

    /**
     * PROMPT 04: Contractual Cash-Flow Maturity Ladder Screen.
     */
    public VBox buildLadderView(String bankId) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        TableView<CashFlowBucket> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<CashFlowBucket, String> colBucket = new TableColumn<>("Maturity Bucket");
        colBucket.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().tenorBucket()));

        TableColumn<CashFlowBucket, String> colInflows = new TableColumn<>("Contractual Inflows (₹)");
        colInflows.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().contractualInflows())));

        TableColumn<CashFlowBucket, String> colOutflows = new TableColumn<>("Contractual Outflows (₹)");
        colOutflows.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().contractualOutflows())));

        TableColumn<CashFlowBucket, String> colGap = new TableColumn<>("Net Funding Gap (₹)");
        colGap.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().netFundingGap())));

        TableColumn<CashFlowBucket, String> colCumGap = new TableColumn<>("Cumulative Funding Gap (₹)");
        colCumGap.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatINR(d.getValue().cumulativeFundingGap())));

        table.getColumns().addAll(colBucket, colInflows, colOutflows, colGap, colCumGap);
        table.getItems().setAll(bankService.getCashFlowLadder(bankId));

        Label explanation = new Label("Interpretation: A negative net funding gap in the 1D or 2-7D bucket indicates projected outflows exceed contractual inflows, requiring liquidity buffer drawdown or repo borrowing.");
        explanation.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        Label ladderHeader = new Label("Contractual Maturity Mismatch & Structural Liquidity Ladder");
        ladderHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        root.getChildren().addAll(ladderHeader, table, explanation);
        VBox.setVgrow(table, Priority.ALWAYS);
        this.activeViewRefresher = () -> table.getItems().setAll(bankService.getCashFlowLadder(bankId));
        return root;
    }

    // Modal dialogs
    public void showOpenAccountModal(String bankId) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Open Synthetic Deposit Account");
        dialog.setHeaderText("Create a new demo account for selected institution");

        VBox box = new VBox(10);
        box.setPadding(new Insets(10));

        TextField custIdField = new TextField(bankId + "-CUST-1001");
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Savings CASA", "Current CASA", "Corporate Current");
        typeCombo.setValue("Savings CASA");
        TextField depositField = new TextField("50000.00");

        box.getChildren().addAll(new Label("Customer ID:"), custIdField, new Label("Account Type:"), typeCombo, new Label("Initial Deposit Amount (₹):"), depositField);
        dialog.getDialogPane().setContent(box);

        ButtonType btnSubmit = new ButtonType("Create Demo Account", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSubmit, ButtonType.CANCEL);

        dialog.setResultConverter(b -> {
            if (b == btnSubmit) {
                try {
                    BigDecimal amount = new BigDecimal(depositField.getText());
                    return bankService.openSyntheticAccount(bankId, custIdField.getText(), typeCombo.getValue(), amount);
                } catch (Exception ex) {
                    return false;
                }
            }
            return false;
        });

        dialog.showAndWait().ifPresent(success -> {
            if (success) {
                onDataChanged.run();
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Demo account created successfully in synthetic repository.");
                a.showAndWait();
            }
        });
    }

    public void showPostTransactionModal(String bankId) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Record Demo Transaction");
        dialog.setHeaderText("Post cash deposit or withdrawal event to ledger");

        VBox box = new VBox(10);
        box.setPadding(new Insets(10));

        TextField accField = new TextField(bankId + "-ACC-5001");
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Deposit", "Withdrawal");
        typeCombo.setValue("Deposit");
        TextField amtField = new TextField("250000.00");

        box.getChildren().addAll(new Label("Target Account ID:"), accField, new Label("Action:"), typeCombo, new Label("Amount (₹):"), amtField);
        dialog.getDialogPane().setContent(box);

        ButtonType btnSubmit = new ButtonType("Post Demo Transaction", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSubmit, ButtonType.CANCEL);

        dialog.setResultConverter(b -> {
            if (b == btnSubmit) {
                try {
                    BigDecimal amount = new BigDecimal(amtField.getText());
                    return bankService.recordDepositTransaction(bankId, accField.getText(), amount, typeCombo.getValue());
                } catch (Exception ex) {
                    return false;
                }
            }
            return false;
        });

        dialog.showAndWait().ifPresent(success -> {
            if (success) {
                onDataChanged.run();
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Transaction posted to ledger fixture.");
                a.showAndWait();
            }
        });
    }

    private void showOriginateLoanModal(String bankId) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Originate Demo Credit Facility");
        dialog.setHeaderText("Create synthetic term loan or working capital line");

        VBox box = new VBox(10);
        box.setPadding(new Insets(10));

        TextField custField = new TextField(bankId + "-CUST-1002");
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Home Loan", "Working Capital Loan", "MSME Term Loan");
        typeCombo.setValue("Home Loan");
        TextField amtField = new TextField("2500000.00");
        TextField rateField = new TextField("8.75");

        box.getChildren().addAll(new Label("Borrower Customer ID:"), custField, new Label("Facility Category:"), typeCombo, new Label("Sanction Amount (₹):"), amtField, new Label("Interest Rate (%):"), rateField);
        dialog.getDialogPane().setContent(box);

        ButtonType btnSubmit = new ButtonType("Originate Facility", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSubmit, ButtonType.CANCEL);

        dialog.setResultConverter(b -> {
            if (b == btnSubmit) {
                try {
                    BigDecimal amt = new BigDecimal(amtField.getText());
                    BigDecimal rate = new BigDecimal(rateField.getText());
                    return bankService.originateDemoLoan(bankId, custField.getText(), typeCombo.getValue(), amt, rate);
                } catch (Exception ex) {
                    return false;
                }
            }
            return false;
        });

        dialog.showAndWait().ifPresent(success -> {
            if (success) {
                onDataChanged.run();
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Demo credit facility originated successfully.");
                a.showAndWait();
            }
        });
    }
}
