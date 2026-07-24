package com.respawnhost.core.model;

public final class PriceOverride {
    private Double monthly;
    private Double hourly;

    public PriceOverride() {
    }

    public PriceOverride(Double monthly, Double hourly) {
        this.monthly = monthly;
        this.hourly = hourly;
    }

    public Double getMonthly() {
        return monthly;
    }

    public Double getHourly() {
        return hourly;
    }
}
