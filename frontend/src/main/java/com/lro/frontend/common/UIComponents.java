package com.lro.frontend.common;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/**
 * Reusable UI component factory enforcing the shared LRO Design System:
 * Consistent KPI cards, status chips/badges, simulation notices, and plain-language tooltips.
 */
public final class UIComponents {

    private UIComponents() {}

    /**
     * Creates a prominent Synthetic Data Disclaimer banner required by Prompt 1 & 7.
     */
    public static HBox createSimulatorNoticeBanner() {
        HBox banner = new HBox(12);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(6, 16, 6, 16));
        banner.setStyle("-fx-background-color: #FEF3C7; -fx-border-color: #F59E0B; -fx-border-width: 0 0 1 0;");

        Label badge = new Label("SIMULATION ONLY");
        badge.setStyle("-fx-background-color: #D97706; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 10px;");

        Label text = new Label("All institutions, quotes, metrics and recommendations are synthetic for research/education. No real monetary trading or affiliation.");
        text.setStyle("-fx-text-fill: #92400E; -fx-font-size: 11px; -fx-font-weight: 600;");

        banner.getChildren().addAll(badge, text);
        return banner;
    }

    /**
     * Creates a standard KPI Card with title, formatted value, subtitle/trend, and contextual explanation tooltip.
     */
    public static VBox createKpiCard(String title, String value, String subtitle, String glossaryHelp, String accentColor) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-border-width: 1px;");
        card.setMinWidth(180);
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-transform: uppercase;");
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
     * Creates a Status Chip (e.g. Healthy, Distressed, Filled, Cancelled, In-Progress).
     */
    public static Label createStatusChip(String status, String type) {
        Label chip = new Label(status);
        chip.setStyle(getStatusStyle(type));
        return chip;
    }

    private static String getStatusStyle(String type) {
        String base = "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 12px; ";
        return switch (type.toLowerCase()) {
            case "success", "healthy", "filled" -> base + "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-border-color: #86EFAC; -fx-border-radius: 12px;";
            case "warning", "sma", "partial" -> base + "-fx-background-color: #FEF9C3; -fx-text-fill: #A16207; -fx-border-color: #FDE047; -fx-border-radius: 12px;";
            case "danger", "distressed", "npa", "crash", "rejected" -> base + "-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-border-color: #FCA5A5; -fx-border-radius: 12px;";
            case "info", "normal", "accepted" -> base + "-fx-background-color: #DBEAFE; -fx-text-fill: #1D4ED8; -fx-border-color: #93C5FD; -fx-border-radius: 12px;";
            default -> base + "-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-border-color: #CBD5E1; -fx-border-radius: 12px;";
        };
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
