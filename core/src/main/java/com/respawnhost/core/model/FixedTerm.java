package com.respawnhost.core.model;

public final class FixedTerm {
    private int termDays;
    private double price;
    private double effectiveMonthly;
    private int discountPercent;

    public FixedTerm() {
    }

    public FixedTerm(int termDays, double price, double effectiveMonthly, int discountPercent) {
        this.termDays = termDays;
        this.price = price;
        this.effectiveMonthly = effectiveMonthly;
        this.discountPercent = discountPercent;
    }

    public int getTermDays() {
        return termDays;
    }

    public double getPrice() {
        return price;
    }

    public double getEffectiveMonthly() {
        return effectiveMonthly;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }
}
