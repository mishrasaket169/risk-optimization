package com.lro.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable, high-precision monetary value wrapper enforcing Banker's rounding (HALF_EVEN)
 * and strict 2-decimal scale for currency arithmetic.
 */
public final class Money implements Comparable<Money>, Serializable {

    private static final long serialVersionUID = 1L;
    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_EVEN;
    public static final int DEFAULT_SCALE = 2;

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null")
                .setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    @JsonCreator
    public static Money of(BigDecimal amount) {
        if (amount == null) return ZERO;
        return new Money(amount);
    }

    public static Money of(String amountStr) {
        if (amountStr == null || amountStr.isBlank()) return ZERO;
        return new Money(new BigDecimal(amountStr.trim()));
    }

    public static Money of(long wholeUnits) {
        return new Money(BigDecimal.valueOf(wholeUnits));
    }

    public static Money of(double val) {
        return new Money(BigDecimal.valueOf(val));
    }

    @JsonValue
    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return "INR";
    }

    public boolean isGreaterThan(Money other) {
        return this.compareTo(other) > 0;
    }

    public boolean isLessThan(Money other) {
        return this.compareTo(other) < 0;
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "Cannot add null Money");
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        Objects.requireNonNull(other, "Cannot subtract null Money");
        return new Money(this.amount.subtract(other.amount));
    }

    public Money multiply(long factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)));
    }

    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "Factor cannot be null");
        return new Money(this.amount.multiply(factor));
    }

    public Money divide(BigDecimal divisor) {
        Objects.requireNonNull(divisor, "Divisor cannot be null");
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Division by zero in Money arithmetic");
        }
        return new Money(this.amount.divide(divisor, DEFAULT_SCALE, DEFAULT_ROUNDING));
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public int compareTo(Money o) {
        return this.amount.compareTo(o.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return this.amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return "₹ " + amount.toPlainString();
    }
}
