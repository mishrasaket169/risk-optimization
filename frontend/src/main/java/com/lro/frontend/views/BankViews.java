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
import java.util.List;

/**
 * Bank Simulator View Builders implementing Prompts 02, 03, and 04:
 * Dashboard, Bank Selector, Customer directory, Accounts, Deposits, Loans & NPAs,
 * Treasury holdings, Double-entry Ledger, and Cash-Flow maturity ladder.
 */
public class BankViews {

    private final BankService bankService;
    private final Runnable onDataChanged;

    public BankViews(BankService bankService, Runnable onDataChanged) {
        this.bankService = bankService;
        this.onDataChanged = onDataChanged;
    }

    /**
     * PROMPT 02: Bank Overview Dashboard with 4-bank selector, asset/liability KPIs,
     * NPA ratios, maturity summaries, and simulation disclaimers.
     */
    public VBox buildDashboard(String selectedBankId, java.util.function.Consumer<String> onSelectBank) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));

        BankProfile profile = bankService.getBankProfile(selectedBankId).orElseThrow();

        // 1. Bank Selector Bar
        HBox selectorBar = new HBox(12);
        selectorBar.setAlignment(Pos.CENTER_LEFT);
        selectorBar.setPadding(new Insets(10, 16, 10, 16));
        selectorBar.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px;");

        Label selLabel = new Label("Selected Financial Institution:");
        selLabel.setStyle("-fx-text-fill: #1E293B; -fx-font-weight: bold;");

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

        Label statusChip = UIComponents.createStatusChip(profile.healthStatus(), profile.healthStatus());
        Label tierLabel = new Label("(" + profile.tierDescription() + ")");
        tierLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        selectorBar.getChildren().addAll(selLabel, bankCombo, statusChip, tierLabel);

        // 2. High-Level KPI Cards
        HBox kpiRow1 = new HBox(12);
        kpiRow1.getChildren().addAll(
                UIComponents.createKpiCard("Total Assets", Formatters.formatINRCrores(profile.totalAssets()), "Verified Book Size", "Total resources owned by the bank including loans and investments.", "#0284C7"),
                UIComponents.createKpiCard("Total Deposits", Formatters.formatINRCrores(profile.totalDeposits()), "Customer CASA & FDs", "Core funding source from retail and corporate depositors.", "#059669"),
                UIComponents.createKpiCard("Gross Loan Portfolio", Formatters.formatINRCrores(profile.totalLoans()), "Sanctioned Credit Assets", "Total outstanding advances to retail, SME and corporate borrowers.", "#6366F1"),
                UIComponents.createKpiCard("Gross NPA Ratio", Formatters.formatPercent(profile.grossNpaRatio()), profile.healthStatus().equals("Distressed") ? "PCA Watchlist" : "Optimal Quality", "Share of loans overdue >90 days under RBI IRAC prudential norms.", profile.grossNpaRatio().doubleValue() > 5.0 ? "#DC2626" : "#059669")
        );

        HBox kpiRow2 = new HBox(12);
        kpiRow2.getChildren().addAll(
                UIComponents.createKpiCard("Liquid Cash & RBI Balance", Formatters.formatINRCrores(profile.cashBalance()), "CRR & Clearing Vault", "Physical currency and deposits maintained with RBI.", "#0284C7"),
                UIComponents.createKpiCard("Treasury Securities", Formatters.formatINRCrores(profile.securitiesHoldings()), "G-Secs, T-Bills, Bonds", "Sovereign and corporate fixed income securities for SLR and trading.", "#D97706"),
                UIComponents.createKpiCard("Net Interest Margin (NIM)", Formatters.formatPercent(profile.netInterestMargin()), "Annualized Yield Spread", "Difference between interest income on loans and interest paid on deposits.", "#DB2777"),
                UIComponents.createKpiCard("Synthetic Customers", Formatters.formatQuantity(profile.customerCount()), "Deterministic Seed Base", "Number of fictional customer entities generated for this bank profile.", "#475569")
        );

        // 3. Operational Quick Action & Summary Panel
        HBox summaryPanel = new HBox(16);
        summaryPanel.setPadding(new Insets(16));
        summaryPanel.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px;");

        VBox leftSummary = new VBox(8);
        HBox.setHgrow(leftSummary, Priority.ALWAYS);
        Label sumTitle = new Label("Institutional Solvency & Liquidity Profile");
        sumTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        String profileDesc = profile.healthStatus().equals("Distressed")
                ? "WARNING: This simulated institution exhibits elevated credit impairment (14.80% Gross NPA), compressed Net Interest Margin, and heightened 1-day cash-flow deficit. Simulates Prompt Corrective Action (PCA) stress conditions."
                : "STABLE: Healthy capital adequacy, robust deposit stickiness, low delinquency (<2.5%), and strong sovereign G-Sec liquid asset buffer exceeding mandatory SLR requirements.";

        Label sumDesc = new Label(profileDesc);
        sumDesc.setWrapText(true);
        sumDesc.setStyle("-fx-text-fill: " + (profile.healthStatus().equals("Distressed") ? "#DC2626" : "#475569") + "; -fx-font-size: 12px;");

        leftSummary.getChildren().addAll(sumTitle, sumDesc);

        Button btnNewAcc = new Button("+ Open Demo Account");
        btnNewAcc.getStyleClass().add("btn-primary");
        btnNewAcc.setOnAction(e -> showOpenAccountModal(selectedBankId));

        Button btnPostTxn = new Button("⚡ Record Demo Cash Flow");
        btnPostTxn.getStyleClass().add("btn-secondary");
        btnPostTxn.setOnAction(e -> showPostTransactionModal(selectedBankId));

        HBox actions = new HBox(8, btnNewAcc, btnPostTxn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        summaryPanel.getChildren().addAll(leftSummary, actions);

        root.getChildren().addAll(selectorBar, kpiRow1, kpiRow2, summaryPanel);
        return root;
    }

    /**
     * PROMPT 03: Customers Directory Screen with search, segment filter, and KYC badges.
     */
    public VBox buildCustomersView(String bankId) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Search customer name or ID...");
        searchField.setPrefWidth(260);

        ComboBox<String> segmentCombo = new ComboBox<>();
        segmentCombo.getItems().addAll("All", "Retail", "SME", "Corporate");
        segmentCombo.setValue("All");

        Label countLabel = new Label();
        countLabel.setStyle("-fx-text-fill: #64748B;");

        filterBar.getChildren().addAll(new Label("Filter Segment:"), segmentCombo, searchField, countLabel);

        TableView<CustomerRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

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

        Runnable refreshTable = () -> {
            List<CustomerRecord> list = bankService.getCustomers(bankId, searchField.getText(), segmentCombo.getValue());
            table.getItems().setAll(list);
            countLabel.setText("Showing " + list.size() + " synthetic records");
        };

        searchField.textProperty().addListener((obs, o, n) -> refreshTable.run());
        segmentCombo.setOnAction(e -> refreshTable.run());
        refreshTable.run();

        root.getChildren().addAll(filterBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    /**
     * PROMPT 03: Operational Accounts & CASA Balances Screen.
     */
    public VBox buildAccountsView(String bankId) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        TextField searchField = new TextField();
        searchField.setPromptText("Search by account ID or customer ID...");
        searchField.setPrefWidth(280);

        TableView<AccountRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

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

        Runnable refreshTable = () -> {
            List<AccountRecord> list = bankService.getAccounts(bankId, searchField.getText());
            table.getItems().setAll(list);
        };
        searchField.textProperty().addListener((obs, o, n) -> refreshTable.run());
        refreshTable.run();

        root.getChildren().addAll(new HBox(12, new Label("Search:"), searchField), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    /**
     * PROMPT 03: Fixed & Recurring Deposits Screen.
     */
    public VBox buildDepositsView(String bankId) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        TableView<DepositRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

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

        Label sectionHeader = new Label("Active Fixed Term & Recurring Deposits Portfolio");
        sectionHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        root.getChildren().addAll(sectionHeader, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    /**
     * PROMPT 03: Loans, Repayments, and IRAC NPA Classification Screen.
     */
    public VBox buildLoansView(String bankId) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> npaCombo = new ComboBox<>();
        npaCombo.getItems().addAll("All", "NPA Only", "Standard", "SMA-0", "SMA-1", "SMA-2", "Sub-Standard NPA");
        npaCombo.setValue("All");

        Button btnNewLoan = new Button("+ Originate Demo Loan");
        btnNewLoan.getStyleClass().add("btn-primary");
        btnNewLoan.setOnAction(e -> showOriginateLoanModal(bankId));

        filterBar.getChildren().addAll(new Label("Filter Asset Classification:"), npaCombo, btnNewLoan);

        TableView<LoanRecord> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

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

        Runnable refreshTable = () -> table.getItems().setAll(bankService.getLoans(bankId, npaCombo.getValue()));
        npaCombo.setOnAction(e -> refreshTable.run());
        refreshTable.run();

        root.getChildren().addAll(filterBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    /**
     * PROMPT 04: Treasury Holdings (G-Secs, T-Bills, Corporate Bonds, Equities, Book vs Market Value).
     */
    public VBox buildTreasuryView(String bankId) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        TableView<TreasuryHolding> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

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

        TableColumn<TreasuryHolding, String> colYield = new TableColumn<>("Yield / Coupon");
        colYield.setCellValueFactory(d -> new SimpleStringProperty(Formatters.formatPercent(d.getValue().couponOrYieldPercent())));

        table.getColumns().addAll(colInst, colCat, colQty, colBook, colMtm, colProceeds, colEncumbrance, colYield);
        table.getItems().setAll(bankService.getTreasuryHoldings(bankId));

        Label notice = new Label("Notice: Realizable proceeds incorporate estimated haircuts and order-book liquidation slippage.");
        notice.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        Label sectionHeader = new Label("Bank Treasury Investment Portfolio & Encumbrance Ledger");
        sectionHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        root.getChildren().addAll(sectionHeader, table, notice);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    /**
     * PROMPT 04: Banking Transaction Ledger (Double-Entry Debit & Credit Entries).
     */
    public VBox buildLedgerView(String bankId) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));

        TableView<LedgerEntry> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

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

        TableColumn<LedgerEntry, String> colNarration = new TableColumn<>("Transaction Narration");
        colNarration.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().narration()));

        TableColumn<LedgerEntry, String> colStatus = new TableColumn<>("Posting Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status()));

        table.getColumns().addAll(colTxn, colTime, colAcc, colDebit, colCredit, colNarration, colStatus);
        table.getItems().setAll(bankService.getLedgerEntries(bankId));

        Label ledgerHeader = new Label("Double-Entry Authoritative Financial Ledger");
        ledgerHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        root.getChildren().addAll(ledgerHeader, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
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
        return root;
    }

    // Modal dialogs
    private void showOpenAccountModal(String bankId) {
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

    private void showPostTransactionModal(String bankId) {
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
