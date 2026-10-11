package com.lro.frontend.common;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;

import java.util.List;

/**
 * Reusable UI component factory enforcing the authentic LRO Ecosystem Design System:
 * - High-contrast clean foundation (#F8FAFC, #FFFFFF)
 * - Financial metric color coding (Cyan #0284C7, Emerald #059669, Indigo #4F46E5, Amber #D97706, Red #DC2626)
 * - Soft Blue selection states (#EFF6FF, border #BFDBFE, text #2563EB)
 * - Basel/RBI compliant notices, tooltips, and glossary dialogs.
 */
public final class UIComponents {

    private UIComponents() {}

    public record SourceItem(String label, String value, String colorHex) {}

    /**
     * Prominent Synthetic Data Disclaimer banner.
     */
    public static HBox createSimulatorNoticeBanner() {
        HBox banner = new HBox(12);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(6, 16, 6, 16));
        banner.setStyle("-fx-background-color: #FEF3C7; -fx-border-color: #F59E0B; -fx-border-width: 0 0 1 0;");

        Label badge = new Label("SIMULATION ONLY");
        badge.setStyle("-fx-background-color: #D97706; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-font-size: 10px;");

        Label text = new Label("All institutions, quotes, metrics and recommendations are synthetic for research/education. No real monetary trading or affiliation.");
        text.setStyle("-fx-text-fill: #92400E; -fx-font-size: 11px; -fx-font-weight: 600;");

        banner.getChildren().addAll(badge, text);
        return banner;
    }

    /**
     * Official LRO KPI Card with metric-specific accent color, title, subtitle, and explanation tooltip.
     */
    public static VBox createKpiCard(String title, String value, String subtitle, String glossaryHelp, String accentColor) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-border-width: 1px; -fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.03), 6, 0, 0, 2);");
        card.setMinWidth(180);
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: 700;");
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        if (glossaryHelp != null && !glossaryHelp.isEmpty()) {
            Label helpIcon = new Label("ⓘ");
            helpIcon.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-cursor: hand;");
            Tooltip tooltip = new Tooltip(glossaryHelp);
            tooltip.setStyle("-fx-font-size: 11px; -fx-wrap-text: true; -fx-pref-width: 260px;");
            Tooltip.install(helpIcon, tooltip);
            header.getChildren().addAll(lblTitle, helpIcon);
        } else {
            header.getChildren().add(lblTitle);
        }

        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-text-fill: " + (accentColor != null ? accentColor : "#0F172A") + "; -fx-font-size: 21px; -fx-font-weight: 800;");

        Label lblSubtitle = new Label(subtitle);
        lblSubtitle.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        card.getChildren().addAll(header, lblValue, lblSubtitle);
        return card;
    }

    /**
     * KPI card with delta percentage badge aligned with LRO color palette.
     */
    public static VBox createLroKpiCard(String title, String value, String deltaText, boolean isPositive, String subtitle, String glossaryHelp) {
        String accentColor = isPositive ? "#059669" : "#DC2626";
        if (title.contains("Cash") || title.contains("Assets") || title.contains("Turnover")) {
            accentColor = "#0284C7";
        } else if (title.contains("Deposits") || title.contains("HQLA") || title.contains("LCR") || title.contains("Index")) {
            accentColor = "#059669";
        } else if (title.contains("Loan") || title.contains("Movement")) {
            accentColor = "#4F46E5";
        }

        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-border-width: 1px; -fx-effect: dropshadow(three-pass-box, rgba(15,23,42,0.03), 6, 0, 0, 2);");
        card.setMinWidth(200);
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: 700;");
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        if (glossaryHelp != null && !glossaryHelp.isEmpty()) {
            Label helpIcon = new Label("ⓘ");
            helpIcon.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-cursor: hand;");
            Tooltip tooltip = new Tooltip(glossaryHelp);
            tooltip.setStyle("-fx-font-size: 11px; -fx-wrap-text: true; -fx-pref-width: 260px;");
            Tooltip.install(helpIcon, tooltip);
            header.getChildren().addAll(lblTitle, helpIcon);
        } else {
            header.getChildren().add(lblTitle);
        }

        HBox valRow = new HBox(10);
        valRow.setAlignment(Pos.CENTER_LEFT);

        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-size: 21px; -fx-font-weight: 800;");

        Label deltaBadge = new Label(deltaText);
        deltaBadge.getStyleClass().add(isPositive ? "delta-green" : "delta-red");

        valRow.getChildren().addAll(lblValue, deltaBadge);

        Label lblSubtitle = new Label(subtitle);
        lblSubtitle.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        card.getChildren().addAll(header, valRow, lblSubtitle);
        return card;
    }



    /**
     * Status Chip / Pill badge matching the authentic LRO style.
     */
    public static Label createStatusChip(String status, String type) {
        Label chip = new Label(status);
        String base = "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 12px; ";
        String style = switch (type.toLowerCase()) {
            case "success", "healthy", "filled", "active", "standard", "normal" -> 
                base + "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-border-color: #86EFAC; -fx-border-radius: 12px;";
            case "warning", "sma", "partial", "watchlist", "regimes" -> 
                base + "-fx-background-color: #FEF9C3; -fx-text-fill: #A16207; -fx-border-color: #FDE047; -fx-border-radius: 12px;";
            case "danger", "distressed", "npa", "crash", "rejected", "canceled" -> 
                base + "-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-border-color: #FCA5A5; -fx-border-radius: 12px;";
            case "info", "tier 1", "accepted" -> 
                base + "-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8; -fx-border-color: #93C5FD; -fx-border-radius: 12px;";
            default -> 
                base + "-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-border-color: #CBD5E1; -fx-border-radius: 12px;";
        };
        chip.setStyle(style);
        return chip;
    }

    public static Label createStatusPill(String status, String type) {
        return createStatusChip(status, type);
    }

    /**
     * Flow Dynamics / Trend Chart Card.
     */
    public static VBox createChartCard(String title, String metricValue, String metricDelta, String metricSub, String activeFilter, Node chartNode) {
        VBox card = new VBox(12);
        card.getStyleClass().add("lro-card");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("card-title");
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        HBox filterBox = new HBox(2);
        filterBox.getStyleClass().add("filter-pill-container");
        filterBox.setAlignment(Pos.CENTER);

        String[] filters = {"Day", "Week", "Year"};
        for (String f : filters) {
            Button btn = new Button(f);
            btn.getStyleClass().add(f.equalsIgnoreCase(activeFilter) ? "filter-pill-btn-active" : "filter-pill-btn");
            filterBox.getChildren().add(btn);
        }

        header.getChildren().addAll(lblTitle, filterBox);

        VBox callout = new VBox(2);
        HBox valRow = new HBox(8);
        valRow.setAlignment(Pos.CENTER_LEFT);

        Label lblSub = new Label(metricSub);
        lblSub.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        Label lblVal = new Label(metricValue);
        lblVal.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 18px; -fx-font-weight: 800;");

        Label lblDelta = new Label(metricDelta);
        lblDelta.getStyleClass().add("delta-green");

        valRow.getChildren().addAll(lblVal, lblDelta);
        callout.getChildren().addAll(lblSub, valRow);

        card.getChildren().addAll(header, callout, chartNode);
        return card;
    }

    /**
     * Dual-Bar Flow Chart with LRO color scheme (Slate/Navy #0F172A & Sand #E2E8F0).
     */
    public static VBox createDualBarChart(String[] days, double[] navyValues, double[] beigeValues, String tooltipDay, String tooltipVal, String primaryColorHex) {
        VBox container = new VBox(8);
        container.setPadding(new Insets(8, 0, 8, 0));

        HBox chartRow = new HBox(18);
        chartRow.setAlignment(Pos.BOTTOM_CENTER);
        chartRow.setPrefHeight(140);

        double maxVal = 1.0;
        for (double v : navyValues) maxVal = Math.max(maxVal, v);
        for (double v : beigeValues) maxVal = Math.max(maxVal, v);

        for (int i = 0; i < days.length; i++) {
            String day = days[i];
            double navyH = Math.max(12, (navyValues[i] / maxVal) * 100);
            double beigeH = Math.max(12, (beigeValues[i] / maxVal) * 100);

            VBox col = new VBox(5);
            col.setAlignment(Pos.BOTTOM_CENTER);
            HBox.setHgrow(col, Priority.ALWAYS);

            if (day.equalsIgnoreCase(tooltipDay)) {
                VBox tooltipPill = new VBox(2);
                tooltipPill.getStyleClass().add("dark-tooltip-pill");
                tooltipPill.setAlignment(Pos.CENTER);
                Label t1 = new Label("Today :");
                t1.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 9px; -fx-font-weight: bold;");
                Label t2 = new Label(tooltipVal);
                t2.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: 800;");
                tooltipPill.getChildren().addAll(t1, t2);
                col.getChildren().add(tooltipPill);
            }

            HBox bars = new HBox(4);
            bars.setAlignment(Pos.BOTTOM_CENTER);

            Rectangle beigeBar = new Rectangle(9, beigeH);
            beigeBar.setArcWidth(4);
            beigeBar.setArcHeight(4);
            beigeBar.setFill(Color.web("#E2E8F0"));

            Rectangle navyBar = new Rectangle(9, navyH);
            navyBar.setArcWidth(4);
            navyBar.setArcHeight(4);
            navyBar.setFill(Color.web(primaryColorHex != null ? primaryColorHex : "#0F172A"));

            bars.getChildren().addAll(beigeBar, navyBar);

            Label dayLbl = new Label(day);
            dayLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: 600;");

            col.getChildren().addAll(bars, dayLbl);
            chartRow.getChildren().add(col);
        }

        container.getChildren().add(chartRow);
        return container;
    }

    public static VBox createDualBarChart(String[] days, double[] navyValues, double[] beigeValues, String tooltipDay, String tooltipVal) {
        return createDualBarChart(days, navyValues, beigeValues, tooltipDay, tooltipVal, "#0F172A");
    }

    /**
     * Regional / Geographic Exposure Widget.
     */
    public static VBox createRegionalExposureWidget(String title, String period, String highlightCountry, String highlightLoyalty, String highlightCount, String highlightColor) {
        VBox card = new VBox(10);
        card.getStyleClass().add("lro-card");
        card.setMinWidth(280);
        card.setPrefWidth(310);

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("card-title");
        Label lblPeriod = new Label(period);
        lblPeriod.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        titleBox.getChildren().addAll(lblTitle, lblPeriod);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label calIcon = new Label("📅");
        calIcon.setStyle("-fx-font-size: 13px; -fx-padding: 4; -fx-border-color: #E2E8F0; -fx-border-radius: 4px;");
        top.getChildren().addAll(titleBox, calIcon);

        HBox tools = new HBox(6);
        tools.setAlignment(Pos.CENTER_LEFT);
        String[] toolLabels = {"🔍", "➕", "➖"};
        for (String tl : toolLabels) {
            Button b = new Button(tl);
            b.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-radius: 4px; -fx-font-size: 10px; -fx-padding: 3 6; -fx-cursor: hand;");
            tools.getChildren().add(b);
        }

        StackPane mapArea = new StackPane();
        mapArea.setPrefHeight(150);
        mapArea.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8px;");

        Pane shapesPane = new Pane();
        Rectangle land1 = new Rectangle(18, 18, 95, 45);
        land1.setArcWidth(12); land1.setArcHeight(12);
        land1.setFill(Color.web("#E2E8F0"));

        Rectangle land2 = new Rectangle(125, 14, 135, 55);
        land2.setArcWidth(12); land2.setArcHeight(12);
        land2.setFill(Color.web(highlightColor != null ? highlightColor : "#2563EB"));

        Rectangle land3 = new Rectangle(45, 75, 75, 50);
        land3.setArcWidth(12); land3.setArcHeight(12);
        land3.setFill(Color.web("#E2E8F0"));

        Rectangle land4 = new Rectangle(150, 80, 85, 40);
        land4.setArcWidth(12); land4.setArcHeight(12);
        land4.setFill(Color.web("#E2E8F0"));

        shapesPane.getChildren().addAll(land1, land2, land3, land4);

        VBox darkCard = new VBox(3);
        darkCard.getStyleClass().add("dark-tooltip-pill");
        darkCard.setMaxWidth(145);
        darkCard.setAlignment(Pos.CENTER_LEFT);

        Label countryLbl = new Label(highlightCountry);
        countryLbl.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: 800; -fx-font-size: 11px;");

        HBox loyaltyRow = new HBox(6);
        loyaltyRow.setAlignment(Pos.CENTER_LEFT);
        Label loyTitle = new Label("Health / Quality");
        loyTitle.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 9px;");
        Label loyBadge = new Label(highlightLoyalty);
        loyBadge.setStyle("-fx-text-fill: #38BDF8; -fx-font-weight: bold; -fx-font-size: 10px;");
        loyaltyRow.getChildren().addAll(loyTitle, loyBadge);

        Label countLbl = new Label(highlightCount);
        countLbl.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 10px; -fx-font-weight: bold;");

        darkCard.getChildren().addAll(countryLbl, loyaltyRow, countLbl);
        StackPane.setAlignment(darkCard, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(darkCard, new Insets(8));

        mapArea.getChildren().addAll(shapesPane, darkCard);
        card.getChildren().addAll(top, tools, mapArea);
        return card;
    }

    /**
     * Composition & Distribution Donut Widget.
     */
    public static VBox createDonutSourceWidget(String title, String subtitle, String centerValueLabel, String centerValue, List<SourceItem> sources, String buttonText, Runnable onAction, String accentColor) {
        VBox card = new VBox(10);
        card.getStyleClass().add("lro-card-soft");
        card.setMinWidth(280);
        card.setPrefWidth(310);

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("card-title");
        Label lblSub = new Label(subtitle);
        lblSub.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        titleBox.getChildren().addAll(lblTitle, lblSub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label gear = new Label("⚙️");
        gear.setStyle("-fx-font-size: 12px; -fx-cursor: hand;");
        top.getChildren().addAll(titleBox, gear);

        HBox body = new HBox(14);
        body.setAlignment(Pos.CENTER_LEFT);

        VBox statsCol = new VBox(6);
        statsCol.setAlignment(Pos.CENTER_LEFT);
        for (SourceItem s : sources) {
            VBox item = new VBox(1);
            Label name = new Label(s.label());
            name.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: 600;");
            Label val = new Label(s.value());
            val.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 14px; -fx-font-weight: 800;");
            item.getChildren().addAll(name, val);
            statsCol.getChildren().add(item);
        }
        HBox.setHgrow(statsCol, Priority.ALWAYS);

        StackPane donut = new StackPane();
        donut.setPrefSize(95, 95);

        Arc bgArc = new Arc(47, 47, 40, 40, 0, 360);
        bgArc.setType(ArcType.OPEN);
        bgArc.setFill(null);
        bgArc.setStroke(Color.web("#E2E8F0"));
        bgArc.setStrokeWidth(11);

        Arc activeArc = new Arc(47, 47, 40, 40, 90, 240);
        activeArc.setType(ArcType.OPEN);
        activeArc.setFill(null);
        activeArc.setStroke(Color.web(accentColor != null ? accentColor : "#0F172A"));
        activeArc.setStrokeWidth(11);

        VBox centerText = new VBox(1);
        centerText.setAlignment(Pos.CENTER);
        Label cLabel = new Label(centerValueLabel);
        cLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 9px; -fx-font-weight: bold;");
        Label cVal = new Label(centerValue);
        cVal.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 11px; -fx-font-weight: 800;");
        centerText.getChildren().addAll(cLabel, cVal);

        donut.getChildren().addAll(bgArc, activeArc, centerText);
        body.getChildren().addAll(statsCol, donut);

        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label moreLink = new Label("See more statistics");
        moreLink.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-weight: 600; -fx-cursor: hand;");
        HBox.setHgrow(moreLink, Priority.ALWAYS);

        Button btnDetails = new Button(buttonText != null ? buttonText : "See Details ↗");
        btnDetails.getStyleClass().add("btn-primary");
        if (accentColor != null) {
            btnDetails.setStyle("-fx-background-color: " + accentColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 7 14; -fx-cursor: hand;");
        }
        if (onAction != null) {
            btnDetails.setOnAction(e -> onAction.run());
        }

        footer.getChildren().addAll(moreLink, btnDetails);
        card.getChildren().addAll(top, body, footer);
        return card;
    }

    /**
     * Plain-language "What does this mean?" Dialog modal.
     */
    public static void showGlossaryDialog(String term, String definition, String formula, String regulatoryContext) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Financial Glossary & Educational Guide");
        alert.setHeaderText(term);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label defLabel = new Label("Plain-Language Meaning:\n" + definition);
        defLabel.setWrapText(true);
        defLabel.setStyle("-fx-font-size: 13px;");

        content.getChildren().add(defLabel);

        if (formula != null && !formula.isEmpty()) {
            Label formulaLabel = new Label("Calculation / Logic:\n" + formula);
            formulaLabel.setStyle("-fx-font-family: monospace; -fx-background-color: #F1F5F9; -fx-padding: 8; -fx-background-radius: 4;");
            content.getChildren().add(formulaLabel);
        }

        if (regulatoryContext != null && !regulatoryContext.isEmpty()) {
            Label regLabel = new Label("Regulatory Context (RBI / Basel):\n" + regulatoryContext);
            regLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
            regLabel.setWrapText(true);
            content.getChildren().add(regLabel);
        }

        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }
}
