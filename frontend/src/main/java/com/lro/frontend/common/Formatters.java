package com.lro.frontend.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Monetary and temporal formatting utilities ensuring high-precision arithmetic,
 * explicit Indian Rupee (INR) notation (₹, Lakhs, Crores), and timezone-explicit timestamps.
 */
public final class Formatters {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(DEFAULT_ZONE);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(DEFAULT_ZONE);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(DEFAULT_ZONE);

    private Formatters() {}

    /**
     * Formats monetary amount in INR with Indian numbering grouping (Lakhs, Crores).
     * Example: ₹ 12,34,567.89
     */
    public static String formatINR(BigDecimal amount) {
        if (amount == null) return "₹ 0.00";
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_EVEN);
        String s = rounded.toPlainString();
        String[] parts = s.split("\\.");
        String intPart = parts[0];
        String decPart = parts.length > 1 ? parts[1] : "00";

        boolean negative = intPart.startsWith("-");
        if (negative) intPart = intPart.substring(1);

        StringBuilder sb = new StringBuilder();
        int len = intPart.length();
        if (len <= 3) {
            sb.append(intPart);
        } else {
            // Last 3 digits
            String last3 = intPart.substring(len - 3);
            String rest = intPart.substring(0, len - 3);
            // Group rest by 2
            StringBuilder restSb = new StringBuilder();
            for (int i = 0; i < rest.length(); i++) {
                if (i > 0 && (rest.length() - i) % 2 == 0) {
                    restSb.append(",");
                }
                restSb.append(rest.charAt(i));
            }
            sb.append(restSb).append(",").append(last3);
        }

        return (negative ? "-₹ " : "₹ ") + sb.toString() + "." + decPart;
    }

    /**
     * Formats large Indian financial quantities into readable Crores with exact notation.
     * Example: ₹ 1,250.40 Cr
     */
    public static String formatINRCrores(BigDecimal amount) {
        if (amount == null) return "₹ 0.00 Cr";
        BigDecimal crores = amount.divide(new BigDecimal("10000000"), 2, RoundingMode.HALF_EVEN);
        return formatINR(crores) + " Cr";
    }

    /**
     * Formats percentage with 2 decimal places.
     * Example: 8.75%
     */
    public static String formatPercent(BigDecimal percent) {
        if (percent == null) return "0.00%";
        return percent.setScale(2, RoundingMode.HALF_EVEN).toPlainString() + "%";
    }

    /**
     * Formats basis points (bps).
     * Example: 45.2 bps
     */
    public static String formatBps(BigDecimal bps) {
        if (bps == null) return "0.0 bps";
        return bps.setScale(1, RoundingMode.HALF_EVEN).toPlainString() + " bps";
    }

    /**
     * Formats integer/whole counts with Indian comma groupings.
     */
    public static String formatQuantity(long count) {
        String s = String.valueOf(Math.abs(count));
        int len = s.length();
        if (len <= 3) return (count < 0 ? "-" : "") + s;

        String last3 = s.substring(len - 3);
        String rest = s.substring(0, len - 3);
        StringBuilder restSb = new StringBuilder();
        for (int i = 0; i < rest.length(); i++) {
            if (i > 0 && (rest.length() - i) % 2 == 0) {
                restSb.append(",");
            }
            restSb.append(rest.charAt(i));
        }
        return (count < 0 ? "-" : "") + restSb.toString() + "," + last3;
    }

    /**
     * Formats OffsetDateTime or Instant to Indian Standard Time (IST).
     */
    public static String formatDateTime(Instant instant) {
        if (instant == null) return "—";
        return DATE_TIME_FORMATTER.format(instant) + " IST";
    }

    public static String formatDate(Instant instant) {
        if (instant == null) return "—";
        return DATE_FORMATTER.format(instant);
    }

    public static String formatTime(Instant instant) {
        if (instant == null) return "—";
        return TIME_FORMATTER.format(instant);
    }
}
