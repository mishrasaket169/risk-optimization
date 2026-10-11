package com.lro.app;

import com.lro.analytics.LiquidityMetricsEngine;
import com.lro.common.model.Money;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Compliance & Reporting Exporter implementing Prompt 25.
 * Generates audit-ready CSV exports for bank liquidity and stress testing.
 */
public class CsvReportExporter {

    public static String exportLiquidityStatementCsv(String bankId, Money cash, Money hqla, Money gap1d, Money gap30d, BigDecimal lcr) {
        StringWriter sw = new StringWriter();
        sw.append("REPORT,Liquidity Position Statement\n");
        sw.append("INSTITUTION,").append(bankId).append("\n");
        sw.append("TIMESTAMP,").append(Instant.now().toString()).append("\n");
        sw.append("STATUS,SYNTHETIC RESEARCH SIMULATOR\n\n");

        sw.append("METRIC,VALUE_INR,DESCRIPTION\n");
        sw.append("Cash Reserve,").append(cash.getAmount().toPlainString()).append(",Immediate Vault & RBI Balance\n");
        sw.append("Eligible HQLA,").append(hqla.getAmount().toPlainString()).append(",Level 1 & 2 Liquid Securities after Haircuts\n");
        sw.append("1-Day Funding Gap,").append(gap1d.getAmount().toPlainString()).append(",Net Cash Outflow due within 24 Hours\n");
        sw.append("30-Day Funding Gap,").append(gap30d.getAmount().toPlainString()).append(",Cumulative 30-Day Stressed Liquidity Gap\n");
        sw.append("Educational LCR,").append(lcr.toPlainString()).append("%,Simulated Liquidity Coverage Ratio (Min 100%)\n");

        return sw.toString();
    }
}
